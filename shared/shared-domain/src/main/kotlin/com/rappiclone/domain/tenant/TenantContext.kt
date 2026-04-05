package com.rappiclone.domain.tenant

import kotlinx.coroutines.ThreadContextElement
import kotlin.coroutines.CoroutineContext

/**
 * Contexto de tenant propagado via coroutine context.
 * Todo request carrega tenant_id — resolve via geolocalizacao no Gateway.
 * Toda query de banco filtra por tenant_id automaticamente.
 */
data class TenantContext(
    val tenantId: String,
    val zoneId: String? = null,
    val microRegionId: String? = null
) {
    companion object Key : CoroutineContext.Key<TenantContextElement>
}

/**
 * Coroutine context element que carrega o TenantContext.
 * Permite acessar via coroutineContext[TenantContext] dentro de qualquer suspend function.
 */
class TenantContextElement(
    val tenant: TenantContext
) : ThreadContextElement<TenantContext?>(Key) {

    companion object Key : CoroutineContext.Key<TenantContextElement>

    // Thread-local pra interop com codigo nao-coroutine (JDBC, gRPC interceptors)
    private val threadLocal = ThreadLocal<TenantContext?>()

    override fun updateThreadContext(context: CoroutineContext): TenantContext? {
        val old = threadLocal.get()
        threadLocal.set(tenant)
        return old
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: TenantContext?) {
        threadLocal.set(oldState)
    }

    companion object {
        private val threadLocal = ThreadLocal<TenantContext?>()

        /**
         * Acesso ao tenant a partir de qualquer thread (JDBC, interceptors).
         */
        fun current(): TenantContext =
            threadLocal.get() ?: throw IllegalStateException("TenantContext nao definido. Request sem tenant?")

        fun currentOrNull(): TenantContext? = threadLocal.get()
    }
}
