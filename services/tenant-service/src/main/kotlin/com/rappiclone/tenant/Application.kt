package com.rappiclone.tenant

import com.rappiclone.infra.config.loadServiceConfig
import com.rappiclone.infra.ktor.installBasePlugins
import com.rappiclone.tenant.repository.TenantRepository
import com.rappiclone.tenant.repository.ZoneRepository
import com.rappiclone.tenant.routes.tenantRoutes
import com.rappiclone.tenant.service.TenantService
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.rappiclone.tenant.Application")

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
        ?: throw IllegalStateException("Database config obrigatoria para tenant-service")

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

    val tenantRepository = TenantRepository()
    val zoneRepository = ZoneRepository()
    val tenantService = TenantService(tenantRepository, zoneRepository)

    installBasePlugins(config.name)

    routing {
        tenantRoutes(tenantService)
    }

    logger.info("${config.name} pronto!")
}
