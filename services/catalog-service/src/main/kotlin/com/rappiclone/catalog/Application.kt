package com.rappiclone.catalog

import com.rappiclone.infra.config.loadServiceConfig
import com.rappiclone.infra.ktor.installBasePlugins
import com.rappiclone.catalog.repository.ProductRepository
import com.rappiclone.catalog.repository.StoreRepository
import com.rappiclone.catalog.routes.catalogRoutes
import com.rappiclone.catalog.service.CatalogService
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.rappiclone.catalog.Application")

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
        ?: throw IllegalStateException("Database config obrigatoria para catalog-service")

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

    val storeRepository = StoreRepository()
    val productRepository = ProductRepository()
    val catalogService = CatalogService(storeRepository, productRepository)

    installBasePlugins(config.name)

    routing {
        catalogRoutes(catalogService)
    }

    logger.info("${config.name} pronto!")
}
