plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

application {
    mainClass.set("com.rappiclone.geolocation.ApplicationKt")
}

val ktorVersion: String by rootProject.extra

dependencies {
    implementation(project(":shared:shared-infra"))

    // Ktor client pra chamar Nominatim e OSRM
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")

    testImplementation(project(":shared:shared-test"))
}
