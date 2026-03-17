/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.contrib.dynamic.policy.opamppolling.OpampPollingIntervalPolicy;
import io.opentelemetry.contrib.dynamic.policy.opamppolling.OpampPollingIntervalPolicyImplementer;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class OpampPollingIntervalPolicyImplementerTest {
  @AfterEach
  void cleanup() {
    OpampPolicyProvider.resetForTest();
  }

  @Test
  void appliesConfiguredInterval() {
    OpampPollingIntervalPolicyImplementer implementer = new OpampPollingIntervalPolicyImplementer();

    implementer.onPoliciesChanged(
        singletonList(new OpampPollingIntervalPolicy(Duration.ofSeconds(2))));

    assertThat(OpampPolicyProvider.getGlobalPollingIntervalForTest())
        .isEqualTo(Duration.ofSeconds(2));
  }

  @Test
  void typeOnlyPolicyResetsToDefault() {
    OpampPollingIntervalPolicyImplementer implementer = new OpampPollingIntervalPolicyImplementer();
    OpampPolicyProvider.setGlobalPollingInterval(Duration.ofSeconds(5));

    implementer.onPoliciesChanged(
        singletonList(new TelemetryPolicy(OpampPollingIntervalPolicy.POLICY_TYPE)));

    assertThat(OpampPolicyProvider.getGlobalPollingIntervalForTest())
        .isEqualTo(OpampPollingIntervalPolicy.DEFAULT_POLLING_INTERVAL);
  }
}
