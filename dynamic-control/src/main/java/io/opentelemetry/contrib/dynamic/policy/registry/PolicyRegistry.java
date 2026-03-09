/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.contrib.dynamic.policy.registry;

import io.opentelemetry.contrib.dynamic.policy.OpampPolicyProvider;
import io.opentelemetry.contrib.dynamic.policy.PolicyImplementer;
import io.opentelemetry.contrib.dynamic.policy.PolicyProvider;
import io.opentelemetry.contrib.dynamic.policy.PolicyStore;
import io.opentelemetry.contrib.dynamic.policy.TelemetryPolicy;
import io.opentelemetry.contrib.dynamic.policy.PolicyValidator;
import io.opentelemetry.contrib.dynamic.policy.opamppolling.OpampPollingIntervalPolicy;
import io.opentelemetry.contrib.dynamic.policy.opamppolling.OpampPollingIntervalPolicyImplementer;
import io.opentelemetry.contrib.dynamic.policy.opamppolling.OpampPollingIntervalValidator;
import io.opentelemetry.contrib.dynamic.policy.logsexport.DelegatingLogRecordExporter;
import io.opentelemetry.contrib.dynamic.policy.logsexport.LogExportEnabledPolicy;
import io.opentelemetry.contrib.dynamic.policy.logsexport.LogExportEnabledPolicyImplementer;
import io.opentelemetry.contrib.dynamic.policy.logsexport.LogExportEnabledValidator;
import io.opentelemetry.contrib.dynamic.policy.metricsexport.DelegatingMetricExporter;
import io.opentelemetry.contrib.dynamic.policy.metricsexport.MetricExportEnabledPolicy;
import io.opentelemetry.contrib.dynamic.policy.metricsexport.MetricExportEnabledPolicyImplementer;
import io.opentelemetry.contrib.dynamic.policy.metricsexport.MetricExportEnabledValidator;
import io.opentelemetry.contrib.dynamic.policy.source.SourceKind;
import io.opentelemetry.contrib.dynamic.policy.traceexport.DelegatingSpanExporter;
import io.opentelemetry.contrib.dynamic.policy.traceexport.TraceExportEnabledPolicy;
import io.opentelemetry.contrib.dynamic.policy.traceexport.TraceExportEnabledPolicyImplementer;
import io.opentelemetry.contrib.dynamic.policy.traceexport.TraceExportEnabledValidator;
import io.opentelemetry.contrib.dynamic.policy.tracesampling.DelegatingSampler;
import io.opentelemetry.contrib.dynamic.policy.tracesampling.TraceSamplingRatePolicy;
import io.opentelemetry.contrib.dynamic.policy.tracesampling.TraceSamplingRatePolicyImplementer;
import io.opentelemetry.contrib.dynamic.policy.tracesampling.TraceSamplingValidator;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/** Initializes policy registry/config wiring from auto-configuration. */
public final class PolicyRegistry {
  private static final String POLICY_INIT_CONFIG_PROPERTY =
      "otel.java.experimental.telemetry.policy.init";
  private static final Map<String, Class<? extends TelemetryPolicy>> REGISTERED_POLICY_TYPES =
      new ConcurrentHashMap<>();
  private static final Map<Class<? extends TelemetryPolicy>, Supplier<PolicyValidator>>
      VALIDATOR_FACTORIES = createValidatorFactories();
  private static final Map<Class<? extends TelemetryPolicy>, ImplementerFactory>
      IMPLEMENTER_FACTORIES = createImplementerFactories();
  private static final Logger logger = Logger.getLogger(PolicyRegistry.class.getName());
  private static final PolicyInitConfigReader configReader = new PolicyInitConfigReader();
  private static final AtomicBoolean sourcesActivated = new AtomicBoolean(false);
  private static final Set<Class<? extends TelemetryPolicy>> registeredImplementers =
      ConcurrentHashMap.newKeySet();
  private static final CopyOnWriteArrayList<Closeable> activeSourceWatches =
      new CopyOnWriteArrayList<>();
  private static final Map<PolicyProvider, List<TelemetryPolicy>> sourcePolicies =
      new ConcurrentHashMap<>();
  private static final PolicyStore policyStore = new PolicyStore();

  @FunctionalInterface
  private interface ImplementerFactory {
    @Nullable
    PolicyImplementer create();
  }

  static {
    TraceSamplingRatePolicy.registerPolicyType();
    TraceExportEnabledPolicy.registerPolicyType();
    MetricExportEnabledPolicy.registerPolicyType();
    LogExportEnabledPolicy.registerPolicyType();
    OpampPollingIntervalPolicy.registerPolicyType();
  }

  public static void registerPolicyType(
      String policyType, Class<? extends TelemetryPolicy> policyClass) {
    Objects.requireNonNull(policyType, "policyType cannot be null");
    Objects.requireNonNull(policyClass, "policyClass cannot be null");
    Class<? extends TelemetryPolicy> existing = REGISTERED_POLICY_TYPES.put(policyType, policyClass);
    if (existing != null && !existing.equals(policyClass)) {
      throw new IllegalStateException(
          "Policy type '" + policyType + "' is already registered by " + existing.getName());
    }
  }

  public static void init(AutoConfigurationCustomizer autoConfiguration) {
    autoConfiguration.addPropertiesCustomizer(
        config -> {
          String mappingPath = config.getString(POLICY_INIT_CONFIG_PROPERTY);
          if (mappingPath == null || mappingPath.trim().isEmpty()) {
            return Collections.emptyMap();
          }
          try {
            PolicyInitConfig initConfig = configReader.read(Paths.get(mappingPath));
            logger.log(
                Level.INFO,
                "Loaded telemetry policy init config with {0} source definitions from {1}",
                new Object[] {initConfig.getSources().size(), mappingPath});
            resolveAndInitializeMappedPolicyTypes(initConfig, autoConfiguration);
            activateSources(initConfig, config);
          } catch (IOException e) {
            throw new IllegalArgumentException(
                "Failed to load telemetry policy init config from " + mappingPath, e);
          }
          return Collections.emptyMap();
        });
  }

  private static void resolveAndInitializeMappedPolicyTypes(
      PolicyInitConfig initConfig, AutoConfigurationCustomizer autoConfiguration) {
    Set<Class<? extends TelemetryPolicy>> initializedPolicyClasses = new HashSet<>();
    for (PolicySourceConfig source : initConfig.getSources()) {
      for (PolicyMappingTypeAndKey mapping : source.getMappings()) {
        mapping.resolvePolicyTypeClass(REGISTERED_POLICY_TYPES);
        String mappedPolicyType = mapping.getConfiguredPolicyType();
        Class<? extends TelemetryPolicy> policyClass = mapping.getPolicyType();
        if (policyClass == null) {
          throw new IllegalArgumentException(
              "Unknown policyType '"
                  + mappedPolicyType
                  + "' in mapping for source kind '"
                  + source.getKind().toConfigValue()
                  + "' key '"
                  + mapping.getSourceKey()
                  + "'");
        }
        initializePolicyClass(policyClass, autoConfiguration, initializedPolicyClasses);
        logger.log(
            Level.INFO,
            "Mapped policyType ''{0}'' to class ''{1}''",
            new Object[] {mappedPolicyType, policyClass.getName()});
      }
    }
  }

  private static void initializePolicyClass(
      Class<? extends TelemetryPolicy> policyClass,
      AutoConfigurationCustomizer autoConfiguration,
      Set<Class<? extends TelemetryPolicy>> initializedPolicyClasses) {
    if (!initializedPolicyClasses.add(policyClass)) {
      return;
    }
    invokePolicyInitializer(policyClass, autoConfiguration);
    logger.log(Level.INFO, "Initialized policy class ''{0}''", policyClass.getName());
  }

  private static void invokePolicyInitializer(
      Class<? extends TelemetryPolicy> policyClass, AutoConfigurationCustomizer autoConfiguration) {
    Method initializeMethod;
    try {
      initializeMethod =
          policyClass.getMethod("initialize", AutoConfigurationCustomizer.class);
    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException(
          "No static initialize(AutoConfigurationCustomizer) method found for policy class '"
              + policyClass.getName()
              + "'",
          e);
    }

    int modifiers = initializeMethod.getModifiers();
    if (!Modifier.isStatic(modifiers) || !Modifier.isPublic(modifiers)) {
      throw new IllegalArgumentException(
          "Policy initializer must be public static for class '" + policyClass.getName() + "'");
    }

    try {
      initializeMethod.invoke(null, autoConfiguration);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException(
          "Cannot access initializer for policy class '" + policyClass.getName() + "'",
          e);
    } catch (InvocationTargetException e) {
      throw new IllegalStateException(
          "Policy initializer failed for class '" + policyClass.getName() + "'",
          e.getCause() != null ? e.getCause() : e);
    }
  }

  private static void activateSources(PolicyInitConfig initConfig, ConfigProperties config) {
    if (!sourcesActivated.compareAndSet(false, true)) {
      return;
    }
    registerImplementers(collectMappedPolicyClasses(initConfig));
    for (PolicySourceConfig source : initConfig.getSources()) {
      if (source.getKind() != SourceKind.OPAMP) {
        continue;
      }
      PolicyProvider provider = createOpampProvider(source, config);
      if (provider == null) {
        continue;
      }
      try {
        List<TelemetryPolicy> initialPolicies = provider.fetchPolicies();
        updatePoliciesForSource(provider, initialPolicies);
      } catch (Exception e) {
        logger.log(
            Level.INFO,
            "Failed to fetch initial policies for OpAMP source ''{0}''",
            source.getResolvedLocation());
      }
      Closeable watch =
          provider.startWatching(
              policies -> {
                updatePoliciesForSource(provider, policies);
                logger.log(
                    Level.INFO,
                    "OpAMP source ''{0}'' produced {1} policies",
                    new Object[] {source.getResolvedLocation(), policies.size()});
              });
      activeSourceWatches.add(watch);
      logger.log(
          Level.INFO,
          "Activated OpAMP policy source with location key ''{0}''",
          source.getResolvedLocation());
    }
  }

  @Nullable
  private static PolicyProvider createOpampProvider(PolicySourceConfig source, ConfigProperties config) {
    Set<Class<? extends TelemetryPolicy>> mappedClasses =
        collectMappedPolicyClasses(source.getMappings());
    ArrayList<PolicyValidator> validators = new ArrayList<>();
    for (Class<? extends TelemetryPolicy> policyClass : mappedClasses) {
      Supplier<PolicyValidator> factory = VALIDATOR_FACTORIES.get(policyClass);
      if (factory != null) {
        validators.add(factory.get());
      }
    }
    if (validators.isEmpty()) {
      logger.log(
          Level.INFO,
          "Skipping OpAMP source ''{0}'' because no validators matched configured mappings",
          source.getResolvedLocation());
      return null;
    }
    return new OpampPolicyProvider(
        config, source.getResolvedLocation(), source.getFormat(), source.getMappings(), validators);
  }

  private static void registerImplementers(Set<Class<? extends TelemetryPolicy>> mappedPolicyClasses) {
    for (Class<? extends TelemetryPolicy> policyClass : mappedPolicyClasses) {
      ImplementerFactory factory = IMPLEMENTER_FACTORIES.get(policyClass);
      if (factory == null) {
        continue;
      }
      if (!registeredImplementers.add(policyClass)) {
        continue;
      }
      PolicyImplementer implementer = factory.create();
      if (implementer == null) {
        registeredImplementers.remove(policyClass);
        continue;
      }
      policyStore.registerImplementer(implementer);
    }
  }

  private static void updatePoliciesForSource(
      PolicyProvider provider, List<TelemetryPolicy> policiesFromSource) {
    List<TelemetryPolicy> snapshot =
        policiesFromSource == null
            ? Collections.emptyList()
            : Collections.unmodifiableList(new ArrayList<>(policiesFromSource));
    sourcePolicies.put(provider, snapshot);
    ArrayList<TelemetryPolicy> merged = new ArrayList<>();
    for (List<TelemetryPolicy> policies : sourcePolicies.values()) {
      merged.addAll(policies);
    }
    policyStore.updatePolicies(merged);
  }

  static void resetForTest() {
    for (Closeable watch : activeSourceWatches) {
      try {
        watch.close();
      } catch (IOException e) {
        logger.log(Level.INFO, "Failed to close active source watch during test reset", e);
      }
    }
    activeSourceWatches.clear();
    sourcePolicies.clear();
    sourcesActivated.set(false);
    registeredImplementers.clear();
    DelegatingSpanExporter.resetForTest();
    DelegatingMetricExporter.resetForTest();
    DelegatingLogRecordExporter.resetForTest();
    OpampPolicyProvider.resetForTest();
  }

  private static Set<Class<? extends TelemetryPolicy>> collectMappedPolicyClasses(
      PolicyInitConfig initConfig) {
    LinkedHashSet<Class<? extends TelemetryPolicy>> result = new LinkedHashSet<>();
    for (PolicySourceConfig source : initConfig.getSources()) {
      result.addAll(collectMappedPolicyClasses(source.getMappings()));
    }
    return result;
  }

  private static Set<Class<? extends TelemetryPolicy>> collectMappedPolicyClasses(
      List<PolicyMappingTypeAndKey> mappings) {
    LinkedHashSet<Class<? extends TelemetryPolicy>> result = new LinkedHashSet<>();
    for (PolicyMappingTypeAndKey mapping : mappings) {
      Class<? extends TelemetryPolicy> policyClass = mapping.getPolicyType();
      if (policyClass != null) {
        result.add(policyClass);
      }
    }
    return result;
  }

  private static Map<Class<? extends TelemetryPolicy>, Supplier<PolicyValidator>>
      createValidatorFactories() {
    HashMap<Class<? extends TelemetryPolicy>, Supplier<PolicyValidator>> factories = new HashMap<>();
    factories.put(TraceSamplingRatePolicy.class, TraceSamplingValidator::new);
    factories.put(TraceExportEnabledPolicy.class, TraceExportEnabledValidator::new);
    factories.put(MetricExportEnabledPolicy.class, MetricExportEnabledValidator::new);
    factories.put(LogExportEnabledPolicy.class, LogExportEnabledValidator::new);
    factories.put(OpampPollingIntervalPolicy.class, OpampPollingIntervalValidator::new);
    return Collections.unmodifiableMap(factories);
  }

  private static Map<Class<? extends TelemetryPolicy>, ImplementerFactory>
      createImplementerFactories() {
    HashMap<Class<? extends TelemetryPolicy>, ImplementerFactory> factories = new HashMap<>();
    factories.put(
        TraceSamplingRatePolicy.class,
        () -> {
          DelegatingSampler sampler = TraceSamplingRatePolicy.getInitializedSampler();
          if (sampler == null) {
            logger.info("TraceSamplingRatePolicy mapped but sampler is not initialized yet");
            return null;
          }
          return new TraceSamplingRatePolicyImplementer(sampler);
        });
    factories.put(TraceExportEnabledPolicy.class, TraceExportEnabledPolicyImplementer::new);
    factories.put(MetricExportEnabledPolicy.class, MetricExportEnabledPolicyImplementer::new);
    factories.put(LogExportEnabledPolicy.class, LogExportEnabledPolicyImplementer::new);
    factories.put(OpampPollingIntervalPolicy.class, OpampPollingIntervalPolicyImplementer::new);
    return Collections.unmodifiableMap(factories);
  }

  private PolicyRegistry() {}
}
