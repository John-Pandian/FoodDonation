package com.it.fooddonation.data.model

import kotlinx.serialization.Serializable
import java.util.UUID

/**
 * Message sending states
 */
enum class MessageStatus {
    SENDING,    // Message is being sent
    SENT,       // Message sent successfully
    FAILED      // Message failed to send
}

/**
 * Represents a message between a donor and receiver
 *
 * @property id Unique identifier for the message
 * @property donationId (Optional) ID of the donation this conversation is about - null for consolidated conversations
 * @property senderId ID of the user who sent the message
 * @property receiverId ID of the user who will receive the message
 * @property message The text content of the message
 * @property timestamp When the message was sent
 * @property status Status of the message (sending, sent, failed) - not stored in database
 * @property isRead Whether the message has been read by the receiver - stored in database
 * @property senderName Name of the sender - fetched dynamically from profiles, not stored in database
 */
@Serializable
data class Message(
    val id: String = UUID.randomUUID().toString(),
    val donationId: String? = null,
    val senderId: String,
    val receiverId: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    @kotlinx.serialization.Transient
    val status: MessageStatus = MessageStatus.SENT,
    @kotlinx.serialization.Transient
    val isRead: Boolean = false,
    @kotlinx.serialization.Transient
    val senderName: String = "" // Fetched dynamically from profiles table
)
