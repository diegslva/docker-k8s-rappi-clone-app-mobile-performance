package com.rappiclone.profile.model

import kotlinx.serialization.Serializable

// --- Requests ---

@Serializable
data class CreateProfileRequest(
    val userId: String,
    val firstName: String,
    val lastName: String,
    val displayName: String? = null,
    val phone: String? = null,
    val language: String = "pt-BR"
)

@Serializable
data class UpdateProfileRequest(
    val firstName: String? = null,
    val lastName: String? = null,
    val displayName: String? = null,
    val phone: String? = null,
    val avatarUrl: String? = null,
    val language: String? = null
)

@Serializable
data class CreateAddressRequest(
    val label: String = "Casa",
    val street: String,
    val number: String,
    val complement: String? = null,
    val neighborhood: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isDefault: Boolean = false
)

// --- Responses ---

@Serializable
data class ProfileResponse(
    val id: String,
    val userId: String,
    val tenantId: String,
    val firstName: String,
    val lastName: String,
    val displayName: String?,
    val phone: String?,
    val avatarUrl: String?,
    val language: String,
    val addresses: List<AddressResponse> = emptyList()
)

@Serializable
data class AddressResponse(
    val id: String,
    val label: String,
    val street: String,
    val number: String,
    val complement: String?,
    val neighborhood: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val latitude: Double?,
    val longitude: Double?,
    val isDefault: Boolean
) {
    val formatted: String
        get() = buildString {
            append("$street, $number")
            if (!complement.isNullOrBlank()) append(" - $complement")
            append(" - $neighborhood, $city - $state, $zipCode")
        }
}
