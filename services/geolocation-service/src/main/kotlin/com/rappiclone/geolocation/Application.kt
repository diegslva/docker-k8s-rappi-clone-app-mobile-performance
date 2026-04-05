package com.rappiclone.geolocation

import com.rappiclone.infra.config.loadServiceConfig
import com.rappiclone.infra.ktor.installBasePlugins
import com.rappiclone.geolocation.provider.NominatimProvider
import com.rappiclone.geolocation.provider.OsrmProvider
import com.rappiclone.geolocation.provider.GeocodingProvider
import com.rappiclone.geolocation.provider.RoutingProvider
import com.rappiclone.geolocation.routes.geolocationRoutes
import com.rappiclone.geolocation.service.GeolocationService
import com.typesafe.config.ConfigFactory
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.rappiclone.geolocation.Application")

fun main() {
    val config = loadServiceConfig()
    logger.info("Iniciando ${config.name} na porta ${config.port}")

    embeddedServer(Netty, port = config.port) {
        module(config)
    }.start(wait = true)
}

fun Application.module(
    config: com.rappiclone.infra.config.ServiceConfig = loadServiceConfig(),
    geocodingOverride: GeocodingProvider? = null,
    routingOverride: RoutingProvider? = null
) {
    val appConfig = ConfigFactory.load()

    val httpClient = HttpClient(CIO) {
        engine { requestTimeout = 10_000 }
    }

    val geocodingProvider = geocodingOverride ?: NominatimProvider(
        baseUrl = appConfig.getString("nominatim.baseUrl"),
        httpClient = httpClient
    )

    val routingProvider = routingOverride ?: OsrmProvider(
        baseUrl = appConfig.getString("osrm.baseUrl"),
        httpClient = httpClient
    )

    val geolocationService = GeolocationService(geocodingProvider, routingProvider)

    installBasePlugins(config.name)

    routing {
        geolocationRoutes(geolocationService)
    }

    logger.info("${config.name} pronto!")
}
