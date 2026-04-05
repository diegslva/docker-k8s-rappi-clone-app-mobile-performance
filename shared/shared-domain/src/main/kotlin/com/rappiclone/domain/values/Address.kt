package com.rappiclone.domain.values

import kotlinx.serialization.Serializable

@Serializable
data class Address(
    val street: String,
    val number: String,
    val complement: String? = null,
    val neighborhood: String,
    val city: String,
    val state: String,
    val zipCode: String,
    val country: String = "BR",
    val location: Location? = null
) {
    val formatted: String
        get() = buildString {
            append("$street, $number")
            if (!complement.isNullOrBlank()) append(" - $complement")
            append(" - $neighborhood, $city - $state, $zipCode")
        }
}
