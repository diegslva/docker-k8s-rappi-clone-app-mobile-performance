package com.rappiclone.domain.errors

import kotlinx.serialization.Serializable

/**
 * Hierarquia de erros de dominio usando sealed class.
 * Exhaustive when garante que todo caso e tratado.
 */
sealed class DomainError(
    val code: String,
    val message: String,
    val httpStatus: Int = 400
) {
    // --- Auth ---
    class InvalidCredentials : DomainError("AUTH_INVALID_CREDENTIALS", "Credenciais invalidas", 401)
    class TokenExpired : DomainError("AUTH_TOKEN_EXPIRED", "Token expirado", 401)
    class InsufficientPermissions : DomainError("AUTH_INSUFFICIENT_PERMISSIONS", "Permissoes insuficientes", 403)

    // --- Tenant ---
    class TenantNotFound(tenantId: String) : DomainError("TENANT_NOT_FOUND", "Tenant nao encontrado: $tenantId", 404)
    class TenantInactive(tenantId: String) : DomainError("TENANT_INACTIVE", "Tenant inativo: $tenantId", 403)
    class LocationOutsideTenant : DomainError("TENANT_LOCATION_OUTSIDE", "Localizacao fora de area de operacao", 400)

    // --- Store ---
    class StoreNotFound(storeId: String) : DomainError("STORE_NOT_FOUND", "Loja nao encontrada: $storeId", 404)
    class StoreClosed(storeId: String) : DomainError("STORE_CLOSED", "Loja fechada: $storeId", 400)
    class StoreNotApproved(storeId: String) : DomainError("STORE_NOT_APPROVED", "Loja nao aprovada: $storeId", 403)

    // --- Product ---
    class ProductNotFound(productId: String) : DomainError("PRODUCT_NOT_FOUND", "Produto nao encontrado: $productId", 404)
    class ProductOutOfStock(productId: String) : DomainError("PRODUCT_OUT_OF_STOCK", "Produto sem estoque: $productId", 400)

    // --- Cart ---
    class CartEmpty : DomainError("CART_EMPTY", "Carrinho vazio", 400)
    class CartMinimumNotMet(minimum: String) : DomainError("CART_MINIMUM_NOT_MET", "Pedido minimo nao atingido: R$$minimum", 400)

    // --- Order ---
    class OrderNotFound(orderId: String) : DomainError("ORDER_NOT_FOUND", "Pedido nao encontrado: $orderId", 404)
    class InvalidOrderTransition(from: String, to: String) : DomainError("ORDER_INVALID_TRANSITION", "Transicao invalida: $from -> $to", 400)
    class OrderNotCancellable(orderId: String) : DomainError("ORDER_NOT_CANCELLABLE", "Pedido nao pode ser cancelado: $orderId", 400)

    // --- Payment ---
    class PaymentFailed(reason: String) : DomainError("PAYMENT_FAILED", "Pagamento falhou: $reason", 400)
    class PaymentMethodNotSupported(method: String) : DomainError("PAYMENT_METHOD_NOT_SUPPORTED", "Metodo de pagamento nao suportado: $method", 400)

    // --- Courier ---
    class CourierNotFound(courierId: String) : DomainError("COURIER_NOT_FOUND", "Entregador nao encontrado: $courierId", 404)
    class NoCourierAvailable : DomainError("COURIER_NONE_AVAILABLE", "Nenhum entregador disponivel na regiao", 503)

    // --- Generic ---
    class NotFound(entity: String, id: String) : DomainError("NOT_FOUND", "$entity nao encontrado: $id", 404)
    class Conflict(detail: String) : DomainError("CONFLICT", detail, 409)
    class ValidationError(detail: String) : DomainError("VALIDATION_ERROR", detail, 400)
    class InternalError(detail: String) : DomainError("INTERNAL_ERROR", detail, 500)
}

/**
 * Envelope de erro serializado na API.
 */
@Serializable
data class ApiError(
    val code: String,
    val message: String,
    val details: Map<String, String>? = null
)

/**
 * Exception wrapper pra DomainError.
 * Permite throw e catch em qualquer camada.
 */
class DomainException(val error: DomainError) : RuntimeException(error.message)
