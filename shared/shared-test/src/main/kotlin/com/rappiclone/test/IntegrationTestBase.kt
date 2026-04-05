package com.rappiclone.test

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * Container Postgres compartilhado entre todos os testes de integracao.
 * Sobe UMA VEZ por suite de testes (singleton pattern).
 * Cada teste roda em transacao que faz rollback (isolamento).
 */
object TestPostgres {
    val container: PostgreSQLContainer<*> = PostgreSQLContainer("postgres:17-alpine")
        .withDatabaseName("rappiclone_test")
        .withUsername("test")
        .withPassword("test")
        .apply { start() }

    val jdbcUrl: String get() = container.jdbcUrl
    val username: String get() = container.username
    val password: String get() = container.password
}

/**
 * Cria um HttpClient configurado pra testes de integracao.
 * Usa kotlinx.serialization pra JSON, com headers de tenant.
 */
fun ApplicationTestBuilder.createTestClient(tenantId: String = "test-tenant"): HttpClient {
    return createClient {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = false
                ignoreUnknownKeys = true
                encodeDefaults = true
            })
        }
    }.also { client ->
        // Nao da pra setar default headers aqui — cada request precisa setar manualmente
    }
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
