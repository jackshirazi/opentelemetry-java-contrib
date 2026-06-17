/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.tracesampling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import org.junit.jupiter.api.Test;

class TraceSamplingRatePolicyTest {

  @Test
  void constructorStoresProbabilityAndType() {
    TraceSamplingRatePolicy policy = tracePolicy(0.25);

    assertThat(policy.getIdentity().getId()).isEqualTo("trace-policy");
    assertThat(policy.getIdentity().getName()).isEqualTo("Trace policy");
    assertThat(policy.getProbability()).isEqualTo(0.25);
    assertThat(policy.getType()).isEqualTo(TraceSamplingRatePolicy.POLICY_TYPE);
  }

  @Test
  void constructorNormalizesNegativeZeroToPositiveZero() {
    TraceSamplingRatePolicy negativeZero = tracePolicy(-0.0);
    TraceSamplingRatePolicy positiveZero = tracePolicy(0.0);

    assertThat(negativeZero.getProbability()).isEqualTo(0.0);
    assertThat(Double.doubleToRawLongBits(negativeZero.getProbability()))
        .isEqualTo(Double.doubleToRawLongBits(0.0));
    assertThat(negativeZero).isEqualTo(positiveZero);
    assertThat(negativeZero.hashCode()).isEqualTo(positiveZero.hashCode());
  }

  @Test
  void constructorRejectsOutOfRangeOrNaNProbabilities() {
    assertThatThrownBy(() -> tracePolicy(Double.NaN))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("probability must be within [0.0, 1.0]");
    assertThatThrownBy(() -> tracePolicy(-0.001))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("probability must be within [0.0, 1.0]");
    assertThatThrownBy(() -> tracePolicy(1.001))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("probability must be within [0.0, 1.0]");
  }

  @Test
  void equalsAndHashCodeUseProbability() {
    TraceSamplingRatePolicy a = tracePolicy(0.5);
    TraceSamplingRatePolicy b = tracePolicy(0.5);
    TraceSamplingRatePolicy c = tracePolicy(0.75);

    assertThat(a).isEqualTo(b);
    assertThat(a.hashCode()).isEqualTo(b.hashCode());
    assertThat(a).isNotEqualTo(c);
    assertThat(a).isNotEqualTo(null);
    assertThat(a).isNotEqualTo("not-a-policy");
  }

  @Test
  void equalsAndHashCodeUseIdentity() {
    TraceSamplingRatePolicy a = new TraceSamplingRatePolicy("trace-a", "Trace A", 0.5);
    TraceSamplingRatePolicy b = new TraceSamplingRatePolicy("trace-b", "Trace B", 0.5);

    assertThat(a).isNotEqualTo(b);
  }

  @Test
  void initializeRejectsNullCustomizer() {
    assertThatThrownBy(() -> TraceSamplingRatePolicy.initialize(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("autoConfiguration cannot be null");
  }

  @Test
  void initializeStoresDelegatingSamplerAndRegistersCustomizer() {
    AutoConfigurationCustomizer customizer = mock(AutoConfigurationCustomizer.class);

    TraceSamplingRatePolicy.initialize(customizer);

    assertThat(TraceSamplingRatePolicy.getInitializedSampler()).isNotNull();
    verify(customizer).addSamplerCustomizer(any());
  }

  @Test
  void createSamplerAcceptsBoundaryProbabilities() {
    Sampler zero = TraceSamplingRatePolicy.createSampler(0.0);
    Sampler negativeZero = TraceSamplingRatePolicy.createSampler(-0.0);
    Sampler one = TraceSamplingRatePolicy.createSampler(1.0);

    assertThat(zero).isNotNull();
    assertThat(negativeZero).isNotNull();
    assertThat(one).isNotNull();
  }

  @Test
  void createSamplerRejectsOutOfRangeOrNaNProbabilities() {
    assertThatThrownBy(() -> TraceSamplingRatePolicy.createSampler(Double.NaN))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("probability must be within [0.0, 1.0]");
    assertThatThrownBy(() -> TraceSamplingRatePolicy.createSampler(-0.01))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("probability must be within [0.0, 1.0]");
    assertThatThrownBy(() -> TraceSamplingRatePolicy.createSampler(1.01))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("probability must be within [0.0, 1.0]");
  }

  private static TraceSamplingRatePolicy tracePolicy(double probability) {
    return new TraceSamplingRatePolicy("trace-policy", "Trace policy", probability);
  }
}
