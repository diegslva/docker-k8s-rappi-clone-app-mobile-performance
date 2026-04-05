package com.rappiclone.onboarding.repository

import com.rappiclone.domain.enums.OnboardingStatus
import com.rappiclone.domain.enums.StoreCategory
import com.rappiclone.domain.enums.TaxRegime
import com.rappiclone.onboarding.model.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.OffsetDateTime
import java.util.UUID

class StoreApplicationRepository {

    private fun ResultRow.toResponse(documents: List<DocumentResponse> = emptyList()): StoreApplicationResponse =
        StoreApplicationResponse(
            id = this[StoreApplicationsTable.id].value.toString(),
            tenantId = this[StoreApplicationsTable.tenantId],
            ownerUserId = this[StoreApplicationsTable.ownerUserId].toString(),
            storeName = this[StoreApplicationsTable.storeName],
            storeCategory = StoreCategory.valueOf(this[StoreApplicationsTable.storeCategory]),
            taxId = this[StoreApplicationsTable.taxId],
            taxRegime = TaxRegime.valueOf(this[StoreApplicationsTable.taxRegime]),
            legalName = this[StoreApplicationsTable.legalName],
            phone = this[StoreApplicationsTable.phone],
            email = this[StoreApplicationsTable.email],
            status = OnboardingStatus.valueOf(this[StoreApplicationsTable.status]),
            rejectionReason = this[StoreApplicationsTable.rejectionReason],
            documents = documents,
            createdAt = this[StoreApplicationsTable.createdAt].toString()
        )

    private fun ResultRow.toDocResponse(): DocumentResponse = DocumentResponse(
        id = this[StoreApplicationDocumentsTable.id].value.toString(),
        documentType = this[StoreApplicationDocumentsTable.documentType],
        mediaId = this[StoreApplicationDocumentsTable.mediaId].toString(),
        status = this[StoreApplicationDocumentsTable.status]
    )

    suspend fun create(request: CreateStoreApplicationRequest, ownerUserId: UUID, tenantId: String): StoreApplicationResponse = dbQuery {
        val now = OffsetDateTime.now()
        val id = StoreApplicationsTable.insertAndGetId {
            it[StoreApplicationsTable.tenantId] = tenantId
            it[StoreApplicationsTable.ownerUserId] = ownerUserId
            it[storeName] = request.storeName
            it[storeCategory] = request.storeCategory.name
            it[taxId] = request.taxId
            it[taxRegime] = request.taxRegime.name
            it[legalName] = request.legalName
            it[phone] = request.phone
            it[email] = request.email
            it[street] = request.street
            it[number] = request.number
            it[complement] = request.complement
            it[neighborhood] = request.neighborhood
            it[city] = request.city
            it[state] = request.state
            it[zipCode] = request.zipCode
            it[latitude] = request.latitude
            it[longitude] = request.longitude
            it[createdAt] = now
            it[updatedAt] = now
        }
        findByIdInternal(id.value, tenantId)!!
    }

    suspend fun findById(id: UUID, tenantId: String): StoreApplicationResponse? = dbQuery {
        findByIdInternal(id, tenantId)
    }

    suspend fun findByTenant(tenantId: String, status: OnboardingStatus?): List<StoreApplicationResponse> = dbQuery {
        StoreApplicationsTable.selectAll()
            .where {
                val conditions = mutableListOf(StoreApplicationsTable.tenantId eq tenantId)
                if (status != null) conditions.add(StoreApplicationsTable.status eq status.name)
                conditions.reduce { acc, op -> acc and op }
            }
            .orderBy(StoreApplicationsTable.createdAt, SortOrder.DESC)
            .map { it.toResponse() }
    }

    suspend fun updateStatus(id: UUID, tenantId: String, status: OnboardingStatus, reviewedBy: UUID?, rejectionReason: String?): Boolean = dbQuery {
        StoreApplicationsTable.update({
            (StoreApplicationsTable.id eq id) and (StoreApplicationsTable.tenantId eq tenantId)
        }) {
            it[StoreApplicationsTable.status] = status.name
            it[StoreApplicationsTable.reviewedBy] = reviewedBy
            it[StoreApplicationsTable.rejectionReason] = rejectionReason
            it[reviewedAt] = OffsetDateTime.now()
            it[updatedAt] = OffsetDateTime.now()
        } > 0
    }

    suspend fun addDocument(applicationId: UUID, documentType: String, mediaId: UUID): DocumentResponse = dbQuery {
        val id = StoreApplicationDocumentsTable.insertAndGetId {
            it[StoreApplicationDocumentsTable.applicationId] = applicationId
            it[StoreApplicationDocumentsTable.documentType] = documentType
            it[StoreApplicationDocumentsTable.mediaId] = mediaId
            it[createdAt] = OffsetDateTime.now()
        }
        StoreApplicationDocumentsTable.selectAll()
            .where { StoreApplicationDocumentsTable.id eq id }
            .single()
            .toDocResponse()
    }

    private fun findByIdInternal(id: UUID, tenantId: String): StoreApplicationResponse? {
        val app = StoreApplicationsTable.selectAll()
            .where { (StoreApplicationsTable.id eq id) and (StoreApplicationsTable.tenantId eq tenantId) }
            .singleOrNull() ?: return null

        val docs = StoreApplicationDocumentsTable.selectAll()
            .where { StoreApplicationDocumentsTable.applicationId eq id }
            .map { it.toDocResponse() }

        return app.toResponse(docs)
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
