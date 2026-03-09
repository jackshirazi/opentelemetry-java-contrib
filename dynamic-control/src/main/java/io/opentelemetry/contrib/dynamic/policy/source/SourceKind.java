/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.source;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Locale;

/** Supported policy source kinds. */
public enum SourceKind {
  OPAMP,
  FILE,
  HTTP,
  CUSTOM;

  @JsonCreator
  public static SourceKind fromConfigValue(String value) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException("kind cannot be null or empty");
    }
    String normalized = value.trim().toUpperCase(Locale.ROOT);
    try {
      return SourceKind.valueOf(normalized);
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException(
          "Unsupported source kind '"
              + value
              + "'. Supported kinds are: opamp, file, http, custom.",
          e);
    }
  }

  @JsonValue
  public String toConfigValue() {
    return name().toLowerCase(Locale.ROOT);
  }
}
