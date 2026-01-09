/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import com.fasterxml.jackson.databind.JsonNode;
import io.opentelemetry.contrib.dynamic.policy.util.JsonMergePatchUtil;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PolicyAggregator {

  public List<TelemetryPolicy> merge(List<List<TelemetryPolicy>> sources) {
    Map<String, JsonNode> byType = new HashMap<>();

    for (List<TelemetryPolicy> list : sources) {
      for (TelemetryPolicy p : list) {
        byType.merge(p.getType(), p.getSpec(), PolicyAggregator::mergeJsonMergePatch);
      }
    }

    return byType.entrySet().stream()
        .map(e -> new TelemetryPolicy(e.getKey(), e.getValue()))
        .collect(Collectors.toList());
  }

  private static JsonNode mergeJsonMergePatch(JsonNode base, JsonNode patch) {
    try {
      return JsonMergePatchUtil.mergeJsonMergePatch(base, patch);
    } catch (Exception e) {
      throw new IllegalStateException("Failed to merge JSON patches", e);
    }
  }
}
