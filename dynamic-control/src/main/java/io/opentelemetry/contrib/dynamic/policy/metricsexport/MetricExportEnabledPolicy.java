/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.metricsexport;

import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicy;
import io.opentelemetry.contrib.dynamic.policy.registry.PolicyRegistry;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import java.util.Objects;

/** Policy that enables/disables metric export dynamically. */
public final class MetricExportEnabledPolicy extends TelemetryPolicy {
  public static final String POLICY_TYPE = "metric_export_enabled_policy";

  private final boolean enabled;

  public MetricExportEnabledPolicy(boolean enabled) {
    super(POLICY_TYPE);
    this.enabled = enabled;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public static void registerPolicyType() {
    PolicyRegistry.registerPolicyType(POLICY_TYPE, MetricExportEnabledPolicy.class);
  }

  public static void initialize(AutoConfigurationCustomizer autoConfiguration) {
    Objects.requireNonNull(autoConfiguration, "autoConfiguration cannot be null");
    autoConfiguration.addMetricExporterCustomizer(
        (metricExporter, config) -> new DelegatingMetricExporter(metricExporter));
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof MetricExportEnabledPolicy)) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }
    MetricExportEnabledPolicy that = (MetricExportEnabledPolicy) o;
    return that.canEqual(this) && enabled == that.enabled;
  }

  @Override
  protected boolean canEqual(Object other) {
    return other instanceof MetricExportEnabledPolicy;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), enabled);
  }
}
