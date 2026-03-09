/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.traceexport;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SpanExporter;
import java.util.Collection;
import java.util.Objects;

/** A {@link SpanExporter} wrapper that can enable/disable exports dynamically. */
public final class DelegatingSpanExporter implements SpanExporter {
  private static volatile boolean exportEnabled = true;

  private volatile SpanExporter delegate;

  public DelegatingSpanExporter(SpanExporter initialDelegate) {
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
  public CompletableResultCode export(Collection<SpanData> spans) {
    if (exportEnabled) {
      return delegate.export(spans);
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
}
