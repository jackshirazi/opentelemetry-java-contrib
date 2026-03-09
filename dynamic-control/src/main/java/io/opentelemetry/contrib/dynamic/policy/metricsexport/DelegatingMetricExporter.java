/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.metricsexport;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.InstrumentType;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import java.util.Collection;
import java.util.Objects;

/** A {@link MetricExporter} wrapper that can enable/disable exports dynamically. */
public final class DelegatingMetricExporter implements MetricExporter {
  private static volatile boolean exportEnabled = true;

  private volatile MetricExporter delegate;

  public DelegatingMetricExporter(MetricExporter initialDelegate) {
    this.delegate = Objects.requireNonNull(initialDelegate, "initialDelegate cannot be null");
  }

  public static void setExportEnabled(boolean enabled) {
    exportEnabled = enabled;
  }

  public static boolean isExportEnabled() {
    return exportEnabled;
  }

  public static void resetForTest() {
    exportEnabled = true;
  }

  @Override
  public CompletableResultCode export(Collection<MetricData> metrics) {
    if (exportEnabled) {
      return delegate.export(metrics);
    }
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode flush() {
    return delegate.flush();
  }

  @Override
  public CompletableResultCode shutdown() {
    return delegate.shutdown();
  }

  @Override
  public AggregationTemporality getAggregationTemporality(InstrumentType instrumentType) {
    return delegate.getAggregationTemporality(instrumentType);
  }
}
