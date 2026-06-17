/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.tracesampling;

import com.fasterxml.jackson.databind.JsonNode;
import io.opentelemetry.contrib.dynamic.policy.AbstractSourcePolicyValidator;
import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicy;
import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicyIdentity;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/**
 * Validator for trace sampling policies.
 *
 * <p>This validator handles the "trace-sampling" policy type.
 */
public final class TraceSamplingValidator extends AbstractSourcePolicyValidator {
  private static final Logger logger = Logger.getLogger(TraceSamplingValidator.class.getName());

  @Override
  public String getPolicyType() {
    return TraceSamplingRatePolicy.POLICY_TYPE;
  }

  @Override
  @Nullable
  protected TelemetryPolicy validateJsonValue(JsonNode valueNode) {
    if (!valueNode.isObject()) {
      Double probability = parseDouble(valueNode);
      return probability == null
          ? null
          : createPolicy(TraceSamplingRatePolicy.DEFAULT_IDENTITY, probability);
    }
    TelemetryPolicyIdentity identity = parseIdentity(valueNode);
    if (identity == null) {
      return null;
    }
    JsonNode probabilityNode = valueNode.get("probability");
    if (probabilityNode == null) {
      return null;
    }
    Double probability = parseDouble(probabilityNode);
    if (probability == null) {
      return null;
    }
    return createPolicy(identity, probability);
  }

  @Override
  @Nullable
  protected TelemetryPolicy validateKeyValueValue(String value) {
    Map<String, String> fields = parseNamedFields(value);
    if (fields.isEmpty()) {
      return createKeyValuePolicy(TraceSamplingRatePolicy.DEFAULT_IDENTITY, value);
    }
    return createKeyValuePolicy(
        createIdentity(
            fields.get("id") == null
                ? TraceSamplingRatePolicy.DEFAULT_IDENTITY.getId()
                : fields.get("id"),
            fields.get("name") == null
                ? TraceSamplingRatePolicy.DEFAULT_IDENTITY.getName()
                : fields.get("name")),
        fields.get("probability"));
  }

  @Nullable
  private static TelemetryPolicy createKeyValuePolicy(
      @Nullable TelemetryPolicyIdentity identity, @Nullable String valueText) {
    if (identity == null) {
      return null;
    }
    if (valueText == null) {
      return null;
    }
    Double probability = parseDouble(valueText);
    if (probability == null) {
      return null;
    }
    return createPolicy(identity, probability);
  }

  private static Map<String, String> parseNamedFields(String value) {
    HashMap<String, String> fields = new HashMap<String, String>();
    String[] entries = value.split(",");
    for (String entry : entries) {
      int separatorIndex = entry.indexOf('=');
      if (separatorIndex <= 0) {
        return new HashMap<String, String>();
      }
      String key = entry.substring(0, separatorIndex).trim();
      String fieldValue = entry.substring(separatorIndex + 1).trim();
      if (key.isEmpty()) {
        return new HashMap<String, String>();
      }
      fields.put(key, fieldValue);
    }
    return fields;
  }

  @Nullable
  private static TelemetryPolicy createPolicy(TelemetryPolicyIdentity identity, double probability) {
    try {
      return new TraceSamplingRatePolicy(identity, probability);
    } catch (IllegalArgumentException e) {
      logger.info(
          "Invalid trace-sampling probability '"
              + probability
              + "' will be ignored: "
              + e.getMessage());
      return null;
    }
  }

  @Nullable
  private static TelemetryPolicyIdentity parseIdentity(JsonNode valueNode) {
    JsonNode idNode = valueNode.get("id");
    JsonNode nameNode = valueNode.get("name");
    String id =
        idNode == null || !idNode.isValueNode()
            ? TraceSamplingRatePolicy.DEFAULT_IDENTITY.getId()
            : idNode.asText();
    String name =
        nameNode == null || !nameNode.isValueNode()
            ? TraceSamplingRatePolicy.DEFAULT_IDENTITY.getName()
            : nameNode.asText();
    return createIdentity(id, name);
  }

  @Nullable
  private static TelemetryPolicyIdentity createIdentity(String id, String name) {
    try {
      return new TelemetryPolicyIdentity(id, name);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

}
