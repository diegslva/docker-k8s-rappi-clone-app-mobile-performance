plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

val ktorVersion: String by rootProject.extra
val logbackVersion: String by rootProject.extra
val logstashEncoderVersion: String by rootProject.extra
val hikariVersion: String by rootProject.extra
val exposedVersion: String by rootProject.extra
val postgresDriverVersion: String by rootProject.extra
val flywayVersion: String by rootProject.extra
val redisVersion: String by rootProject.extra
val kafkaVersion: String by rootProject.extra
val micrometerVersion: String by rootProject.extra
val opentelemetryVersion: String by rootProject.extra
val resilience4jVersion: String by rootProject.extra
val hivemqMqttVersion: String by rootProject.extra
val grpcVersion: String by rootProject.extra
val grpcKotlinVersion: String by rootProject.extra

dependencies {
    api(project(":shared:shared-domain"))

    // Ktor server
    api("io.ktor:ktor-server-core:$ktorVersion")
    api("io.ktor:ktor-server-netty:$ktorVersion")
    api("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    api("io.ktor:ktor-server-cors:$ktorVersion")
    api("io.ktor:ktor-server-auth:$ktorVersion")
    api("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    api("io.ktor:ktor-server-status-pages:$ktorVersion")
    api("io.ktor:ktor-server-call-logging:$ktorVersion")
    api("io.ktor:ktor-server-call-id:$ktorVersion")
    api("io.ktor:ktor-server-metrics-micrometer:$ktorVersion")
    api("io.ktor:ktor-server-websockets:$ktorVersion")
    api("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // Logging
    api("ch.qos.logback:logback-classic:$logbackVersion")
    api("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

    // Database
    api("com.zaxxer:HikariCP:$hikariVersion")
    api("org.jetbrains.exposed:exposed-core:$exposedVersion")
    api("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    api("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    api("org.jetbrains.exposed:exposed-kotlin-datetime:$exposedVersion")
    api("org.postgresql:postgresql:$postgresDriverVersion")
    api("org.flywaydb:flyway-core:$flywayVersion")
    api("org.flywaydb:flyway-database-postgresql:$flywayVersion")

    // Redis
    api("io.lettuce:lettuce-core:$redisVersion")

    // Kafka
    api("org.apache.kafka:kafka-clients:$kafkaVersion")

    // MQTT
    api("com.hivemq:hivemq-mqtt-client:$hivemqMqttVersion")

    // Metrics and tracing
    api("io.micrometer:micrometer-registry-prometheus:$micrometerVersion")
    api("io.opentelemetry:opentelemetry-api:$opentelemetryVersion")
    api("io.opentelemetry:opentelemetry-sdk:$opentelemetryVersion")
    api("io.opentelemetry:opentelemetry-exporter-otlp:$opentelemetryVersion")

    // gRPC
    api("io.grpc:grpc-kotlin-stub:$grpcKotlinVersion")
    api("io.grpc:grpc-netty-shaded:$grpcVersion")
    api("io.grpc:grpc-protobuf:$grpcVersion")

    // Resilience
    api("io.github.resilience4j:resilience4j-kotlin:$resilience4jVersion")
    api("io.github.resilience4j:resilience4j-circuitbreaker:$resilience4jVersion")
    api("io.github.resilience4j:resilience4j-retry:$resilience4jVersion")

    // Config
    api("com.typesafe:config:1.4.3")
}
