/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.metricsexport;

import io.opentelemetry.contrib.dynamic.policy.PolicyImplementer;
import io.opentelemetry.contrib.dynamic.policy.PolicyValidator;
import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicy;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/** Applies metric export enabled policy updates to all delegating metric exporters. */
public final class MetricExportEnabledPolicyImplementer implements PolicyImplementer {
  private static final Logger logger =
      Logger.getLogger(MetricExportEnabledPolicyImplementer.class.getName());
  private static final List<PolicyValidator> VALIDATORS =
      Collections.<PolicyValidator>singletonList(new MetricExportEnabledValidator());

  @Override
  public List<PolicyValidator> getValidators() {
    return VALIDATORS;
  }

  @Override
  public void onPoliciesChanged(List<TelemetryPolicy> policies) {
    for (TelemetryPolicy policy : policies) {
      if (!MetricExportEnabledPolicy.POLICY_TYPE.equals(policy.getType())) {
        continue;
      }
      if (!(policy instanceof MetricExportEnabledPolicy)) {
        DelegatingMetricExporter.setExportEnabled(true);
        logger.info("Applied metric export policy reset: enabled=true");
        continue;
      }
      boolean enabled = ((MetricExportEnabledPolicy) policy).isEnabled();
      DelegatingMetricExporter.setExportEnabled(enabled);
      logger.info("Applied metric export policy update: enabled=" + enabled);
    }
  }
}
