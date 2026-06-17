package com.kbo.collector;

import com.kbo.collector.dto.GameCollectResult;
import com.kbo.collector.dto.kboapi.KboGameDto;
import com.kbo.collector.dto.kboapi.KboGameListRequest;
import com.kbo.collector.dto.kboapi.KboGameListResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

/**
 * koreabaseball.com 비공식 API(/ws/Main.asmx/GetKboGameList) 기반 DataCollector.
 * 엔드포인트/파라미터/주의사항 출처: .omc/research/kbo-api-endpoints.md
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "collector", name = "use-mock", havingValue = "false", matchIfMissing = true)
public class KboApiCollector implements DataCollector {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String GAME_LIST_PATH = "/ws/Main.asmx/GetKboGameList";

    // 리버스 엔지니어링으로 확인된 유일한 종료 상태 코드. 그 외 코드는 미확인.
    private static final String FINISHED_STATE_CODE = "3";

    // 레이트리밋 추정 차단 시 5초 대기 후 재시도하면 복구되는 것을 확인함 (research 문서 참고).
    private static final int MAX_ATTEMPTS = 2;

    private final RestTemplate restTemplate;

    @Value("${collector.kbo.base-url}")
    private String baseUrl;

    @Value("${collector.kbo.league-id}")
    private String leagueId;

    @Value("${collector.kbo.series-id}")
    private String seriesId;

    @Value("${collector.kbo.retry-delay-ms:5000}")
    private long retryDelayMs;

    private volatile boolean available = true;

    @Override
    public List<GameCollectResult> collectTodayGames(LocalDate date) {
        RestClientException lastError = null;
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                KboGameListResponse response = callGameList(date);
                available = true;
                if (response == null || response.game() == null) {
                    return Collections.emptyList();
                }
                return response.game().stream()
                        .map(this::toGameCollectResult)
                        .toList();
            } catch (RestClientException e) {
                lastError = e;
                log.warn("[KboApiCollector] KBO API 호출 실패 ({}/{}번째 시도): {}", attempt, MAX_ATTEMPTS, e.getMessage());
                if (attempt < MAX_ATTEMPTS) {
                    sleep(retryDelayMs);
                }
            }
        }
        available = false;
        log.warn("[KboApiCollector] 재시도 후에도 실패, 마지막 오류: {}", lastError.getMessage());
        return Collections.emptyList();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public boolean isAvailable() {
        return available;
    }

    private KboGameListResponse callGameList(LocalDate date) {
        KboGameListRequest request = new KboGameListRequest(leagueId, seriesId, date.format(DATE_FORMAT));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf("application/json;charset=UTF-8"));
        headers.set(HttpHeaders.USER_AGENT,
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36");
        headers.set(HttpHeaders.REFERER, baseUrl + "/Schedule/GameCenter/Main.aspx");

        return restTemplate.exchange(
                baseUrl + GAME_LIST_PATH,
                HttpMethod.POST,
                new HttpEntity<>(request, headers),
                KboGameListResponse.class
        ).getBody();
    }

    private GameCollectResult toGameCollectResult(KboGameDto dto) {
        return GameCollectResult.builder()
                .homeTeamName(dto.homeName())
                .awayTeamName(dto.awayName())
                .homeScore(parseScore(dto.homeScore()))
                .awayScore(parseScore(dto.awayScore()))
                .status(toStatus(dto))
                .inning(dto.inning() == null ? 0 : dto.inning())
                .stadium(dto.stadium())
                .startTime(dto.startTime())
                .build();
    }

    private int parseScore(String score) {
        if (score == null || score.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(score.trim());
        } catch (NumberFormatException e) {
            log.warn("[KboApiCollector] 점수 파싱 실패, 0으로 대체: '{}'", score);
            return 0;
        }
    }

    private String toStatus(KboGameDto dto) {
        if (FINISHED_STATE_CODE.equals(dto.stateCode())) {
            return "FINISHED";
        }
        return (dto.inning() != null && dto.inning() > 0) ? "IN_PROGRESS" : "SCHEDULED";
    }
}
