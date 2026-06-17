package com.kbo.collector;

import com.kbo.collector.dto.GameCollectResult;
import com.kbo.collector.dto.kboapi.KboGameDto;
import com.kbo.collector.dto.kboapi.KboGameListResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KboApiCollectorTest {

    @Mock
    private RestTemplate restTemplate;

    private KboApiCollector collector;

    @BeforeEach
    void setUp() {
        collector = new KboApiCollector(restTemplate);
        ReflectionTestUtils.setField(collector, "baseUrl", "https://www.koreabaseball.com");
        ReflectionTestUtils.setField(collector, "leagueId", "1");
        ReflectionTestUtils.setField(collector, "seriesId", "0,1");
    }

    @Test
    void collectTodayGames_mapsFinishedGameCorrectly() {
        KboGameDto dto = new KboGameDto(
                "20250701SSOB0", "18:30", "잠실", "두산", "삼성",
                "3", 9, "1", "4"
        );
        stubResponse(new KboGameListResponse(List.of(dto)));

        List<GameCollectResult> results = collector.collectTodayGames(LocalDate.of(2025, 7, 1));

        assertThat(results).hasSize(1);
        GameCollectResult result = results.get(0);
        assertThat(result.getHomeTeamName()).isEqualTo("두산");
        assertThat(result.getAwayTeamName()).isEqualTo("삼성");
        assertThat(result.getHomeScore()).isEqualTo(1);
        assertThat(result.getAwayScore()).isEqualTo(4);
        assertThat(result.getStatus()).isEqualTo("FINISHED");
        assertThat(result.getInning()).isEqualTo(9);
        assertThat(collector.isAvailable()).isTrue();
    }

    @Test
    void collectTodayGames_scheduledGameHasZeroScoreAndStatus() {
        KboGameDto dto = new KboGameDto(
                "20250701LGLT0", "18:30", "사직", "롯데", "LG",
                "0", null, "", ""
        );
        stubResponse(new KboGameListResponse(List.of(dto)));

        List<GameCollectResult> results = collector.collectTodayGames(LocalDate.of(2025, 7, 1));

        GameCollectResult result = results.get(0);
        assertThat(result.getHomeScore()).isZero();
        assertThat(result.getAwayScore()).isZero();
        assertThat(result.getStatus()).isEqualTo("SCHEDULED");
        assertThat(result.getInning()).isZero();
    }

    @Test
    void collectTodayGames_returnsEmptyListAndMarksUnavailableWhenAllRetriesFail() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(KboGameListResponse.class)))
                .thenThrow(new RestClientException("connection refused"));

        List<GameCollectResult> results = collector.collectTodayGames(LocalDate.of(2025, 7, 1));

        assertThat(results).isEmpty();
        assertThat(collector.isAvailable()).isFalse();
    }

    @Test
    void collectTodayGames_recoversOnRetryAfterTransientFailure() {
        KboGameDto dto = new KboGameDto(
                "20250701LGLT0", "18:30", "사직", "롯데", "LG",
                "3", 9, "2", "3"
        );
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(KboGameListResponse.class)))
                .thenThrow(new RestClientException("rate limited"))
                .thenReturn(ResponseEntity.ok(new KboGameListResponse(List.of(dto))));

        List<GameCollectResult> results = collector.collectTodayGames(LocalDate.of(2025, 7, 1));

        assertThat(results).hasSize(1);
        assertThat(collector.isAvailable()).isTrue();
    }

    @Test
    void collectTodayGames_malformedScoreFallsBackToZeroInsteadOfThrowing() {
        KboGameDto dto = new KboGameDto(
                "20250701LGLT0", "18:30", "사직", "롯데", "LG",
                "3", 9, "우천취소", "3"
        );
        stubResponse(new KboGameListResponse(List.of(dto)));

        List<GameCollectResult> results = collector.collectTodayGames(LocalDate.of(2025, 7, 1));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getHomeScore()).isZero();
        assertThat(results.get(0).getAwayScore()).isEqualTo(3);
    }

    private void stubResponse(KboGameListResponse response) {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(KboGameListResponse.class)))
                .thenReturn(ResponseEntity.ok(response));
    }
}
