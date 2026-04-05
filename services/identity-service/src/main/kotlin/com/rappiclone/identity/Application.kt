package com.rappiclone.identity

import com.rappiclone.identity.repository.RefreshTokenRepository
import com.rappiclone.identity.repository.UserRepository
import com.rappiclone.identity.routes.authRoutes
import com.rappiclone.identity.service.AuthService
import com.rappiclone.identity.service.JwtService
import com.rappiclone.identity.service.PasswordService
import com.rappiclone.infra.config.loadServiceConfig
import com.rappiclone.infra.ktor.installBasePlugins
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.rappiclone.identity.Application")

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
    // Database
    val dbConfig = config.database
        ?: throw IllegalStateException("Database config obrigatoria para identity-service")

    val dataSource = HikariDataSource(HikariConfig().apply {
        jdbcUrl = dbConfig.url
        username = dbConfig.user
        password = dbConfig.password
        maximumPoolSize = dbConfig.maxPoolSize
        driverClassName = "org.postgresql.Driver"
    })

    // Flyway migrations
    Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .load()
        .migrate()

    Database.connect(dataSource)

    // Dependencies
    val userRepository = UserRepository()
    val refreshTokenRepository = RefreshTokenRepository()
    val jwtService = JwtService.fromConfig()
    val passwordService = PasswordService()
    val authService = AuthService(userRepository, refreshTokenRepository, jwtService, passwordService)

    // Ktor plugins
    installBasePlugins(config.name)

    // Routes
    routing {
        authRoutes(authService, jwtService)
    }

    logger.info("${config.name} pronto!")
}
