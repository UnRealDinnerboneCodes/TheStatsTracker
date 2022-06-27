package com.unrealdinnerbone.css.json;

import com.unrealdinnerbone.css.json.base.IQueryResult;

import java.util.List;
import java.util.Map;

public record LegendQueryResult(List<Map<String, Long>> data, String[] legend) implements IQueryResult<List<Map<String, Long>>> {}
