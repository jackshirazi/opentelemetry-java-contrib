/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.contrib.dynamic.policy.tracesampling.TraceSamplingRatePolicy;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class PolicyStoreTest {

  @Test
  void updatePoliciesReturnsTrueOnFirstSet() {
    PolicyStore store = new PolicyStore();
    List<TelemetryPolicy> policies = singletonList(tracePolicy(0.5));

    assertThat(store.updatePolicies(policies)).isTrue();
    assertThat(store.getPolicies()).isEqualTo(policies);
  }

  @Test
  void updatePoliciesReturnsFalseWhenEqualContent() {
    PolicyStore store = new PolicyStore();
    assertThat(store.updatePolicies(singletonList(tracePolicy(0.5)))).isTrue();
    assertThat(store.updatePolicies(singletonList(tracePolicy(0.5)))).isFalse();
  }

  @Test
  void updatePoliciesReturnsTrueWhenProbabilityChanges() {
    PolicyStore store = new PolicyStore();
    assertThat(store.updatePolicies(singletonList(tracePolicy(0.25)))).isTrue();
    assertThat(store.updatePolicies(singletonList(tracePolicy(0.75)))).isTrue();
    assertThat(store.getPolicies()).containsExactly(tracePolicy(0.75));
  }

  @Test
  void updatePoliciesReturnsFalseWhenOnlyOrderDiffers() {
    PolicyStore store = new PolicyStore();
    List<TelemetryPolicy> first =
        Arrays.asList(tracePolicyWithId("trace-1", 0.1), tracePolicyWithId("trace-2", 0.2));
    List<TelemetryPolicy> reordered =
        Arrays.asList(tracePolicyWithId("trace-2", 0.2), tracePolicyWithId("trace-1", 0.1));

    assertThat(store.updatePolicies(first)).isTrue();
    assertThat(store.updatePolicies(reordered)).isFalse();
    assertThat(store.getPolicies()).isEqualTo(first);
  }

  @Test
  void updatePoliciesIgnoresDuplicatePoliciesInInput() {
    PolicyStore store = new PolicyStore();
    TraceSamplingRatePolicy p = tracePolicy(0.5);
    assertThat(store.updatePolicies(Arrays.asList(p, tracePolicy(0.5)))).isTrue();
    assertThat(store.getPolicies()).containsExactly(p);
    assertThat(store.updatePolicies(singletonList(tracePolicy(0.5)))).isFalse();
  }

  @Test
  void getPoliciesReturnsEmptyWhenNeverUpdated() {
    assertThat(new PolicyStore().getPolicies()).isEqualTo(Collections.emptyList());
  }

  @Test
  void registerImplementerReceivesCurrentRelevantPolicies() {
    PolicyStore store = new PolicyStore();
    store.updatePolicies(Arrays.asList(tracePolicy(0.5), unrelatedPolicy()));

    PolicyImplementer implementer = mock(PolicyImplementer.class);
    PolicyValidator validator = mock(PolicyValidator.class);
    when(validator.getPolicyType()).thenReturn(TraceSamplingRatePolicy.POLICY_TYPE);
    when(implementer.getValidators()).thenReturn(singletonList(validator));

    store.registerImplementer(implementer);

    verify(implementer).onPoliciesChanged(singletonList(tracePolicy(0.5)));
  }

  @Test
  void updatePoliciesNotifiesRegisteredImplementerWithRelevantPolicies() {
    PolicyStore store = new PolicyStore();
    PolicyImplementer implementer = traceSamplingImplementer();

    store.registerImplementer(implementer);
    clearInvocations(implementer);
    store.updatePolicies(Arrays.asList(unrelatedPolicy(), tracePolicy(0.25)));

    verify(implementer).onPoliciesChanged(singletonList(tracePolicy(0.25)));
  }

  @Test
  void updatePoliciesNotifiesDeletedPolicyWhenPolicyDisappears() {
    PolicyStore store = new PolicyStore();
    PolicyImplementer implementer = traceSamplingImplementer();
    TraceSamplingRatePolicy removedPolicy = tracePolicy(0.5);
    store.updatePolicies(singletonList(removedPolicy));
    store.registerImplementer(implementer);
    clearInvocations(implementer);

    assertThat(store.updatePolicies(Collections.emptyList())).isTrue();

    verify(implementer)
        .onPoliciesChanged(
            argThat(
                policies ->
                    policies.size() == 1
                        && policies.get(0).isDeleted()
                        && policies.get(0).getIdentity().equals(removedPolicy.getIdentity())
                        && policies.get(0).getType().equals(removedPolicy.getType())));
    assertThat(store.getPolicies()).isEmpty();
  }

  @Test
  void updatePoliciesDoesNotNotifyDeletedPolicyWhenPolicyValueChangesWithSameId() {
    PolicyStore store = new PolicyStore();
    PolicyImplementer implementer = traceSamplingImplementer();
    TraceSamplingRatePolicy updatedPolicy = tracePolicy(0.75);
    store.updatePolicies(singletonList(tracePolicy(0.5)));
    store.registerImplementer(implementer);
    clearInvocations(implementer);

    assertThat(store.updatePolicies(singletonList(updatedPolicy))).isTrue();

    verify(implementer).onPoliciesChanged(singletonList(updatedPolicy));
  }

  @Test
  void updatePoliciesNotifiesDeletedPolicyWhenChangedPolicyLaterDisappears() {
    PolicyStore store = new PolicyStore();
    PolicyImplementer implementer = traceSamplingImplementer();
    TraceSamplingRatePolicy updatedPolicy = tracePolicy(0.75);
    store.updatePolicies(singletonList(tracePolicy(0.5)));
    store.updatePolicies(singletonList(updatedPolicy));
    store.registerImplementer(implementer);
    clearInvocations(implementer);

    assertThat(store.updatePolicies(Collections.emptyList())).isTrue();

    verify(implementer)
        .onPoliciesChanged(
            argThat(
                policies ->
                    policies.size() == 1
                        && policies.get(0).isDeleted()
                        && policies.get(0).getIdentity().equals(updatedPolicy.getIdentity())
                        && policies.get(0).getType().equals(updatedPolicy.getType())));
    assertThat(store.getPolicies()).isEmpty();
  }

  @Test
  void updatePoliciesContinuesWhenImplementerThrows() {
    PolicyStore store = new PolicyStore();
    PolicyImplementer failingImplementer = traceSamplingImplementer();
    PolicyImplementer nextImplementer = traceSamplingImplementer();
    List<TelemetryPolicy> updatedPolicies = singletonList(tracePolicy(0.25));
    doThrow(new IllegalStateException("boom"))
        .when(failingImplementer)
        .onPoliciesChanged(updatedPolicies);

    store.registerImplementer(failingImplementer);
    store.registerImplementer(nextImplementer);
    clearInvocations(failingImplementer, nextImplementer);

    assertThat(store.updatePolicies(updatedPolicies)).isTrue();

    verify(failingImplementer).onPoliciesChanged(updatedPolicies);
    verify(nextImplementer).onPoliciesChanged(updatedPolicies);
    assertThat(store.getPolicies()).isEqualTo(updatedPolicies);
  }

  @Test
  void registerImplementerContinuesAfterPreviousImplementerThrows() {
    PolicyStore store = new PolicyStore();
    List<TelemetryPolicy> currentPolicies = singletonList(tracePolicy(0.5));
    assertThat(store.updatePolicies(currentPolicies)).isTrue();

    PolicyImplementer failingImplementer = traceSamplingImplementer();
    PolicyImplementer nextImplementer = traceSamplingImplementer();
    doThrow(new IllegalStateException("boom"))
        .when(failingImplementer)
        .onPoliciesChanged(currentPolicies);

    store.registerImplementer(failingImplementer);
    store.registerImplementer(nextImplementer);

    verify(failingImplementer).onPoliciesChanged(currentPolicies);
    verify(nextImplementer).onPoliciesChanged(currentPolicies);
  }

  private static PolicyImplementer traceSamplingImplementer() {
    PolicyImplementer implementer = mock(PolicyImplementer.class);
    PolicyValidator validator = mock(PolicyValidator.class);
    when(validator.getPolicyType()).thenReturn(TraceSamplingRatePolicy.POLICY_TYPE);
    when(implementer.getValidators()).thenReturn(singletonList(validator));
    return implementer;
  }

  private static TraceSamplingRatePolicy tracePolicy(double probability) {
    return tracePolicyWithId("trace-policy", probability);
  }

  private static TraceSamplingRatePolicy tracePolicyWithId(String id, double probability) {
    return new TraceSamplingRatePolicy(id, "Trace policy " + id, probability);
  }

  private static TelemetryPolicy unrelatedPolicy() {
    return new TestTelemetryPolicy("other-policy", "Other policy", "other-policy");
  }

  private static final class TestTelemetryPolicy implements TelemetryPolicy {
    private final TelemetryPolicyIdentity identity;
    private final String type;

    private TestTelemetryPolicy(String id, String name, String type) {
      this.identity = new TelemetryPolicyIdentity(id, name);
      this.type = type;
    }

    @Override
    public TelemetryPolicyIdentity getIdentity() {
      return identity;
    }

    @Override
    public String getType() {
      return type;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (!(obj instanceof TestTelemetryPolicy)) {
        return false;
      }
      TestTelemetryPolicy that = (TestTelemetryPolicy) obj;
      return identity.equals(that.identity) && type.equals(that.type);
    }

    @Override
    public int hashCode() {
      return Objects.hash(identity, type);
    }
  }
}
