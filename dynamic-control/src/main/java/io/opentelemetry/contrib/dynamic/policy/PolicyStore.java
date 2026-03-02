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

/**
 * Maintains the current set of policies and notifies implementers of changes.
 *
 * <p>Implementers are notified only for the policy types they declare support for via {@link
 * PolicyImplementer#getValidators()}.
 */
final class PolicyStore {

  private final AtomicReference<List<TelemetryPolicy>> current =
      new AtomicReference<>(Collections.unmodifiableList(new ArrayList<>()));

  private final List<PolicyImplementer> implementers = new CopyOnWriteArrayList<>();
  private final Map<PolicyImplementer, List<TelemetryPolicy>> implementerStates =
      new ConcurrentHashMap<>();

  /**
   * Replaces the current policies and notifies implementers of any changes that affect them.
   *
   * @param newPolicies the full set of merged policies
   */
  void updatePolicies(List<TelemetryPolicy> newPolicies) {
    List<TelemetryPolicy> snapshot = Collections.unmodifiableList(new ArrayList<>(newPolicies));
    current.set(snapshot);
    for (PolicyImplementer implementer : implementers) {
      notifyImplementerIfChanged(implementer, snapshot);
    }
  }

  /**
   * Registers a policy implementer and immediately evaluates it against the current policies.
   *
   * @param implementer the implementer to register
   */
  void registerImplementer(PolicyImplementer implementer) {
    implementers.add(implementer);
    List<TelemetryPolicy> policies =
        Objects.requireNonNull(current.get(), "current policy snapshot cannot be null");
    notifyImplementerIfChanged(implementer, policies);
  }

  private void notifyImplementerIfChanged(
      PolicyImplementer implementer, List<TelemetryPolicy> policies) {
    Set<String> interestingTypes =
        implementer.getValidators().stream()
            .map(PolicyValidator::getPolicyType)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());

    List<TelemetryPolicy> relevant =
        policies.stream()
            .filter(policy -> interestingTypes.contains(policy.getType()))
            .collect(Collectors.toList());

    List<TelemetryPolicy> last =
        implementerStates.getOrDefault(implementer, Collections.emptyList());
    if (!new HashSet<>(relevant).equals(new HashSet<>(last))) {
      implementerStates.put(implementer, relevant);

      List<TelemetryPolicy> changes = new ArrayList<>();

      // Added or modified.
      for (TelemetryPolicy policy : relevant) {
        if (!last.contains(policy)) {
          changes.add(policy);
        }
      }

      // Removed.
      Set<String> currentTypes =
          relevant.stream().map(TelemetryPolicy::getType).collect(Collectors.toSet());
      for (TelemetryPolicy policy : last) {
        if (!currentTypes.contains(policy.getType())) {
          changes.add(new TelemetryPolicy(policy.getType()));
        }
      }

      if (!changes.isEmpty()) {
        implementer.onPoliciesChanged(changes);
      }
    }
  }
}
