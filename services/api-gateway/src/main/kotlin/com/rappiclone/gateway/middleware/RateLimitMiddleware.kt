package com.rappiclone.gateway.middleware

import com.rappiclone.domain.errors.ApiError
import com.rappiclone.gateway.config.RateLimitConfig
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

private val logger = LoggerFactory.getLogger("com.rappiclone.gateway.middleware.RateLimit")

/**
 * Rate limiter in-memory com sliding window por IP.
 * Em producao, substituir por Redis (INCR + EXPIRE).
 * Interface abstrata permite trocar implementacao sem mudar o plugin.
 */
interface RateLimiter {
    fun isAllowed(key: String): Boolean
    fun remaining(key: String): Int
}

/**
 * Implementacao in-memory com fixed window de 1 minuto.
 * Suficiente pra dev. Prod usa Redis.
 */
class InMemoryRateLimiter(private val maxRequests: Int) : RateLimiter {
    private data class Window(
        val count: AtomicInteger = AtomicInteger(0),
        val windowStart: AtomicLong = AtomicLong(System.currentTimeMillis())
    )

    private val windows = ConcurrentHashMap<String, Window>()
    private val windowDurationMs = 60_000L // 1 minuto

    override fun isAllowed(key: String): Boolean {
        val window = windows.computeIfAbsent(key) { Window() }
        val now = System.currentTimeMillis()

        // Reset window se expirou
        if (now - window.windowStart.get() > windowDurationMs) {
            window.count.set(0)
            window.windowStart.set(now)
        }

        return window.count.incrementAndGet() <= maxRequests
    }

    override fun remaining(key: String): Int {
        val window = windows[key] ?: return maxRequests
        val now = System.currentTimeMillis()
        if (now - window.windowStart.get() > windowDurationMs) return maxRequests
        return (maxRequests - window.count.get()).coerceAtLeast(0)
    }
}

/**
 * Plugin Ktor que aplica rate limiting por IP.
 */
fun createRateLimitPlugin(config: RateLimitConfig, rateLimiter: RateLimiter? = null) =
    createApplicationPlugin(name = "RateLimitMiddleware") {
        val limiter = rateLimiter ?: InMemoryRateLimiter(config.requestsPerMinute)

        onCall { call ->
            val path = call.request.local.uri
            if (path == "/health/live" || path == "/health/ready" || path == "/metrics") return@onCall

            val clientIp = call.request.local.remoteAddress
            val key = "rl:$clientIp"

            if (!limiter.isAllowed(key)) {
                logger.warn("Rate limit excedido para IP $clientIp")
                call.response.header("Retry-After", "60")
                call.respond(
                    HttpStatusCode.TooManyRequests,
                    ApiError(code = "RATE_LIMIT_EXCEEDED", message = "Limite de requisicoes excedido. Tente novamente em 1 minuto.")
                )
            }
        }
    }
