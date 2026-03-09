/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.opamppolling;

import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicy;
import io.opentelemetry.contrib.dynamic.policy.registry.PolicyRegistry;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import java.time.Duration;
import java.util.Objects;

/** Policy that controls OpAMP HTTP polling interval. */
public final class OpampPollingIntervalPolicy extends TelemetryPolicy {
  public static final String POLICY_TYPE = "OpampPollingIntervalPolicy";
  public static final Duration DEFAULT_POLLING_INTERVAL = Duration.ofSeconds(30);

  private final Duration interval;

  public OpampPollingIntervalPolicy(Duration interval) {
    super(POLICY_TYPE);
    Objects.requireNonNull(interval, "interval cannot be null");
    if (interval.isZero() || interval.isNegative()) {
      throw new IllegalArgumentException("interval must be > 0");
    }
    this.interval = interval;
  }

  public Duration getInterval() {
    return interval;
  }

  public static void registerPolicyType() {
    PolicyRegistry.registerPolicyType(POLICY_TYPE, OpampPollingIntervalPolicy.class);
  }

  public static void initialize(AutoConfigurationCustomizer autoConfiguration) {
    Objects.requireNonNull(autoConfiguration, "autoConfiguration cannot be null");
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof OpampPollingIntervalPolicy)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    OpampPollingIntervalPolicy that = (OpampPollingIntervalPolicy) o;
    return that.canEqual(this) && Objects.equals(interval, that.interval);
  }

  @Override
  protected boolean canEqual(Object other) {
    return other instanceof OpampPollingIntervalPolicy;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), interval);
  }
}
