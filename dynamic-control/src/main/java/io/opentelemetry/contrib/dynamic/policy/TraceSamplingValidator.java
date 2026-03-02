/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import com.fasterxml.jackson.databind.JsonNode;
import io.opentelemetry.contrib.dynamic.policy.source.JsonSourceWrapper;
import io.opentelemetry.contrib.dynamic.policy.source.KeyValueSourceWrapper;
import io.opentelemetry.contrib.dynamic.policy.source.SourceWrapper;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/** Validator for trace sampling policies. */
public final class TraceSamplingValidator implements PolicyValidator {
  private static final Logger logger = Logger.getLogger(TraceSamplingValidator.class.getName());

  @Override
  public String getPolicyType() {
    return TraceSamplingRatePolicy.policyType();
  }

  @Override
  @Nullable
  public TelemetryPolicy validate(SourceWrapper source) {
    if (source instanceof JsonSourceWrapper) {
      return validateJsonNode(((JsonSourceWrapper) source).asJsonNode());
    }
    if (source instanceof KeyValueSourceWrapper) {
      return validateKeyValue(((KeyValueSourceWrapper) source));
    }
    return null;
  }

  @Nullable
  private TelemetryPolicy validateJsonNode(JsonNode node) {
    JsonNode probabilityNode = node.get(getPolicyType());
    if (probabilityNode == null || !probabilityNode.isNumber()) {
      return null;
    }
    return createPolicy(probabilityNode.asDouble());
  }

  @Nullable
  private TelemetryPolicy validateKeyValue(KeyValueSourceWrapper source) {
    String key = source.getKey().trim();
    if (!getPolicyType().equals(key)) {
      return null;
    }

    double probability;
    try {
      probability = Double.parseDouble(source.getValue().trim());
    } catch (NumberFormatException e) {
      return null;
    }
    return createPolicy(probability);
  }

  @Nullable
  private static TelemetryPolicy createPolicy(double probability) {
    try {
      return new TraceSamplingRatePolicy(probability);
    } catch (IllegalArgumentException e) {
      logger.warning("Invalid trace-sampling probability '" + probability + "' will be ignored: " + e.getMessage());
      return null;
    }
  }

}
