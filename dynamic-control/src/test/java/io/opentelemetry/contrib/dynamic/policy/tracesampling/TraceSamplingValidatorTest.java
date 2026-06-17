/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.tracesampling;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicy;
import io.opentelemetry.contrib.dynamic.policy.source.SourceFormat;
import io.opentelemetry.contrib.dynamic.policy.source.SourceWrapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TraceSamplingValidatorTest {

  private static final String TRACE_SAMPLING_POLICY_TYPE = TraceSamplingRatePolicy.POLICY_TYPE;

  private final TraceSamplingValidator validator = new TraceSamplingValidator();

  @Test
  void testGetPolicyType() {
    assertThat(validator.getPolicyType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
  }

  @Test
  void testValidate_ValidJson() {
    String json = jsonObjectForProbability(0.5);
    TelemetryPolicy policy = validator.validate(first(SourceFormat.JSONKEYVALUE.parse(json)));
    assertThat(policy).isNotNull();
    assertThat(policy.isDeleted()).isFalse();
    assertThat(policy.getIdentity().getId()).isEqualTo("trace-policy");
    assertThat(policy.getIdentity().getName()).isEqualTo("Trace policy");
    assertThat(policy.getType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
    assertThat(policy).isInstanceOf(TraceSamplingRatePolicy.class);
    assertThat(((TraceSamplingRatePolicy) policy).getProbability()).isCloseTo(0.5, within(1e-9));
  }

  @ParameterizedTest
  @ValueSource(doubles = {0.0, 1.0})
  void testValidate_ValidJson_BoundaryValues(double probability) {
    String json = jsonObjectForProbability(probability);
    TelemetryPolicy policy = validator.validate(first(SourceFormat.JSONKEYVALUE.parse(json)));
    assertThat(policy).isNotNull();
    assertThat(policy.getIdentity().getId()).isEqualTo("trace-policy");
    assertThat(policy.getIdentity().getName()).isEqualTo("Trace policy");
    assertThat(policy.getType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
    assertThat(policy).isInstanceOf(TraceSamplingRatePolicy.class);
    assertThat(((TraceSamplingRatePolicy) policy).getProbability())
        .isCloseTo(probability, within(1e-9));
  }

  /** Regression: JSON object shape remains supported. */
  @Test
  void testValidate_ValidJson_ObjectShapeWithProbabilityField() {
    String json = jsonObjectForProbability(0.5);
    TelemetryPolicy policy = validator.validate(first(SourceFormat.JSONKEYVALUE.parse(json)));
    assertThat(policy).isNotNull();
    assertThat(policy.getType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
    assertThat(policy).isInstanceOf(TraceSamplingRatePolicy.class);
    assertThat(((TraceSamplingRatePolicy) policy).getProbability()).isCloseTo(0.5, within(1e-9));
  }

  @ParameterizedTest
  @ValueSource(doubles = {0.0, 1.0})
  void testValidate_ValidJson_ObjectShape_BoundaryValues(double probability) {
    String json = jsonObjectForProbability(probability);
    TelemetryPolicy policy = validator.validate(first(SourceFormat.JSONKEYVALUE.parse(json)));
    assertThat(policy).isNotNull();
    assertThat(((TraceSamplingRatePolicy) policy).getProbability())
        .isCloseTo(probability, within(1e-9));
  }

  /**
   * String probabilities in JSON are accepted via {@code parseDouble} on textual nodes.
   */
  @Test
  void testValidate_ValidJson_ProbabilityAsQuotedStringInObject() {
    String json =
        "{\""
            + TRACE_SAMPLING_POLICY_TYPE
            + "\": {\"id\":\"trace-policy\",\"name\":\"Trace policy\",\"probability\": \"0.625\"}}";
    TelemetryPolicy policy = validator.validate(first(SourceFormat.JSONKEYVALUE.parse(json)));
    assertThat(policy).isNotNull();
    assertThat(((TraceSamplingRatePolicy) policy).getProbability()).isCloseTo(0.625, within(1e-9));
  }

  @Test
  void testValidate_ValidJson_FlatProbabilityUsesDefaultIdentity() {
    String json = "{\"" + TRACE_SAMPLING_POLICY_TYPE + "\": 0.375}";
    TelemetryPolicy policy = validator.validate(first(SourceFormat.JSONKEYVALUE.parse(json)));
    assertThat(policy).isNotNull();
    assertThat(policy.getIdentity().getId())
        .isEqualTo(TraceSamplingRatePolicy.DEFAULT_IDENTITY.getId());
    assertThat(policy.getIdentity().getName())
        .isEqualTo(TraceSamplingRatePolicy.DEFAULT_IDENTITY.getName());
    assertThat(((TraceSamplingRatePolicy) policy).getProbability()).isCloseTo(0.375, within(1e-9));
  }

  @Test
  void testValidate_ValidJson_FlatProbabilityAsQuotedStringUsesDefaultIdentity() {
    String json = "{\"" + TRACE_SAMPLING_POLICY_TYPE + "\": \"0.375\"}";
    TelemetryPolicy policy = validator.validate(first(SourceFormat.JSONKEYVALUE.parse(json)));
    assertThat(policy).isNotNull();
    assertThat(policy.getIdentity().getId())
        .isEqualTo(TraceSamplingRatePolicy.DEFAULT_IDENTITY.getId());
    assertThat(policy.getIdentity().getName())
        .isEqualTo(TraceSamplingRatePolicy.DEFAULT_IDENTITY.getName());
    assertThat(((TraceSamplingRatePolicy) policy).getProbability()).isCloseTo(0.375, within(1e-9));
  }

  @Test
  void testValidate_InvalidJson_MissingPolicyType() {
    String json = "{\"other-policy\": 0.5}";
    assertThat(validator.validate(first(SourceFormat.JSONKEYVALUE.parse(json)))).isNull();
  }

  @Test
  void testValidate_JsonUsesDefaultIdentityWhenIdentityIsMissing() {
    String json = "{\"" + TRACE_SAMPLING_POLICY_TYPE + "\": {\"probability\": 0.5}}";
    TelemetryPolicy policy = validator.validate(first(SourceFormat.JSONKEYVALUE.parse(json)));
    assertThat(policy).isNotNull();
    assertThat(policy.getIdentity().getId())
        .isEqualTo(TraceSamplingRatePolicy.DEFAULT_IDENTITY.getId());
    assertThat(policy.getIdentity().getName())
        .isEqualTo(TraceSamplingRatePolicy.DEFAULT_IDENTITY.getName());
    assertThat(((TraceSamplingRatePolicy) policy).getProbability()).isCloseTo(0.5, within(1e-9));
  }

  @Test
  void testValidate_JsonUsesDefaultNameWhenNameIsMissing() {
    String json =
        "{\""
            + TRACE_SAMPLING_POLICY_TYPE
            + "\": {\"id\":\"custom-id\",\"probability\": 0.5}}";
    TelemetryPolicy policy = validator.validate(first(SourceFormat.JSONKEYVALUE.parse(json)));
    assertThat(policy).isNotNull();
    assertThat(policy.getIdentity().getId()).isEqualTo("custom-id");
    assertThat(policy.getIdentity().getName())
        .isEqualTo(TraceSamplingRatePolicy.DEFAULT_IDENTITY.getName());
  }

  @Test
  void testValidate_JsonUsesDefaultIdWhenIdIsMissing() {
    String json =
        "{\""
            + TRACE_SAMPLING_POLICY_TYPE
            + "\": {\"name\":\"Custom name\",\"probability\": 0.5}}";
    TelemetryPolicy policy = validator.validate(first(SourceFormat.JSONKEYVALUE.parse(json)));
    assertThat(policy).isNotNull();
    assertThat(policy.getIdentity().getId())
        .isEqualTo(TraceSamplingRatePolicy.DEFAULT_IDENTITY.getId());
    assertThat(policy.getIdentity().getName()).isEqualTo("Custom name");
  }

  @ParameterizedTest
  @ValueSource(doubles = {-0.1, 1.1})
  void testValidate_InvalidJson_ProbabilityOutOfRange(double probability) {
    String json = jsonObjectForProbability(probability);
    assertThat(validator.validate(first(SourceFormat.JSONKEYVALUE.parse(json)))).isNull();
  }

  @Test
  void testValidate_InvalidJson_ValueNotNumber() {
    String json = "{\"" + TRACE_SAMPLING_POLICY_TYPE + "\": \"high\"}";
    assertThat(validator.validate(first(SourceFormat.JSONKEYVALUE.parse(json)))).isNull();
  }

  @Test
  void testValidate_ValidKeyValue() {
    String keyValue =
        TRACE_SAMPLING_POLICY_TYPE + "=id=trace-policy,name=Trace policy,probability=0.5";
    TelemetryPolicy policy = validator.validate(first(SourceFormat.KEYVALUE.parse(keyValue)));
    assertThat(policy).isNotNull();
    assertThat(policy.getIdentity().getId()).isEqualTo("trace-policy");
    assertThat(policy.getIdentity().getName()).isEqualTo("Trace policy");
    assertThat(policy.getType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
    assertThat(policy).isInstanceOf(TraceSamplingRatePolicy.class);
    assertThat(((TraceSamplingRatePolicy) policy).getProbability()).isCloseTo(0.5, within(1e-9));
  }

  @Test
  void testValidate_ValidKeyValueNamedFieldsAreOrderIndependent() {
    String keyValue =
        TRACE_SAMPLING_POLICY_TYPE + "=probability=0.5,name=Trace policy,id=trace-policy";
    TelemetryPolicy policy = validator.validate(first(SourceFormat.KEYVALUE.parse(keyValue)));
    assertThat(policy).isNotNull();
    assertThat(policy.getIdentity().getId()).isEqualTo("trace-policy");
    assertThat(policy.getIdentity().getName()).isEqualTo("Trace policy");
    assertThat(((TraceSamplingRatePolicy) policy).getProbability()).isCloseTo(0.5, within(1e-9));
  }

  @Test
  void testValidate_KeyValueUsesDefaultIdentityWhenIdentityIsMissing() {
    String keyValue = TRACE_SAMPLING_POLICY_TYPE + "=0.5";
    TelemetryPolicy policy = validator.validate(first(SourceFormat.KEYVALUE.parse(keyValue)));
    assertThat(policy).isNotNull();
    assertThat(policy.getIdentity().getId())
        .isEqualTo(TraceSamplingRatePolicy.DEFAULT_IDENTITY.getId());
    assertThat(policy.getIdentity().getName())
        .isEqualTo(TraceSamplingRatePolicy.DEFAULT_IDENTITY.getName());
    assertThat(((TraceSamplingRatePolicy) policy).getProbability()).isCloseTo(0.5, within(1e-9));
  }

  @Test
  void testValidate_KeyValueNamedFieldsUseDefaultIdentityWhenIdentityIsMissing() {
    String keyValue = TRACE_SAMPLING_POLICY_TYPE + "=probability=0.5";
    TelemetryPolicy policy = validator.validate(first(SourceFormat.KEYVALUE.parse(keyValue)));
    assertThat(policy).isNotNull();
    assertThat(policy.getIdentity().getId())
        .isEqualTo(TraceSamplingRatePolicy.DEFAULT_IDENTITY.getId());
    assertThat(policy.getIdentity().getName())
        .isEqualTo(TraceSamplingRatePolicy.DEFAULT_IDENTITY.getName());
    assertThat(((TraceSamplingRatePolicy) policy).getProbability()).isCloseTo(0.5, within(1e-9));
  }

  @Test
  void testValidate_KeyValueNamedFieldsUseDefaultNameWhenNameIsMissing() {
    String keyValue = TRACE_SAMPLING_POLICY_TYPE + "=id=custom-id,probability=0.5";
    TelemetryPolicy policy = validator.validate(first(SourceFormat.KEYVALUE.parse(keyValue)));
    assertThat(policy).isNotNull();
    assertThat(policy.getIdentity().getId()).isEqualTo("custom-id");
    assertThat(policy.getIdentity().getName())
        .isEqualTo(TraceSamplingRatePolicy.DEFAULT_IDENTITY.getName());
    assertThat(((TraceSamplingRatePolicy) policy).getProbability()).isCloseTo(0.5, within(1e-9));
  }

  @Test
  void testValidate_KeyValueNamedFieldsUseDefaultIdWhenIdIsMissing() {
    String keyValue = TRACE_SAMPLING_POLICY_TYPE + "=name=Custom name,probability=0.5";
    TelemetryPolicy policy = validator.validate(first(SourceFormat.KEYVALUE.parse(keyValue)));
    assertThat(policy).isNotNull();
    assertThat(policy.getIdentity().getId())
        .isEqualTo(TraceSamplingRatePolicy.DEFAULT_IDENTITY.getId());
    assertThat(policy.getIdentity().getName()).isEqualTo("Custom name");
    assertThat(((TraceSamplingRatePolicy) policy).getProbability()).isCloseTo(0.5, within(1e-9));
  }

  @Test
  void testValidate_InvalidKeyValue_WrongKey() {
    assertThat(validator.validate(first(SourceFormat.KEYVALUE.parse("other.key=0.5")))).isNull();
  }

  @Test
  void testValidate_InvalidKeyValue_NotNumber() {
    String keyValue =
        TRACE_SAMPLING_POLICY_TYPE + "=id=trace-policy,name=Trace policy,probability=invalid";
    assertThat(validator.validate(first(SourceFormat.KEYVALUE.parse(keyValue)))).isNull();
  }

  private static String jsonObjectForProbability(double probability) {
    return "{\""
        + TRACE_SAMPLING_POLICY_TYPE
        + "\": {\"id\":\"trace-policy\",\"name\":\"Trace policy\",\"probability\": "
        + probability
        + "}}";
  }

  private static SourceWrapper first(List<SourceWrapper> parsedSources) {
    assertThat(parsedSources).isNotNull();
    assertThat(parsedSources).isNotEmpty();
    return parsedSources.get(0);
  }
}
