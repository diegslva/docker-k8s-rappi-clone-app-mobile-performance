package com.rappiclone.domain.enums

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class OrderStatusTest {

    @Test
    fun `fluxo completo feliz deve ser valido`() {
        val happyPath = listOf(
            OrderStatus.CREATED,
            OrderStatus.PAYMENT_PENDING,
            OrderStatus.PAYMENT_CONFIRMED,
            OrderStatus.STORE_RECEIVED,
            OrderStatus.STORE_PREPARING,
            OrderStatus.READY_FOR_PICKUP,
            OrderStatus.COURIER_ASSIGNED,
            OrderStatus.COURIER_EN_ROUTE_TO_STORE,
            OrderStatus.COURIER_AT_STORE,
            OrderStatus.PICKED_UP,
            OrderStatus.COURIER_EN_ROUTE_TO_CUSTOMER,
            OrderStatus.ARRIVING,
            OrderStatus.DELIVERED
        )

        for (i in 0 until happyPath.size - 1) {
            val from = happyPath[i]
            val to = happyPath[i + 1]
            assertTrue(from.canTransitionTo(to), "$from -> $to deveria ser valido")
        }
    }

    @Test
    fun `fluxo com fulfillment parcial deve ser valido`() {
        assertTrue(OrderStatus.STORE_PREPARING.canTransitionTo(OrderStatus.PARTIALLY_FULFILLED))
        assertTrue(OrderStatus.PARTIALLY_FULFILLED.canTransitionTo(OrderStatus.READY_FOR_PICKUP))
    }

    @Test
    fun `transicoes invalidas devem ser rejeitadas`() {
        // Nao pode pular etapas
        assertFalse(OrderStatus.CREATED.canTransitionTo(OrderStatus.DELIVERED))
        assertFalse(OrderStatus.CREATED.canTransitionTo(OrderStatus.PICKED_UP))
        assertFalse(OrderStatus.PAYMENT_PENDING.canTransitionTo(OrderStatus.STORE_PREPARING))

        // Nao pode voltar
        assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.CREATED))
        assertFalse(OrderStatus.PICKED_UP.canTransitionTo(OrderStatus.STORE_PREPARING))
    }

    @Test
    fun `cancelamento deve ser possivel antes de PICKED_UP`() {
        val cancellableStates = listOf(
            OrderStatus.CREATED,
            OrderStatus.PAYMENT_PENDING,
            OrderStatus.PAYMENT_CONFIRMED,
            OrderStatus.STORE_RECEIVED,
            OrderStatus.STORE_PREPARING,
            OrderStatus.PARTIALLY_FULFILLED,
            OrderStatus.READY_FOR_PICKUP,
            OrderStatus.COURIER_ASSIGNED,
            OrderStatus.COURIER_EN_ROUTE_TO_STORE,
            OrderStatus.COURIER_AT_STORE
        )

        cancellableStates.forEach { status ->
            assertTrue(status.canTransitionTo(OrderStatus.CANCELLED), "$status deveria permitir cancelamento")
            assertTrue(status.isCancellable(), "$status.isCancellable() deveria ser true")
        }
    }

    @Test
    fun `cancelamento nao deve ser possivel apos PICKED_UP`() {
        val nonCancellable = listOf(
            OrderStatus.PICKED_UP,
            OrderStatus.COURIER_EN_ROUTE_TO_CUSTOMER,
            OrderStatus.ARRIVING,
            OrderStatus.DELIVERED,
            OrderStatus.CANCELLED,
            OrderStatus.FAILED
        )

        nonCancellable.forEach { status ->
            assertFalse(status.isCancellable(), "$status.isCancellable() deveria ser false")
        }
    }

    @Test
    fun `FAILED deve ser possivel de qualquer estado ativo`() {
        OrderStatus.entries
            .filter { it.isActive() }
            .forEach { status ->
                assertTrue(status.canTransitionTo(OrderStatus.FAILED), "$status -> FAILED deveria ser valido")
            }
    }

    @Test
    fun `estados terminais devem ser corretos`() {
        assertTrue(OrderStatus.DELIVERED.isTerminal())
        assertTrue(OrderStatus.CANCELLED.isTerminal())
        assertTrue(OrderStatus.FAILED.isTerminal())

        // Todos os outros devem ser ativos
        OrderStatus.entries
            .filter { it !in listOf(OrderStatus.DELIVERED, OrderStatus.CANCELLED, OrderStatus.FAILED) }
            .forEach { status ->
                assertFalse(status.isTerminal(), "$status nao deveria ser terminal")
                assertTrue(status.isActive(), "$status deveria ser ativo")
            }
    }

    @Test
    fun `estados terminais nao devem ter transicoes`() {
        assertFalse(OrderStatus.DELIVERED.canTransitionTo(OrderStatus.CREATED))
        assertFalse(OrderStatus.CANCELLED.canTransitionTo(OrderStatus.CREATED))
        assertFalse(OrderStatus.FAILED.canTransitionTo(OrderStatus.CREATED))

        // Nenhuma transicao possivel a partir de terminais
        OrderStatus.entries.forEach { target ->
            assertFalse(OrderStatus.DELIVERED.canTransitionTo(target), "DELIVERED -> $target nao deveria ser possivel")
            assertFalse(OrderStatus.CANCELLED.canTransitionTo(target), "CANCELLED -> $target nao deveria ser possivel")
            assertFalse(OrderStatus.FAILED.canTransitionTo(target), "FAILED -> $target nao deveria ser possivel")
        }
    }
}
