/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class TraceSamplingRatePolicy extends TelemetryPolicy {
  private static final AtomicReference<String> POLICY_TYPE = new AtomicReference<>("trace-sampling");

  private final double probability;

  public TraceSamplingRatePolicy(double probability) {
    super(policyType());
    if (Double.isNaN(probability) || probability < 0.0 || probability > 1.0) {
      throw new IllegalArgumentException("probability must be within [0.0, 1.0]");
    }
    this.probability = probability;
  }

  public static String policyType() {
    return Objects.requireNonNull(POLICY_TYPE.get(), "policyType cannot be null");
  }

  public static void setPolicyType(String policyType) {
    Objects.requireNonNull(policyType, "policyType cannot be null");
    if (policyType.trim().isEmpty()) {
      throw new IllegalArgumentException("policyType cannot be empty");
    }
    POLICY_TYPE.set(policyType);
  }

  public double getProbability() {
    return probability;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TraceSamplingRatePolicy)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    TraceSamplingRatePolicy that = (TraceSamplingRatePolicy) o;
    return that.canEqual(this) && Double.compare(that.probability, probability) == 0;
  }

  @Override
  protected boolean canEqual(Object other) {
    return other instanceof TraceSamplingRatePolicy;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), probability);
  }
}
