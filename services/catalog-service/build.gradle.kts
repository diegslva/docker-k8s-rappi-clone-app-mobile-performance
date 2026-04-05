plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

application {
    mainClass.set("com.rappiclone.catalog.ApplicationKt")
}

dependencies {
    implementation(project(":shared:shared-infra"))

    // CSV parsing pra upload em massa de produtos
    implementation("com.github.doyaaaaaken:kotlin-csv-jvm:1.10.0")

    testImplementation(project(":shared:shared-test"))
}
