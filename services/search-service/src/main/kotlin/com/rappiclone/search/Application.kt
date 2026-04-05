package com.rappiclone.search

import com.rappiclone.infra.config.loadServiceConfig
import com.rappiclone.infra.ktor.installBasePlugins
import com.rappiclone.search.index.SearchIndex
import com.rappiclone.search.routes.searchRoutes
import com.rappiclone.search.service.SearchService
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.rappiclone.search.Application")

fun main() {
    val config = loadServiceConfig()
    logger.info("Iniciando ${config.name} na porta ${config.port}")

    embeddedServer(Netty, port = config.port) {
        module(config)
    }.start(wait = true)
}

fun Application.module(
    config: com.rappiclone.infra.config.ServiceConfig = loadServiceConfig(),
    searchIndexOverride: SearchIndex? = null
) {
    // TODO: implementar ElasticsearchSearchIndex quando Elasticsearch estiver rodando
    val searchIndex = searchIndexOverride
        ?: throw IllegalStateException("SearchIndex implementation required. Use searchIndexOverride or configure Elasticsearch.")

    val searchService = SearchService(searchIndex)

    installBasePlugins(config.name)

    routing {
        searchRoutes(searchService)
    }

    logger.info("${config.name} pronto!")
}
