package com.kbo.collector.dto.kboapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KboGameListResponse(List<KboGameDto> game) {
}
