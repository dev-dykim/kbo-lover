package com.kbo.scheduler;

import com.kbo.cache.GameCacheService;
import com.kbo.collector.DataCollector;
import com.kbo.collector.dto.GameCollectResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameCollectorSchedulerTest {

    @Mock
    private DataCollector dataCollector;

    @Mock
    private GameCacheService gameCacheService;

    private GameCollectorScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new GameCollectorScheduler(dataCollector, gameCacheService);
    }

    @Test
    void collectTodayGames_savesResultToCacheOnSuccess() {
        List<GameCollectResult> games = List.of(GameCollectResult.builder().homeTeamName("두산").build());
        when(dataCollector.collectTodayGames(any(LocalDate.class))).thenReturn(games);

        scheduler.collectTodayGames();

        verify(gameCacheService).saveTodayGames(games);
    }

    @Test
    void collectTodayGames_doesNotTouchCacheWhenCollectorThrows() {
        when(dataCollector.collectTodayGames(any(LocalDate.class))).thenThrow(new RuntimeException("boom"));

        scheduler.collectTodayGames();

        verify(gameCacheService, never()).saveTodayGames(any());
    }

    @Test
    void collectTodayGames_doesNotOverwriteCacheWhenCollectorReturnsEmptyAndUnavailable() {
        // 실제 실패 경로: KboApiCollector는 예외를 던지지 않고 빈 리스트 + isAvailable()=false를 반환한다.
        when(dataCollector.collectTodayGames(any(LocalDate.class))).thenReturn(List.of());
        when(dataCollector.isAvailable()).thenReturn(false);

        scheduler.collectTodayGames();

        verify(gameCacheService, never()).saveTodayGames(any());
    }

    @Test
    void collectTodayGames_savesEmptyResultWhenCollectorIsAvailable() {
        // 진짜로 오늘 경기가 없는 날(휴식일)은 빈 리스트를 그대로 캐시에 반영해야 한다.
        when(dataCollector.collectTodayGames(any(LocalDate.class))).thenReturn(List.of());
        when(dataCollector.isAvailable()).thenReturn(true);

        scheduler.collectTodayGames();

        verify(gameCacheService).saveTodayGames(List.of());
    }
}
