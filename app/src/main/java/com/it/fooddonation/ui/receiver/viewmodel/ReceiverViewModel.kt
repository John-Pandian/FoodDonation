package com.it.fooddonation.ui.receiver.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.it.fooddonation.data.model.Donation
import com.it.fooddonation.data.model.UserProfile
import com.it.fooddonation.data.remote.SupabaseClient
import com.it.fooddonation.data.repository.AuthRepository
import com.it.fooddonation.data.repository.DonationRepository
import com.it.fooddonation.data.service.EmailService
import io.github.jan.supabase.postgrest.from
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
 * UI state for the receiver dashboard
 */
data class ReceiverDashboardState(
    val availableDonations: List<Donation> = emptyList(),
    val myDonations: List<Donation> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val unreadMessageCount: Int = 0
)

/**
 * ViewModel for managing receiver dashboard and donation browsing/acceptance
 */
class ReceiverViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val donationRepository = DonationRepository.getInstance()
    private val authRepository = AuthRepository()
    private val messagingRepository = com.it.fooddonation.data.repository.MessagingRepository.getInstance()
    private val notificationRepository = com.it.fooddonation.data.repository.NotificationRepository.getInstance()
    private val supabase = SupabaseClient.client

    private val _dashboardState = MutableStateFlow(ReceiverDashboardState())
    val dashboardState: StateFlow<ReceiverDashboardState> = _dashboardState.asStateFlow()

    // Current receiver ID from authenticated user
    private var currentReceiverId: String? = null
    private var currentReceiverName: String? = null

    // Realtime channels for cleanup
    private var donationsChannel: io.github.jan.supabase.realtime.RealtimeChannel? = null
    private var messagesChannel: io.github.jan.supabase.realtime.RealtimeChannel? = null
    private var notificationsChannel: io.github.jan.supabase.realtime.RealtimeChannel? = null

    companion object {
        private const val TAG = "ReceiverViewModel"
    }

    init {
        // Realtime listeners will be set up after we have the receiver ID
        // This prevents subscribing to updates before we know which receiver we are
    }

    /**
     * Load available donations for browsing
     */
    fun loadAvailableDonations() {
        viewModelScope.launch {
            Log.d(TAG, "🔄🔄🔄 loadAvailableDonations() called")
            _dashboardState.value = _dashboardState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                // Get current user profile
                val userProfile = authRepository.getCurrentUser()
                if (userProfile == null) {
                    Log.e(TAG, "❌ User not authenticated")
                    _dashboardState.value = _dashboardState.value.copy(
                        isLoading = false,
                        errorMessage = "User not authenticated"
                    )
                    return@launch
                }

                // Set up realtime listeners on first load
                val isFirstLoad = currentReceiverId == null
                currentReceiverId = userProfile.id
                currentReceiverName = userProfile.fullName ?: "Unknown"

                if (isFirstLoad) {
                    Log.d(TAG, "🆕 First load - setting up realtime listeners for receiver: ${userProfile.id}")
                    setupRealtimeListener()
                    setupMessageRealtimeListener()
                    setupNotificationRealtimeListener()
                } else {
                    Log.d(TAG, "♻️ Reloading donations (realtime already set up)")
                }

                Log.d(TAG, "📡 Fetching available donations from repository...")
                val donations = donationRepository.getAvailableDonations()
                Log.d(TAG, "📦 Received ${donations.size} donations from repository")

                _dashboardState.value = _dashboardState.value.copy(
                    availableDonations = donations.sortedByDescending { it.createdAt },
                    isLoading = false
                )
                Log.d(TAG, "✅ State updated with ${donations.size} available donations")

                // Also load unread message count
                loadUnreadMessageCount()
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to load donations: ${e.message}", e)
                _dashboardState.value = _dashboardState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load donations"
                )
            }
        }
    }

    /**
     * Load donations accepted by current receiver
     */
    fun loadMyDonations() {
        viewModelScope.launch {
            try {
                val userProfile = authRepository.getCurrentUser()
                if (userProfile == null) {
                    return@launch
                }

                currentReceiverId = userProfile.id
                currentReceiverName = userProfile.fullName ?: "Unknown"

                val donations = donationRepository.getMyAcceptedDonations(userProfile.id)
                _dashboardState.value = _dashboardState.value.copy(
                    myDonations = donations.sortedByDescending { it.createdAt }
                )
            } catch (e: Exception) {
                _dashboardState.value = _dashboardState.value.copy(
                    errorMessage = e.message ?: "Failed to load your donations"
                )
            }
        }
    }

    /**
     * Accept a donation
     */
    fun acceptDonation(donationId: String) {
        viewModelScope.launch {
            val receiverId = currentReceiverId
            val receiverName = currentReceiverName

            if (receiverId == null || receiverName == null) {
                _dashboardState.value = _dashboardState.value.copy(
                    errorMessage = "User not authenticated"
                )
                return@launch
            }

            // First, get the donation to know the donor
            val donation = _dashboardState.value.availableDonations.find { it.id == donationId }

            donationRepository.acceptDonation(donationId, receiverId, receiverName)
                .onSuccess {
                    _dashboardState.value = _dashboardState.value.copy(
                        successMessage = "Donation accepted successfully!"
                    )

                    // Create notification for the donor
                    donation?.let {
                        notificationRepository.createNotification(
                            userId = it.donorId,
                            type = com.it.fooddonation.data.model.NotificationType.DONATION_ACCEPTED,
                            title = "Donation Accepted!",
                            message = "$receiverName has accepted your donation",
                            donationId = donationId
                        )
                    }

                    // Send email notification to the donor (non-blocking)
                    donation?.let { don ->
                        viewModelScope.launch {
                            try {
                                val donorProfile = supabase.from("profiles")
                                    .select { filter { eq("id", don.donorId) } }
                                    .decodeSingle<UserProfile>()

                                EmailService.sendDonationAcceptedEmail(
                                    recipientEmail = donorProfile.email,
                                    donorName = donorProfile.fullName ?: don.donorName,
                                    receiverName = receiverName ?: "A receiver",
                                    donation = don
                                )
                            } catch (e: Exception) {
                                Log.e(TAG, "Failed to send email notification: ${e.message}", e)
                            }
                        }
                    }

                    // Reload both lists
                    loadAvailableDonations()
                    loadMyDonations()
                }
                .onFailure { error ->
                    _dashboardState.value = _dashboardState.value.copy(
                        errorMessage = error.message ?: "Failed to accept donation"
                    )
                }
        }
    }

    /**
     * Mark donation as picked up with people served count
     */
    fun markAsPickedUp(donationId: String, peopleServed: Int) {
        viewModelScope.launch {
            val receiverName = currentReceiverName

            // Get the donation to know the donor
            val donation = _dashboardState.value.myDonations.find { it.id == donationId }

            donationRepository.markAsPickedUp(
                donationId = donationId,
                peopleServed = peopleServed
            ).onSuccess {
                _dashboardState.value = _dashboardState.value.copy(
                    successMessage = "Marked as picked up! $peopleServed people served."
                )

                // Create notification for the donor
                donation?.let {
                    notificationRepository.createNotification(
                        userId = it.donorId,
                        type = com.it.fooddonation.data.model.NotificationType.DONATION_PICKED_UP,
                        title = "Donation Picked Up!",
                        message = "$receiverName has picked up your donation. $peopleServed people served.",
                        donationId = donationId
                    )
                }

                loadMyDonations()
            }.onFailure { error ->
                _dashboardState.value = _dashboardState.value.copy(
                    errorMessage = error.message ?: "Failed to update status"
                )
            }
        }
    }

    /**
     * Refresh donations (for pull-to-refresh)
     */
    fun refreshDonations() {
        viewModelScope.launch {
            Log.d(TAG, "🔄 Manual refresh triggered")
            _dashboardState.value = _dashboardState.value.copy(isRefreshing = true)

            try {
                // Reload both available and my donations
                val userProfile = authRepository.getCurrentUser()
                if (userProfile != null) {
                    currentReceiverId = userProfile.id
                    currentReceiverName = userProfile.fullName ?: "Unknown"

                    // Load available donations
                    val availableDonations = donationRepository.getAvailableDonations()

                    // Load my donations
                    val myDonations = donationRepository.getMyAcceptedDonations(userProfile.id)

                    _dashboardState.value = _dashboardState.value.copy(
                        availableDonations = availableDonations.sortedByDescending { it.createdAt },
                        myDonations = myDonations.sortedByDescending { it.createdAt },
                        isRefreshing = false
                    )

                    // Also refresh unread count
                    loadUnreadMessageCount()

                    Log.d(TAG, "✅ Manual refresh completed - ${availableDonations.size} available, ${myDonations.size} accepted")
                } else {
                    _dashboardState.value = _dashboardState.value.copy(isRefreshing = false)
                }
            } catch (e: Exception) {
                Log.e(TAG, "❌ Refresh failed: ${e.message}", e)
                _dashboardState.value = _dashboardState.value.copy(
                    isRefreshing = false,
                    errorMessage = "Failed to refresh"
                )
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearError() {
        _dashboardState.value = _dashboardState.value.copy(errorMessage = null)
    }

    /**
     * Clear success message
     */
    fun clearSuccess() {
        _dashboardState.value = _dashboardState.value.copy(successMessage = null)
    }

    /**
     * Setup real-time listener for donation updates
     * Only subscribes once and filters updates for the current receiver
     */
    private fun setupRealtimeListener() {
        viewModelScope.launch {
            try {
                // Unsubscribe from previous channel if it exists
                donationsChannel?.let { channel ->
                    try {
                        channel.unsubscribe()
                        Log.d(TAG, "Unsubscribed from previous donations channel")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to unsubscribe from previous channel", e)
                    }
                }

                val channelName = "receiver-donations-changes-${System.currentTimeMillis()}"
                Log.d(TAG, "🔧🔧🔧 Setting up real-time listener for receiver: $currentReceiverId")
                Log.d(TAG, "Channel name: $channelName")
                Log.d(TAG, "Subscribing to table: donations")

                val channel = supabase.channel(channelName)
                donationsChannel = channel

                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "donations"
                }
                Log.d(TAG, "PostgresChangeFlow created for 'donations' table")

                changeFlow.onEach { action ->
                    Log.d(TAG, "✅✅✅ REALTIME EVENT RECEIVED ✅✅✅")
                    Log.d(TAG, "Action type: ${action.javaClass.simpleName}")
                    Log.d(TAG, "Action details: $action")
                    Log.d(TAG, "Current receiver ID: $currentReceiverId")

                    // Only reload if we have a receiver ID
                    if (currentReceiverId == null) {
                        Log.e(TAG, "⚠️⚠️⚠️ SKIPPING UPDATE - receiver ID not set yet")
                        return@onEach
                    }

                    when (action) {
                        is PostgresAction.Update -> {
                            Log.d(TAG, "📝📝📝 Donation UPDATED - reloading both lists")
                            Log.d(TAG, "Update record: ${action.record}")
                            // Reload both available and my donations
                            loadAvailableDonations()
                            loadMyDonations()
                        }
                        is PostgresAction.Insert -> {
                            Log.d(TAG, "➕➕➕ NEW DONATION INSERTED - reloading available donations")
                            Log.d(TAG, "New record: ${action.record}")
                            loadAvailableDonations()
                        }
                        is PostgresAction.Delete -> {
                            Log.d(TAG, "🗑️🗑️🗑️ Donation DELETED - reloading both lists")
                            Log.d(TAG, "Old record: ${action.oldRecord}")
                            loadAvailableDonations()
                            loadMyDonations()
                        }
                        else -> {
                            Log.d(TAG, "❓ Other action type: $action")
                        }
                    }
                }.launchIn(viewModelScope)

                Log.d(TAG, "Calling channel.subscribe()...")
                channel.subscribe()
                Log.d(TAG, "🔔🔔🔔 Successfully subscribed to real-time donation updates for channel: $channelName")
                Log.d(TAG, "Waiting for donation events...")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to setup real-time listener", e)
                e.printStackTrace()
            }
        }
    }

    /**
     * Load unread message count for the current user (includes both messages and notifications)
     */
    fun loadUnreadMessageCount() {
        viewModelScope.launch {
            try {
                val userId = currentReceiverId ?: return@launch
                val messageCount = messagingRepository.getUnreadMessageCount(userId)
                val notificationCount = notificationRepository.getUnreadNotificationCount(userId)
                val totalCount = messageCount + notificationCount
                _dashboardState.value = _dashboardState.value.copy(
                    unreadMessageCount = totalCount
                )
                Log.d(TAG, "Unread count updated: $messageCount messages + $notificationCount notifications = $totalCount")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load unread count", e)
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
                val channel = supabase.channel("receiver-messages-changes-${System.currentTimeMillis()}")
                messagesChannel = channel

                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "messages"
                }

                changeFlow.onEach { action ->
                    Log.d(TAG, "✅ Message real-time update received: ${action.javaClass.simpleName}")

                    if (currentReceiverId == null) {
                        Log.d(TAG, "⚠️ Skipping message update - receiver ID not set yet")
                        return@onEach
                    }

                    when (action) {
                        is PostgresAction.Insert, is PostgresAction.Update -> {
                            Log.d(TAG, "📨 Message changed - reloading unread count")
                            loadUnreadMessageCount()
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
                val channel = supabase.channel("receiver-notifications-changes-${System.currentTimeMillis()}")
                notificationsChannel = channel

                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "notifications"
                }

                changeFlow.onEach { action ->
                    Log.d(TAG, "✅ Notification real-time update received: ${action.javaClass.simpleName}")

                    if (currentReceiverId == null) {
                        Log.d(TAG, "⚠️ Skipping notification update - receiver ID not set yet")
                        return@onEach
                    }

                    when (action) {
                        is PostgresAction.Insert, is PostgresAction.Update -> {
                            Log.d(TAG, "🔔 Notification changed - reloading unread count")
                            loadUnreadMessageCount()
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
                donationsChannel?.unsubscribe()
                Log.d(TAG, "Cleaned up donations channel")

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
