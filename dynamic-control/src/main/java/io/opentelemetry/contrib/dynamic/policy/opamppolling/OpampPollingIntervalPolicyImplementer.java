/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.opamppolling;

import io.opentelemetry.contrib.dynamic.policy.OpampPolicyProvider;
import io.opentelemetry.contrib.dynamic.policy.PolicyImplementer;
import io.opentelemetry.contrib.dynamic.policy.PolicyValidator;
import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicy;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/** Applies OpAMP polling interval updates to active OpAMP providers. */
public final class OpampPollingIntervalPolicyImplementer implements PolicyImplementer {
  private static final Logger logger =
      Logger.getLogger(OpampPollingIntervalPolicyImplementer.class.getName());
  private static final List<PolicyValidator> VALIDATORS =
      Collections.<PolicyValidator>singletonList(new OpampPollingIntervalValidator());

  @Override
  public List<PolicyValidator> getValidators() {
    return VALIDATORS;
  }

  @Override
  public void onPoliciesChanged(List<TelemetryPolicy> policies) {
    for (TelemetryPolicy policy : policies) {
      if (!OpampPollingIntervalPolicy.POLICY_TYPE.equals(policy.getType())) {
        continue;
      }
      if (!(policy instanceof OpampPollingIntervalPolicy)) {
        Duration fallback = OpampPollingIntervalPolicy.DEFAULT_POLLING_INTERVAL;
        OpampPolicyProvider.setGlobalPollingInterval(fallback);
        logger.info("Applied OpAMP polling interval reset: " + fallback);
        continue;
      }
      Duration interval = ((OpampPollingIntervalPolicy) policy).getInterval();
      OpampPolicyProvider.setGlobalPollingInterval(interval);
      logger.info("Applied OpAMP polling interval update: " + interval);
    }
  }
}
