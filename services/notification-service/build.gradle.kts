plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

dependencies {
    implementation(project(":shared:shared-infra"))

    testImplementation(project(":shared:shared-test"))
}
