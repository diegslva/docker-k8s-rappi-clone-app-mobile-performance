package com.rappiclone.identity.repository

import com.rappiclone.identity.model.RefreshTokensTable
import kotlinx.coroutines.Dispatchers
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.time.OffsetDateTime
import java.util.UUID

data class RefreshTokenRecord(
    val id: UUID,
    val userId: UUID,
    val token: String,
    val expiresAt: OffsetDateTime,
    val revoked: Boolean,
    val createdAt: OffsetDateTime
)

class RefreshTokenRepository {

    suspend fun create(userId: UUID, token: String, expiresAt: OffsetDateTime): RefreshTokenRecord = dbQuery {
        val now = OffsetDateTime.now()
        val id = RefreshTokensTable.insertAndGetId {
            it[RefreshTokensTable.userId] = userId
            it[RefreshTokensTable.token] = token
            it[RefreshTokensTable.expiresAt] = expiresAt
            it[RefreshTokensTable.createdAt] = now
        }
        RefreshTokenRecord(id.value, userId, token, expiresAt, false, now)
    }

    suspend fun findByToken(token: String): RefreshTokenRecord? = dbQuery {
        RefreshTokensTable.selectAll()
            .where { (RefreshTokensTable.token eq token) and (RefreshTokensTable.revoked eq false) }
            .map {
                RefreshTokenRecord(
                    id = it[RefreshTokensTable.id].value,
                    userId = it[RefreshTokensTable.userId],
                    token = it[RefreshTokensTable.token],
                    expiresAt = it[RefreshTokensTable.expiresAt],
                    revoked = it[RefreshTokensTable.revoked],
                    createdAt = it[RefreshTokensTable.createdAt]
                )
            }
            .singleOrNull()
    }

    suspend fun revoke(token: String): Unit = dbQuery {
        RefreshTokensTable.update({ RefreshTokensTable.token eq token }) {
            it[revoked] = true
        }
    }

    suspend fun revokeAllForUser(userId: UUID): Unit = dbQuery {
        RefreshTokensTable.update({ RefreshTokensTable.userId eq userId }) {
            it[revoked] = true
        }
    }

    private suspend fun <T> dbQuery(block: suspend () -> T): T =
        newSuspendedTransaction(Dispatchers.IO) { block() }
}
