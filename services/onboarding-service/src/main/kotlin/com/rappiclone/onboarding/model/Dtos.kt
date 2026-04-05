package com.rappiclone.onboarding.model

import com.rappiclone.domain.enums.OnboardingStatus
import com.rappiclone.domain.enums.StoreCategory
import com.rappiclone.domain.enums.TaxRegime
import com.rappiclone.domain.enums.CourierVehicle
import kotlinx.serialization.Serializable

// --- Value types ---

@JvmInline
@Serializable
value class Cpf(val value: String) {
    init {
        val digits = value.replace(Regex("[^0-9]"), "")
        require(digits.length == 11) { "CPF deve ter 11 digitos: $value" }
        require(!digits.all { it == digits[0] }) { "CPF invalido: todos os digitos iguais" }
    }

    val formatted: String
        get() {
            val d = value.replace(Regex("[^0-9]"), "")
            return "${d.substring(0,3)}.${d.substring(3,6)}.${d.substring(6,9)}-${d.substring(9,11)}"
        }
}

@JvmInline
@Serializable
value class Cnpj(val value: String) {
    init {
        val digits = value.replace(Regex("[^0-9]"), "")
        require(digits.length == 14) { "CNPJ deve ter 14 digitos: $value" }
    }

    val formatted: String
        get() {
            val d = value.replace(Regex("[^0-9]"), "")
            return "${d.substring(0,2)}.${d.substring(2,5)}.${d.substring(5,8)}/${d.substring(8,12)}-${d.substring(12,14)}"
        }
}

@Serializable
enum class StoreDocumentType {
    CNPJ_CARD,
    ALVARA,
    HEALTH_LICENSE,
    OWNER_ID,
    BANK_STATEMENT,
    CONTRACT_SIGNED
}

@Serializable
enum class CourierDocumentType {
    CPF_FRONT,
    CPF_BACK,
    CNH_FRONT,
    CNH_BACK,
    SELFIE_WITH_DOCUMENT,
    VEHICLE_REGISTRATION,
    CRIMINAL_RECORD
}

// --- Requests ---

@Serializable
data class CreateStoreApplicationRequest(
    val storeName: String,
    val storeCategory: StoreCategory,
    val taxId: String,
    val taxRegime: TaxRegime = TaxRegime.SIMPLES_NACIONAL,
    val legalName: String,
    val phone: String,
    val email: String,
    val street: String,
    val number: String,
    val complement: String? = null,
    val neighborhood: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val latitude: Double? = null,
    val longitude: Double? = null
)

@Serializable
data class CreateCourierApplicationRequest(
    val fullName: String,
    val cpf: String,
    val phone: String,
    val email: String,
    val dateOfBirth: String,
    val vehicleType: CourierVehicle,
    val vehiclePlate: String? = null,
    val cnhNumber: String? = null,
    val cnhCategory: String? = null,
    val cnhExpiry: String? = null
)

@Serializable
data class AddDocumentRequest(
    val documentType: String,
    val mediaId: String
)

@Serializable
data class ReviewApplicationRequest(
    val approved: Boolean,
    val rejectionReason: String? = null
)

// --- Responses ---

@Serializable
data class StoreApplicationResponse(
    val id: String,
    val tenantId: String,
    val ownerUserId: String,
    val storeName: String,
    val storeCategory: StoreCategory,
    val taxId: String,
    val taxRegime: TaxRegime,
    val legalName: String,
    val phone: String,
    val email: String,
    val status: OnboardingStatus,
    val rejectionReason: String?,
    val documents: List<DocumentResponse> = emptyList(),
    val createdAt: String
)

@Serializable
data class CourierApplicationResponse(
    val id: String,
    val tenantId: String,
    val userId: String,
    val fullName: String,
    val cpf: String,
    val phone: String,
    val email: String,
    val vehicleType: CourierVehicle,
    val vehiclePlate: String?,
    val status: OnboardingStatus,
    val rejectionReason: String?,
    val documents: List<DocumentResponse> = emptyList(),
    val createdAt: String
)

@Serializable
data class DocumentResponse(
    val id: String,
    val documentType: String,
    val mediaId: String,
    val status: String
)
