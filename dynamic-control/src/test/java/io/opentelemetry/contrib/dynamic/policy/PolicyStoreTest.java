/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.contrib.dynamic.policy.source.SourceWrapper;
import io.opentelemetry.contrib.dynamic.policy.tracesampling.TraceSamplingRatePolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class PolicyStoreTest {

  @Test
  void notifiesImplementerOnChangesAndRemovals() {
    PolicyStore store = new PolicyStore();
    TestImplementer implementer = new TestImplementer(TraceSamplingRatePolicy.POLICY_TYPE);
    store.registerImplementer(implementer);
    assertThat(implementer.notifications).isEmpty();

    TelemetryPolicy samplingHalf = new TraceSamplingRatePolicy(0.5);
    TelemetryPolicy other = new TelemetryPolicy("other-policy");
    store.updatePolicies(Arrays.asList(samplingHalf, other));

    assertThat(implementer.notifications).hasSize(1);
    assertThat(implementer.notifications.get(0)).containsExactly(samplingHalf);

    store.updatePolicies(Arrays.asList(samplingHalf, other));
    assertThat(implementer.notifications).hasSize(1);

    TelemetryPolicy samplingUpdated = new TraceSamplingRatePolicy(0.75);
    store.updatePolicies(Arrays.asList(samplingUpdated, other));

    assertThat(implementer.notifications).hasSize(2);
    assertThat(implementer.notifications.get(1)).containsExactly(samplingUpdated);

    store.updatePolicies(Collections.singletonList(other));

    assertThat(implementer.notifications).hasSize(3);
    assertThat(implementer.notifications.get(2)).hasSize(1);
    assertThat(implementer.notifications.get(2).get(0).getType())
        .isEqualTo(TraceSamplingRatePolicy.POLICY_TYPE);
    assertThat(implementer.notifications.get(2).get(0)).isExactlyInstanceOf(TelemetryPolicy.class);
  }

  private static class TestImplementer implements PolicyImplementer {
    private final List<PolicyValidator> validators;
    private final List<List<TelemetryPolicy>> notifications = new ArrayList<>();

    private TestImplementer(String policyType) {
      this.validators = Collections.singletonList(new TestValidator(policyType));
    }

    @Override
    public void onPoliciesChanged(List<TelemetryPolicy> policies) {
      notifications.add(new ArrayList<>(policies));
    }

    @Override
    public List<PolicyValidator> getValidators() {
      return validators;
    }
  }

  private static class TestValidator implements PolicyValidator {
    private final String policyType;

    private TestValidator(String policyType) {
      this.policyType = policyType;
    }

    @Override
    public TelemetryPolicy validate(SourceWrapper source) {
      return null;
    }

    @Override
    public String getPolicyType() {
      return policyType;
    }
  }
}
