/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import java.util.List;
import java.util.function.Consumer;

public interface PolicyProvider {
  /** Retrieves the full list of policies from this provider. */
  List<TelemetryPolicy> fetchPolicies() throws Exception;

  /** Optional: for push mechanisms. */
  default void startWatching(Consumer<List<TelemetryPolicy>> onUpdate) {}
}
