/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class TelemetryPolicyManager {
  private static final Logger logger = Logger.getLogger(TelemetryPolicyManager.class.getName());

  private final List<PolicyProvider> providers;
  private final PolicyStore store;
  private final ScheduledExecutorService executor;
  private final PolicyAggregator aggregator = new PolicyAggregator();
  @javax.annotation.Nullable private ScheduledFuture<?> task;

  public TelemetryPolicyManager(List<PolicyProvider> providers, PolicyStore store) {
    this.providers = new ArrayList<>(providers);
    this.store = store;
    this.executor = Executors.newSingleThreadScheduledExecutor(new DaemonThreadFactory());
  }

  public void start() {
    this.task = executor.scheduleWithFixedDelay(this::run, 0, 1, TimeUnit.MINUTES);
  }

  public void stop() {
    if (task != null) {
      task.cancel(false);
    }
    executor.shutdown();
  }

  private void run() {
    try {
      List<List<TelemetryPolicy>> allPolicies = new ArrayList<>();
      for (PolicyProvider provider : providers) {
        allPolicies.add(provider.fetchPolicies());
      }
      List<TelemetryPolicy> merged = aggregator.merge(allPolicies);
      store.updatePolicies(merged);
    } catch (Throwable e) {
      logger.log(Level.WARNING, "Failed to update policies", e);
    }
  }

  private static class DaemonThreadFactory implements ThreadFactory {
    @Override
    public Thread newThread(Runnable r) {
      Thread t = new Thread(r, "telemetry-policy-manager");
      t.setDaemon(true);
      return t;
    }
  }
}
