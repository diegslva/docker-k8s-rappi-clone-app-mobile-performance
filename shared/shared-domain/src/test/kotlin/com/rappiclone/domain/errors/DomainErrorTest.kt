package com.rappiclone.domain.errors

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class DomainErrorTest {

    @Test
    fun `erros de auth devem ter HTTP 401 ou 403`() {
        assertEquals(401, DomainError.InvalidCredentials().httpStatus)
        assertEquals(401, DomainError.TokenExpired().httpStatus)
        assertEquals(403, DomainError.InsufficientPermissions().httpStatus)
    }

    @Test
    fun `erros de tenant devem ter codigos corretos`() {
        val notFound = DomainError.TenantNotFound("londrina-pr")
        assertEquals(404, notFound.httpStatus)
        assertEquals("TENANT_NOT_FOUND", notFound.code)
        assertTrue(notFound.message.contains("londrina-pr"))

        val inactive = DomainError.TenantInactive("londrina-pr")
        assertEquals(403, inactive.httpStatus)

        val outside = DomainError.LocationOutsideTenant()
        assertEquals(400, outside.httpStatus)
    }

    @Test
    fun `erros de store devem incluir storeId na mensagem`() {
        val notFound = DomainError.StoreNotFound("store-123")
        assertTrue(notFound.message.contains("store-123"))
        assertEquals(404, notFound.httpStatus)

        val closed = DomainError.StoreClosed("store-123")
        assertTrue(closed.message.contains("store-123"))
        assertEquals(400, closed.httpStatus)
    }

    @Test
    fun `erros de order devem ter codigos corretos`() {
        val invalidTransition = DomainError.InvalidOrderTransition("CREATED", "DELIVERED")
        assertEquals(400, invalidTransition.httpStatus)
        assertTrue(invalidTransition.message.contains("CREATED"))
        assertTrue(invalidTransition.message.contains("DELIVERED"))

        val notCancellable = DomainError.OrderNotCancellable("order-456")
        assertEquals(400, notCancellable.httpStatus)
    }

    @Test
    fun `erros de payment devem ter codigos corretos`() {
        val failed = DomainError.PaymentFailed("saldo insuficiente")
        assertEquals(400, failed.httpStatus)
        assertTrue(failed.message.contains("saldo insuficiente"))

        val notSupported = DomainError.PaymentMethodNotSupported("BITCOIN")
        assertEquals(400, notSupported.httpStatus)
        assertTrue(notSupported.message.contains("BITCOIN"))
    }

    @Test
    fun `NoCourierAvailable deve ser 503`() {
        val error = DomainError.NoCourierAvailable()
        assertEquals(503, error.httpStatus)
        assertEquals("COURIER_NONE_AVAILABLE", error.code)
    }

    @Test
    fun `erros genericos devem ter HTTP correto`() {
        assertEquals(404, DomainError.NotFound("Produto", "123").httpStatus)
        assertEquals(409, DomainError.Conflict("duplicado").httpStatus)
        assertEquals(400, DomainError.ValidationError("campo invalido").httpStatus)
        assertEquals(500, DomainError.InternalError("boom").httpStatus)
    }

    @Test
    fun `DomainException deve carregar o erro`() {
        val error = DomainError.InvalidCredentials()
        val exception = DomainException(error)

        assertEquals(error, exception.error)
        assertEquals("Credenciais invalidas", exception.message)
    }

    @Test
    fun `ApiError deve serializar codigo e mensagem`() {
        val apiError = ApiError(
            code = "TEST_ERROR",
            message = "mensagem de teste",
            details = mapOf("campo" to "valor")
        )
        assertEquals("TEST_ERROR", apiError.code)
        assertEquals("mensagem de teste", apiError.message)
        assertEquals("valor", apiError.details?.get("campo"))
    }

    @Test
    fun `ApiError sem details deve ser null`() {
        val apiError = ApiError(code = "TEST", message = "test")
        assertNull(apiError.details)
    }
}
