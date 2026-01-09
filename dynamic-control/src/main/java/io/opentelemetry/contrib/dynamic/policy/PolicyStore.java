/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class PolicyStore {

  private final AtomicReference<List<TelemetryPolicy>> current =
      new AtomicReference<>(Collections.unmodifiableList(new ArrayList<>()));

  private final List<PolicyImplementer> implementers = new CopyOnWriteArrayList<>();
  private final Map<PolicyImplementer, List<TelemetryPolicy>> implementerStates = new ConcurrentHashMap<>();

  public void updatePolicies(List<TelemetryPolicy> newPolicies) {
    current.set(newPolicies);
    for (PolicyImplementer implementer : implementers) {
      notifyImplementerIfChanged(implementer, newPolicies);
    }
  }

  public void registerImplementer(PolicyImplementer implementer) {
    implementers.add(implementer);
    List<TelemetryPolicy> policies = current.get();
    if (policies != null) {
      notifyImplementerIfChanged(implementer, policies);
    }
  }

  private void notifyImplementerIfChanged(PolicyImplementer implementer, List<TelemetryPolicy> policies) {
    Set<String> interestingTypes =
        implementer.getValidators().stream()
            .map(PolicyValidater::getPolicyType)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    List<TelemetryPolicy> relevant =
        policies.stream()
            .filter(p -> interestingTypes.contains(p.getType()))
            .collect(Collectors.toList());

    List<TelemetryPolicy> last = implementerStates.getOrDefault(implementer, Collections.emptyList());
    if (!new HashSet<>(relevant).equals(new HashSet<>(last))) {
      implementerStates.put(implementer, relevant);

      List<TelemetryPolicy> changes = new ArrayList<>();

      // Added or Modified
      for (TelemetryPolicy p : relevant) {
        if (!last.contains(p)) {
          changes.add(p);
        }
      }

      // Removed
      Set<String> currentTypes =
          relevant.stream().map(TelemetryPolicy::getType).collect(Collectors.toSet());
      for (TelemetryPolicy p : last) {
        if (!currentTypes.contains(p.getType())) {
          changes.add(new TelemetryPolicy(p.getType(), null));
        }
      }

      if (!changes.isEmpty()) {
        implementer.onPoliciesChanged(changes);
      }
    }
  }
}
