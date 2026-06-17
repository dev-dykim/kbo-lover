package com.kbo.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbo.collector.dto.GameCollectResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameCacheServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private GameCacheService gameCacheService;

    @BeforeEach
    void setUp() {
        gameCacheService = new GameCacheService(redisTemplate, new ObjectMapper());
    }

    @Test
    void saveTodayGames_storesUnderExpectedKeyWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        List<GameCollectResult> games = List.of(
                GameCollectResult.builder().homeTeamName("두산").awayTeamName("삼성").build()
        );

        gameCacheService.saveTodayGames(games);

        verify(valueOperations).set(eq(GameCacheService.TODAY_GAMES_KEY), eq(games), eq(Duration.ofHours(20)));
    }

    @Test
    void loadTodayGames_returnsEmptyListWhenNothingCached() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(GameCacheService.TODAY_GAMES_KEY)).thenReturn(null);

        List<GameCollectResult> result = gameCacheService.loadTodayGames();

        assertThat(result).isEmpty();
    }

    @Test
    void loadTodayGames_returnsCachedListWhenPresent() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        List<GameCollectResult> cached = List.of(
                GameCollectResult.builder().homeTeamName("LG").awayTeamName("KIA").build()
        );
        when(valueOperations.get(GameCacheService.TODAY_GAMES_KEY)).thenReturn(cached);

        List<GameCollectResult> result = gameCacheService.loadTodayGames();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getHomeTeamName()).isEqualTo("LG");
    }

    @Test
    void loadTodayGames_convertsRawMapElementsBackToGameCollectResult() {
        // GenericJackson2JsonRedisSerializer가 @class 메타데이터 없이 역직렬화하면
        // 리스트 원소가 GameCollectResult가 아닌 LinkedHashMap으로 올 수 있다.
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        Map<String, Object> rawMap = Map.of(
                "homeTeamName", "두산",
                "awayTeamName", "삼성",
                "homeScore", 1,
                "awayScore", 4,
                "status", "FINISHED",
                "inning", 9
        );
        when(valueOperations.get(GameCacheService.TODAY_GAMES_KEY)).thenReturn(List.of(rawMap));

        List<GameCollectResult> result = gameCacheService.loadTodayGames();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getHomeTeamName()).isEqualTo("두산");
        assertThat(result.get(0).getAwayScore()).isEqualTo(4);
    }
}
