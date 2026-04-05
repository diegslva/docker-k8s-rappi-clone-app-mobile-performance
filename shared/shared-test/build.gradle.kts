plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

val testcontainersVersion: String by rootProject.extra
val ktorVersion: String by rootProject.extra

dependencies {
    api(project(":shared:shared-domain"))
    api(project(":shared:shared-infra"))

    api("org.testcontainers:testcontainers:$testcontainersVersion")
    api("org.testcontainers:junit-jupiter:$testcontainersVersion")
    api("org.testcontainers:postgresql:$testcontainersVersion")
    api("org.testcontainers:kafka:$testcontainersVersion")
    api("org.testcontainers:elasticsearch:$testcontainersVersion")

    api("io.ktor:ktor-server-test-host:$ktorVersion")
    api("io.ktor:ktor-client-content-negotiation:$ktorVersion")
}
