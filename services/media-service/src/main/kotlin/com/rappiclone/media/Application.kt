package com.rappiclone.media

import com.rappiclone.infra.config.loadServiceConfig
import com.rappiclone.infra.ktor.installBasePlugins
import com.rappiclone.media.repository.MediaRepository
import com.rappiclone.media.routes.mediaRoutes
import com.rappiclone.media.service.ImageProcessor
import com.rappiclone.media.service.MediaService
import com.rappiclone.media.storage.MinioObjectStorage
import com.typesafe.config.ConfigFactory
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.rappiclone.media.Application")

fun main() {
    val config = loadServiceConfig()
    logger.info("Iniciando ${config.name} na porta ${config.port}")

    embeddedServer(Netty, port = config.port) {
        module(config)
    }.start(wait = true)
}

fun Application.module(
    config: com.rappiclone.infra.config.ServiceConfig = loadServiceConfig(),
    objectStorageOverride: com.rappiclone.media.storage.ObjectStorage? = null
) {
    val dbConfig = config.database
        ?: throw IllegalStateException("Database config obrigatoria para media-service")

    val dataSource = HikariDataSource(HikariConfig().apply {
        jdbcUrl = dbConfig.url
        username = dbConfig.user
        password = dbConfig.password
        maximumPoolSize = dbConfig.maxPoolSize
        driverClassName = "org.postgresql.Driver"
    })

    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .load()
        .migrate()

    Database.connect(dataSource)

    // Object storage (MinIO ou override pra testes)
    val appConfig = ConfigFactory.load()
    val objectStorage = objectStorageOverride ?: MinioObjectStorage(
        endpoint = appConfig.getString("minio.endpoint"),
        accessKey = appConfig.getString("minio.accessKey"),
        secretKey = appConfig.getString("minio.secretKey"),
        publicBaseUrl = appConfig.getString("minio.publicBaseUrl")
    )

    val bucket = if (appConfig.hasPath("minio.bucket")) appConfig.getString("minio.bucket") else "rappiclone-media"

    val mediaRepository = MediaRepository()
    val imageProcessor = ImageProcessor()
    val mediaService = MediaService(mediaRepository, objectStorage, imageProcessor, bucket)

    installBasePlugins(config.name)

    routing {
        mediaRoutes(mediaService)
    }

    logger.info("${config.name} pronto!")
}
