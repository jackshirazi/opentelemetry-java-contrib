/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public final class LinePerPolicyFileProvider implements PolicyProvider {
  private static final Logger logger = Logger.getLogger(LinePerPolicyFileProvider.class.getName());
  private final Path file;
  private final List<PolicyValidater> validators;

  public LinePerPolicyFileProvider(Path file, List<PolicyValidater> validators) {
    this.file = file;
    this.validators = new ArrayList<>(validators);
  }

  @Override
  public List<TelemetryPolicy> fetchPolicies() throws IOException {
    List<TelemetryPolicy> policies = new ArrayList<>();
    if (!Files.exists(file)) {
      return policies;
    }

    List<String> lines = Files.readAllLines(file);
    for (String line : lines) {
      String trimmedLine = line.trim();
      if (trimmedLine.isEmpty()) {
        continue;
      }

      TelemetryPolicy policy = null;

      if (trimmedLine.startsWith("{")) {
        for (PolicyValidater validator : validators) {
          if (trimmedLine.contains("\"" + validator.getPolicyType() + "\"")) {
            policy = validator.validate(trimmedLine);
            if (policy != null) {
              break;
            }
          }
        }
      } else {
        int idx = trimmedLine.indexOf('=');
        if (idx > 0) {
          String key = trimmedLine.substring(0, idx).trim();
          String valueStr = trimmedLine.substring(idx + 1).trim();

          for (PolicyValidater validator : validators) {
            String alias = validator.getAlias();
            if (alias != null && alias.equals(key)) {
              policy = validator.validateAlias(key, valueStr);
              if (policy != null) {
                break;
              }
            }
          }
        }
      }

      if (policy == null) {
        logger.info("No validator found for line: " + trimmedLine);
        continue;
      }

      policies.add(policy);
    }
    return policies;
  }
}
