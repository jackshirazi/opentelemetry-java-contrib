/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import java.util.List;

public interface PolicyImplementer {
  void onPoliciesChanged(List<TelemetryPolicy> policies);

  List<PolicyValidater> getValidators();
}
