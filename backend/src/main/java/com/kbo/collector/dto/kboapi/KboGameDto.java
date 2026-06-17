package com.kbo.collector.dto.kboapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * koreabaseball.com /ws/Main.asmx/GetKboGameList 응답의 game[] 항목 매핑.
 * 필드명은 KBO 응답 JSON 키 그대로 (스네이크/대문자), 매핑 근거는
 * .omc/research/kbo-api-endpoints.md 참고.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record KboGameDto(
        @JsonProperty("G_ID") String gameId,
        @JsonProperty("G_TM") String startTime,
        @JsonProperty("S_NM") String stadium,
        @JsonProperty("HOME_NM") String homeName,
        @JsonProperty("AWAY_NM") String awayName,
        @JsonProperty("GAME_STATE_SC") String stateCode,
        @JsonProperty("GAME_INN_NO") Integer inning,
        @JsonProperty("B_SCORE_CN") String homeScore,
        @JsonProperty("T_SCORE_CN") String awayScore
) {
}
