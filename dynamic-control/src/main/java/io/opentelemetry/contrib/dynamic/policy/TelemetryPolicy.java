/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

/**
 * Represents a single telemetry policy with spec-required identity and policy type.
 *
 * <p>Policies are immutable data carriers. Concrete implementations must provide value-based
 * {@code equals} and {@code hashCode} implementations because {@link PolicyStore} deduplicates and
 * detects changes using policy equality.
 *
 * @see io.opentelemetry.contrib.dynamic.policy
 */
public interface TelemetryPolicy {
  TelemetryPolicyIdentity getIdentity();

  String getType();

  default boolean isDeleted() {
    return false;
  }
}
