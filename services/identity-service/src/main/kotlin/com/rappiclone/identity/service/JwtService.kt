package com.rappiclone.identity.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.rappiclone.domain.enums.UserRole
import com.typesafe.config.ConfigFactory
import java.util.Date
import java.util.UUID

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val accessTokenExpirationMinutes: Long,
    val refreshTokenExpirationDays: Long
)

data class TokenPayload(
    val userId: String,
    val email: String,
    val role: UserRole
)

class JwtService(private val config: JwtConfig) {

    private val algorithm = Algorithm.HMAC256(config.secret)
    private val verifier = JWT.require(algorithm)
        .withIssuer(config.issuer)
        .build()

    fun generateAccessToken(userId: UUID, email: String, role: UserRole): String {
        val now = Date()
        val expiration = Date(now.time + config.accessTokenExpirationMinutes * 60 * 1000)

        return JWT.create()
            .withIssuer(config.issuer)
            .withSubject(userId.toString())
            .withClaim("email", email)
            .withClaim("role", role.name)
            .withIssuedAt(now)
            .withExpiresAt(expiration)
            .withJWTId(UUID.randomUUID().toString())
            .sign(algorithm)
    }

    fun generateRefreshToken(): String = UUID.randomUUID().toString()

    fun verifyAccessToken(token: String): TokenPayload? {
        return try {
            val decoded = verifier.verify(token)

            val exp = decoded.expiresAt
            if (exp == null || exp.before(Date())) return null

            TokenPayload(
                userId = decoded.subject,
                email = decoded.getClaim("email").asString(),
                role = UserRole.valueOf(decoded.getClaim("role").asString())
            )
        } catch (e: JWTVerificationException) {
            null
        }
    }

    val accessTokenExpirationSeconds: Long
        get() = config.accessTokenExpirationMinutes * 60

    companion object {
        fun fromConfig(): JwtService {
            val config = ConfigFactory.load().getConfig("jwt")
            return JwtService(
                JwtConfig(
                    secret = config.getString("secret"),
                    issuer = config.getString("issuer"),
                    accessTokenExpirationMinutes = config.getLong("accessTokenExpirationMinutes"),
                    refreshTokenExpirationDays = config.getLong("refreshTokenExpirationDays")
                )
            )
        }
    }
}
