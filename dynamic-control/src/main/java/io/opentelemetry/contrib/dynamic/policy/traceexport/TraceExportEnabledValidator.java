/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.traceexport;

import com.fasterxml.jackson.databind.JsonNode;
import io.opentelemetry.contrib.dynamic.policy.AbstractSourcePolicyValidator;
import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicy;
import javax.annotation.Nullable;

/** Validator for trace export enabled policies. */
public final class TraceExportEnabledValidator extends AbstractSourcePolicyValidator {

  @Override
  public String getPolicyType() {
    return TraceExportEnabledPolicy.POLICY_TYPE;
  }

  @Override
  @Nullable
  protected TelemetryPolicy validateJsonValue(JsonNode valueNode) {
    Boolean enabled = parseBoolean(valueNode);
    if (enabled == null) {
      return null;
    }
    return new TraceExportEnabledPolicy(enabled);
  }

  @Override
  @Nullable
  protected TelemetryPolicy validateKeyValueValue(String value) {
    Boolean enabled = parseBoolean(value);
    if (enabled == null) {
      return null;
    }
    return new TraceExportEnabledPolicy(enabled);
  }
}
