/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.tracesampling;

import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicy;
import io.opentelemetry.contrib.dynamic.policy.registry.PolicyRegistry;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.extension.incubator.trace.samplers.ComposableSampler;
import io.opentelemetry.sdk.extension.incubator.trace.samplers.CompositeSampler;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nullable;

public final class TraceSamplingRatePolicy extends TelemetryPolicy {
  public static final String POLICY_TYPE = "trace_sampling_rate_policy";
  private static final AtomicReference<DelegatingSampler> INITIALIZED_SAMPLER =
      new AtomicReference<>();

  private final double probability;

  public TraceSamplingRatePolicy(double probability) {
    super(POLICY_TYPE);
    if (Double.isNaN(probability) || probability < 0.0 || probability > 1.0) {
      throw new IllegalArgumentException("probability must be within [0.0, 1.0]");
    }
    this.probability = probability;
  }

  public double getProbability() {
    return probability;
  }

  /** Registers this policy type with {@link PolicyRegistry}. */
  public static void registerPolicyType() {
    PolicyRegistry.registerPolicyType(POLICY_TYPE, TraceSamplingRatePolicy.class);
  }

  /**
   * Initializes runtime wiring for this policy type.
   *
   * <p>The activation wiring is implemented in a follow-up change; this method is the policy-owned
   * initialization hook used by {@code PolicyRegistry}.
   */
  public static void initialize(AutoConfigurationCustomizer autoConfiguration) {
    Objects.requireNonNull(autoConfiguration, "autoConfiguration cannot be null");
    Sampler initialDelegate = createSampler(1.0);
    DelegatingSampler delegatingSampler = new DelegatingSampler(initialDelegate);
    INITIALIZED_SAMPLER.set(delegatingSampler);
    autoConfiguration.addSamplerCustomizer((sampler, config) -> delegatingSampler);
  }

  public static Sampler createSampler(double probability) {
    if (Double.isNaN(probability) || probability < 0.0 || probability > 1.0) {
      throw new IllegalArgumentException("probability must be within [0.0, 1.0]");
    }
    return CompositeSampler.wrap(
        ComposableSampler.parentThreshold(ComposableSampler.probability(probability)));
  }

  @Nullable
  public static DelegatingSampler getInitializedSampler() {
    return INITIALIZED_SAMPLER.get();
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
