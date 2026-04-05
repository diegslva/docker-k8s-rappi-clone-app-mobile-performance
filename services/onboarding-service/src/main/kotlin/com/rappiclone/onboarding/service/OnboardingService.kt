package com.rappiclone.onboarding.service

import com.rappiclone.domain.enums.OnboardingStatus
import com.rappiclone.domain.errors.DomainError
import com.rappiclone.domain.errors.DomainException
import com.rappiclone.onboarding.model.*
import com.rappiclone.onboarding.repository.CourierApplicationRepository
import com.rappiclone.onboarding.repository.StoreApplicationRepository
import org.slf4j.LoggerFactory
import java.util.UUID

class OnboardingService(
    private val storeRepo: StoreApplicationRepository,
    private val courierRepo: CourierApplicationRepository
) {
    private val logger = LoggerFactory.getLogger(OnboardingService::class.java)

    // --- Store Applications ---

    suspend fun createStoreApplication(
        request: CreateStoreApplicationRequest,
        ownerUserId: UUID,
        tenantId: String
    ): StoreApplicationResponse {
        require(request.storeName.isNotBlank()) { "storeName obrigatorio" }
        require(request.taxId.isNotBlank()) { "taxId (CNPJ) obrigatorio" }
        require(request.legalName.isNotBlank()) { "legalName obrigatorio" }
        require(request.state.length == 2) { "state deve ter 2 caracteres (UF)" }

        // Valida CNPJ
        Cnpj(request.taxId)

        val app = storeRepo.create(request, ownerUserId, tenantId)
        logger.info("Store application criada: ${app.id} (${app.storeName}) tenant=$tenantId")
        return app
    }

    suspend fun getStoreApplication(id: String, tenantId: String): StoreApplicationResponse {
        return storeRepo.findById(UUID.fromString(id), tenantId)
            ?: throw DomainException(DomainError.NotFound("StoreApplication", id))
    }

    suspend fun listStoreApplications(tenantId: String, status: OnboardingStatus?): List<StoreApplicationResponse> {
        return storeRepo.findByTenant(tenantId, status)
    }

    suspend fun addStoreDocument(applicationId: String, tenantId: String, request: AddDocumentRequest): DocumentResponse {
        val app = storeRepo.findById(UUID.fromString(applicationId), tenantId)
            ?: throw DomainException(DomainError.NotFound("StoreApplication", applicationId))

        if (app.status == OnboardingStatus.APPROVED || app.status == OnboardingStatus.REJECTED) {
            throw DomainException(DomainError.ValidationError("Nao pode adicionar documentos a uma aplicacao ${app.status}"))
        }

        val doc = storeRepo.addDocument(
            UUID.fromString(applicationId),
            request.documentType,
            UUID.fromString(request.mediaId)
        )

        // Transiciona pra DOCUMENTS_SUBMITTED se estava PENDING
        if (app.status == OnboardingStatus.PENDING_DOCUMENTS) {
            storeRepo.updateStatus(UUID.fromString(applicationId), tenantId, OnboardingStatus.DOCUMENTS_SUBMITTED, null, null)
        }

        logger.info("Documento adicionado a store application $applicationId: ${request.documentType}")
        return doc
    }

    suspend fun reviewStoreApplication(
        applicationId: String,
        tenantId: String,
        reviewedBy: UUID,
        request: ReviewApplicationRequest
    ): StoreApplicationResponse {
        val app = storeRepo.findById(UUID.fromString(applicationId), tenantId)
            ?: throw DomainException(DomainError.NotFound("StoreApplication", applicationId))

        if (app.status == OnboardingStatus.APPROVED || app.status == OnboardingStatus.REJECTED) {
            throw DomainException(DomainError.ValidationError("Aplicacao ja foi revisada: ${app.status}"))
        }

        val newStatus = if (request.approved) OnboardingStatus.APPROVED else OnboardingStatus.REJECTED
        if (!request.approved && request.rejectionReason.isNullOrBlank()) {
            throw DomainException(DomainError.ValidationError("Motivo de rejeicao obrigatorio"))
        }

        storeRepo.updateStatus(UUID.fromString(applicationId), tenantId, newStatus, reviewedBy, request.rejectionReason)

        logger.info("Store application $applicationId revisada: $newStatus por $reviewedBy")
        return storeRepo.findById(UUID.fromString(applicationId), tenantId)!!
    }

    // --- Courier Applications ---

    suspend fun createCourierApplication(
        request: CreateCourierApplicationRequest,
        userId: UUID,
        tenantId: String
    ): CourierApplicationResponse {
        require(request.fullName.isNotBlank()) { "fullName obrigatorio" }
        require(request.phone.isNotBlank()) { "phone obrigatorio" }
        require(request.email.isNotBlank()) { "email obrigatorio" }

        // Valida CPF
        Cpf(request.cpf)

        // Valida CNH se veiculo motorizado
        if (request.vehicleType in listOf(com.rappiclone.domain.enums.CourierVehicle.MOTORCYCLE, com.rappiclone.domain.enums.CourierVehicle.CAR)) {
            require(!request.cnhNumber.isNullOrBlank()) { "cnhNumber obrigatorio para ${request.vehicleType}" }
            require(!request.cnhExpiry.isNullOrBlank()) { "cnhExpiry obrigatorio para ${request.vehicleType}" }
        }

        val app = courierRepo.create(request, userId, tenantId)
        logger.info("Courier application criada: ${app.id} (${app.fullName}) tenant=$tenantId")
        return app
    }

    suspend fun getCourierApplication(id: String, tenantId: String): CourierApplicationResponse {
        return courierRepo.findById(UUID.fromString(id), tenantId)
            ?: throw DomainException(DomainError.NotFound("CourierApplication", id))
    }

    suspend fun listCourierApplications(tenantId: String, status: OnboardingStatus?): List<CourierApplicationResponse> {
        return courierRepo.findByTenant(tenantId, status)
    }

    suspend fun addCourierDocument(applicationId: String, tenantId: String, request: AddDocumentRequest): DocumentResponse {
        val app = courierRepo.findById(UUID.fromString(applicationId), tenantId)
            ?: throw DomainException(DomainError.NotFound("CourierApplication", applicationId))

        if (app.status == OnboardingStatus.APPROVED || app.status == OnboardingStatus.REJECTED) {
            throw DomainException(DomainError.ValidationError("Nao pode adicionar documentos a uma aplicacao ${app.status}"))
        }

        val doc = courierRepo.addDocument(
            UUID.fromString(applicationId),
            request.documentType,
            UUID.fromString(request.mediaId)
        )

        if (app.status == OnboardingStatus.PENDING_DOCUMENTS) {
            courierRepo.updateStatus(UUID.fromString(applicationId), tenantId, OnboardingStatus.DOCUMENTS_SUBMITTED, null, null)
        }

        logger.info("Documento adicionado a courier application $applicationId: ${request.documentType}")
        return doc
    }

    suspend fun reviewCourierApplication(
        applicationId: String,
        tenantId: String,
        reviewedBy: UUID,
        request: ReviewApplicationRequest
    ): CourierApplicationResponse {
        val app = courierRepo.findById(UUID.fromString(applicationId), tenantId)
            ?: throw DomainException(DomainError.NotFound("CourierApplication", applicationId))

        if (app.status == OnboardingStatus.APPROVED || app.status == OnboardingStatus.REJECTED) {
            throw DomainException(DomainError.ValidationError("Aplicacao ja foi revisada: ${app.status}"))
        }

        val newStatus = if (request.approved) OnboardingStatus.APPROVED else OnboardingStatus.REJECTED
        if (!request.approved && request.rejectionReason.isNullOrBlank()) {
            throw DomainException(DomainError.ValidationError("Motivo de rejeicao obrigatorio"))
        }

        courierRepo.updateStatus(UUID.fromString(applicationId), tenantId, newStatus, reviewedBy, request.rejectionReason)

        logger.info("Courier application $applicationId revisada: $newStatus por $reviewedBy")
        return courierRepo.findById(UUID.fromString(applicationId), tenantId)!!
    }
}
