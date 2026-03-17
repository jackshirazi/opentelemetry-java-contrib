/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.registry.yaml;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.opentelemetry.contrib.dynamic.policy.registry.json.PolicyInitConfig;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

/** Reads the policy init mapping configuration file. */
public final class PolicyInitConfigReader {
  private static final ObjectMapper MAPPER = new ObjectMapper(new YAMLFactory());

  public PolicyInitConfig read(Path path) throws IOException {
    Objects.requireNonNull(path, "path cannot be null");
    return MAPPER.readValue(path.toFile(), PolicyInitConfig.class);
  }
}
