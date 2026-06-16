package com.kbo.collector;

import com.kbo.collector.dto.GameCollectResult;

import java.time.LocalDate;
import java.util.List;

/**
 * KBO 데이터 수집 인터페이스 (Strategy Pattern)
 * - KboApiCollector: 실제 KBO 비공식 API 수집
 * - MockFixtureCollector: 로컬 JSON fixture 기반 테스트용
 */
public interface DataCollector {

    List<GameCollectResult> collectTodayGames(LocalDate date);

    boolean isAvailable();
}
