package com.rappiclone.domain.enums

import kotlinx.serialization.Serializable

@Serializable
enum class OrderStatus {
    CREATED,
    PAYMENT_PENDING,
    PAYMENT_CONFIRMED,
    STORE_RECEIVED,
    STORE_PREPARING,
    PARTIALLY_FULFILLED,
    READY_FOR_PICKUP,
    COURIER_ASSIGNED,
    COURIER_EN_ROUTE_TO_STORE,
    COURIER_AT_STORE,
    PICKED_UP,
    COURIER_EN_ROUTE_TO_CUSTOMER,
    ARRIVING,
    DELIVERED,
    CANCELLED,
    FAILED;

    fun canTransitionTo(next: OrderStatus): Boolean = next in allowedTransitions()

    private fun allowedTransitions(): Set<OrderStatus> = when (this) {
        CREATED -> setOf(PAYMENT_PENDING, CANCELLED, FAILED)
        PAYMENT_PENDING -> setOf(PAYMENT_CONFIRMED, CANCELLED, FAILED)
        PAYMENT_CONFIRMED -> setOf(STORE_RECEIVED, CANCELLED, FAILED)
        STORE_RECEIVED -> setOf(STORE_PREPARING, CANCELLED, FAILED)
        STORE_PREPARING -> setOf(PARTIALLY_FULFILLED, READY_FOR_PICKUP, CANCELLED, FAILED)
        PARTIALLY_FULFILLED -> setOf(READY_FOR_PICKUP, CANCELLED, FAILED)
        READY_FOR_PICKUP -> setOf(COURIER_ASSIGNED, CANCELLED, FAILED)
        COURIER_ASSIGNED -> setOf(COURIER_EN_ROUTE_TO_STORE, CANCELLED, FAILED)
        COURIER_EN_ROUTE_TO_STORE -> setOf(COURIER_AT_STORE, CANCELLED, FAILED)
        COURIER_AT_STORE -> setOf(PICKED_UP, CANCELLED, FAILED)
        PICKED_UP -> setOf(COURIER_EN_ROUTE_TO_CUSTOMER, FAILED)
        COURIER_EN_ROUTE_TO_CUSTOMER -> setOf(ARRIVING, FAILED)
        ARRIVING -> setOf(DELIVERED, FAILED)
        DELIVERED -> emptySet()
        CANCELLED -> emptySet()
        FAILED -> emptySet()
    }

    fun isTerminal(): Boolean = this in setOf(DELIVERED, CANCELLED, FAILED)
    fun isActive(): Boolean = !isTerminal()
    fun isCancellable(): Boolean = this !in setOf(PICKED_UP, COURIER_EN_ROUTE_TO_CUSTOMER, ARRIVING, DELIVERED, CANCELLED, FAILED)
}
