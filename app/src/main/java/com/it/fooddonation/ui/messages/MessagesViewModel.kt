package com.it.fooddonation.ui.messages

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.it.fooddonation.data.model.Notification
import com.it.fooddonation.data.remote.SupabaseClient
import com.it.fooddonation.data.repository.AuthRepository
import com.it.fooddonation.data.repository.MessagingRepository
import com.it.fooddonation.data.repository.NotificationRepository
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * UI state for messages list screen
 */
data class MessagesListState(
    val conversations: List<MessagingRepository.ConversationSummary> = emptyList(),
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel for managing messages list
 */
class MessagesViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val messagingRepository = MessagingRepository.getInstance()
    private val notificationRepository = NotificationRepository.getInstance()
    private val authRepository = AuthRepository()
    private val supabase = SupabaseClient.client

    private val _state = MutableStateFlow(MessagesListState())
    val state: StateFlow<MessagesListState> = _state.asStateFlow()

    // Current user ID for filtering realtime updates
    private var currentUserId: String? = null

    // Realtime channels for cleanup
    private var messagesChannel: io.github.jan.supabase.realtime.RealtimeChannel? = null
    private var notificationsChannel: io.github.jan.supabase.realtime.RealtimeChannel? = null

    companion object {
        private const val TAG = "MessagesViewModel"
    }

    init {
        // Realtime listeners will be set up after we have the user ID
    }

    /**
     * Load all conversations and notifications for the current user
     */
    fun loadConversations() {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                // Get current user
                val userProfile = authRepository.getCurrentUser()
                if (userProfile == null) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = "User not authenticated"
                    )
                    return@launch
                }

                // Set up realtime listeners on first load
                val isFirstLoad = currentUserId == null
                currentUserId = userProfile.id

                if (isFirstLoad) {
                    Log.d(TAG, "First load - setting up realtime listeners for user: ${userProfile.id}")
                    setupMessageRealtimeListener()
                    setupNotificationRealtimeListener()
                }

                Log.d(TAG, "Loading conversations and notifications for user: ${userProfile.id}")

                // Get conversations and notifications
                val conversations = messagingRepository.getConversationsForUser(userProfile.id)
                val notifications = notificationRepository.getNotificationsForUser(userProfile.id)

                _state.value = _state.value.copy(
                    conversations = conversations,
                    notifications = notifications,
                    isLoading = false
                )

                Log.d(TAG, "Loaded ${conversations.size} conversations and ${notifications.size} notifications")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load conversations and notifications", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load data"
                )
            }
        }
    }

    /**
     * Mark a notification as read
     */
    fun markNotificationAsRead(notificationId: String) {
        viewModelScope.launch {
            notificationRepository.markNotificationAsRead(notificationId)
            loadConversations() // Reload to update UI
        }
    }

    /**
     * Mark all notifications as read (called when user views the notifications screen)
     * Updates database but keeps UI highlighting so user can see which are new
     * When they navigate back and return, fresh load will show them as read
     */
    fun markAllNotificationsAsRead() {
        viewModelScope.launch {
            try {
                val userId = currentUserId ?: return@launch
                Log.d(TAG, "Marking all notifications as read in database for user: $userId")

                // Mark as read in database
                notificationRepository.markAllNotificationsAsRead(userId)

                // DON'T update local state - keep the red borders visible during this session
                // When user navigates away and comes back, reload will fetch them as read

                Log.d(TAG, "Successfully marked all notifications as read in database (UI highlighting preserved)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark all notifications as read", e)
            }
        }
    }

    /**
     * Setup real-time listener for message updates
     */
    private fun setupMessageRealtimeListener() {
        viewModelScope.launch {
            try {
                // Unsubscribe from previous channel if it exists
                messagesChannel?.let { channel ->
                    try {
                        channel.unsubscribe()
                        Log.d(TAG, "Unsubscribed from previous messages channel")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to unsubscribe from previous messages channel", e)
                    }
                }

                Log.d(TAG, "Setting up message real-time listener...")
                val channel = supabase.channel("messages-list-changes-${System.currentTimeMillis()}")
                messagesChannel = channel

                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "messages"
                }

                changeFlow.onEach { action ->
                    Log.d(TAG, "✅ Message real-time update received: ${action.javaClass.simpleName}")

                    if (currentUserId == null) {
                        Log.d(TAG, "⚠️ Skipping message update - user ID not set yet")
                        return@onEach
                    }

                    when (action) {
                        is PostgresAction.Insert, is PostgresAction.Update -> {
                            Log.d(TAG, "📨 Message changed - reloading conversations")
                            loadConversations()
                        }
                        else -> {
                            Log.d(TAG, "Other message action: $action")
                        }
                    }
                }.launchIn(viewModelScope)

                channel.subscribe()
                Log.d(TAG, "🔔 Successfully subscribed to real-time message updates")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to setup message real-time listener", e)
                e.printStackTrace()
            }
        }
    }

    /**
     * Setup real-time listener for notification updates
     */
    private fun setupNotificationRealtimeListener() {
        viewModelScope.launch {
            try {
                // Unsubscribe from previous channel if it exists
                notificationsChannel?.let { channel ->
                    try {
                        channel.unsubscribe()
                        Log.d(TAG, "Unsubscribed from previous notifications channel")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to unsubscribe from previous notifications channel", e)
                    }
                }

                Log.d(TAG, "Setting up notification real-time listener...")
                val channel = supabase.channel("notifications-list-changes-${System.currentTimeMillis()}")
                notificationsChannel = channel

                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "notifications"
                }

                changeFlow.onEach { action ->
                    Log.d(TAG, "✅ Notification real-time update received: ${action.javaClass.simpleName}")

                    if (currentUserId == null) {
                        Log.d(TAG, "⚠️ Skipping notification update - user ID not set yet")
                        return@onEach
                    }

                    when (action) {
                        is PostgresAction.Insert, is PostgresAction.Update -> {
                            Log.d(TAG, "🔔 Notification changed - reloading conversations")
                            loadConversations()
                        }
                        else -> {
                            Log.d(TAG, "Other notification action: $action")
                        }
                    }
                }.launchIn(viewModelScope)

                channel.subscribe()
                Log.d(TAG, "🔔 Successfully subscribed to real-time notification updates")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to setup notification real-time listener", e)
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel clearing - unsubscribing from channels")

        // Properly cleanup realtime channels
        viewModelScope.launch {
            try {
                messagesChannel?.unsubscribe()
                Log.d(TAG, "Cleaned up messages channel")

                notificationsChannel?.unsubscribe()
                Log.d(TAG, "Cleaned up notifications channel")
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up channels", e)
            }
        }

        Log.d(TAG, "ViewModel cleared")
    }
}
