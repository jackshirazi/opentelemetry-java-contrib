/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.traceexport;

import io.opentelemetry.contrib.dynamic.policy.PolicyImplementer;
import io.opentelemetry.contrib.dynamic.policy.PolicyValidator;
import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicy;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/** Applies trace export enabled policy updates to all delegating span exporters. */
public final class TraceExportEnabledPolicyImplementer implements PolicyImplementer {
  private static final Logger logger =
      Logger.getLogger(TraceExportEnabledPolicyImplementer.class.getName());
  private static final List<PolicyValidator> VALIDATORS =
      Collections.<PolicyValidator>singletonList(new TraceExportEnabledValidator());

  @Override
  public List<PolicyValidator> getValidators() {
    return VALIDATORS;
  }

  @Override
  public void onPoliciesChanged(List<TelemetryPolicy> policies) {
    for (TelemetryPolicy policy : policies) {
      if (!TraceExportEnabledPolicy.POLICY_TYPE.equals(policy.getType())) {
        continue;
      }
      if (!(policy instanceof TraceExportEnabledPolicy)) {
        DelegatingSpanExporter.setExportEnabled(true);
        logger.info("Applied trace export policy reset: enabled=true");
        continue;
      }
      boolean enabled = ((TraceExportEnabledPolicy) policy).isEnabled();
      DelegatingSpanExporter.setExportEnabled(enabled);
      logger.info("Applied trace export policy update: enabled=" + enabled);
    }
  }
}
