/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.registry.json;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

/** Root config holding all configured policy sources. */
public final class PolicyInitConfig {
  private final List<PolicySourceConfig> sources;

  @JsonCreator
  public PolicyInitConfig(@Nullable @JsonProperty("sources") List<PolicySourceConfig> sources) {
    this.sources =
        sources == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(sources));
  }

  public List<PolicySourceConfig> getSources() {
    return sources;
  }
}
