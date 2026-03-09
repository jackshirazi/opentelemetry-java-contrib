/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.metricsexport;

import com.fasterxml.jackson.databind.JsonNode;
import io.opentelemetry.contrib.dynamic.policy.AbstractSourcePolicyValidator;
import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicy;
import javax.annotation.Nullable;

/** Validator for metric export enabled policies. */
public final class MetricExportEnabledValidator extends AbstractSourcePolicyValidator {

  @Override
  public String getPolicyType() {
    return MetricExportEnabledPolicy.POLICY_TYPE;
  }

  @Override
  @Nullable
  protected TelemetryPolicy validateJsonValue(JsonNode valueNode) {
    Boolean enabled = parseBoolean(valueNode);
    if (enabled == null) {
      return null;
    }
    return new MetricExportEnabledPolicy(enabled);
  }

  @Override
  @Nullable
  protected TelemetryPolicy validateKeyValueValue(String value) {
    Boolean enabled = parseBoolean(value);
    if (enabled == null) {
      return null;
    }
    return new MetricExportEnabledPolicy(enabled);
  }
}
