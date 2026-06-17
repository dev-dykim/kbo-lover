package com.kbo.scheduler;

import com.kbo.cache.GameCacheService;
import com.kbo.collector.DataCollector;
import com.kbo.collector.dto.GameCollectResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class GameCollectorScheduler {

    private static final ZoneId KBO_ZONE = ZoneId.of("Asia/Seoul");

    private final DataCollector dataCollector;
    private final GameCacheService gameCacheService;

    @Scheduled(fixedDelayString = "${collector.schedule.rate-ms}")
    public void collectTodayGames() {
        try {
            List<GameCollectResult> games = dataCollector.collectTodayGames(LocalDate.now(KBO_ZONE));
            if (games.isEmpty() && !dataCollector.isAvailable()) {
                log.warn("[GameCollectorScheduler] 수집 실패(unavailable), 기존 캐시 유지");
                return;
            }
            gameCacheService.saveTodayGames(games);
            log.info("[GameCollectorScheduler] {}건 수집 완료", games.size());
        } catch (Exception e) {
            log.warn("[GameCollectorScheduler] 수집 실패, 기존 캐시 유지: {}", e.getMessage());
        }
    }
}
