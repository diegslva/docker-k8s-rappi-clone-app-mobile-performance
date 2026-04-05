plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

application {
    mainClass.set("com.rappiclone.gateway.ApplicationKt")
}

val ktorVersion: String by rootProject.extra

dependencies {
    implementation(project(":shared:shared-infra"))

    // Ktor client pra proxy requests pros microservices
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-logging:$ktorVersion")

    // JWT validation (mesma lib do identity-service)
    implementation("com.auth0:java-jwt:4.4.0")

    testImplementation(project(":shared:shared-test"))
}
