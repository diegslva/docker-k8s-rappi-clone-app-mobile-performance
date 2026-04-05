package com.rappiclone.identity.service

import at.favre.lib.crypto.bcrypt.BCrypt

/**
 * Hashing de senha com bcrypt direto (sem passlib — incompativel com bcrypt >= 5.0).
 * Cost factor 12: ~250ms por hash — bom equilíbrio segurança/performance.
 */
class PasswordService {

    private val cost = 12

    fun hash(password: String): String =
        BCrypt.withDefaults().hashToString(cost, password.toCharArray())

    fun verify(password: String, hashedPassword: String): Boolean =
        BCrypt.verifyer().verify(password.toCharArray(), hashedPassword).verified

    /**
     * Dummy hash pra timing attack prevention.
     * Quando usuario nao existe, roda bcrypt mesmo assim
     * pra equalizar tempo de resposta.
     */
    fun dummyVerify() {
        verify("dummy", BCrypt.withDefaults().hashToString(cost, "dummy".toCharArray()))
    }
}
