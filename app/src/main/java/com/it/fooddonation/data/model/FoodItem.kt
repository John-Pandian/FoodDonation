package com.it.fooddonation.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Represents a single food item within a donation
 */
@Serializable
data class FoodItem(
    val id: String = UUID.randomUUID().toString(),
    val foodName: String,
    val quantity: String,
    val imageUri: String? = null
)
