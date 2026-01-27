package com.it.fooddonation.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Type of notification
 */
enum class NotificationType {
    DONATION_ACCEPTED,      // When receiver accepts a donation
    DONATION_PICKED_UP,     // When receiver marks donation as picked up
    NEW_MESSAGE             // When you receive a new message
}

/**
 * Represents an in-app notification
 *
 * @property id Unique identifier for the notification
 * @property userId ID of the user who will receive this notification
 * @property type Type of notification
 * @property title Notification title
 * @property message Notification message content
 * @property donationId Related donation ID (if applicable)
 * @property isRead Whether the notification has been read
 * @property createdAt When the notification was created
 */
@Serializable
data class Notification(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val type: String,  // Stored as string in DB
    val title: String,
    val message: String,
    val donationId: String? = null,
    val isRead: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
