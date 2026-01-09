/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.json.Json;
import jakarta.json.JsonMergePatch;
import jakarta.json.JsonReader;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import java.io.StringReader;

/**
 * Merge JSON "patch" (as JSON Merge Patch RFC 7386) into base using JSON-P, converting between
 * Jackson JsonNode and JSON-P JsonValue via String.
 */
public final class JsonMergePatchUtil {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * Merge 'patch' into 'base' using JSON Merge Patch semantics. Returns a new JsonNode (does not
   * mutate inputs).
   */
  public static JsonNode mergeJsonMergePatch(JsonNode base, JsonNode patch) throws Exception {
    // Convert Jackson nodes to JSON text
    String baseJsonText =
        MAPPER.writeValueAsString(base == null ? MAPPER.createObjectNode() : base);
    String patchJsonText =
        MAPPER.writeValueAsString(patch == null ? MAPPER.createObjectNode() : patch);

    // Parse into JSON-P types
    try (JsonReader baseReader = Json.createReader(new StringReader(baseJsonText));
        JsonReader patchReader = Json.createReader(new StringReader(patchJsonText))) {

      JsonStructure baseJson = baseReader.read();
      JsonValue patchJson = patchReader.read();

      // Create merge patch and apply
      JsonMergePatch mergePatch = Json.createMergePatch(patchJson);
      JsonValue result = mergePatch.apply(baseJson);

      // Convert result back to Jackson JsonNode
      // JsonValue#toString yields valid JSON text for the structure
      String resultText = result.toString();
      return MAPPER.readTree(resultText);
    }
  }

  private JsonMergePatchUtil() {}
}
