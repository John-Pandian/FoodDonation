package com.it.fooddonation.data.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfile(
    val id: String,
    val email: String,
    val role: String,
    val fullName: String? = null,
    val phoneNumber: String? = null,
    val createdAt: String? = null
)
