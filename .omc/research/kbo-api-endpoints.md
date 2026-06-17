# KBO 비공식 API 리버스 엔지니어링 결과

조사일: 2026-06-17
방법: 브라우저 DevTools 대신 `curl`로 koreabaseball.com 페이지 소스를 받아 jQuery `$.ajax` 호출부 정적 분석 후, 실제 POST 요청으로 검증.

## 결론 (Go/No-Go)

**GO.** `koreabaseball.com`이 ASP.NET ASMX 웹서비스(`/ws/*.asmx/*`)를 통해 일정/스코어/순위 데이터를 JSON으로 제공한다. 인증 불필요, 별도 API 키 불필요. 단, 짧은 시간에 반복 요청하면 차단(아래 "주의사항" 참고)되므로 polling 간격 설계가 필요하다.

## 핵심 엔드포인트

### 1. `/ws/Main.asmx/GetKboGameList` — 일별 경기 목록 + 실시간 상태 (★ 가장 중요)

- Method: `POST`
- Headers: `Content-Type: application/json; charset=UTF-8`
- Body:
  ```json
  { "leId": "1", "srId": "0,1", "date": "20250701" }
  ```
  - `leId`: 리그 ID, `1` = KBO 1군
  - `srId`: 시리즈 ID 목록 (comma-separated). `0` = 정규시즌, `1` = 시범경기 등
  - `date`: `yyyyMMdd`
- 응답 예시 (필드 일부):
  ```json
  {
    "game": [
      {
        "G_ID": "20250701SSOB0",
        "G_DT": "20250701", "G_TM": "18:30", "S_NM": "잠실",
        "AWAY_ID": "SS", "HOME_ID": "OB", "AWAY_NM": "삼성", "HOME_NM": "두산",
        "GAME_STATE_SC": "3",
        "GAME_INN_NO": 9, "GAME_TB_SC": "B",
        "T_SCORE_CN": "4", "B_SCORE_CN": "1",
        "STRIKE_CN": 1, "BALL_CN": 2, "OUT_CN": 3,
        "W_PIT_P_NM": "후라도", "SV_PIT_P_NM": "", "L_PIT_P_NM": "최민석",
        "VOD_CK": 1, "LINEUP_CK": 30
      }
    ]
  }
  ```
- **이 응답 하나로 일정 + 스코어 + 이닝/볼카운트/아웃카운트 + 선발/승패투수까지 다 나온다.** Phase 2의 `KboApiCollector` + 30초 polling 전략을 그대로 이 엔드포인트 기반으로 구현 가능.
- `GAME_STATE_SC` 추정: `0`=예정, `1`=진행중(추정), `3`=종료 (정확한 매핑은 당일 경기로 추가 확인 필요)

### 2. `/ws/Schedule.asmx/GetMonthSchedule` — 월간 일정 (캘린더 뷰용)

- Method: `POST`, body: `{ "leId": 1, "srIdList": "0,9,6", "seasonId": "2025", "gameMonth": "07" }`
- `GetScheduleList`도 동일 계열이나 이번 조사에서는 정확한 파라미터 조합을 못 찾음 (404성 에러 응답). 캘린더 뷰가 필요해지면 추가 조사.

### 3. `/ws/Schedule.asmx/GetScoreBoardScroll`, `/ws/Schedule.asmx/GetBoxScoreScroll` — 경기 상세 박스스코어

- Method: `POST`, body: `{ "leId": ..., "srId": ..., "seasonId": ..., "gameId": "..." }`
- 페이지 JS상으로는 `leId`/`srId`/`seasonId`/`gameId`를 `.game-list-n > li.on` 엘리먼트의 `data-*` 속성에서 읽어옴 (GameCenter Main.aspx가 부모 페이지로 떠 있어야 하는 구조). 이번 조사에서는 정확한 srId 조합을 못 찾아 매번 에러 응답만 받음.
- MVP(일정/스코어/순위)에는 필수 아님. 라인업·타순별 상세 박스스코어가 필요해지는 시점에 GameCenter Main.aspx를 실제로 띄워서(브라우저) Network 탭으로 정확한 파라미터를 한 번 더 확인 권장.

### 4. 팀 순위 — API 아님, 서버사이드 렌더링 HTML 테이블

- URL: `GET https://www.koreabaseball.com/Record/TeamRank/TeamRankDaily.aspx`
- JSON API가 아니라 `<table class="tData">` 안에 순위/팀명/승/패/무/승률/게임차/최근10경기/연속/홈/방문이 그대로 박혀서 내려온다.
- 백엔드에서는 Jsoup 같은 HTML 파서로 테이블을 긁어야 함. (`leId`/시즌 등 쿼리 파라미터로 필터링 가능할 것으로 보이나 미확인)

## 기타 발견된 ASMX 엔드포인트 (미검증, 향후 필요시 활용)

- `/ws/About.asmx/GetBTogether`
- `/ws/Controls.asmx/GetMonthList`, `GetYearList`, `GetSearchPlayer`
- `/ws/Main.asmx/GetKboGameDate` — `{ "leId": "1", "srId": ..., "date": "..." }`, 이전/다음 경기일 네비게이션용으로 추정

## 주의사항 / 운영 시 고려할 점

1. **레이트리밋 추정 존재.** 짧은 간격으로 `GetScoreBoardScroll`을 잘못된 파라미터로 여러 번 찔렀더니, 직후 정상 동작했던 `GetKboGameList`까지 정상 JSON 대신 `에러 | KBO홈페이지` HTML 에러 페이지를 반환. 5초 대기 후 재시도하니 다시 정상 동작. → 정확한 임계치는 불명확하나, 계획된 **30초 polling 주기는 안전권**으로 보임. 다만 재시도/백오프 로직은 필수.
2. 세션 쿠키나 `Referer`/`User-Agent` 헤더가 응답에 필수인지는 명확히 검증되지 않음 (둘 다 없어도 첫 호출은 성공했음). 그러나 실서비스에서는 일반 브라우저처럼 `User-Agent`, `Referer` 헤더를 세팅하는 것을 권장 (차단 회피 차원).
3. 비공식 API이므로 KBO 측 정책 변경/구조 변경에 의해 언제든 깨질 수 있음 → `DataCollector` 인터페이스의 `MockFixtureCollector` 폴백 전략(이미 Phase 1에서 구현됨)을 유지하고, `KboApiCollector` 실패 시 자동 폴백하도록 설계할 것.
4. 응답 인코딩: UTF-8 JSON, 정상.

## 다음 단계 (Phase 2 착수 가능)

- `KboApiCollector.java`: `GetKboGameList`를 호출해 당일 경기 목록 + 실시간 스코어/이닝/카운트를 가져오는 컬렉터로 구현
- `GameCollectorScheduler.java`: `@Scheduled(fixedRate = 30000)`으로 polling, 실패 시 Redis 캐시 폴백
- 팀 순위는 별도 배치(예: 1일 1회 또는 경기 종료 후)로 `TeamRankDaily.aspx` HTML 파싱
