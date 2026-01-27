package com.it.fooddonation.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Represents a food donation created by a donor
 * Can contain multiple food items
 *
 * @property id Unique identifier for the donation
 * @property donorId ID of the user who created the donation
 * @property donorName Name of the donor who created the donation
 * @property foodItems List of food items in this donation
 * @property description Optional description with additional details
 * @property status Current status of the donation
 * @property receiverId ID of the receiver who accepted the donation (null if not accepted)
 * @property receiverName Name of the receiver (null if not accepted)
 * @property peopleServed Number of people who were served by this donation (null until receiver reports)
 * @property createdAt Timestamp when the donation was created
 */
@Serializable
data class Donation(
    val id: String = UUID.randomUUID().toString(),
    val donorId: String,
    val donorName: String = "",
    val foodItems: List<FoodItem> = emptyList(),
    val description: String = "",
    val status: DonationStatus = DonationStatus.AVAILABLE,
    val receiverId: String? = null,
    val receiverName: String? = null,
    val peopleServed: Int? = null,
    val createdAt: Long = System.currentTimeMillis()
)
