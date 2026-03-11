/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.traceexport;

import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicy;
import io.opentelemetry.contrib.dynamic.policy.registry.PolicyRegistry;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import java.util.Objects;

/** Policy that enables/disables trace export dynamically. */
public final class TraceExportEnabledPolicy extends TelemetryPolicy {
  public static final String POLICY_TYPE = "trace_export_enabled_policy";

  private final boolean enabled;

  public TraceExportEnabledPolicy(boolean enabled) {
    super(POLICY_TYPE);
    this.enabled = enabled;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public static void registerPolicyType() {
    PolicyRegistry.registerPolicyType(POLICY_TYPE, TraceExportEnabledPolicy.class);
  }

  public static void initialize(AutoConfigurationCustomizer autoConfiguration) {
    Objects.requireNonNull(autoConfiguration, "autoConfiguration cannot be null");
    autoConfiguration.addSpanExporterCustomizer(
        (spanExporter, config) -> new DelegatingSpanExporter(spanExporter));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof TraceExportEnabledPolicy)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    TraceExportEnabledPolicy that = (TraceExportEnabledPolicy) o;
    return that.canEqual(this) && enabled == that.enabled;
  }

  @Override
  protected boolean canEqual(Object other) {
    return other instanceof TraceExportEnabledPolicy;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), enabled);
  }
}
