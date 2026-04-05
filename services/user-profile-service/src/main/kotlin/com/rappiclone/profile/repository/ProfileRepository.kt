package com.rappiclone.profile.repository

import com.rappiclone.profile.model.*
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.OffsetDateTime
import java.util.UUID

class ProfileRepository {

    private fun ResultRow.toProfileResponse(addresses: List<AddressResponse> = emptyList()): ProfileResponse =
        ProfileResponse(
            id = this[ProfilesTable.id].value.toString(),
            userId = this[ProfilesTable.userId].toString(),
            tenantId = this[ProfilesTable.tenantId],
            firstName = this[ProfilesTable.firstName],
            lastName = this[ProfilesTable.lastName],
            displayName = this[ProfilesTable.displayName],
            phone = this[ProfilesTable.phone],
            avatarUrl = this[ProfilesTable.avatarUrl],
            language = this[ProfilesTable.language],
            addresses = addresses
        )

    private fun ResultRow.toAddressResponse(): AddressResponse = AddressResponse(
        id = this[AddressesTable.id].value.toString(),
        label = this[AddressesTable.label],
        street = this[AddressesTable.street],
        number = this[AddressesTable.number],
        complement = this[AddressesTable.complement],
        neighborhood = this[AddressesTable.neighborhood],
        city = this[AddressesTable.city],
        state = this[AddressesTable.state],
        zipCode = this[AddressesTable.zipCode],
        latitude = this[AddressesTable.latitude],
        longitude = this[AddressesTable.longitude],
        isDefault = this[AddressesTable.isDefault]
    )

    suspend fun findByUserId(userId: UUID, tenantId: String): ProfileResponse? = dbQuery {
        val profile = ProfilesTable.selectAll()
            .where { (ProfilesTable.userId eq userId) and (ProfilesTable.tenantId eq tenantId) }
            .singleOrNull() ?: return@dbQuery null

        val profileId = profile[ProfilesTable.id].value
        val addresses = AddressesTable.selectAll()
            .where { AddressesTable.profileId eq profileId }
            .orderBy(AddressesTable.isDefault, SortOrder.DESC)
            .map { it.toAddressResponse() }

        profile.toProfileResponse(addresses)
    }

    suspend fun findById(id: UUID, tenantId: String): ProfileResponse? = dbQuery {
        val profile = ProfilesTable.selectAll()
            .where { (ProfilesTable.id eq id) and (ProfilesTable.tenantId eq tenantId) }
            .singleOrNull() ?: return@dbQuery null

        val addresses = AddressesTable.selectAll()
            .where { AddressesTable.profileId eq id }
            .orderBy(AddressesTable.isDefault, SortOrder.DESC)
            .map { it.toAddressResponse() }

        profile.toProfileResponse(addresses)
    }

    suspend fun create(request: CreateProfileRequest, tenantId: String): ProfileResponse = dbQuery {
        val now = OffsetDateTime.now()
        val id = ProfilesTable.insertAndGetId {
            it[userId] = UUID.fromString(request.userId)
            it[ProfilesTable.tenantId] = tenantId
            it[firstName] = request.firstName
            it[lastName] = request.lastName
            it[displayName] = request.displayName ?: "${request.firstName} ${request.lastName}"
            it[phone] = request.phone
            it[language] = request.language
            it[createdAt] = now
            it[updatedAt] = now
        }

        ProfilesTable.selectAll()
            .where { ProfilesTable.id eq id }
            .single()
            .toProfileResponse()
    }

    suspend fun update(id: UUID, tenantId: String, request: UpdateProfileRequest): ProfileResponse? = dbQuery {
        val updated = ProfilesTable.update({
            (ProfilesTable.id eq id) and (ProfilesTable.tenantId eq tenantId)
        }) { stmt ->
            request.firstName?.let { stmt[firstName] = it }
            request.lastName?.let { stmt[lastName] = it }
            request.displayName?.let { stmt[displayName] = it }
            request.phone?.let { stmt[phone] = it }
            request.avatarUrl?.let { stmt[avatarUrl] = it }
            request.language?.let { stmt[language] = it }
            stmt[updatedAt] = OffsetDateTime.now()
        }
        if (updated > 0) findByIdInternal(id, tenantId) else null
    }

    suspend fun addAddress(profileId: UUID, tenantId: String, request: CreateAddressRequest): AddressResponse = dbQuery {
        // Se marcou como default, remove default de outros
        if (request.isDefault) {
            AddressesTable.update({ AddressesTable.profileId eq profileId }) {
                it[isDefault] = false
            }
        }

        val now = OffsetDateTime.now()
        val id = AddressesTable.insertAndGetId {
            it[AddressesTable.profileId] = profileId
            it[AddressesTable.tenantId] = tenantId
            it[label] = request.label
            it[street] = request.street
            it[number] = request.number
            it[complement] = request.complement
            it[neighborhood] = request.neighborhood
            it[city] = request.city
            it[state] = request.state
            it[zipCode] = request.zipCode
            it[latitude] = request.latitude
            it[longitude] = request.longitude
            it[isDefault] = request.isDefault
            it[createdAt] = now
            it[updatedAt] = now
        }

        AddressesTable.selectAll()
            .where { AddressesTable.id eq id }
            .single()
            .toAddressResponse()
    }

    suspend fun deleteAddress(addressId: UUID, profileId: UUID): Boolean = dbQuery {
        AddressesTable.deleteWhere {
            (AddressesTable.id eq addressId) and (AddressesTable.profileId eq profileId)
        } > 0
    }

    private fun findByIdInternal(id: UUID, tenantId: String): ProfileResponse? {
        val profile = ProfilesTable.selectAll()
            .where { (ProfilesTable.id eq id) and (ProfilesTable.tenantId eq tenantId) }
            .singleOrNull() ?: return null

        val addresses = AddressesTable.selectAll()
            .where { AddressesTable.profileId eq id }
            .orderBy(AddressesTable.isDefault, SortOrder.DESC)
            .map { it.toAddressResponse() }

        return profile.toProfileResponse(addresses)
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
