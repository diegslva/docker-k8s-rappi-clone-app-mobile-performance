package com.rappiclone.profile.integration

import com.rappiclone.infra.config.DatabaseConfig
import com.rappiclone.infra.config.ServiceConfig
import com.rappiclone.profile.model.*
import com.rappiclone.profile.module
import com.rappiclone.test.TestPostgres
import com.rappiclone.test.withTenant
import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import java.util.UUID

/**
 * Testes de integracao do User Profile Service.
 *
 * Roda com Postgres REAL via Testcontainers.
 * Flyway executa as migrations automaticamente.
 * Testa o fluxo completo: HTTP request -> route -> service -> repository -> banco -> response.
 */
@Tag("integration")
class ProfileIntegrationTest {

    companion object {
        private val tenantId = "londrina-pr"

        // Config apontando pro Testcontainers Postgres
        private val testConfig = ServiceConfig(
            name = "user-profile-service-test",
            port = 0,
            database = DatabaseConfig(
                url = TestPostgres.jdbcUrl,
                user = TestPostgres.username,
                password = TestPostgres.password,
                maxPoolSize = 5
            ),
            redis = null,
            kafka = null,
            mqtt = null
        )
    }

    private fun ApplicationTestBuilder.configuredClient() = createClient {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
        }
    }

    @Test
    fun `deve criar perfil e retornar 201`() = testApplication {
        application { module(testConfig) }
        val client = configuredClient()
        val userId = UUID.randomUUID().toString()

        val response = client.post("/api/v1/users/profile") {
            contentType(ContentType.Application.Json)
            withTenant(tenantId)
            setBody(CreateProfileRequest(
                userId = userId,
                firstName = "Diego",
                lastName = "Silva",
                phone = "+5543999999999"
            ))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val profile = response.body<ProfileResponse>()
        assertEquals("Diego", profile.firstName)
        assertEquals("Silva", profile.lastName)
        assertEquals(tenantId, profile.tenantId)
        assertEquals(userId, profile.userId)
        assertNotNull(profile.id)
    }

    @Test
    fun `deve buscar perfil por userId`() = testApplication {
        application { module(testConfig) }
        val client = configuredClient()
        val userId = UUID.randomUUID().toString()

        // Cria primeiro
        client.post("/api/v1/users/profile") {
            contentType(ContentType.Application.Json)
            withTenant(tenantId)
            setBody(CreateProfileRequest(userId = userId, firstName = "Maria", lastName = "Santos"))
        }

        // Busca
        val response = client.get("/api/v1/users/$userId/profile") {
            withTenant(tenantId)
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val profile = response.body<ProfileResponse>()
        assertEquals("Maria", profile.firstName)
        assertEquals("Santos", profile.lastName)
    }

    @Test
    fun `deve retornar 404 pra perfil inexistente`() = testApplication {
        application { module(testConfig) }
        val client = configuredClient()

        val response = client.get("/api/v1/users/${UUID.randomUUID()}/profile") {
            withTenant(tenantId)
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `deve atualizar perfil`() = testApplication {
        application { module(testConfig) }
        val client = configuredClient()
        val userId = UUID.randomUUID().toString()

        // Cria
        client.post("/api/v1/users/profile") {
            contentType(ContentType.Application.Json)
            withTenant(tenantId)
            setBody(CreateProfileRequest(userId = userId, firstName = "Joao", lastName = "Souza"))
        }

        // Atualiza
        val response = client.put("/api/v1/users/$userId/profile") {
            contentType(ContentType.Application.Json)
            withTenant(tenantId)
            setBody(UpdateProfileRequest(firstName = "Joao Pedro", phone = "+5543888888888"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val updated = response.body<ProfileResponse>()
        assertEquals("Joao Pedro", updated.firstName)
        assertEquals("+5543888888888", updated.phone)
    }

    @Test
    fun `deve rejeitar perfil duplicado com 409`() = testApplication {
        application { module(testConfig) }
        val client = configuredClient()
        val userId = UUID.randomUUID().toString()

        // Cria primeiro
        client.post("/api/v1/users/profile") {
            contentType(ContentType.Application.Json)
            withTenant(tenantId)
            setBody(CreateProfileRequest(userId = userId, firstName = "Ana", lastName = "Lima"))
        }

        // Tenta criar de novo
        val response = client.post("/api/v1/users/profile") {
            contentType(ContentType.Application.Json)
            withTenant(tenantId)
            setBody(CreateProfileRequest(userId = userId, firstName = "Ana", lastName = "Lima"))
        }

        assertEquals(HttpStatusCode.Conflict, response.status)
    }

    @Test
    fun `deve adicionar endereco ao perfil`() = testApplication {
        application { module(testConfig) }
        val client = configuredClient()
        val userId = UUID.randomUUID().toString()

        // Cria perfil
        client.post("/api/v1/users/profile") {
            contentType(ContentType.Application.Json)
            withTenant(tenantId)
            setBody(CreateProfileRequest(userId = userId, firstName = "Pedro", lastName = "Oliveira"))
        }

        // Adiciona endereco
        val response = client.post("/api/v1/users/$userId/addresses") {
            contentType(ContentType.Application.Json)
            withTenant(tenantId)
            setBody(CreateAddressRequest(
                label = "Casa",
                street = "Rua Sergipe",
                number = "445",
                complement = "Apto 301",
                neighborhood = "Centro",
                city = "Londrina",
                state = "PR",
                zipCode = "86010-000",
                latitude = -23.3045,
                longitude = -51.1696,
                isDefault = true
            ))
        }

        assertEquals(HttpStatusCode.Created, response.status)
        val address = response.body<AddressResponse>()
        assertEquals("Rua Sergipe", address.street)
        assertEquals("445", address.number)
        assertEquals("Apto 301", address.complement)
        assertEquals("Centro", address.neighborhood)
        assertEquals("Londrina", address.city)
        assertEquals("PR", address.state)
        assertEquals("86010-000", address.zipCode)
        assertEquals(-23.3045, address.latitude)
        assertTrue(address.isDefault)
    }

    @Test
    fun `deve listar enderecos quando buscar perfil`() = testApplication {
        application { module(testConfig) }
        val client = configuredClient()
        val userId = UUID.randomUUID().toString()

        // Cria perfil + 2 enderecos
        client.post("/api/v1/users/profile") {
            contentType(ContentType.Application.Json)
            withTenant(tenantId)
            setBody(CreateProfileRequest(userId = userId, firstName = "Lucas", lastName = "Mendes"))
        }

        client.post("/api/v1/users/$userId/addresses") {
            contentType(ContentType.Application.Json)
            withTenant(tenantId)
            setBody(CreateAddressRequest(
                label = "Casa", street = "Rua A", number = "100",
                neighborhood = "Centro", city = "Londrina", state = "PR", zipCode = "86010-000",
                isDefault = true
            ))
        }

        client.post("/api/v1/users/$userId/addresses") {
            contentType(ContentType.Application.Json)
            withTenant(tenantId)
            setBody(CreateAddressRequest(
                label = "Trabalho", street = "Rua B", number = "200",
                neighborhood = "Gleba", city = "Londrina", state = "PR", zipCode = "86050-000"
            ))
        }

        // Busca perfil — deve ter 2 enderecos
        val response = client.get("/api/v1/users/$userId/profile") {
            withTenant(tenantId)
        }

        val profile = response.body<ProfileResponse>()
        assertEquals(2, profile.addresses.size)
        // Default vem primeiro
        assertEquals("Casa", profile.addresses[0].label)
        assertTrue(profile.addresses[0].isDefault)
    }

    @Test
    fun `deve deletar endereco`() = testApplication {
        application { module(testConfig) }
        val client = configuredClient()
        val userId = UUID.randomUUID().toString()

        // Cria perfil + endereco
        client.post("/api/v1/users/profile") {
            contentType(ContentType.Application.Json)
            withTenant(tenantId)
            setBody(CreateProfileRequest(userId = userId, firstName = "Fernanda", lastName = "Costa"))
        }

        val addressResponse = client.post("/api/v1/users/$userId/addresses") {
            contentType(ContentType.Application.Json)
            withTenant(tenantId)
            setBody(CreateAddressRequest(
                street = "Rua X", number = "10",
                neighborhood = "Bairro", city = "Londrina", state = "PR", zipCode = "86000-000"
            ))
        }
        val address = addressResponse.body<AddressResponse>()

        // Deleta
        val deleteResponse = client.delete("/api/v1/users/$userId/addresses/${address.id}") {
            withTenant(tenantId)
        }

        assertEquals(HttpStatusCode.NoContent, deleteResponse.status)

        // Verifica que sumiu
        val profileResponse = client.get("/api/v1/users/$userId/profile") {
            withTenant(tenantId)
        }
        val profile = profileResponse.body<ProfileResponse>()
        assertTrue(profile.addresses.isEmpty())
    }

    @Test
    fun `deve isolar dados por tenant`() = testApplication {
        application { module(testConfig) }
        val client = configuredClient()
        val userId = UUID.randomUUID().toString()

        // Cria perfil no tenant londrina-pr
        client.post("/api/v1/users/profile") {
            contentType(ContentType.Application.Json)
            withTenant("londrina-pr")
            setBody(CreateProfileRequest(userId = userId, firstName = "Tenant", lastName = "Test"))
        }

        // Busca no tenant sao-paulo-sp — nao deve encontrar
        val response = client.get("/api/v1/users/$userId/profile") {
            withTenant("sao-paulo-sp")
        }

        assertEquals(HttpStatusCode.NotFound, response.status)
    }
}
