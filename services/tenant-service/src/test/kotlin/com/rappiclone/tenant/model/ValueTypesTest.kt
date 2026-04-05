package com.rappiclone.tenant.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ValueTypesTest {

    // --- TenantId ---

    @Test
    fun `TenantId valido deve aceitar slug lowercase com hifens`() {
        assertDoesNotThrow { TenantId("londrina-pr") }
        assertDoesNotThrow { TenantId("sao-paulo-sp") }
        assertDoesNotThrow { TenantId("fortaleza-ce") }
        assertDoesNotThrow { TenantId("abc") }
    }

    @Test
    fun `TenantId deve rejeitar caracteres invalidos`() {
        assertThrows(IllegalArgumentException::class.java) { TenantId("Londrina-PR") }
        assertThrows(IllegalArgumentException::class.java) { TenantId("londrina pr") }
        assertThrows(IllegalArgumentException::class.java) { TenantId("londrina_pr") }
        assertThrows(IllegalArgumentException::class.java) { TenantId("londrina@pr") }
    }

    @Test
    fun `TenantId deve rejeitar tamanho invalido`() {
        assertThrows(IllegalArgumentException::class.java) { TenantId("ab") }
        assertThrows(IllegalArgumentException::class.java) { TenantId("a".repeat(101)) }
    }

    // --- ZoneSlug ---

    @Test
    fun `ZoneSlug valido deve aceitar slug lowercase`() {
        assertDoesNotThrow { ZoneSlug("zona-sul") }
        assertDoesNotThrow { ZoneSlug("centro") }
        assertDoesNotThrow { ZoneSlug("zona-norte-1") }
    }

    @Test
    fun `ZoneSlug deve rejeitar caracteres invalidos`() {
        assertThrows(IllegalArgumentException::class.java) { ZoneSlug("Zona Sul") }
        assertThrows(IllegalArgumentException::class.java) { ZoneSlug("zona_sul") }
    }

    // --- BrazilianState ---

    @Test
    fun `BrazilianState deve aceitar UFs validas`() {
        assertDoesNotThrow { BrazilianState("PR") }
        assertDoesNotThrow { BrazilianState("SP") }
        assertDoesNotThrow { BrazilianState("CE") }
        assertDoesNotThrow { BrazilianState("RJ") }
        assertDoesNotThrow { BrazilianState("AM") }
    }

    @Test
    fun `BrazilianState deve rejeitar UFs invalidas`() {
        assertThrows(IllegalArgumentException::class.java) { BrazilianState("XX") }
        assertThrows(IllegalArgumentException::class.java) { BrazilianState("pr") }
        assertThrows(IllegalArgumentException::class.java) { BrazilianState("P") }
        assertThrows(IllegalArgumentException::class.java) { BrazilianState("PRA") }
    }

    @Test
    fun `BrazilianState deve cobrir todos os 27 estados`() {
        val allStates = listOf(
            "AC", "AL", "AP", "AM", "BA", "CE", "DF", "ES", "GO",
            "MA", "MT", "MS", "MG", "PA", "PB", "PR", "PE", "PI",
            "RJ", "RN", "RS", "RO", "RR", "SC", "SP", "SE", "TO"
        )
        allStates.forEach { uf ->
            assertDoesNotThrow { BrazilianState(uf) }
        }
        assertEquals(27, allStates.size)
    }
}
