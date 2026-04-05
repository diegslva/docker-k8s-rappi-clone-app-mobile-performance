plugins {
    kotlin("jvm") version "2.1.10" apply false
    kotlin("plugin.serialization") version "2.1.10" apply false
    id("com.google.protobuf") version "0.9.4" apply false
}

val ktorVersion = "3.1.1"
val kotlinVersion = "2.1.10"
val kotlinxSerializationVersion = "1.8.0"
val kotlinxCoroutinesVersion = "1.10.1"
val kotlinxDatetimeVersion = "0.6.2"
val grpcVersion = "1.70.0"
val grpcKotlinVersion = "1.4.3"
val protobufVersion = "4.30.2"
val logbackVersion = "1.5.16"
val logstashEncoderVersion = "8.0"
val hikariVersion = "6.2.1"
val exposedVersion = "0.58.0"
val postgresDriverVersion = "42.7.5"
val flywayVersion = "11.3.1"
val redisVersion = "6.5.2.RELEASE"
val kafkaVersion = "3.9.0"
val micrometerVersion = "1.14.4"
val opentelemetryVersion = "1.47.0"
val resilience4jVersion = "2.3.0"
val hivemqMqttVersion = "1.3.5"
val junitVersion = "5.11.4"
val testcontainersVersion = "1.20.5"
val mockkVersion = "1.13.14"

allprojects {
    group = "com.rappiclone"
    version = "0.1.0"

    extra["ktorVersion"] = ktorVersion
    extra["kotlinVersion"] = kotlinVersion
    extra["kotlinxSerializationVersion"] = kotlinxSerializationVersion
    extra["kotlinxCoroutinesVersion"] = kotlinxCoroutinesVersion
    extra["kotlinxDatetimeVersion"] = kotlinxDatetimeVersion
    extra["grpcVersion"] = grpcVersion
    extra["grpcKotlinVersion"] = grpcKotlinVersion
    extra["protobufVersion"] = protobufVersion
    extra["logbackVersion"] = logbackVersion
    extra["logstashEncoderVersion"] = logstashEncoderVersion
    extra["hikariVersion"] = hikariVersion
    extra["exposedVersion"] = exposedVersion
    extra["postgresDriverVersion"] = postgresDriverVersion
    extra["flywayVersion"] = flywayVersion
    extra["redisVersion"] = redisVersion
    extra["kafkaVersion"] = kafkaVersion
    extra["micrometerVersion"] = micrometerVersion
    extra["opentelemetryVersion"] = opentelemetryVersion
    extra["resilience4jVersion"] = resilience4jVersion
    extra["hivemqMqttVersion"] = hivemqMqttVersion
    extra["junitVersion"] = junitVersion
    extra["testcontainersVersion"] = testcontainersVersion
    extra["mockkVersion"] = mockkVersion
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            freeCompilerArgs.addAll("-Xjsr305=strict", "-opt-in=kotlin.RequiresOptIn")
        }
    }

    tasks.withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }

    // Task separada pra testes unitarios (sem Docker/Testcontainers)
    tasks.register<Test>("unitTest") {
        useJUnitPlatform {
            excludeTags("integration")
        }
        description = "Roda apenas testes unitarios (sem Testcontainers)"
        group = "verification"
    }

    // Task separada pra testes de integracao (precisa de Docker)
    tasks.register<Test>("integrationTest") {
        useJUnitPlatform {
            includeTags("integration")
        }
        description = "Roda testes de integracao com Testcontainers (precisa de Docker)"
        group = "verification"
    }

    dependencies {
        val implementation by configurations
        val testImplementation by configurations

        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$kotlinxCoroutinesVersion")
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVersion")
        implementation("org.jetbrains.kotlinx:kotlinx-datetime:$kotlinxDatetimeVersion")

        val testRuntimeOnly by configurations

        testImplementation(platform("org.junit:junit-bom:$junitVersion"))
        testImplementation("org.junit.jupiter:junit-jupiter")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
        testImplementation("io.mockk:mockk:$mockkVersion")
        testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$kotlinxCoroutinesVersion")
    }
}
