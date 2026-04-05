package com.rappiclone.domain.events

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Contrato base pra todos os eventos de dominio.
 * Todo evento carrega tenant_id pra isolamento multi-tenant.
 */
interface DomainEvent {
    val eventId: String
    val eventType: String
    val aggregateId: String
    val tenantId: String
    val timestamp: Instant
    val version: Int get() = 1
}

/**
 * Envelope de evento pra serializacao no Kafka.
 * Wrapper generico que carrega metadata + payload serializado.
 */
@Serializable
data class EventEnvelope(
    val eventId: String,
    val eventType: String,
    val aggregateId: String,
    val tenantId: String,
    val timestamp: String,
    val version: Int = 1,
    val payload: String,
    val source: String,
    val correlationId: String? = null,
    val causationId: String? = null
)
