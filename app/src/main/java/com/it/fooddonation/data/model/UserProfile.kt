package com.it.fooddonation.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val email: String,
    val role: String,
    @SerialName("full_name")
    val fullName: String? = null,
    @SerialName("phone_number")
    val phoneNumber: String? = null,
    val address: String? = null,
    val city: String? = null,
    @SerialName("postal_code")
    val postalCode: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)
