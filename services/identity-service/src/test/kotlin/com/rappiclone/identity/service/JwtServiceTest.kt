package com.rappiclone.identity.service

import com.rappiclone.domain.enums.UserRole
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.util.UUID

class JwtServiceTest {

    private val jwtService = JwtService(
        JwtConfig(
            secret = "test-secret-minimum-256-bits-long-for-hmac256-algorithm!!",
            issuer = "rappiclone-test",
            accessTokenExpirationMinutes = 30,
            refreshTokenExpirationDays = 30
        )
    )

    @Test
    fun `generateAccessToken deve gerar token valido`() {
        val userId = UUID.randomUUID()
        val email = "test@rappiclone.com"
        val role = UserRole.CUSTOMER

        val token = jwtService.generateAccessToken(userId, email, role)

        assertNotNull(token)
        assertTrue(token.isNotBlank())
    }

    @Test
    fun `verifyAccessToken deve retornar payload correto`() {
        val userId = UUID.randomUUID()
        val email = "test@rappiclone.com"
        val role = UserRole.MERCHANT

        val token = jwtService.generateAccessToken(userId, email, role)
        val payload = jwtService.verifyAccessToken(token)

        assertNotNull(payload)
        assertEquals(userId.toString(), payload!!.userId)
        assertEquals(email, payload.email)
        assertEquals(role, payload.role)
    }

    @Test
    fun `verifyAccessToken deve retornar null pra token invalido`() {
        val payload = jwtService.verifyAccessToken("token-invalido-qualquer")

        assertNull(payload)
    }

    @Test
    fun `verifyAccessToken deve retornar null pra token de outro issuer`() {
        val otherJwt = JwtService(
            JwtConfig(
                secret = "outro-secret-completamente-diferente-do-original-256bits!!",
                issuer = "outro-issuer",
                accessTokenExpirationMinutes = 30,
                refreshTokenExpirationDays = 30
            )
        )
        val token = otherJwt.generateAccessToken(UUID.randomUUID(), "x@x.com", UserRole.CUSTOMER)

        val payload = jwtService.verifyAccessToken(token)

        assertNull(payload)
    }

    @Test
    fun `verifyAccessToken deve rejeitar token expirado`() {
        val expiredJwt = JwtService(
            JwtConfig(
                secret = "test-secret-minimum-256-bits-long-for-hmac256-algorithm!!",
                issuer = "rappiclone-test",
                accessTokenExpirationMinutes = 0, // expira imediatamente
                refreshTokenExpirationDays = 30
            )
        )
        val token = expiredJwt.generateAccessToken(UUID.randomUUID(), "x@x.com", UserRole.CUSTOMER)

        // Token com expiracao 0 minutos ja nasce expirado
        val payload = expiredJwt.verifyAccessToken(token)

        assertNull(payload)
    }

    @Test
    fun `generateRefreshToken deve gerar tokens unicos`() {
        val token1 = jwtService.generateRefreshToken()
        val token2 = jwtService.generateRefreshToken()

        assertNotEquals(token1, token2)
    }

    @Test
    fun `accessTokenExpirationSeconds deve calcular corretamente`() {
        assertEquals(1800, jwtService.accessTokenExpirationSeconds) // 30 min * 60
    }

    @Test
    fun `token deve conter todas as roles`() {
        UserRole.entries.forEach { role ->
            val token = jwtService.generateAccessToken(UUID.randomUUID(), "test@test.com", role)
            val payload = jwtService.verifyAccessToken(token)

            assertNotNull(payload)
            assertEquals(role, payload!!.role)
        }
    }
}
