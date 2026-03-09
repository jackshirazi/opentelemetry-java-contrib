/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.logsexport;

import io.opentelemetry.contrib.dynamic.policy.PolicyImplementer;
import io.opentelemetry.contrib.dynamic.policy.PolicyValidator;
import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicy;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/** Applies log export enabled policy updates to all delegating log exporters. */
public final class LogExportEnabledPolicyImplementer implements PolicyImplementer {
  private static final Logger logger =
      Logger.getLogger(LogExportEnabledPolicyImplementer.class.getName());
  private static final List<PolicyValidator> VALIDATORS =
      Collections.<PolicyValidator>singletonList(new LogExportEnabledValidator());

  @Override
  public List<PolicyValidator> getValidators() {
    return VALIDATORS;
  }

  @Override
  public void onPoliciesChanged(List<TelemetryPolicy> policies) {
    for (TelemetryPolicy policy : policies) {
      if (!LogExportEnabledPolicy.POLICY_TYPE.equals(policy.getType())) {
        continue;
      }
      if (!(policy instanceof LogExportEnabledPolicy)) {
        DelegatingLogRecordExporter.setExportEnabled(true);
        logger.info("Applied log export policy reset: enabled=true");
        continue;
      }
      boolean enabled = ((LogExportEnabledPolicy) policy).isEnabled();
      DelegatingLogRecordExporter.setExportEnabled(enabled);
      logger.info("Applied log export policy update: enabled=" + enabled);
    }
  }
}
