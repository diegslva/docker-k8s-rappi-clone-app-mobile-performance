package com.rappiclone.onboarding.repository

import com.rappiclone.domain.enums.CourierVehicle
import com.rappiclone.domain.enums.OnboardingStatus
import com.rappiclone.onboarding.model.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.OffsetDateTime
import java.util.UUID

class CourierApplicationRepository {

    private fun ResultRow.toResponse(documents: List<DocumentResponse> = emptyList()): CourierApplicationResponse =
        CourierApplicationResponse(
            id = this[CourierApplicationsTable.id].value.toString(),
            tenantId = this[CourierApplicationsTable.tenantId],
            userId = this[CourierApplicationsTable.userId].toString(),
            fullName = this[CourierApplicationsTable.fullName],
            cpf = this[CourierApplicationsTable.cpf],
            phone = this[CourierApplicationsTable.phone],
            email = this[CourierApplicationsTable.email],
            vehicleType = CourierVehicle.valueOf(this[CourierApplicationsTable.vehicleType]),
            vehiclePlate = this[CourierApplicationsTable.vehiclePlate],
            status = OnboardingStatus.valueOf(this[CourierApplicationsTable.status]),
            rejectionReason = this[CourierApplicationsTable.rejectionReason],
            documents = documents,
            createdAt = this[CourierApplicationsTable.createdAt].toString()
        )

    private fun ResultRow.toDocResponse(): DocumentResponse = DocumentResponse(
        id = this[CourierApplicationDocumentsTable.id].value.toString(),
        documentType = this[CourierApplicationDocumentsTable.documentType],
        mediaId = this[CourierApplicationDocumentsTable.mediaId].toString(),
        status = this[CourierApplicationDocumentsTable.status]
    )

    suspend fun create(request: CreateCourierApplicationRequest, userId: UUID, tenantId: String): CourierApplicationResponse = dbQuery {
        val now = OffsetDateTime.now()
        val cpf = Cpf(request.cpf)

        val id = CourierApplicationsTable.insertAndGetId {
            it[CourierApplicationsTable.tenantId] = tenantId
            it[CourierApplicationsTable.userId] = userId
            it[fullName] = request.fullName
            it[CourierApplicationsTable.cpf] = cpf.value.replace(Regex("[^0-9]"), "")
            it[phone] = request.phone
            it[email] = request.email
            it[dateOfBirth] = kotlinx.datetime.LocalDate.parse(request.dateOfBirth)
            it[vehicleType] = request.vehicleType.name
            it[vehiclePlate] = request.vehiclePlate
            it[cnhNumber] = request.cnhNumber
            it[cnhCategory] = request.cnhCategory
            it[cnhExpiry] = request.cnhExpiry?.let { kotlinx.datetime.LocalDate.parse(it) }
            it[createdAt] = now
            it[updatedAt] = now
        }
        findByIdInternal(id.value, tenantId)!!
    }

    suspend fun findById(id: UUID, tenantId: String): CourierApplicationResponse? = dbQuery {
        findByIdInternal(id, tenantId)
    }

    suspend fun findByTenant(tenantId: String, status: OnboardingStatus?): List<CourierApplicationResponse> = dbQuery {
        CourierApplicationsTable.selectAll()
            .where {
                val conditions = mutableListOf(CourierApplicationsTable.tenantId eq tenantId)
                if (status != null) conditions.add(CourierApplicationsTable.status eq status.name)
                conditions.reduce { acc, op -> acc and op }
            }
            .orderBy(CourierApplicationsTable.createdAt, SortOrder.DESC)
            .map { it.toResponse() }
    }

    suspend fun updateStatus(id: UUID, tenantId: String, status: OnboardingStatus, reviewedBy: UUID?, rejectionReason: String?): Boolean = dbQuery {
        CourierApplicationsTable.update({
            (CourierApplicationsTable.id eq id) and (CourierApplicationsTable.tenantId eq tenantId)
        }) {
            it[CourierApplicationsTable.status] = status.name
            it[CourierApplicationsTable.reviewedBy] = reviewedBy
            it[CourierApplicationsTable.rejectionReason] = rejectionReason
            it[reviewedAt] = OffsetDateTime.now()
            it[updatedAt] = OffsetDateTime.now()
        } > 0
    }

    suspend fun addDocument(applicationId: UUID, documentType: String, mediaId: UUID): DocumentResponse = dbQuery {
        val id = CourierApplicationDocumentsTable.insertAndGetId {
            it[CourierApplicationDocumentsTable.applicationId] = applicationId
            it[CourierApplicationDocumentsTable.documentType] = documentType
            it[CourierApplicationDocumentsTable.mediaId] = mediaId
            it[createdAt] = OffsetDateTime.now()
        }
        CourierApplicationDocumentsTable.selectAll()
            .where { CourierApplicationDocumentsTable.id eq id }
            .single()
            .toDocResponse()
    }

    private fun findByIdInternal(id: UUID, tenantId: String): CourierApplicationResponse? {
        val app = CourierApplicationsTable.selectAll()
            .where { (CourierApplicationsTable.id eq id) and (CourierApplicationsTable.tenantId eq tenantId) }
            .singleOrNull() ?: return null

        val docs = CourierApplicationDocumentsTable.selectAll()
            .where { CourierApplicationDocumentsTable.applicationId eq id }
            .map { it.toDocResponse() }

        return app.toResponse(docs)
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
