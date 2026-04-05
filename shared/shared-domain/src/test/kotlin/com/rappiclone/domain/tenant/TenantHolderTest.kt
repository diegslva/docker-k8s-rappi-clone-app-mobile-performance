package com.rappiclone.domain.tenant

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.AfterEach

class TenantHolderTest {

    @AfterEach
    fun cleanup() {
        TenantHolder.clear()
    }

    @Test
    fun `set e current devem funcionar`() {
        val context = TenantContext(tenantId = "londrina-pr", zoneId = "centro", microRegionId = "centro-historico")
        TenantHolder.set(context)

        val retrieved = TenantHolder.current()
        assertEquals("londrina-pr", retrieved.tenantId)
        assertEquals("centro", retrieved.zoneId)
        assertEquals("centro-historico", retrieved.microRegionId)
    }

    @Test
    fun `tenantId shortcut deve retornar o id`() {
        TenantHolder.set(TenantContext(tenantId = "sao-paulo-sp"))
        assertEquals("sao-paulo-sp", TenantHolder.tenantId)
    }

    @Test
    fun `current sem set deve lancar excecao`() {
        assertThrows(IllegalStateException::class.java) {
            TenantHolder.current()
        }
    }

    @Test
    fun `currentOrNull sem set deve retornar null`() {
        assertNull(TenantHolder.currentOrNull())
    }

    @Test
    fun `currentOrNull com set deve retornar contexto`() {
        TenantHolder.set(TenantContext(tenantId = "fortaleza-ce"))
        assertNotNull(TenantHolder.currentOrNull())
        assertEquals("fortaleza-ce", TenantHolder.currentOrNull()?.tenantId)
    }

    @Test
    fun `clear deve limpar o contexto`() {
        TenantHolder.set(TenantContext(tenantId = "londrina-pr"))
        assertNotNull(TenantHolder.currentOrNull())

        TenantHolder.clear()
        assertNull(TenantHolder.currentOrNull())
    }

    @Test
    fun `zoneId e microRegionId devem ser opcionais`() {
        val context = TenantContext(tenantId = "londrina-pr")
        assertNull(context.zoneId)
        assertNull(context.microRegionId)
    }
}
