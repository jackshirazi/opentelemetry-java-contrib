/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import io.opentelemetry.contrib.dynamic.sampler.DelegatingSampler;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import java.nio.file.Paths;
import java.util.Collections;

public final class TelemetryPolicyConfig {

  public static void init(AutoConfigurationCustomizer autoConfiguration) {
    autoConfiguration.addSamplerCustomizer(
        (sampler, config) -> {
          String flatFile = config.getString("otel.java.experimental.telemetry.policy.flatfile");
          if (flatFile == null) {
            return sampler;
          }

          DelegatingSampler delegatingSampler = new DelegatingSampler(sampler);
          TraceSamplingRatePolicyImplementer implementer =
              new TraceSamplingRatePolicyImplementer(delegatingSampler);

          PolicyStore store = new PolicyStore();
          store.registerImplementer(implementer);

          LinePerPolicyFileProvider provider =
              new LinePerPolicyFileProvider(Paths.get(flatFile), implementer.getValidators());

          TelemetryPolicyManager manager =
              new TelemetryPolicyManager(Collections.singletonList(provider), store);
          manager.start();

          return delegatingSampler;
        });
  }

  private TelemetryPolicyConfig() {}
}
