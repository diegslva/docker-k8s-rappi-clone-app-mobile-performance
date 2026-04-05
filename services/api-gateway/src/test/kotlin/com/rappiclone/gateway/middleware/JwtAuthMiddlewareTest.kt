package com.rappiclone.gateway.middleware

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.rappiclone.domain.enums.UserRole
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.util.Date
import java.util.UUID

class JwtAuthMiddlewareTest {

    private val secret = "test-secret-minimum-256-bits-long-for-hmac256-algorithm!!"
    private val issuer = "rappiclone-test"
    private val algorithm = Algorithm.HMAC256(secret)

    private fun generateToken(
        userId: String = UUID.randomUUID().toString(),
        email: String = "test@test.com",
        role: String = "CUSTOMER",
        issuer: String = this.issuer,
        expiresAt: Date = Date(System.currentTimeMillis() + 3600000)
    ): String = JWT.create()
        .withIssuer(issuer)
        .withSubject(userId)
        .withClaim("email", email)
        .withClaim("role", role)
        .withIssuedAt(Date())
        .withExpiresAt(expiresAt)
        .sign(algorithm)

    @Test
    fun `PUBLIC_ROUTES deve conter todas as rotas publicas`() {
        assertTrue(PUBLIC_ROUTES.contains("/api/v1/auth/register"))
        assertTrue(PUBLIC_ROUTES.contains("/api/v1/auth/login"))
        assertTrue(PUBLIC_ROUTES.contains("/api/v1/auth/refresh"))
        assertTrue(PUBLIC_ROUTES.contains("/health/live"))
        assertTrue(PUBLIC_ROUTES.contains("/health/ready"))
        assertTrue(PUBLIC_ROUTES.contains("/metrics"))
    }

    @Test
    fun `PUBLIC_ROUTES nao deve conter rotas protegidas`() {
        assertFalse(PUBLIC_ROUTES.contains("/api/v1/orders"))
        assertFalse(PUBLIC_ROUTES.contains("/api/v1/cart"))
        assertFalse(PUBLIC_ROUTES.contains("/api/v1/users"))
    }

    @Test
    fun `token valido deve ser decodificavel`() {
        val userId = UUID.randomUUID().toString()
        val token = generateToken(userId = userId, email = "diego@rappiclone.com", role = "MERCHANT")

        val verifier = JWT.require(algorithm).withIssuer(issuer).build()
        val decoded = verifier.verify(token)

        assertEquals(userId, decoded.subject)
        assertEquals("diego@rappiclone.com", decoded.getClaim("email").asString())
        assertEquals("MERCHANT", decoded.getClaim("role").asString())
    }

    @Test
    fun `token com role invalida deve ser detectavel`() {
        val token = generateToken(role = "SUPER_ADMIN")

        val verifier = JWT.require(algorithm).withIssuer(issuer).build()
        val decoded = verifier.verify(token)
        val roleStr = decoded.getClaim("role").asString()

        assertThrows(IllegalArgumentException::class.java) {
            UserRole.valueOf(roleStr)
        }
    }

    @Test
    fun `token expirado deve falhar verificacao`() {
        val token = generateToken(expiresAt = Date(System.currentTimeMillis() - 1000))

        val verifier = JWT.require(algorithm).withIssuer(issuer).build()
        assertThrows(com.auth0.jwt.exceptions.TokenExpiredException::class.java) {
            verifier.verify(token)
        }
    }

    @Test
    fun `token com issuer errado deve falhar verificacao`() {
        val token = generateToken(issuer = "outro-issuer")

        val verifier = JWT.require(algorithm).withIssuer(issuer).build()
        assertThrows(com.auth0.jwt.exceptions.IncorrectClaimException::class.java) {
            verifier.verify(token)
        }
    }

    @Test
    fun `token com secret errado deve falhar verificacao`() {
        val wrongAlgorithm = Algorithm.HMAC256("wrong-secret-completely-different-from-original!!")
        val token = JWT.create()
            .withIssuer(issuer)
            .withSubject("user-123")
            .withClaim("email", "test@test.com")
            .withClaim("role", "CUSTOMER")
            .withExpiresAt(Date(System.currentTimeMillis() + 3600000))
            .sign(wrongAlgorithm)

        val verifier = JWT.require(algorithm).withIssuer(issuer).build()
        assertThrows(com.auth0.jwt.exceptions.SignatureVerificationException::class.java) {
            verifier.verify(token)
        }
    }

    @Test
    fun `AuthenticatedUser deve carregar dados corretos`() {
        val user = AuthenticatedUser(
            userId = "user-123",
            email = "diego@rappiclone.com",
            role = UserRole.ADMIN
        )
        assertEquals("user-123", user.userId)
        assertEquals("diego@rappiclone.com", user.email)
        assertEquals(UserRole.ADMIN, user.role)
    }

    @Test
    fun `todas as roles devem ser aceitas no token`() {
        val verifier = JWT.require(algorithm).withIssuer(issuer).build()

        UserRole.entries.forEach { role ->
            val token = generateToken(role = role.name)
            val decoded = verifier.verify(token)
            assertEquals(role, UserRole.valueOf(decoded.getClaim("role").asString()))
        }
    }
}
