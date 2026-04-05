package com.rappiclone.geolocation.model

import kotlinx.serialization.Serializable

// --- Geocoding ---

@Serializable
data class GeocodeRequest(
    val address: String,
    val city: String? = null,
    val state: String? = null,
    val country: String = "BR"
)

@Serializable
data class ReverseGeocodeRequest(
    val latitude: Double,
    val longitude: Double
)

@Serializable
data class GeocodeResult(
    val latitude: Double,
    val longitude: Double,
    val displayName: String,
    val street: String?,
    val number: String?,
    val neighborhood: String?,
    val city: String?,
    val state: String?,
    val zipCode: String?,
    val country: String?,
    val confidence: Double
)

// --- Routing ---

@Serializable
data class RouteRequest(
    val originLatitude: Double,
    val originLongitude: Double,
    val destinationLatitude: Double,
    val destinationLongitude: Double
)

@Serializable
data class RouteResult(
    val distanceMeters: Double,
    val durationSeconds: Double,
    val distanceKm: Double,
    val durationMinutes: Double,
    val geometry: String?
)

// --- Distance Matrix ---

@Serializable
data class DistanceRequest(
    val originLatitude: Double,
    val originLongitude: Double,
    val destinationLatitude: Double,
    val destinationLongitude: Double
)

@Serializable
data class DistanceResult(
    val distanceMeters: Double,
    val durationSeconds: Double,
    val distanceKm: Double,
    val durationMinutes: Double
)

// --- Address Validation ---

@Serializable
data class ValidateAddressRequest(
    val street: String,
    val number: String,
    val neighborhood: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String = "BR"
)

@Serializable
data class ValidateAddressResult(
    val isValid: Boolean,
    val normalizedAddress: String?,
    val latitude: Double?,
    val longitude: Double?,
    val confidence: Double
)
