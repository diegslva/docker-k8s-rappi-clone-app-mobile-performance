package com.rappiclone.onboarding

import com.rappiclone.infra.config.loadServiceConfig
import com.rappiclone.infra.ktor.installBasePlugins
import com.rappiclone.onboarding.repository.CourierApplicationRepository
import com.rappiclone.onboarding.repository.StoreApplicationRepository
import com.rappiclone.onboarding.routes.onboardingRoutes
import com.rappiclone.onboarding.service.OnboardingService
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.rappiclone.onboarding.Application")

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
        ?: throw IllegalStateException("Database config obrigatoria para onboarding-service")

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

    val storeRepo = StoreApplicationRepository()
    val courierRepo = CourierApplicationRepository()
    val onboardingService = OnboardingService(storeRepo, courierRepo)

    installBasePlugins(config.name)

    routing {
        onboardingRoutes(onboardingService)
    }

    logger.info("${config.name} pronto!")
}
