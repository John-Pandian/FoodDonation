package com.it.fooddonation.data.repository

import android.util.Log
import com.it.fooddonation.data.model.Notification
import com.it.fooddonation.data.model.NotificationType
import com.it.fooddonation.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * DTO for notifications in Supabase
 */
@Serializable
data class NotificationDTO(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    val type: String,
    val title: String,
    val message: String,
    @SerialName("donation_id")
    val donationId: String? = null,
    @SerialName("is_read")
    val isRead: Boolean = false,
    @SerialName("created_at")
    val createdAt: Long
)

/**
 * Repository for managing notifications
 */
class NotificationRepository private constructor() {

    private val supabase = SupabaseClient.client

    companion object {
        private const val TAG = "NotificationRepository"

        @Volatile
        private var instance: NotificationRepository? = null

        fun getInstance(): NotificationRepository {
            return instance ?: synchronized(this) {
                instance ?: NotificationRepository().also { instance = it }
            }
        }
    }

    /**
     * Create a notification
     */
    suspend fun createNotification(
        userId: String,
        type: NotificationType,
        title: String,
        message: String,
        donationId: String? = null
    ): Result<Unit> {
        return try {
            Log.d(TAG, "createNotification: Creating notification for user: $userId, type: $type")

            val notification = Notification(
                userId = userId,
                type = type.name,
                title = title,
                message = message,
                donationId = donationId
            )

            val notificationDTO = NotificationDTO(
                id = notification.id,
                userId = notification.userId,
                type = notification.type,
                title = notification.title,
                message = notification.message,
                donationId = notification.donationId,
                isRead = notification.isRead,
                createdAt = notification.createdAt
            )

            supabase.from("notifications").insert(notificationDTO)

            Log.d(TAG, "createNotification: Successfully created notification")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "createNotification: Failed", e)
            Result.failure(e)
        }
    }

    /**
     * Get all notifications for a user
     */
    suspend fun getNotificationsForUser(userId: String): List<Notification> {
        return try {
            Log.d(TAG, "getNotificationsForUser: Fetching notifications for user: $userId")

            val notificationDTOs = supabase.from("notifications")
                .select {
                    filter {
                        eq("user_id", userId)
                    }
                }
                .decodeList<NotificationDTO>()

            val notifications = notificationDTOs.map { dto ->
                Notification(
                    id = dto.id,
                    userId = dto.userId,
                    type = dto.type,
                    title = dto.title,
                    message = dto.message,
                    donationId = dto.donationId,
                    isRead = dto.isRead,
                    createdAt = dto.createdAt
                )
            }.sortedByDescending { it.createdAt }

            Log.d(TAG, "getNotificationsForUser: Found ${notifications.size} notifications")
            notifications
        } catch (e: Exception) {
            Log.e(TAG, "getNotificationsForUser: Failed", e)
            emptyList()
        }
    }

    /**
     * Get count of unread notifications for a user
     */
    suspend fun getUnreadNotificationCount(userId: String): Int {
        return try {
            Log.d(TAG, "getUnreadNotificationCount: Fetching unread count for user: $userId")

            val notificationDTOs = supabase.from("notifications")
                .select {
                    filter {
                        eq("user_id", userId)
                        eq("is_read", false)
                    }
                }
                .decodeList<NotificationDTO>()

            val count = notificationDTOs.size
            Log.d(TAG, "getUnreadNotificationCount: Found $count unread notifications")
            count
        } catch (e: Exception) {
            Log.e(TAG, "getUnreadNotificationCount: Failed", e)
            0
        }
    }

    /**
     * Mark notification as read
     */
    suspend fun markNotificationAsRead(notificationId: String): Result<Unit> {
        return try {
            Log.d(TAG, "markNotificationAsRead: Marking notification as read: $notificationId")

            val updateData = buildJsonObject {
                put("is_read", true)
            }

            supabase.from("notifications")
                .update(updateData) {
                    filter {
                        eq("id", notificationId)
                    }
                }

            Log.d(TAG, "markNotificationAsRead: Successfully marked as read")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "markNotificationAsRead: Failed", e)
            Result.failure(e)
        }
    }

    /**
     * Mark all notifications as read for a user
     */
    suspend fun markAllNotificationsAsRead(userId: String): Result<Unit> {
        return try {
            Log.d(TAG, "markAllNotificationsAsRead: Marking all notifications as read for user: $userId")

            val updateData = buildJsonObject {
                put("is_read", true)
            }

            supabase.from("notifications")
                .update(updateData) {
                    filter {
                        eq("user_id", userId)
                        eq("is_read", false)
                    }
                }

            Log.d(TAG, "markAllNotificationsAsRead: Successfully marked all as read")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "markAllNotificationsAsRead: Failed", e)
            Result.failure(e)
        }
    }

    /**
     * Delete a notification
     */
    suspend fun deleteNotification(notificationId: String): Result<Unit> {
        return try {
            Log.d(TAG, "deleteNotification: Deleting notification: $notificationId")

            supabase.from("notifications")
                .delete {
                    filter {
                        eq("id", notificationId)
                    }
                }

            Log.d(TAG, "deleteNotification: Successfully deleted")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteNotification: Failed", e)
            Result.failure(e)
        }
    }
}
