package com.rappiclone.gateway.middleware

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class RateLimitTest {

    @Test
    fun `deve permitir requests dentro do limite`() {
        val limiter = InMemoryRateLimiter(maxRequests = 5)

        repeat(5) {
            assertTrue(limiter.isAllowed("client-1"), "Request ${it + 1} deveria ser permitida")
        }
    }

    @Test
    fun `deve bloquear apos exceder limite`() {
        val limiter = InMemoryRateLimiter(maxRequests = 3)

        repeat(3) { assertTrue(limiter.isAllowed("client-1")) }
        assertFalse(limiter.isAllowed("client-1"), "4o request deveria ser bloqueado")
        assertFalse(limiter.isAllowed("client-1"), "5o request deveria ser bloqueado")
    }

    @Test
    fun `clientes diferentes devem ter limites independentes`() {
        val limiter = InMemoryRateLimiter(maxRequests = 2)

        repeat(2) { assertTrue(limiter.isAllowed("client-a")) }
        assertFalse(limiter.isAllowed("client-a"))

        // client-b nao deve ser afetado
        assertTrue(limiter.isAllowed("client-b"))
        assertTrue(limiter.isAllowed("client-b"))
        assertFalse(limiter.isAllowed("client-b"))
    }

    @Test
    fun `remaining deve decrementar corretamente`() {
        val limiter = InMemoryRateLimiter(maxRequests = 5)

        assertEquals(5, limiter.remaining("client-1"))

        limiter.isAllowed("client-1")
        assertEquals(4, limiter.remaining("client-1"))

        repeat(3) { limiter.isAllowed("client-1") }
        assertEquals(1, limiter.remaining("client-1"))

        limiter.isAllowed("client-1")
        assertEquals(0, limiter.remaining("client-1"))

        // Nao deve ficar negativo
        limiter.isAllowed("client-1")
        assertEquals(0, limiter.remaining("client-1"))
    }

    @Test
    fun `remaining de cliente desconhecido deve retornar max`() {
        val limiter = InMemoryRateLimiter(maxRequests = 10)
        assertEquals(10, limiter.remaining("nunca-visto"))
    }
}
