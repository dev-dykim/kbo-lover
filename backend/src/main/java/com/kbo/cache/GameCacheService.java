package com.kbo.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbo.collector.dto.GameCollectResult;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * 당일 경기 수집 결과를 Redis에 캐싱한다.
 * KboApiCollector 호출이 실패해도 마지막으로 저장된 값이 그대로 남아있는 것 자체가
 * 폴백 역할을 한다 (실패 시 덮어쓰지 않음 — GameCollectorScheduler 참고).
 */
@Component
@RequiredArgsConstructor
public class GameCacheService {

    static final String TODAY_GAMES_KEY = "kbo:games:today";
    private static final Duration TTL = Duration.ofHours(20);

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public void saveTodayGames(List<GameCollectResult> games) {
        redisTemplate.opsForValue().set(TODAY_GAMES_KEY, games, TTL);
    }

    public List<GameCollectResult> loadTodayGames() {
        Object cached = redisTemplate.opsForValue().get(TODAY_GAMES_KEY);
        if (!(cached instanceof List<?> list)) {
            return Collections.emptyList();
        }
        // GenericJackson2JsonRedisSerializer가 항상 GameCollectResult로 역직렬화한다고
        // 보장할 수 없으므로(@class 메타데이터 누락 등) 원소별로 안전하게 변환한다.
        return list.stream()
                .map(item -> objectMapper.convertValue(item, GameCollectResult.class))
                .toList();
    }
}
