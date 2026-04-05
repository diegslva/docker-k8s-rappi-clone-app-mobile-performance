package com.rappiclone.tenant.service

import com.rappiclone.domain.errors.DomainException
import com.rappiclone.tenant.model.*
import com.rappiclone.tenant.repository.TenantRepository
import com.rappiclone.tenant.repository.ZoneRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class TenantServiceTest {

    private val tenantRepository = mockk<TenantRepository>()
    private val zoneRepository = mockk<ZoneRepository>()
    private val tenantService = TenantService(tenantRepository, zoneRepository)

    private val londrinalTenant = TenantResponse(
        id = "londrina-pr",
        name = "Londrina",
        state = "PR",
        country = "BR",
        timezone = "America/Sao_Paulo",
        currency = "BRL",
        isActive = true,
        serviceFeePct = 10.0,
        minOrderValue = 15.0
    )

    @BeforeEach
    fun setup() {
        clearAllMocks()
    }

    @Test
    fun `createTenant deve criar tenant com dados validos`() = runTest {
        coEvery { tenantRepository.findById("londrina-pr") } returns null
        coEvery { tenantRepository.create(any(), any(), any(), any(), any(), any()) } returns londrinalTenant

        val request = CreateTenantRequest(
            id = "londrina-pr",
            name = "Londrina",
            state = "PR"
        )

        val result = tenantService.createTenant(request)

        assertEquals("londrina-pr", result.id)
        assertEquals("Londrina", result.name)
        assertEquals("PR", result.state)
        coVerify { tenantRepository.create("londrina-pr", "Londrina", "PR", "America/Sao_Paulo", 10.0, 15.0) }
    }

    @Test
    fun `createTenant deve rejeitar tenant duplicado`() = runTest {
        coEvery { tenantRepository.findById("londrina-pr") } returns londrinalTenant

        val request = CreateTenantRequest(id = "londrina-pr", name = "Londrina", state = "PR")

        assertThrows(DomainException::class.java) {
            kotlinx.coroutines.test.runTest { tenantService.createTenant(request) }
        }
    }

    @Test
    fun `createTenant deve rejeitar TenantId invalido`() = runTest {
        val request = CreateTenantRequest(id = "Londrina PR", name = "Londrina", state = "PR")

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.test.runTest { tenantService.createTenant(request) }
        }
    }

    @Test
    fun `createTenant deve rejeitar UF invalida`() = runTest {
        coEvery { tenantRepository.findById("londrina-xx") } returns null
        val request = CreateTenantRequest(id = "londrina-xx", name = "Londrina", state = "XX")

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.test.runTest { tenantService.createTenant(request) }
        }
    }

    @Test
    fun `getTenant deve retornar tenant existente`() = runTest {
        coEvery { tenantRepository.findById("londrina-pr") } returns londrinalTenant

        val result = tenantService.getTenant("londrina-pr")

        assertEquals("londrina-pr", result.id)
    }

    @Test
    fun `getTenant deve lancar excecao pra tenant inexistente`() = runTest {
        coEvery { tenantRepository.findById("nao-existe") } returns null

        assertThrows(DomainException::class.java) {
            kotlinx.coroutines.test.runTest { tenantService.getTenant("nao-existe") }
        }
    }

    @Test
    fun `listTenants deve retornar todos`() = runTest {
        coEvery { tenantRepository.findAll(false) } returns listOf(londrinalTenant)

        val result = tenantService.listTenants()

        assertEquals(1, result.size)
        assertEquals("londrina-pr", result[0].id)
    }

    @Test
    fun `updateTenant deve atualizar campos fornecidos`() = runTest {
        val updated = londrinalTenant.copy(name = "Londrina Metropolitana", serviceFeePct = 12.0)
        coEvery { tenantRepository.update("londrina-pr", "Londrina Metropolitana", null, 12.0, null) } returns updated

        val request = UpdateTenantRequest(name = "Londrina Metropolitana", serviceFeePct = 12.0)
        val result = tenantService.updateTenant("londrina-pr", request)

        assertEquals("Londrina Metropolitana", result.name)
        assertEquals(12.0, result.serviceFeePct)
    }

    @Test
    fun `createZone deve validar que tenant existe`() = runTest {
        coEvery { tenantRepository.findById("nao-existe") } returns null

        val request = CreateZoneRequest(
            name = "Centro",
            slug = "centro",
            polygonWkt = "POLYGON((-51.17 -23.30, -51.15 -23.30, -51.15 -23.28, -51.17 -23.28, -51.17 -23.30))"
        )

        assertThrows(DomainException::class.java) {
            kotlinx.coroutines.test.runTest { tenantService.createZone("nao-existe", request) }
        }
    }

    @Test
    fun `createZone deve validar ZoneSlug`() = runTest {
        val request = CreateZoneRequest(
            name = "Centro",
            slug = "Centro Invalido",
            polygonWkt = "POLYGON((-51.17 -23.30, -51.15 -23.30, -51.15 -23.28, -51.17 -23.28, -51.17 -23.30))"
        )

        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.test.runTest { tenantService.createZone("londrina-pr", request) }
        }
    }

    @Test
    fun `resolveLocation deve retornar erro quando fora de area`() = runTest {
        coEvery { zoneRepository.resolveLocation(any(), any()) } returns null

        val request = ResolveLocationRequest(latitude = 0.0, longitude = 0.0)

        assertThrows(DomainException::class.java) {
            kotlinx.coroutines.test.runTest { tenantService.resolveLocation(request) }
        }
    }

    @Test
    fun `resolveLocation deve retornar tenant e zona quando dentro de area`() = runTest {
        val resolved = ResolvedLocationResponse(
            tenantId = "londrina-pr",
            zoneId = "uuid-zona-centro",
            zoneName = "Centro",
            microRegionId = null,
            microRegionName = null
        )
        coEvery { zoneRepository.resolveLocation(-23.3045, -51.1696) } returns resolved

        val request = ResolveLocationRequest(latitude = -23.3045, longitude = -51.1696)
        val result = tenantService.resolveLocation(request)

        assertEquals("londrina-pr", result.tenantId)
        assertEquals("Centro", result.zoneName)
    }
}
