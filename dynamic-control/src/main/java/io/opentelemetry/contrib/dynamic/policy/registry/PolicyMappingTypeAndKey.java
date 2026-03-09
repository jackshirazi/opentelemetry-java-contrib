/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.registry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicy;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nullable;

/** Maps a source key to an internal policy type. */
public final class PolicyMappingTypeAndKey {
  private final String sourceKey;
  private final String configuredPolicyType;
  @Nullable private Class<? extends TelemetryPolicy> policyType;

  @JsonCreator
  public PolicyMappingTypeAndKey(
      @JsonProperty("sourceKey") String sourceKey, @JsonProperty("policyType") String policyType) {
    this.sourceKey = Objects.requireNonNull(sourceKey, "sourceKey cannot be null");
    this.configuredPolicyType = Objects.requireNonNull(policyType, "policyType cannot be null");
  }

  public String getSourceKey() {
    return sourceKey;
  }

  public String getConfiguredPolicyType() {
    return configuredPolicyType;
  }

  public void resolvePolicyTypeClass(Map<String, Class<? extends TelemetryPolicy>> registeredPolicyTypes) {
    Objects.requireNonNull(registeredPolicyTypes, "registeredPolicyTypes cannot be null");
    this.policyType = registeredPolicyTypes.get(configuredPolicyType);
  }

  @Nullable
  public Class<? extends TelemetryPolicy> getPolicyType() {
    return policyType;
  }
}
