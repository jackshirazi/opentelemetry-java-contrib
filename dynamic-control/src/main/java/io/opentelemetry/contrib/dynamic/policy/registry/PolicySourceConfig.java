/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.registry;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.contrib.dynamic.policy.source.SourceFormat;
import io.opentelemetry.contrib.dynamic.policy.source.SourceKind;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** Declares one policy source with its format and policy mappings. */
public final class PolicySourceConfig {
  private static final String DEFAULT_SENTINEL = "DEFAULT";
  private static final String OPAMP_DEFAULT_LOCATION = "";
  private static final String HTTP_DEFAULT_LOCATION = "http://localhost:80";
  private static final String FILE_DEFAULT_NAME = "policies.conf";

  private final SourceKind kind;
  private final SourceFormat format;
  private final String location;
  private final List<PolicyMappingTypeAndKey> mappings;

  @JsonCreator
  public PolicySourceConfig(
      @JsonProperty("kind") SourceKind kind,
      @JsonProperty("format") SourceFormat format,
      @JsonProperty("location") String location,
      @JsonProperty("mappings") List<PolicyMappingTypeAndKey> mappings) {
    this.kind = Objects.requireNonNull(kind, "kind cannot be null");
    this.format = Objects.requireNonNull(format, "format cannot be null");
    this.location = requireNonEmpty(location, "location cannot be null");
    this.mappings =
        mappings == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(mappings));
  }

  public SourceKind getKind() {
    return kind;
  }

  public SourceFormat getFormat() {
    return format;
  }

  public String getConfiguredLocation() {
    return location;
  }

  /** Returns source location with DEFAULT resolved by source kind. */
  public String getResolvedLocation() {
    if (!isDefaultLocation(location)) {
      return location;
    }
    if (kind == SourceKind.OPAMP) {
      return OPAMP_DEFAULT_LOCATION;
    }
    if (kind == SourceKind.HTTP) {
      return HTTP_DEFAULT_LOCATION;
    }
    if (kind == SourceKind.FILE) {
      return resolveDefaultFileLocation();
    }
    throw new IllegalArgumentException(
        "No DEFAULT location is defined for source kind '"
            + kind.toConfigValue()
            + "'. Use an explicit location (recommended for custom sources).");
  }

  public List<PolicyMappingTypeAndKey> getMappings() {
    return mappings;
  }

  private static boolean isDefaultLocation(String location) {
    return DEFAULT_SENTINEL.equalsIgnoreCase(location);
  }

  @CanIgnoreReturnValue
  private static String requireNonEmpty(String value, String fieldName) {
    Objects.requireNonNull(value, fieldName + " cannot be null");
    if (value.trim().isEmpty()) {
      throw new IllegalArgumentException(fieldName + " cannot be empty");
    }
    return value;
  }

  private static String resolveDefaultFileLocation() {
    try {
      Path codeLocation =
          Paths.get(
              PolicySourceConfig.class
                  .getProtectionDomain()
                  .getCodeSource()
                  .getLocation()
                  .toURI());
      Path extensionDir = codeLocation.toFile().isFile() ? codeLocation.getParent() : codeLocation;
      if (extensionDir != null) {
        return extensionDir.resolve(FILE_DEFAULT_NAME).toAbsolutePath().normalize().toString();
      }
    } catch (URISyntaxException | RuntimeException ignored) {
      // Fall through to working-directory fallback.
    }
    return Paths.get(FILE_DEFAULT_NAME).toAbsolutePath().normalize().toString();
  }
}
