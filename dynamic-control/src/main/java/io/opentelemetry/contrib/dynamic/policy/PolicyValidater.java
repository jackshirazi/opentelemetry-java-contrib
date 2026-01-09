/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import javax.annotation.Nullable;

public interface PolicyValidater {
  // Standard validation: input is a JSON string containing the policy
  @Nullable
  TelemetryPolicy validate(String json);

  // Alias validation: key matches alias, value is property value
  @Nullable
  TelemetryPolicy validateAlias(String key, String value);

  String getPolicyType();

  String getAlias();
}
