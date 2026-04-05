plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    application
}

application {
    mainClass.set("com.rappiclone.media.ApplicationKt")
}

dependencies {
    implementation(project(":shared:shared-infra"))

    // MinIO SDK (S3-compatible object storage)
    implementation("io.minio:minio:8.5.14")

    // Image processing (resize, thumbnails)
    implementation("net.coobird:thumbnailator:0.4.20")

    testImplementation(project(":shared:shared-test"))
    // Testcontainers MinIO
    testImplementation("org.testcontainers:testcontainers:2.0.4")
}
