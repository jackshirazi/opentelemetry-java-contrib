/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.logsexport;

import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicy;
import io.opentelemetry.contrib.dynamic.policy.registry.PolicyRegistry;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import java.util.Objects;

/** Policy that enables/disables log export dynamically. */
public final class LogExportEnabledPolicy extends TelemetryPolicy {
  public static final String POLICY_TYPE = "log_export_enabled_policy";

  private final boolean enabled;

  public LogExportEnabledPolicy(boolean enabled) {
    super(POLICY_TYPE);
    this.enabled = enabled;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public static void registerPolicyType() {
    PolicyRegistry.registerPolicyType(POLICY_TYPE, LogExportEnabledPolicy.class);
  }

  public static void initialize(AutoConfigurationCustomizer autoConfiguration) {
    Objects.requireNonNull(autoConfiguration, "autoConfiguration cannot be null");
    autoConfiguration.addLogRecordExporterCustomizer(
        (logRecordExporter, config) -> new DelegatingLogRecordExporter(logRecordExporter));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof LogExportEnabledPolicy)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    LogExportEnabledPolicy that = (LogExportEnabledPolicy) o;
    return that.canEqual(this) && enabled == that.enabled;
  }

  @Override
  protected boolean canEqual(Object other) {
    return other instanceof LogExportEnabledPolicy;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), enabled);
  }
}
