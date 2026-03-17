/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.registry.yaml;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.contrib.dynamic.policy.registry.json.PolicyInitConfig;
import io.opentelemetry.contrib.dynamic.policy.source.SourceFormat;
import io.opentelemetry.contrib.dynamic.policy.source.SourceKind;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PolicyInitConfigReaderTest {

  @TempDir Path tempDir;

  @Test
  void readsSourceAndMappingsFromYamlFile() throws Exception {
    Path configFile = tempDir.resolve("policy-init.yaml");
    String yaml =
        "sources:\n"
            + "  - kind: opamp\n"
            + "    format: jsonkeyvalue\n"
            + "    location: wss://opamp.example.com/v1\n"
            + "    mappings:\n"
            + "      - sourceKey: sampling_rate\n"
            + "        policyType: trace-sampling\n"
            + "  - kind: file\n"
            + "    format: keyvalue\n"
            + "    location: DEFAULT\n"
            + "    mappings:\n"
            + "      - sourceKey: send_logs\n"
            + "        policyType: send_logs\n";
    Files.write(configFile, yaml.getBytes(StandardCharsets.UTF_8));

    PolicyInitConfig config = new PolicyInitConfigReader().read(configFile);

    assertThat(config.getSources()).hasSize(2);
    assertThat(config.getSources().get(0).getKind()).isEqualTo(SourceKind.OPAMP);
    assertThat(config.getSources().get(0).getFormat()).isEqualTo(SourceFormat.JSONKEYVALUE);
    assertThat(config.getSources().get(0).getMappings()).hasSize(1);
    assertThat(config.getSources().get(0).getMappings().get(0).getSourceKey())
        .isEqualTo("sampling_rate");
    assertThat(config.getSources().get(0).getMappings().get(0).getConfiguredPolicyType())
        .isEqualTo("trace-sampling");
    assertThat(config.getSources().get(0).getConfiguredLocation())
        .isEqualTo("wss://opamp.example.com/v1");
    assertThat(config.getSources().get(0).getResolvedLocation())
        .isEqualTo("wss://opamp.example.com/v1");
    assertThat(config.getSources().get(1).getConfiguredLocation()).isEqualTo("DEFAULT");
    assertThat(config.getSources().get(1).getResolvedLocation())
        .endsWith(File.separator + "policies.conf");
  }
}
