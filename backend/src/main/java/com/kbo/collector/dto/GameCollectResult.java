package com.kbo.collector.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.extern.jackson.Jacksonized;

@Getter
@Builder
@Jacksonized
public class GameCollectResult {

    private String homeTeamName;
    private String awayTeamName;
    private int homeScore;
    private int awayScore;
    private String status;   // SCHEDULED, IN_PROGRESS, FINISHED
    private int inning;
    private String stadium;
    private String startTime;
}
