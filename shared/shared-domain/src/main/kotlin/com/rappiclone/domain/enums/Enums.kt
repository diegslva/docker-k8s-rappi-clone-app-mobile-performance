package com.rappiclone.domain.enums

import kotlinx.serialization.Serializable

@Serializable
enum class UserRole {
    CUSTOMER,
    COURIER,
    MERCHANT,
    SUPPORT,
    ADMIN
}

@Serializable
enum class PaymentStatus {
    PENDING,
    AUTHORIZED,
    CAPTURED,
    FAILED,
    REFUNDED,
    PARTIALLY_REFUNDED,
    CANCELLED
}

@Serializable
enum class PaymentMethod {
    PIX,
    CREDIT_CARD,
    DEBIT_CARD,
    CASH_ON_DELIVERY,
    WALLET
}

@Serializable
enum class CourierStatus {
    OFFLINE,
    ONLINE,
    ASSIGNED,
    EN_ROUTE_TO_STORE,
    AT_STORE,
    EN_ROUTE_TO_CUSTOMER,
    DELIVERING
}

@Serializable
enum class CourierVehicle {
    MOTORCYCLE,
    BICYCLE,
    CAR,
    ON_FOOT
}

@Serializable
enum class StoreCategory {
    RESTAURANT,
    GROCERY,
    PHARMACY,
    PET_SHOP,
    CONVENIENCE,
    BAKERY,
    LIQUOR_STORE,
    OTHER
}

@Serializable
enum class StoreStatus {
    PENDING_APPROVAL,
    ACTIVE,
    INACTIVE,
    SUSPENDED,
    CLOSED
}

@Serializable
enum class OnboardingStatus {
    PENDING_DOCUMENTS,
    DOCUMENTS_SUBMITTED,
    UNDER_REVIEW,
    APPROVED,
    REJECTED,
    SUSPENDED
}

@Serializable
enum class TicketStatus {
    OPEN,
    ASSIGNED,
    IN_PROGRESS,
    WAITING_CUSTOMER,
    WAITING_MERCHANT,
    RESOLVED,
    CLOSED
}

@Serializable
enum class TicketCategory {
    ORDER_ISSUE,
    PAYMENT_ISSUE,
    DELIVERY_ISSUE,
    PRODUCT_QUALITY,
    MISSING_ITEMS,
    WRONG_ORDER,
    REFUND_REQUEST,
    ACCOUNT_ISSUE,
    COURIER_BEHAVIOR,
    OTHER
}

@Serializable
enum class NotificationChannel {
    PUSH,
    SMS,
    EMAIL,
    IN_APP
}

@Serializable
enum class TaxRegime {
    SIMPLES_NACIONAL,
    LUCRO_PRESUMIDO,
    LUCRO_REAL,
    MEI
}
