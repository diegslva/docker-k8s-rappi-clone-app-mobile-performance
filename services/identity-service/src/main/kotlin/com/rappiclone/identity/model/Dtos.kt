package com.rappiclone.identity.model

import com.rappiclone.domain.enums.UserRole
import kotlinx.serialization.Serializable

// --- Requests ---

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val phone: String? = null,
    val role: UserRole = UserRole.CUSTOMER
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@Serializable
data class RefreshTokenRequest(
    val refreshToken: String
)

@Serializable
data class VerifyOtpRequest(
    val code: String
)

@Serializable
data class RequestOtpRequest(
    val phone: String
)

// --- Responses ---

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val user: UserResponse
)

@Serializable
data class UserResponse(
    val id: String,
    val email: String,
    val phone: String?,
    val role: UserRole,
    val isVerified: Boolean
)

@Serializable
data class MessageResponse(
    val message: String
)
