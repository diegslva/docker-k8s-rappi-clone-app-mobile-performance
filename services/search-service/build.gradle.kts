plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

application {
    mainClass.set("com.rappiclone.search.ApplicationKt")
}

val ktorVersion: String by rootProject.extra

dependencies {
    implementation(project(":shared:shared-infra"))

    // Elasticsearch client
    implementation("co.elastic.clients:elasticsearch-java:8.17.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")

    // Ktor client (pra Elasticsearch REST)
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")

    testImplementation(project(":shared:shared-test"))
}
