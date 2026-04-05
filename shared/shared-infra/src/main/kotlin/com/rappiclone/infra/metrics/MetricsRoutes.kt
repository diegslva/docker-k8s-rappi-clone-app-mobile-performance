package com.rappiclone.infra.metrics

import io.ktor.http.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

/**
 * Registry global de metricas Prometheus.
 * Cada servico usa o mesmo registry via shared-infra.
 */
val prometheusRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

/**
 * Endpoint /metrics pra scrape do Prometheus.
 */
fun Routing.metricsRoutes() {
    get("/metrics") {
        call.respond(HttpStatusCode.OK, prometheusRegistry.scrape())
    }
}
