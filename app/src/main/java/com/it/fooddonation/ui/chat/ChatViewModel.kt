package com.it.fooddonation.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.it.fooddonation.data.model.Message
import com.it.fooddonation.data.remote.SupabaseClient
import com.it.fooddonation.data.repository.AuthRepository
import com.it.fooddonation.data.repository.MessagingRepository
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
 * UI state for chat screen
 */
data class ChatState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val errorMessage: String? = null,
    val currentUserId: String = "",
    val currentUserName: String = "",
    val hasLoadedOnce: Boolean = false
)

/**
 * ViewModel for managing chat/messaging
 */
class ChatViewModel : ViewModel() {
    private val messagingRepository = MessagingRepository.getInstance()
    private val authRepository = AuthRepository()
    private val supabase = SupabaseClient.client

    private val _chatState = MutableStateFlow(ChatState())
    val chatState: StateFlow<ChatState> = _chatState.asStateFlow()

    private var currentOtherUserId: String? = null
    private var messagesChannel: io.github.jan.supabase.realtime.RealtimeChannel? = null
    private var isRealtimeSetup = false

    companion object {
        private const val TAG = "ChatViewModel"
    }

    init {
        // Get current user info
        viewModelScope.launch {
            try {
                val userProfile = authRepository.getCurrentUser()
                if (userProfile != null) {
                    _chatState.value = _chatState.value.copy(
                        currentUserId = userProfile.id,
                        currentUserName = userProfile.fullName ?: "User"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get current user", e)
            }
        }

        // Realtime listener will be set up when loadMessages is called
    }

    /**
     * Load messages for a conversation with another user
     */
    fun loadMessages(otherUserId: String) {
        currentOtherUserId = otherUserId

        // Setup realtime listener on first load
        if (!isRealtimeSetup) {
            Log.d(TAG, "First load - setting up realtime listener")
            setupRealtimeListener()
            isRealtimeSetup = true
        }

        viewModelScope.launch {
            _chatState.value = _chatState.value.copy(isLoading = true, errorMessage = null)

            try {
                // Ensure we have current user ID before loading messages
                var currentUserId = _chatState.value.currentUserId
                if (currentUserId.isEmpty()) {
                    Log.d(TAG, "loadMessages: Current user ID not loaded yet, fetching...")
                    val userProfile = authRepository.getCurrentUser()
                    if (userProfile != null) {
                        currentUserId = userProfile.id
                        _chatState.value = _chatState.value.copy(
                            currentUserId = currentUserId,
                            currentUserName = userProfile.fullName ?: "User"
                        )
                    } else {
                        Log.e(TAG, "loadMessages: Failed to get current user")
                        _chatState.value = _chatState.value.copy(
                            isLoading = false,
                            hasLoadedOnce = true,
                            errorMessage = "Failed to load user information"
                        )
                        return@launch
                    }
                }

                Log.d(TAG, "loadMessages: Fetching messages between $currentUserId and $otherUserId")
                val messages = messagingRepository.getMessagesForConversation(currentUserId, otherUserId)
                Log.d(TAG, "loadMessages: Loaded ${messages.size} messages")

                _chatState.value = _chatState.value.copy(
                    messages = messages,
                    isLoading = false,
                    hasLoadedOnce = true
                )

                // Mark messages as read after loading
                markMessagesAsRead(otherUserId)
            } catch (e: Exception) {
                Log.e(TAG, "loadMessages: Failed to load messages", e)
                _chatState.value = _chatState.value.copy(
                    isLoading = false,
                    hasLoadedOnce = true,
                    errorMessage = e.message ?: "Failed to load messages"
                )
            }
        }
    }

    /**
     * Mark all messages in this conversation as read
     */
    private fun markMessagesAsRead(otherUserId: String) {
        viewModelScope.launch {
            try {
                val userId = _chatState.value.currentUserId
                if (userId.isNotEmpty()) {
                    messagingRepository.markMessagesAsRead(userId, otherUserId)
                    Log.d(TAG, "Marked messages as read from user: $otherUserId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to mark messages as read", e)
            }
        }
    }

    /**
     * Send a message with optimistic updates
     */
    fun sendMessage(
        receiverId: String,
        messageText: String,
        onSuccess: () -> Unit = {},
        onError: (String) -> Unit = {}
    ) {
        if (messageText.isBlank()) return

        val message = Message(
            donationId = null, // No longer tied to specific donation
            senderId = _chatState.value.currentUserId,
            receiverId = receiverId,
            message = messageText.trim(),
            status = com.it.fooddonation.data.model.MessageStatus.SENDING,
            senderName = _chatState.value.currentUserName // Set for optimistic UI, not stored in DB
        )

        // Add message optimistically
        _chatState.value = _chatState.value.copy(
            messages = _chatState.value.messages + message,
            isSending = false
        )
        onSuccess()

        viewModelScope.launch {
            messagingRepository.sendMessage(message)
                .onSuccess {
                    // Update message status to SENT
                    _chatState.value = _chatState.value.copy(
                        messages = _chatState.value.messages.map {
                            if (it.id == message.id) {
                                it.copy(status = com.it.fooddonation.data.model.MessageStatus.SENT)
                            } else {
                                it
                            }
                        }
                    )
                }
                .onFailure { error ->
                    // Update message status to FAILED
                    _chatState.value = _chatState.value.copy(
                        messages = _chatState.value.messages.map {
                            if (it.id == message.id) {
                                it.copy(status = com.it.fooddonation.data.model.MessageStatus.FAILED)
                            } else {
                                it
                            }
                        }
                    )
                }
        }
    }

    /**
     * Retry sending a failed message
     */
    fun retryMessage(message: Message) {
        viewModelScope.launch {
            // Update status to SENDING
            _chatState.value = _chatState.value.copy(
                messages = _chatState.value.messages.map {
                    if (it.id == message.id) {
                        it.copy(status = com.it.fooddonation.data.model.MessageStatus.SENDING)
                    } else {
                        it
                    }
                }
            )

            messagingRepository.sendMessage(message)
                .onSuccess {
                    // Update message status to SENT
                    _chatState.value = _chatState.value.copy(
                        messages = _chatState.value.messages.map {
                            if (it.id == message.id) {
                                it.copy(status = com.it.fooddonation.data.model.MessageStatus.SENT)
                            } else {
                                it
                            }
                        }
                    )
                }
                .onFailure {
                    // Update message status back to FAILED
                    _chatState.value = _chatState.value.copy(
                        messages = _chatState.value.messages.map {
                            if (it.id == message.id) {
                                it.copy(status = com.it.fooddonation.data.model.MessageStatus.FAILED)
                            } else {
                                it
                            }
                        }
                    )
                }
        }
    }

    /**
     * Setup real-time listener for new messages
     * Only subscribes once and filters updates for the current donation
     */
    private fun setupRealtimeListener() {
        viewModelScope.launch {
            try {
                // Unsubscribe from previous channel if it exists
                messagesChannel?.let { channel ->
                    try {
                        channel.unsubscribe()
                        Log.d(TAG, "Unsubscribed from previous messages channel")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to unsubscribe from previous channel", e)
                    }
                }

                Log.d(TAG, "Setting up real-time listener for messages...")
                val channel = supabase.channel("chat-messages-changes-${System.currentTimeMillis()}")
                messagesChannel = channel

                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "messages"
                }

                changeFlow.onEach { action ->
                    Log.d(TAG, "✅ Real-time message update: ${action.javaClass.simpleName}")

                    // Only process if we have the other user ID
                    if (currentOtherUserId == null) {
                        Log.d(TAG, "⚠️ Skipping update - other user ID not set yet")
                        return@onEach
                    }

                    when (action) {
                        is PostgresAction.Insert -> {
                            Log.d(TAG, "📨 New message inserted")
                            // Reload messages for current conversation
                            currentOtherUserId?.let { otherUserId ->
                                val currentUserId = _chatState.value.currentUserId
                                val newMessages = messagingRepository.getMessagesForConversation(currentUserId, otherUserId)
                                // Merge with existing messages, keeping optimistic ones
                                val existingIds = _chatState.value.messages.map { it.id }.toSet()

                                _chatState.value = _chatState.value.copy(
                                    messages = (_chatState.value.messages.filter {
                                        it.status == com.it.fooddonation.data.model.MessageStatus.SENDING ||
                                        it.status == com.it.fooddonation.data.model.MessageStatus.FAILED
                                    } + newMessages).sortedBy { it.timestamp }
                                )
                            }
                        }
                        is PostgresAction.Update -> {
                            Log.d(TAG, "📝 Message updated (likely read status changed)")
                            // Reload messages to get updated read status
                            currentOtherUserId?.let { otherUserId ->
                                val currentUserId = _chatState.value.currentUserId
                                val updatedMessages = messagingRepository.getMessagesForConversation(currentUserId, otherUserId)
                                _chatState.value = _chatState.value.copy(
                                    messages = updatedMessages
                                )
                            }
                        }
                        else -> {
                            Log.d(TAG, "Other message action: $action")
                        }
                    }
                }.launchIn(viewModelScope)

                channel.subscribe()
                Log.d(TAG, "🔔 Successfully subscribed to message updates")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to setup real-time listener", e)
                e.printStackTrace()
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _chatState.value = _chatState.value.copy(errorMessage = null)
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ChatViewModel clearing - unsubscribing from channel")

        // Properly cleanup realtime channel
        viewModelScope.launch {
            try {
                messagesChannel?.unsubscribe()
                Log.d(TAG, "Cleaned up messages channel")
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up channel", e)
            }
        }

        Log.d(TAG, "ChatViewModel cleared")
    }
}
