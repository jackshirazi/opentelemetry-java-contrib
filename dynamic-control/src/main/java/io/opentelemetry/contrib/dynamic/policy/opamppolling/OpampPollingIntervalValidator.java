/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.opamppolling;

import com.fasterxml.jackson.databind.JsonNode;
import io.opentelemetry.contrib.dynamic.policy.AbstractSourcePolicyValidator;
import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicy;
import java.time.Duration;
import javax.annotation.Nullable;

/** Validator for OpAMP polling interval policies. */
public final class OpampPollingIntervalValidator extends AbstractSourcePolicyValidator {
  @Override
  public String getPolicyType() {
    return OpampPollingIntervalPolicy.POLICY_TYPE;
  }

  @Override
  @Nullable
  protected TelemetryPolicy validateJsonValue(JsonNode valueNode) {
    return createPolicy(parseDouble(valueNode));
  }

  @Override
  @Nullable
  protected TelemetryPolicy validateKeyValueValue(String value) {
    return createPolicy(parseDouble(value));
  }

  @Nullable
  private static TelemetryPolicy createPolicy(@Nullable Double seconds) {
    if (seconds == null || Double.isNaN(seconds) || seconds <= 0.0) {
      return null;
    }
    long millis = Math.round(seconds * 1000.0d);
    if (millis <= 0L) {
      return null;
    }
    return new OpampPollingIntervalPolicy(Duration.ofMillis(millis));
  }
}
