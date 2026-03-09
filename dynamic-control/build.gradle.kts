import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")
  id("otel.animalsniffer-conventions")
  id("com.gradleup.shadow")
}

description = "Dynamic control of some specific features of the agent"
otelJava.moduleName.set("io.opentelemetry.contrib.dynamic")

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
  implementation(project(":opamp-client"))
  implementation("com.squareup.okhttp3:okhttp")

  annotationProcessor("com.google.auto.service:auto-service")
  compileOnly("com.google.auto.service:auto-service-annotations")

  implementation("com.fasterxml.jackson.core:jackson-databind")

  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  compileOnly("io.opentelemetry:opentelemetry-sdk-extension-incubator")

  testCompileOnly("com.google.auto.service:auto-service-annotations")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
  testImplementation("org.junit.jupiter:junit-jupiter-api")
  testCompileOnly("org.junit.jupiter:junit-jupiter-params")

  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  testImplementation("io.opentelemetry:opentelemetry-sdk-extension-incubator")
  testImplementation("org.assertj:assertj-core")
  testImplementation("org.mockito:mockito-inline")
  testImplementation("org.mockito:mockito-junit-jupiter")
  testImplementation("com.squareup.okhttp3:mockwebserver3")
}

val bundledAllJar by tasks.registering(ShadowJar::class) {
  group = "build"
  description = "Builds additional bundled extension jar with runtime dependencies."

  from(sourceSets.main.get().output)
  configurations = listOf(project.configurations.runtimeClasspath.get())
  archiveClassifier.set("all")
  mergeServiceFiles()
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named("assemble") {
  dependsOn(bundledAllJar)
}
