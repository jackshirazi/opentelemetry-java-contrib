/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.contrib.dynamic.policy.source.JsonSourceWrapper;
import io.opentelemetry.contrib.dynamic.policy.source.KeyValueSourceWrapper;
import io.opentelemetry.contrib.dynamic.policy.source.SourceFormat;
import io.opentelemetry.contrib.dynamic.policy.source.SourceWrapper;
import io.opentelemetry.contrib.dynamic.policy.tracesampling.TraceSamplingRatePolicy;
import io.opentelemetry.contrib.dynamic.policy.tracesampling.TraceSamplingValidator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TraceSamplingValidatorTest {

  private static final String TRACE_SAMPLING_POLICY_TYPE = TraceSamplingRatePolicy.POLICY_TYPE;
  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final TraceSamplingValidator validator = new TraceSamplingValidator();

  @Test
  void testGetPolicyType() {
    assertThat(validator.getPolicyType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
  }

  @Test
  void testValidate_ValidJson() {
    String json = jsonForProbability(0.5);
    TelemetryPolicy policy = validator.validate(wrap(SourceFormat.JSONKEYVALUE, json));
    assertThat(policy).isNotNull();
    assertThat(policy.getType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
    assertThat(policy).isInstanceOf(TraceSamplingRatePolicy.class);
    assertThat(((TraceSamplingRatePolicy) policy).getProbability()).isCloseTo(0.5, within(1e-9));
  }

  @Test
  void testValidate_ValidJsonNodeSource() throws Exception {
    TelemetryPolicy policy =
        validator.validate(
            wrap(SourceFormat.JSONKEYVALUE, MAPPER.readTree(jsonForProbability(0.5))));
    assertThat(policy).isNotNull();
    assertThat(policy).isInstanceOf(TraceSamplingRatePolicy.class);
    assertThat(((TraceSamplingRatePolicy) policy).getProbability()).isCloseTo(0.5, within(1e-9));
  }

  @Test
  void testValidate_ValidJson_StringNumber() {
    String json = "{\"" + TRACE_SAMPLING_POLICY_TYPE + "\": \"0.2\"}";
    TelemetryPolicy policy = validator.validate(wrap(SourceFormat.JSONKEYVALUE, json));
    assertThat(policy).isNotNull();
    assertThat(policy).isInstanceOf(TraceSamplingRatePolicy.class);
    assertThat(((TraceSamplingRatePolicy) policy).getProbability()).isCloseTo(0.2, within(1e-9));
  }

  @Test
  void testValidate_ValidJsonArraySource() {
    String jsonArray = "[{\"other-policy\": 1.0}, {\"" + TRACE_SAMPLING_POLICY_TYPE + "\": 0.5}]";
    List<SourceWrapper> wrappedSources = SourceFormat.JSONKEYVALUE.parse(jsonArray);
    TelemetryPolicy policy = null;
    for (SourceWrapper source : wrappedSources) {
      policy = validator.validate(source);
      if (policy != null) {
        break;
      }
    }
    assertThat(policy).isNotNull();
    assertThat(policy).isInstanceOf(TraceSamplingRatePolicy.class);
    assertThat(((TraceSamplingRatePolicy) policy).getProbability()).isCloseTo(0.5, within(1e-9));
  }

  @ParameterizedTest
  @ValueSource(doubles = {0.0, 1.0})
  void testValidate_ValidJson_BoundaryValues(double probability) {
    String json = jsonForProbability(probability);
    TelemetryPolicy policy = validator.validate(wrap(SourceFormat.JSONKEYVALUE, json));
    assertThat(policy).isNotNull();
    assertThat(policy.getType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
    assertThat(policy).isInstanceOf(TraceSamplingRatePolicy.class);
    assertThat(((TraceSamplingRatePolicy) policy).getProbability())
        .isCloseTo(probability, within(1e-9));
  }

  @Test
  void testValidate_InvalidJson_Malformed() {
    String json = "{invalid-json";
    assertThat(validator.validate(wrap(SourceFormat.JSONKEYVALUE, json))).isNull();
  }

  @Test
  void testValidate_InvalidJson_MissingPolicyType() {
    String json = "{\"other-policy\": 0.5}";
    assertThat(validator.validate(wrap(SourceFormat.JSONKEYVALUE, json))).isNull();
  }

  @Test
  void testValidate_InvalidJson_ValueNotNumber() {
    String json = "{\"" + TRACE_SAMPLING_POLICY_TYPE + "\": \"high\"}";
    assertThat(validator.validate(wrap(SourceFormat.JSONKEYVALUE, json))).isNull();
  }

  @ParameterizedTest
  @ValueSource(doubles = {-0.1, 1.1})
  void testValidate_InvalidJson_ProbabilityOutOfRange(double probability) {
    String json = jsonForProbability(probability);
    assertThat(validator.validate(wrap(SourceFormat.JSONKEYVALUE, json))).isNull();
  }

  @Test
  void testValidate_ValidKeyValue() {
    String keyValue = TRACE_SAMPLING_POLICY_TYPE + "=0.5";
    TelemetryPolicy policy = validator.validate(wrap(SourceFormat.KEYVALUE, keyValue));
    assertThat(policy).isNotNull();
    assertThat(policy.getType()).isEqualTo(TRACE_SAMPLING_POLICY_TYPE);
    assertThat(policy).isInstanceOf(TraceSamplingRatePolicy.class);
    assertThat(((TraceSamplingRatePolicy) policy).getProbability()).isCloseTo(0.5, within(1e-9));
  }

  @Test
  void testValidate_ValidKeyValueEntrySource() {
    TelemetryPolicy policy =
        validator.validate(new KeyValueSourceWrapper(TRACE_SAMPLING_POLICY_TYPE, "0.5"));
    assertThat(policy).isNotNull();
    assertThat(policy).isInstanceOf(TraceSamplingRatePolicy.class);
    assertThat(((TraceSamplingRatePolicy) policy).getProbability()).isCloseTo(0.5, within(1e-9));
  }

  @Test
  void testValidate_ValidMultipleKeyValueLinesSource() {
    String keyValue = "other.key=1.0\n" + TRACE_SAMPLING_POLICY_TYPE + "=0.5";
    List<SourceWrapper> wrappedSources = SourceFormat.KEYVALUE.parse(keyValue);
    TelemetryPolicy policy = null;
    for (SourceWrapper source : wrappedSources) {
      policy = validator.validate(source);
      if (policy != null) {
        break;
      }
    }
    assertThat(policy).isNotNull();
    assertThat(policy).isInstanceOf(TraceSamplingRatePolicy.class);
    assertThat(((TraceSamplingRatePolicy) policy).getProbability()).isCloseTo(0.5, within(1e-9));
  }

  @ParameterizedTest
  @ValueSource(strings = {"0.0", "1.0"})
  void testValidate_ValidKeyValue_BoundaryValues(String probabilityText) {
    String keyValue = TRACE_SAMPLING_POLICY_TYPE + "=" + probabilityText;
    TelemetryPolicy policy = validator.validate(wrap(SourceFormat.KEYVALUE, keyValue));
    assertThat(policy).isNotNull();
    assertThat(policy).isInstanceOf(TraceSamplingRatePolicy.class);
    assertThat(((TraceSamplingRatePolicy) policy).getProbability())
        .isCloseTo(Double.parseDouble(probabilityText), within(1e-9));
  }

  @Test
  void testValidate_InvalidKeyValue_WrongKey() {
    assertThat(validator.validate(wrap(SourceFormat.KEYVALUE, "other.key=0.5"))).isNull();
  }

  @Test
  void testValidate_InvalidKeyValue_NotNumber() {
    assertThat(
            validator.validate(
                wrap(SourceFormat.KEYVALUE, TRACE_SAMPLING_POLICY_TYPE + "=not-a-number")))
        .isNull();
  }

  @ParameterizedTest
  @ValueSource(strings = {"-0.1", "1.1"})
  void testValidate_InvalidKeyValue_OutOfRange(String probabilityText) {
    assertThat(
            validator.validate(
                wrap(SourceFormat.KEYVALUE, TRACE_SAMPLING_POLICY_TYPE + "=" + probabilityText)))
        .isNull();
  }

  private static String jsonForProbability(double probability) {
    return "{\"" + TRACE_SAMPLING_POLICY_TYPE + "\": " + probability + "}";
  }

  private static SourceWrapper wrap(SourceFormat format, Object source) {
    if (format == SourceFormat.JSONKEYVALUE) {
      if (source instanceof String) {
        return first(SourceFormat.JSONKEYVALUE.parse((String) source));
      }
      if (source instanceof JsonNode) {
        return new JsonSourceWrapper((JsonNode) source);
      }
    }
    if (format == SourceFormat.KEYVALUE) {
      if (source instanceof String) {
        return first(SourceFormat.KEYVALUE.parse((String) source));
      }
      if (source instanceof KeyValueSourceWrapper) {
        return (KeyValueSourceWrapper) source;
      }
    }
    return null;
  }

  private static SourceWrapper first(List<SourceWrapper> parsedSources) {
    if (parsedSources == null || parsedSources.isEmpty()) {
      return null;
    }
    return parsedSources.get(0);
  }
}
