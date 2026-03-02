/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TraceSamplingRatePolicyTest {

  @Test
  void constructorAcceptsValidProbability() {
    TraceSamplingRatePolicy policy = new TraceSamplingRatePolicy(0.5);
    assertThat(policy.getType()).isEqualTo(TraceSamplingRatePolicy.policyType());
    assertThat(policy.getProbability()).isCloseTo(0.5, within(1e-9));
  }

  @Test
  void constructorRejectsNaN() {
    assertThatThrownBy(() -> new TraceSamplingRatePolicy(Double.NaN))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("probability must be within [0.0, 1.0]");
  }

  @ParameterizedTest
  @ValueSource(doubles = {-0.1, 1.1})
  void constructorRejectsOutOfRangeProbabilities(double probability) {
    assertThatThrownBy(() -> new TraceSamplingRatePolicy(probability))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("probability must be within [0.0, 1.0]");
  }
}
