/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import com.fasterxml.jackson.databind.JsonNode;
import io.opentelemetry.contrib.dynamic.sampler.DelegatingSampler;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import java.util.Collections;
import java.util.List;

public final class TraceSamplingRatePolicyImplementer implements PolicyImplementer {

  private final DelegatingSampler delegatingSampler;

  public TraceSamplingRatePolicyImplementer(DelegatingSampler delegatingSampler) {
    this.delegatingSampler = delegatingSampler;
  }

  @Override
  public List<PolicyValidater> getValidators() {
    return Collections.singletonList(new TraceSamplingValidater());
  }

  @Override
  public void onPoliciesChanged(List<TelemetryPolicy> policies) {
    for (TelemetryPolicy policy : policies) {
      if ("trace-sampling".equals(policy.getType())) {
        JsonNode spec = policy.getSpec();
        if (spec == null) {
          delegatingSampler.setDelegate(Sampler.alwaysOn());
          continue;
        }
        if (spec.has("probability")) {
          double ratio = spec.get("probability").asDouble(1.0);
          Sampler sampler = Sampler.parentBased(Sampler.traceIdRatioBased(ratio));
          delegatingSampler.setDelegate(sampler);
        }
      }
    }
  }
}
