package com.kbo.collector;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbo.collector.dto.GameCollectResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * 로컬 JSON fixture 기반 DataCollector
 * - KBO API 차단 시 또는 로컬 개발 환경에서 사용
 * - application.yml: collector.use-mock=true 시 활성화
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MockFixtureCollector implements DataCollector {

    private final ObjectMapper objectMapper;

    @Override
    public List<GameCollectResult> collectTodayGames(LocalDate date) {
        try {
            ClassPathResource resource = new ClassPathResource("fixtures/sample-games.json");
            return objectMapper.readValue(resource.getInputStream(), new TypeReference<>() {});
        } catch (IOException e) {
            log.error("[MockFixtureCollector] fixture 파일 로드 실패: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
