package com.rappiclone.test

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.testcontainers.postgresql.PostgreSQLContainer

/**
 * Container Postgres compartilhado entre todos os testes de integracao.
 * Sobe UMA VEZ por suite de testes (singleton pattern).
 */
object TestPostgres {
    val container: PostgreSQLContainer = PostgreSQLContainer("postgres:17-alpine")
        .withDatabaseName("rappiclone_test")
        .withUsername("test")
        .withPassword("test")
        .apply { start() }

    val jdbcUrl: String get() = container.jdbcUrl
    val username: String get() = container.username
    val password: String get() = container.password
}

/**
 * Extension pra adicionar headers padrao de tenant em requests de teste.
 */
fun HttpRequestBuilder.withTenant(tenantId: String = "test-tenant") {
    header("X-Tenant-ID", tenantId)
}

fun HttpRequestBuilder.withAuth(token: String) {
    header("Authorization", "Bearer $token")
}
