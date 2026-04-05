package com.rappiclone.identity.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class PasswordServiceTest {

    private val passwordService = PasswordService()

    @Test
    fun `hash e verify devem ser consistentes`() {
        val password = "minha-senha-segura-123"
        val hashed = passwordService.hash(password)

        assertTrue(passwordService.verify(password, hashed))
    }

    @Test
    fun `senha errada nao deve verificar`() {
        val password = "senha-correta"
        val hashed = passwordService.hash(password)

        assertFalse(passwordService.verify("senha-errada", hashed))
    }

    @Test
    fun `hashes diferentes para mesma senha`() {
        val password = "mesma-senha"
        val hash1 = passwordService.hash(password)
        val hash2 = passwordService.hash(password)

        // bcrypt gera salt aleatorio — hashes devem ser diferentes
        assertNotEquals(hash1, hash2)
        // Mas ambos devem verificar
        assertTrue(passwordService.verify(password, hash1))
        assertTrue(passwordService.verify(password, hash2))
    }

    @Test
    fun `dummyVerify nao deve lancar excecao`() {
        // Apenas garante que nao explode — existe pra timing attack prevention
        assertDoesNotThrow { passwordService.dummyVerify() }
    }
}
