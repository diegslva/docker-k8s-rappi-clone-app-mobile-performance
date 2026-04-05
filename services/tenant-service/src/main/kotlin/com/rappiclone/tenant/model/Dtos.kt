package com.rappiclone.tenant.model

import kotlinx.serialization.Serializable
import java.math.BigDecimal

// --- Value types pra strong typing ---

@JvmInline
@Serializable
value class TenantId(val value: String) {
    init {
        require(value.matches(Regex("^[a-z0-9-]+$"))) {
            "TenantId deve conter apenas letras minusculas, numeros e hifens: $value"
        }
        require(value.length in 3..100) { "TenantId deve ter entre 3 e 100 caracteres: $value" }
    }
}

@JvmInline
@Serializable
value class ZoneSlug(val value: String) {
    init {
        require(value.matches(Regex("^[a-z0-9-]+$"))) { "ZoneSlug invalido: $value" }
    }
}

@JvmInline
@Serializable
value class BrazilianState(val value: String) {
    init {
        require(value.length == 2 && value == value.uppercase()) { "UF invalida: $value" }
        require(value in VALID_STATES) { "UF desconhecida: $value" }
    }

    companion object {
        private val VALID_STATES = setOf(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO",
            "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI",
            "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"
        )
    }
}

// --- Requests ---

@Serializable
data class CreateTenantRequest(
    val id: String,
    val name: String,
    val state: String,
    val timezone: String = "America/Sao_Paulo",
    val serviceFeePct: Double = 10.0,
    val minOrderValue: Double = 15.0
)

@Serializable
data class UpdateTenantRequest(
    val name: String? = null,
    val isActive: Boolean? = null,
    val serviceFeePct: Double? = null,
    val minOrderValue: Double? = null
)

@Serializable
data class CreateZoneRequest(
    val name: String,
    val slug: String,
    val polygonWkt: String,
    val baseDeliveryFee: Double = 5.0,
    val maxDeliveryRadiusKm: Double = 10.0,
    val estimatedDeliveryMinutes: Int = 45
)

@Serializable
data class ResolveLocationRequest(
    val latitude: Double,
    val longitude: Double
)

// --- Responses ---

@Serializable
data class TenantResponse(
    val id: String,
    val name: String,
    val state: String,
    val country: String,
    val timezone: String,
    val currency: String,
    val isActive: Boolean,
    val serviceFeePct: Double,
    val minOrderValue: Double
)

@Serializable
data class ZoneResponse(
    val id: String,
    val tenantId: String,
    val name: String,
    val slug: String,
    val baseDeliveryFee: Double,
    val maxDeliveryRadiusKm: Double,
    val estimatedDeliveryMinutes: Int,
    val isActive: Boolean
)

@Serializable
data class ResolvedLocationResponse(
    val tenantId: String,
    val zoneId: String?,
    val zoneName: String?,
    val microRegionId: String?,
    val microRegionName: String?
)
