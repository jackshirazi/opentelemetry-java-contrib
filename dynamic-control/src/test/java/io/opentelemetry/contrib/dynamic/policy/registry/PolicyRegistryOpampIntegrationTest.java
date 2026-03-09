/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.contrib.dynamic.policy.tracesampling.TraceSamplingRatePolicy;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Function;
import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import okio.Buffer;
import okio.ByteString;
import opamp.proto.AgentConfigFile;
import opamp.proto.AgentConfigMap;
import opamp.proto.AgentRemoteConfig;
import opamp.proto.ServerToAgent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PolicyRegistryOpampIntegrationTest {

  @TempDir Path tempDir;

  @AfterEach
  void cleanup() {
    PolicyRegistry.resetForTest();
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  @Test
  void opampSourceUpdatesDelegatingSamplerFromRemoteConfig() throws Exception {
    try (MockWebServer server = new MockWebServer()) {
      server.start();
      server.enqueue(createServerToAgentResponse("{\"sampling_rate\":0.0}"));

      Path configPath = tempDir.resolve("policy-init.json");
      String configJson =
          "{\n"
              + "  \"sources\": [\n"
              + "    {\n"
              + "      \"kind\": \"opamp\",\n"
              + "      \"format\": \"json\",\n"
              + "      \"location\": \"bilbo\",\n"
              + "      \"mappings\": [\n"
              + "        { \"sourceKey\": \"sampling_rate\", \"policyType\": \"TraceSamplingRatePolicy\" }\n"
              + "      ]\n"
              + "    }\n"
              + "  ]\n"
              + "}\n";
      Files.write(configPath, configJson.getBytes(StandardCharsets.UTF_8));

      AtomicReference<Function<ConfigProperties, Map<String, String>>> propertiesCustomizerRef =
          new AtomicReference<>();
      AtomicReference<BiFunction<Sampler, ConfigProperties, Sampler>> samplerCustomizerRef =
          new AtomicReference<>();

      AutoConfigurationCustomizer autoConfiguration = mock(AutoConfigurationCustomizer.class);
      when(autoConfiguration.addPropertiesCustomizer(any()))
          .thenAnswer(
              invocation -> {
                propertiesCustomizerRef.set(invocation.getArgument(0));
                return autoConfiguration;
              });
      when(autoConfiguration.addSamplerCustomizer(any()))
          .thenAnswer(
              invocation -> {
                samplerCustomizerRef.set(invocation.getArgument(0));
                return autoConfiguration;
              });

      ConfigProperties config = mock(ConfigProperties.class);
      when(config.getString("otel.java.experimental.telemetry.policy.init"))
          .thenReturn(configPath.toString());
      when(config.getString("otel.opamp.service.url")).thenReturn(server.url("/").toString());
      when(config.getString("otel.service.name")).thenReturn("test-service");
      when(config.getMap("otel.resource.attributes")).thenReturn(Collections.emptyMap());
      when(config.getMap("otel.experimental.opamp.headers")).thenReturn(Collections.emptyMap());

      PolicyRegistry.init(autoConfiguration);
      Function<ConfigProperties, Map<String, String>> propertiesCustomizer =
          propertiesCustomizerRef.get();
      assertThat(propertiesCustomizer).isNotNull();
      Map<String, String> unused = propertiesCustomizer.apply(config);
      assertThat(unused).isEmpty();

      BiFunction<Sampler, ConfigProperties, Sampler> samplerCustomizer = samplerCustomizerRef.get();
      assertThat(samplerCustomizer).isNotNull();
      Sampler sampler = samplerCustomizer.apply(Sampler.alwaysOn(), config);

      boolean updated = waitForDropDecision(sampler, Duration.ofSeconds(5));
      assertThat(updated).isTrue();
      assertThat(TraceSamplingRatePolicy.getInitializedSampler()).isNotNull();
    }
  }

  private static MockResponse createServerToAgentResponse(String policyJson) {
    AgentConfigFile configFile =
        new AgentConfigFile.Builder().body(ByteString.encodeUtf8(policyJson)).build();
    AgentConfigMap configMap =
        new AgentConfigMap.Builder().config_map(Collections.singletonMap("bilbo", configFile)).build();
    AgentRemoteConfig remoteConfig = new AgentRemoteConfig.Builder().config(configMap).build();
    ServerToAgent response = new ServerToAgent.Builder().remote_config(remoteConfig).build();

    Buffer body = new Buffer();
    body.write(response.encode());
    return new MockResponse.Builder().code(200).body(body).build();
  }

  private static boolean waitForDropDecision(Sampler sampler, Duration timeout)
      throws InterruptedException {
    long deadline = System.nanoTime() + timeout.toNanos();
    while (System.nanoTime() < deadline) {
      SamplingDecision decision =
          sampler
              .shouldSample(
                  Context.root(),
                  "0123456789abcdef0123456789abcdef",
                  "test-span",
                  SpanKind.INTERNAL,
                  Attributes.empty(),
                  Collections.emptyList())
              .getDecision();
      if (SamplingDecision.DROP.equals(decision)) {
        return true;
      }
      Thread.sleep(50L);
    }
    return SamplingDecision.DROP.equals(
        sampler
            .shouldSample(
                Context.root(),
                "0123456789abcdef0123456789abcdef",
                "test-span",
                SpanKind.INTERNAL,
                Attributes.empty(),
                Collections.emptyList())
            .getDecision());
  }
}
