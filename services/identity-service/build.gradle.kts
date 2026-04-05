plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

application {
    mainClass.set("com.rappiclone.identity.ApplicationKt")
}

dependencies {
    implementation(project(":shared:shared-infra"))

    // Password hashing (bcrypt direto, sem passlib)
    implementation("at.favre.lib:bcrypt:0.10.2")

    // JWT
    implementation("com.auth0:java-jwt:4.4.0")

    testImplementation(project(":shared:shared-test"))
}
