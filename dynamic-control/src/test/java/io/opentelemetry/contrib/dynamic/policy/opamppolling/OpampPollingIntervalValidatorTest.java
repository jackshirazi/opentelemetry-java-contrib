/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.opamppolling;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicy;
import io.opentelemetry.contrib.dynamic.policy.source.SourceFormat;
import io.opentelemetry.contrib.dynamic.policy.source.SourceWrapper;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class OpampPollingIntervalValidatorTest {
  private final OpampPollingIntervalValidator validator = new OpampPollingIntervalValidator();

  @Test
  void validatesJsonSeconds() {
    TelemetryPolicy policy =
        validator.validate(
            first(
                SourceFormat.JSONKEYVALUE.parse(
                    "{\"" + OpampPollingIntervalPolicy.POLICY_TYPE + "\":2.5}")));

    assertThat(policy).isInstanceOf(OpampPollingIntervalPolicy.class);
    assertThat(((OpampPollingIntervalPolicy) policy).getInterval()).isEqualTo(Duration.ofMillis(2500));
  }

  @Test
  void validatesKeyValueSeconds() {
    TelemetryPolicy policy =
        validator.validate(
            first(
                SourceFormat.KEYVALUE.parse(
                    OpampPollingIntervalPolicy.POLICY_TYPE + "=0.5")));

    assertThat(policy).isInstanceOf(OpampPollingIntervalPolicy.class);
    assertThat(((OpampPollingIntervalPolicy) policy).getInterval()).isEqualTo(Duration.ofMillis(500));
  }

  @Test
  void rejectsZeroAndNegative() {
    TelemetryPolicy zero =
        validator.validate(
            first(
                SourceFormat.JSONKEYVALUE.parse(
                    "{\"" + OpampPollingIntervalPolicy.POLICY_TYPE + "\":0}")));
    TelemetryPolicy negative =
        validator.validate(
            first(SourceFormat.KEYVALUE.parse(OpampPollingIntervalPolicy.POLICY_TYPE + "=-1")));

    assertThat(zero).isNull();
    assertThat(negative).isNull();
  }

  private static SourceWrapper first(List<SourceWrapper> values) {
    assertThat(values).isNotNull();
    assertThat(values).isNotEmpty();
    return values.get(0);
  }
}
