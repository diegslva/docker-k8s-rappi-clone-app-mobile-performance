plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

application {
    mainClass.set("com.rappiclone.onboarding.ApplicationKt")
}

dependencies {
    implementation(project(":shared:shared-infra"))

    testImplementation(project(":shared:shared-test"))
}
