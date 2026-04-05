package com.rappiclone.identity.service

import com.rappiclone.domain.enums.UserRole
import com.rappiclone.domain.errors.DomainError
import com.rappiclone.domain.errors.DomainException
import com.rappiclone.identity.model.*
import com.rappiclone.identity.repository.RefreshTokenRepository
import com.rappiclone.identity.repository.UserRepository
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import java.util.UUID

class AuthService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val jwtService: JwtService,
    private val passwordService: PasswordService
) {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    suspend fun register(request: RegisterRequest, tenantId: String?): AuthResponse {
        val existingUser = userRepository.findByEmail(request.email)
        if (existingUser != null) {
            throw DomainException(DomainError.Conflict("Email ja cadastrado: ${request.email}"))
        }

        if (request.phone != null) {
            val existingPhone = userRepository.findByPhone(request.phone)
            if (existingPhone != null) {
                throw DomainException(DomainError.Conflict("Telefone ja cadastrado: ${request.phone}"))
            }
        }

        val hashedPassword = passwordService.hash(request.password)
        val user = userRepository.create(
            email = request.email,
            hashedPassword = hashedPassword,
            phone = request.phone,
            role = request.role,
            tenantId = tenantId
        )

        logger.info("Usuario registrado: ${user.id} (${user.email}, role=${user.role})")
        return generateAuthResponse(user.id, user.email, user.role, user.phone, user.isVerified)
    }

    suspend fun login(request: LoginRequest): AuthResponse {
        val user = userRepository.findByEmail(request.email)

        if (user == null) {
            // Timing attack prevention: roda bcrypt dummy pra equalizar tempo
            passwordService.dummyVerify()
            throw DomainException(DomainError.InvalidCredentials())
        }

        if (!user.isActive) {
            passwordService.dummyVerify()
            throw DomainException(DomainError.InvalidCredentials())
        }

        if (!passwordService.verify(request.password, user.hashedPassword)) {
            throw DomainException(DomainError.InvalidCredentials())
        }

        logger.info("Login bem-sucedido: ${user.id} (${user.email})")
        return generateAuthResponse(user.id, user.email, UserRole.valueOf(user.role.name), user.phone, user.isVerified)
    }

    suspend fun refresh(refreshToken: String): AuthResponse {
        val storedToken = refreshTokenRepository.findByToken(refreshToken)
            ?: throw DomainException(DomainError.InvalidCredentials())

        if (storedToken.expiresAt.isBefore(OffsetDateTime.now())) {
            refreshTokenRepository.revoke(refreshToken)
            throw DomainException(DomainError.TokenExpired())
        }

        val user = userRepository.findById(storedToken.userId)
            ?: throw DomainException(DomainError.InvalidCredentials())

        // Revoga token antigo (rotation)
        refreshTokenRepository.revoke(refreshToken)

        logger.info("Token refreshed: ${user.id}")
        return generateAuthResponse(user.id, user.email, UserRole.valueOf(user.role.name), user.phone, user.isVerified)
    }

    suspend fun getUser(userId: String): UserResponse {
        val user = userRepository.findById(UUID.fromString(userId))
            ?: throw DomainException(DomainError.NotFound("Usuario", userId))

        return UserResponse(
            id = user.id.toString(),
            email = user.email,
            phone = user.phone,
            role = user.role,
            isVerified = user.isVerified
        )
    }

    private suspend fun generateAuthResponse(
        userId: UUID,
        email: String,
        role: UserRole,
        phone: String?,
        isVerified: Boolean
    ): AuthResponse {
        val accessToken = jwtService.generateAccessToken(userId, email, role)
        val refreshToken = jwtService.generateRefreshToken()

        val jwtConfig = jwtService
        val expiresAt = OffsetDateTime.now().plusDays(30) // TODO: usar config
        refreshTokenRepository.create(userId, refreshToken, expiresAt)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = jwtService.accessTokenExpirationSeconds,
            user = UserResponse(
                id = userId.toString(),
                email = email,
                phone = phone,
                role = role,
                isVerified = isVerified
            )
        )
    }
}
