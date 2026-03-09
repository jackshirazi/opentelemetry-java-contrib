/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.logsexport;

import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import java.util.Collection;
import java.util.Objects;

/** A {@link LogRecordExporter} wrapper that can enable/disable exports dynamically. */
public final class DelegatingLogRecordExporter implements LogRecordExporter {
  private static volatile boolean exportEnabled = true;

  private volatile LogRecordExporter delegate;

  public DelegatingLogRecordExporter(LogRecordExporter initialDelegate) {
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
  public CompletableResultCode export(Collection<LogRecordData> logs) {
    if (exportEnabled) {
      return delegate.export(logs);
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
