package com.rappiclone.onboarding.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class ValueTypesTest {

    // --- CPF ---

    @Test
    fun `CPF valido deve aceitar 11 digitos`() {
        assertDoesNotThrow { Cpf("12345678901") }
        assertDoesNotThrow { Cpf("123.456.789-01") }
    }

    @Test
    fun `CPF deve rejeitar quantidade errada de digitos`() {
        assertThrows(IllegalArgumentException::class.java) { Cpf("1234567890") }
        assertThrows(IllegalArgumentException::class.java) { Cpf("123456789012") }
        assertThrows(IllegalArgumentException::class.java) { Cpf("") }
    }

    @Test
    fun `CPF deve rejeitar todos os digitos iguais`() {
        assertThrows(IllegalArgumentException::class.java) { Cpf("11111111111") }
        assertThrows(IllegalArgumentException::class.java) { Cpf("00000000000") }
    }

    @Test
    fun `CPF formatted deve retornar formato correto`() {
        val cpf = Cpf("12345678901")
        assertEquals("123.456.789-01", cpf.formatted)
    }

    // --- CNPJ ---

    @Test
    fun `CNPJ valido deve aceitar 14 digitos`() {
        assertDoesNotThrow { Cnpj("12345678000190") }
        assertDoesNotThrow { Cnpj("12.345.678/0001-90") }
    }

    @Test
    fun `CNPJ deve rejeitar quantidade errada de digitos`() {
        assertThrows(IllegalArgumentException::class.java) { Cnpj("1234567800019") }
        assertThrows(IllegalArgumentException::class.java) { Cnpj("123456780001901") }
        assertThrows(IllegalArgumentException::class.java) { Cnpj("") }
    }

    @Test
    fun `CNPJ formatted deve retornar formato correto`() {
        val cnpj = Cnpj("12345678000190")
        assertEquals("12.345.678/0001-90", cnpj.formatted)
    }
}
