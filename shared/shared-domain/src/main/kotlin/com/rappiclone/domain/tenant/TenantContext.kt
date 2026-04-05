package com.rappiclone.domain.tenant

/**
 * Contexto de tenant propagado em todo request.
 * Todo request carrega tenant_id — resolve via geolocalizacao no Gateway.
 * Toda query de banco filtra por tenant_id automaticamente.
 *
 * Value object puro — sem dependencia de framework.
 * O mecanismo de propagacao (ThreadLocal, coroutine context) fica em shared-infra.
 */
data class TenantContext(
    val tenantId: String,
    val zoneId: String? = null,
    val microRegionId: String? = null
)

/**
 * Thread-local pra acesso ao tenant em qualquer camada.
 * Setado pelo TenantPlugin do Ktor (shared-infra) em cada request.
 */
object TenantHolder {
    private val threadLocal = ThreadLocal<TenantContext?>()

    fun set(context: TenantContext) { threadLocal.set(context) }
    fun clear() { threadLocal.remove() }

    fun current(): TenantContext =
        threadLocal.get() ?: throw IllegalStateException("TenantContext nao definido. Request sem tenant?")

    fun currentOrNull(): TenantContext? = threadLocal.get()

    val tenantId: String get() = current().tenantId
}
