package com.rappiclone.gateway.config

import com.typesafe.config.ConfigFactory

data class GatewayConfig(
    val port: Int,
    val jwt: JwtConfig,
    val services: ServiceEndpoints,
    val rateLimit: RateLimitConfig
)

data class JwtConfig(
    val secret: String,
    val issuer: String
)

data class RateLimitConfig(
    val requestsPerMinute: Int
)

/**
 * Enderecos de todos os microservices.
 * Em dev: localhost com portas diferentes.
 * Em prod: K8s service DNS (ex: http://identity-service:8080).
 */
data class ServiceEndpoints(
    val identity: String,
    val tenant: String,
    val userProfile: String,
    val catalog: String,
    val search: String,
    val cart: String,
    val order: String,
    val payment: String,
    val courier: String,
    val tracking: String,
    val notification: String,
    val rating: String,
    val promotion: String,
    val pricing: String,
    val geolocation: String,
    val media: String
)

fun loadGatewayConfig(): GatewayConfig {
    val config = ConfigFactory.load()
    val svc = config.getConfig("services")

    return GatewayConfig(
        port = config.getInt("service.port"),
        jwt = JwtConfig(
            secret = config.getString("jwt.secret"),
            issuer = config.getString("jwt.issuer")
        ),
        rateLimit = RateLimitConfig(
            requestsPerMinute = config.getInt("rateLimit.requestsPerMinute")
        ),
        services = ServiceEndpoints(
            identity = svc.getString("identity"),
            tenant = svc.getString("tenant"),
            userProfile = svc.getString("userProfile"),
            catalog = svc.getString("catalog"),
            search = svc.getString("search"),
            cart = svc.getString("cart"),
            order = svc.getString("order"),
            payment = svc.getString("payment"),
            courier = svc.getString("courier"),
            tracking = svc.getString("tracking"),
            notification = svc.getString("notification"),
            rating = svc.getString("rating"),
            promotion = svc.getString("promotion"),
            pricing = svc.getString("pricing"),
            geolocation = svc.getString("geolocation"),
            media = svc.getString("media")
        )
    )
}
