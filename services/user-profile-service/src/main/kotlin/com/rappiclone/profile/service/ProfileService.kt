package com.rappiclone.profile.service

import com.rappiclone.domain.errors.DomainError
import com.rappiclone.domain.errors.DomainException
import com.rappiclone.profile.model.*
import com.rappiclone.profile.repository.ProfileRepository
import org.slf4j.LoggerFactory
import java.util.UUID

class ProfileService(
    private val profileRepository: ProfileRepository
) {
    private val logger = LoggerFactory.getLogger(ProfileService::class.java)

    suspend fun createProfile(request: CreateProfileRequest, tenantId: String): ProfileResponse {
        require(request.firstName.isNotBlank()) { "firstName obrigatorio" }
        require(request.lastName.isNotBlank()) { "lastName obrigatorio" }

        val existing = profileRepository.findByUserId(UUID.fromString(request.userId), tenantId)
        if (existing != null) {
            throw DomainException(DomainError.Conflict("Perfil ja existe para userId: ${request.userId}"))
        }

        val profile = profileRepository.create(request, tenantId)
        logger.info("Perfil criado: ${profile.id} para userId=${request.userId} tenant=$tenantId")
        return profile
    }

    suspend fun getProfile(userId: String, tenantId: String): ProfileResponse {
        return profileRepository.findByUserId(UUID.fromString(userId), tenantId)
            ?: throw DomainException(DomainError.NotFound("Perfil", userId))
    }

    suspend fun getProfileById(profileId: String, tenantId: String): ProfileResponse {
        return profileRepository.findById(UUID.fromString(profileId), tenantId)
            ?: throw DomainException(DomainError.NotFound("Perfil", profileId))
    }

    suspend fun updateProfile(userId: String, tenantId: String, request: UpdateProfileRequest): ProfileResponse {
        val existing = profileRepository.findByUserId(UUID.fromString(userId), tenantId)
            ?: throw DomainException(DomainError.NotFound("Perfil", userId))

        return profileRepository.update(UUID.fromString(existing.id), tenantId, request)
            ?: throw DomainException(DomainError.InternalError("Falha ao atualizar perfil"))
    }

    suspend fun addAddress(userId: String, tenantId: String, request: CreateAddressRequest): AddressResponse {
        require(request.street.isNotBlank()) { "street obrigatorio" }
        require(request.number.isNotBlank()) { "number obrigatorio" }
        require(request.neighborhood.isNotBlank()) { "neighborhood obrigatorio" }
        require(request.city.isNotBlank()) { "city obrigatorio" }
        require(request.state.length == 2) { "state deve ter 2 caracteres" }
        require(request.zipCode.isNotBlank()) { "zipCode obrigatorio" }

        val profile = profileRepository.findByUserId(UUID.fromString(userId), tenantId)
            ?: throw DomainException(DomainError.NotFound("Perfil", userId))

        val address = profileRepository.addAddress(UUID.fromString(profile.id), tenantId, request)
        logger.info("Endereco adicionado: ${address.id} ao perfil ${profile.id}")
        return address
    }

    suspend fun deleteAddress(userId: String, tenantId: String, addressId: String): Boolean {
        val profile = profileRepository.findByUserId(UUID.fromString(userId), tenantId)
            ?: throw DomainException(DomainError.NotFound("Perfil", userId))

        val deleted = profileRepository.deleteAddress(UUID.fromString(addressId), UUID.fromString(profile.id))
        if (!deleted) {
            throw DomainException(DomainError.NotFound("Endereco", addressId))
        }
        logger.info("Endereco removido: $addressId do perfil ${profile.id}")
        return true
    }
}
