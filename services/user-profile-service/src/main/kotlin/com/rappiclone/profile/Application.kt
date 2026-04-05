package com.rappiclone.profile

import com.rappiclone.infra.config.loadServiceConfig
import com.rappiclone.infra.ktor.installBasePlugins
import com.rappiclone.profile.repository.ProfileRepository
import com.rappiclone.profile.routes.profileRoutes
import com.rappiclone.profile.service.ProfileService
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.rappiclone.profile.Application")

fun main() {
    val config = loadServiceConfig()
    logger.info("Iniciando ${config.name} na porta ${config.port}")

    embeddedServer(Netty, port = config.port) {
        module(config)
    }.start(wait = true)
}

fun Application.module(
    config: com.rappiclone.infra.config.ServiceConfig = loadServiceConfig()
) {
    val dbConfig = config.database
        ?: throw IllegalStateException("Database config obrigatoria para user-profile-service")

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

    val profileRepository = ProfileRepository()
    val profileService = ProfileService(profileRepository)

    installBasePlugins(config.name)

    routing {
        profileRoutes(profileService)
    }

    logger.info("${config.name} pronto!")
}
