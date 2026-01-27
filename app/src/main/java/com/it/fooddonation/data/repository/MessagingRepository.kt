package com.it.fooddonation.data.repository

import android.util.Log
import com.it.fooddonation.data.model.Message
import com.it.fooddonation.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * DTO for messages in Supabase (without sender_name - fetched from profiles)
 */
@Serializable
data class MessageDTO(
    val id: String,
    @SerialName("donation_id")
    val donationId: String? = null,
    @SerialName("sender_id")
    val senderId: String,
    @SerialName("receiver_id")
    val receiverId: String,
    val message: String,
    val timestamp: Long,
    @SerialName("is_read")
    val isRead: Boolean = false
)

/**
 * DTO for user profile data (minimal fields needed for messages)
 */
@Serializable
data class ProfileDTO(
    val id: String,
    @SerialName("full_name")
    val fullName: String?,
    val email: String? = null,
    val role: String? = null
)

/**
 * Repository for managing messages between donors and receivers
 */
class MessagingRepository {
    private val supabase = SupabaseClient.client

    companion object {
        private const val TAG = "MessagingRepository"

        @Volatile
        private var instance: MessagingRepository? = null

        fun getInstance(): MessagingRepository {
            return instance ?: synchronized(this) {
                instance ?: MessagingRepository().also { instance = it }
            }
        }
    }

    /**
     * Send a message
     */
    suspend fun sendMessage(message: Message): Result<Message> {
        return try {
            Log.d(TAG, "sendMessage: Sending message for donation ${message.donationId}")

            val messageDTO = MessageDTO(
                id = message.id,
                donationId = message.donationId,
                senderId = message.senderId,
                receiverId = message.receiverId,
                message = message.message,
                timestamp = message.timestamp
            )

            supabase.from("messages").insert(messageDTO)
            Log.d(TAG, "sendMessage: Message sent successfully")
            Result.success(message)
        } catch (e: Exception) {
            Log.e(TAG, "sendMessage: Failed", e)
            Result.failure(e)
        }
    }

    /**
     * Get all messages for a conversation between two users
     * Fetches sender names from profiles table to ensure they're always current
     */
    suspend fun getMessagesForConversation(currentUserId: String, otherUserId: String): List<Message> {
        return try {
            Log.d(TAG, "getMessagesForConversation: Fetching messages between $currentUserId and $otherUserId")

            // Get messages sent from current user to other user
            val sentMessages = supabase.from("messages")
                .select {
                    filter {
                        eq("sender_id", currentUserId)
                        eq("receiver_id", otherUserId)
                    }
                }
                .decodeList<MessageDTO>()

            Log.d(TAG, "getMessagesForConversation: Found ${sentMessages.size} sent messages")

            // Get messages received from other user
            val receivedMessages = supabase.from("messages")
                .select {
                    filter {
                        eq("sender_id", otherUserId)
                        eq("receiver_id", currentUserId)
                    }
                }
                .decodeList<MessageDTO>()

            Log.d(TAG, "getMessagesForConversation: Found ${receivedMessages.size} received messages")

            // Combine both lists
            val messageDTOs = (sentMessages + receivedMessages).sortedBy { it.timestamp }

            Log.d(TAG, "getMessagesForConversation: Total ${messageDTOs.size} messages")

            // Get unique sender IDs
            val senderIds = listOf(currentUserId, otherUserId).distinct()
            Log.d(TAG, "getMessagesForConversation: Unique sender IDs: $senderIds")

            // Fetch profiles for all senders
            val profiles = if (senderIds.isNotEmpty()) {
                try {
                    val profilesList = supabase.from("profiles")
                        .select(columns = io.github.jan.supabase.postgrest.query.Columns.list("id", "full_name", "email", "role")) {
                            filter {
                                isIn("id", senderIds)
                            }
                        }
                        .decodeList<ProfileDTO>()
                    Log.d(TAG, "getMessagesForConversation: Successfully fetched ${profilesList.size} profiles")
                    profilesList.forEach { profile ->
                        Log.d(TAG, "getMessagesForConversation: Profile - id: ${profile.id}, name: ${profile.fullName}")
                    }
                    profilesList
                } catch (e: Exception) {
                    Log.e(TAG, "getMessagesForConversation: Failed to fetch profiles", e)
                    emptyList()
                }
            } else {
                emptyList()
            }

            // Create a map of sender ID to name
            val senderNamesMap = profiles.associate {
                it.id to (it.fullName ?: "Unknown User")
            }
            Log.d(TAG, "getMessagesForConversation: Sender names map: $senderNamesMap")

            messageDTOs.map { dto ->
                val senderName = senderNamesMap[dto.senderId] ?: "Unknown User"
                Log.d(TAG, "getMessagesForConversation: Message from ${dto.senderId} -> name: $senderName")
                Message(
                    id = dto.id,
                    donationId = dto.donationId,
                    senderId = dto.senderId,
                    receiverId = dto.receiverId,
                    message = dto.message,
                    timestamp = dto.timestamp,
                    isRead = dto.isRead,
                    senderName = senderName
                )
            }.sortedBy { it.timestamp }
        } catch (e: Exception) {
            Log.e(TAG, "getMessagesForConversation: Failed", e)
            emptyList()
        }
    }

    /**
     * Get count of unread messages for a specific user
     */
    suspend fun getUnreadMessageCount(userId: String): Int {
        return try {
            Log.d(TAG, "getUnreadMessageCount: Fetching unread count for user: $userId")

            val messageDTOs = supabase.from("messages")
                .select {
                    filter {
                        eq("receiver_id", userId)
                        eq("is_read", false)
                    }
                }
                .decodeList<MessageDTO>()

            val count = messageDTOs.size
            Log.d(TAG, "getUnreadMessageCount: Found $count unread messages")
            count
        } catch (e: Exception) {
            Log.e(TAG, "getUnreadMessageCount: Failed", e)
            0
        }
    }

    /**
     * Mark all messages from a specific sender as read for the current user
     */
    suspend fun markMessagesAsRead(currentUserId: String, otherUserId: String): Result<Unit> {
        return try {
            Log.d(TAG, "markMessagesAsRead: Marking messages as read from $otherUserId to $currentUserId")

            val updateData = buildJsonObject {
                put("is_read", true)
            }

            supabase.from("messages")
                .update(updateData) {
                    filter {
                        eq("sender_id", otherUserId)
                        eq("receiver_id", currentUserId)
                        eq("is_read", false)
                    }
                }

            Log.d(TAG, "markMessagesAsRead: Successfully marked messages as read")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "markMessagesAsRead: Failed", e)
            Result.failure(e)
        }
    }

    /**
     * Data class representing a conversation summary
     */
    data class ConversationSummary(
        val otherUserId: String,
        val otherUserName: String,
        val lastMessage: String,
        val lastMessageTimestamp: Long,
        val unreadCount: Int
    )

    /**
     * Get all conversations for a user (grouped by user pairs, not by donation)
     * Fetches user names from profiles table to ensure they're always current
     */
    suspend fun getConversationsForUser(userId: String): List<ConversationSummary> {
        return try {
            Log.d(TAG, "getConversationsForUser: Fetching conversations for user: $userId")

            // Get all messages where user is either sender or receiver
            val messageDTOs = supabase.from("messages")
                .select()
                .decodeList<MessageDTO>()

            // Filter messages relevant to this user
            val userMessages = messageDTOs.filter {
                it.senderId == userId || it.receiverId == userId
            }

            // Get unique user IDs from conversations
            val allUserIds = userMessages.flatMap { listOf(it.senderId, it.receiverId) }.distinct()

            // Fetch profiles for all users involved in conversations
            val profiles = if (allUserIds.isNotEmpty()) {
                try {
                    val profilesList = supabase.from("profiles")
                        .select(columns = io.github.jan.supabase.postgrest.query.Columns.list("id", "full_name", "email", "role")) {
                            filter {
                                isIn("id", allUserIds)
                            }
                        }
                        .decodeList<ProfileDTO>()
                    Log.d(TAG, "getConversationsForUser: Successfully fetched ${profilesList.size} profiles")
                    profilesList
                } catch (e: Exception) {
                    Log.e(TAG, "getConversationsForUser: Failed to fetch profiles", e)
                    emptyList()
                }
            } else {
                emptyList()
            }

            // Create a map of user ID to name
            val userNamesMap = profiles.associate {
                it.id to (it.fullName ?: "Unknown User")
            }

            Log.d(TAG, "getConversationsForUser: Fetched ${profiles.size} user profiles")

            // Group by the other user (conversation partner) and create conversation summaries
            val conversations = userMessages
                .groupBy { message ->
                    // Determine the other user in each message
                    if (message.senderId == userId) message.receiverId else message.senderId
                }
                .map { (otherUserId, messages) ->
                    val sortedMessages = messages.sortedByDescending { it.timestamp }
                    val lastMessage = sortedMessages.first()

                    // Get the other user's name from the profiles map
                    val otherUserName = userNamesMap[otherUserId] ?: "Unknown User"

                    // Count unread messages (messages where user is receiver and not read)
                    val unreadCount = messages.count {
                        it.receiverId == userId && !it.isRead
                    }

                    ConversationSummary(
                        otherUserId = otherUserId,
                        otherUserName = otherUserName,
                        lastMessage = lastMessage.message,
                        lastMessageTimestamp = lastMessage.timestamp,
                        unreadCount = unreadCount
                    )
                }
                .sortedByDescending { it.lastMessageTimestamp }

            Log.d(TAG, "getConversationsForUser: Found ${conversations.size} conversations")
            conversations
        } catch (e: Exception) {
            Log.e(TAG, "getConversationsForUser: Failed", e)
            emptyList()
        }
    }
}
