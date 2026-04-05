package com.rappiclone.identity.repository

import com.rappiclone.domain.enums.UserRole
import com.rappiclone.identity.model.UsersTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.OffsetDateTime
import java.util.UUID

data class UserRecord(
    val id: UUID,
    val email: String,
    val phone: String?,
    val hashedPassword: String,
    val role: UserRole,
    val isVerified: Boolean,
    val isActive: Boolean,
    val tenantId: String?,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime
)

class UserRepository {

    private fun ResultRow.toUserRecord(): UserRecord = UserRecord(
        id = this[UsersTable.id].value,
        email = this[UsersTable.email],
        phone = this[UsersTable.phone],
        hashedPassword = this[UsersTable.hashedPassword],
        role = UserRole.valueOf(this[UsersTable.role]),
        isVerified = this[UsersTable.isVerified],
        isActive = this[UsersTable.isActive],
        tenantId = this[UsersTable.tenantId],
        createdAt = this[UsersTable.createdAt],
        updatedAt = this[UsersTable.updatedAt]
    )

    suspend fun findByEmail(email: String): UserRecord? = dbQuery {
        UsersTable.selectAll()
            .where { UsersTable.email eq email.lowercase() }
            .map { it.toUserRecord() }
            .singleOrNull()
    }

    suspend fun findById(id: UUID): UserRecord? = dbQuery {
        UsersTable.selectAll()
            .where { UsersTable.id eq id }
            .map { it.toUserRecord() }
            .singleOrNull()
    }

    suspend fun findByPhone(phone: String): UserRecord? = dbQuery {
        UsersTable.selectAll()
            .where { UsersTable.phone eq phone }
            .map { it.toUserRecord() }
            .singleOrNull()
    }

    suspend fun create(
        email: String,
        hashedPassword: String,
        phone: String?,
        role: UserRole,
        tenantId: String?
    ): UserRecord = dbQuery {
        val now = OffsetDateTime.now()
        val id = UsersTable.insertAndGetId {
            it[UsersTable.email] = email.lowercase()
            it[UsersTable.phone] = phone
            it[UsersTable.hashedPassword] = hashedPassword
            it[UsersTable.role] = role.name
            it[UsersTable.tenantId] = tenantId
            it[UsersTable.createdAt] = now
            it[UsersTable.updatedAt] = now
        }
        findByIdInternal(id.value)!!
    }

    suspend fun markVerified(userId: UUID): Unit = dbQuery {
        UsersTable.update({ UsersTable.id eq userId }) {
            it[isVerified] = true
            it[updatedAt] = OffsetDateTime.now()
        }
    }

    private fun findByIdInternal(id: UUID): UserRecord? =
        UsersTable.selectAll()
            .where { UsersTable.id eq id }
            .map { it.toUserRecord() }
            .singleOrNull()

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
