package com.rappiclone.onboarding.model

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestampWithTimeZone
import org.jetbrains.exposed.sql.kotlin.datetime.date

object StoreApplicationsTable : UUIDTable("store_applications") {
    val tenantId = varchar("tenant_id", 100)
    val ownerUserId = uuid("owner_user_id")
    val storeName = varchar("store_name", 255)
    val storeCategory = varchar("store_category", 30)
    val taxId = varchar("tax_id", 20)
    val taxRegime = varchar("tax_regime", 30)
    val legalName = varchar("legal_name", 255)
    val phone = varchar("phone", 20)
    val email = varchar("email", 255)
    val street = varchar("street", 255)
    val number = varchar("number", 20)
    val complement = varchar("complement", 100).nullable()
    val neighborhood = varchar("neighborhood", 100)
    val city = varchar("city", 100)
    val state = varchar("state", 2)
    val zipCode = varchar("zip_code", 10)
    val latitude = double("latitude").nullable()
    val longitude = double("longitude").nullable()
    val status = varchar("status", 30).default("PENDING_DOCUMENTS")
    val rejectionReason = text("rejection_reason").nullable()
    val reviewedBy = uuid("reviewed_by").nullable()
    val reviewedAt = timestampWithTimeZone("reviewed_at").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}

object StoreApplicationDocumentsTable : UUIDTable("store_application_documents") {
    val applicationId = uuid("application_id").references(StoreApplicationsTable.id)
    val documentType = varchar("document_type", 30)
    val mediaId = uuid("media_id")
    val status = varchar("status", 20).default("PENDING")
    val notes = text("notes").nullable()
    val createdAt = timestampWithTimeZone("created_at")
}

object CourierApplicationsTable : UUIDTable("courier_applications") {
    val tenantId = varchar("tenant_id", 100)
    val userId = uuid("user_id")
    val fullName = varchar("full_name", 200)
    val cpf = varchar("cpf", 14)
    val phone = varchar("phone", 20)
    val email = varchar("email", 255)
    val dateOfBirth = date("date_of_birth")
    val vehicleType = varchar("vehicle_type", 20)
    val vehiclePlate = varchar("vehicle_plate", 10).nullable()
    val cnhNumber = varchar("cnh_number", 20).nullable()
    val cnhCategory = varchar("cnh_category", 5).nullable()
    val cnhExpiry = date("cnh_expiry").nullable()
    val status = varchar("status", 30).default("PENDING_DOCUMENTS")
    val rejectionReason = text("rejection_reason").nullable()
    val reviewedBy = uuid("reviewed_by").nullable()
    val reviewedAt = timestampWithTimeZone("reviewed_at").nullable()
    val createdAt = timestampWithTimeZone("created_at")
    val updatedAt = timestampWithTimeZone("updated_at")
}

object CourierApplicationDocumentsTable : UUIDTable("courier_application_documents") {
    val applicationId = uuid("application_id").references(CourierApplicationsTable.id)
    val documentType = varchar("document_type", 30)
    val mediaId = uuid("media_id")
    val status = varchar("status", 20).default("PENDING")
    val notes = text("notes").nullable()
    val createdAt = timestampWithTimeZone("created_at")
}
