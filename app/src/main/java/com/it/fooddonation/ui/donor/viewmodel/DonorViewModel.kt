package com.it.fooddonation.ui.donor.viewmodel

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.it.fooddonation.data.model.Donation
import com.it.fooddonation.data.model.DonationStatus
import com.it.fooddonation.data.remote.SupabaseClient
import com.it.fooddonation.data.repository.DonationRepository
import com.it.fooddonation.data.storage.ImageStorageHelper
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
 * UI state for the donor dashboard
 */
data class DonorDashboardState(
    val donations: List<Donation> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val unreadMessageCount: Int = 0
)

/**
 * ViewModel for managing donor dashboard and donation operations
 */
class DonorViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val donationRepository = DonationRepository.getInstance()
    private val imageStorageHelper = ImageStorageHelper(application)
    private val authRepository = com.it.fooddonation.data.repository.AuthRepository()
    private val messagingRepository = com.it.fooddonation.data.repository.MessagingRepository.getInstance()
    private val notificationRepository = com.it.fooddonation.data.repository.NotificationRepository.getInstance()
    private val supabase = SupabaseClient.client

    private val _dashboardState = MutableStateFlow(DonorDashboardState())
    val dashboardState: StateFlow<DonorDashboardState> = _dashboardState.asStateFlow()

    // Current donor ID from authenticated user
    private var currentDonorId: String? = null

    // Realtime channels for cleanup
    private var donationsChannel: io.github.jan.supabase.realtime.RealtimeChannel? = null
    private var messagesChannel: io.github.jan.supabase.realtime.RealtimeChannel? = null
    private var notificationsChannel: io.github.jan.supabase.realtime.RealtimeChannel? = null

    companion object {
        private const val TAG = "DonorViewModel"
    }

    init {
        // Realtime listeners will be set up after we have the donor ID
        // This prevents subscribing to updates before we know which donor we are
    }

    /**
     * Load all donations for the current donor
     */
    fun loadDonations() {
        viewModelScope.launch {
            _dashboardState.value = _dashboardState.value.copy(
                isLoading = true,
                errorMessage = null
            )

            try {
                // Get current user profile to get donor ID
                val userProfile = authRepository.getCurrentUser()
                if (userProfile == null) {
                    _dashboardState.value = _dashboardState.value.copy(
                        isLoading = false,
                        errorMessage = "User not authenticated"
                    )
                    return@launch
                }

                // Set up realtime listeners on first load
                val isFirstLoad = currentDonorId == null
                currentDonorId = userProfile.id

                if (isFirstLoad) {
                    Log.d(TAG, "First load - setting up realtime listeners for donor: ${userProfile.id}")
                    setupRealtimeListener()
                    setupMessageRealtimeListener()
                    setupNotificationRealtimeListener()
                }

                val donations = donationRepository.getDonationsByDonor(userProfile.id)
                _dashboardState.value = _dashboardState.value.copy(
                    donations = donations.sortedByDescending { it.createdAt },
                    isLoading = false
                )

                // Also load unread message count
                loadUnreadMessageCount()
            } catch (e: Exception) {
                _dashboardState.value = _dashboardState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load donations"
                )
            }
        }
    }

    /**
     * Create a new donation with multiple food items
     * Uploads shared donation image to Supabase Storage if provided, then creates donation record
     */
    fun createDonation(
        foodItems: List<com.it.fooddonation.ui.donor.FoodItemInput>,
        description: String,
        donationImageUri: Uri?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (foodItems.isEmpty()) {
                onError("Please add at least one food item")
                return@launch
            }

            // Get current user profile
            val userProfile = authRepository.getCurrentUser()
            if (userProfile == null) {
                onError("User not authenticated")
                return@launch
            }

            // Note: Address validation is done at screen level before user can access donation form
            val donorId = userProfile.id.also { currentDonorId = it }

            // Upload shared donation image if provided
            var uploadedImageUrl: String? = null
            if (donationImageUri != null) {
                val uploadResult = imageStorageHelper.uploadDonationImage(
                    imageUri = donationImageUri,
                    donorId = donorId
                )

                uploadResult.onSuccess { url ->
                    uploadedImageUrl = url
                }.onFailure { error ->
                    onError("Failed to upload image: ${error.message}")
                    return@launch
                }
            }

            // Create food items - apply shared image to first item only
            val processedFoodItems = foodItems.mapIndexed { index, inputItem ->
                com.it.fooddonation.data.model.FoodItem(
                    foodName = inputItem.foodName.trim(),
                    quantity = inputItem.quantity.trim(),
                    imageUri = if (index == 0) uploadedImageUrl else null
                )
            }

            // Create donation with processed food items
            val donation = Donation(
                donorId = donorId,
                donorName = userProfile.fullName ?: "Anonymous",
                foodItems = processedFoodItems,
                description = description.trim()
            )

            donationRepository.createDonation(donation)
                .onSuccess {
                    loadDonations()
                    onSuccess()
                }
                .onFailure { error ->
                    onError(error.message ?: "Failed to create donation")
                }
        }
    }

    /**
     * Cancel a donation
     */
    fun cancelDonation(donationId: String) {
        viewModelScope.launch {
            donationRepository.cancelDonation(donationId)
                .onSuccess {
                    loadDonations()
                }
                .onFailure { error ->
                    _dashboardState.value = _dashboardState.value.copy(
                        errorMessage = error.message ?: "Failed to cancel donation"
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
     * Set the current donor ID (would be called after authentication)
     */
    fun setDonorId(donorId: String) {
        currentDonorId = donorId
        loadDonations()
    }

    /**
     * Setup real-time listener for donation updates
     * Only subscribes once and filters updates for the current donor
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

                Log.d(TAG, "Setting up real-time listener for donor: $currentDonorId")
                val channel = supabase.channel("donations-changes-${System.currentTimeMillis()}")
                donationsChannel = channel

                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "donations"
                }

                changeFlow.onEach { action ->
                    Log.d(TAG, "✅ Real-time update received: ${action.javaClass.simpleName}")

                    // Only reload if we have a donor ID
                    if (currentDonorId == null) {
                        Log.d(TAG, "⚠️ Skipping update - donor ID not set yet")
                        return@onEach
                    }

                    when (action) {
                        is PostgresAction.Update -> {
                            Log.d(TAG, "📝 Donation UPDATED - reloading for donor: $currentDonorId")
                            loadDonations()
                        }
                        is PostgresAction.Insert -> {
                            Log.d(TAG, "➕ New donation INSERTED - reloading")
                            loadDonations()
                        }
                        is PostgresAction.Delete -> {
                            Log.d(TAG, "🗑️ Donation DELETED - reloading")
                            loadDonations()
                        }
                        else -> {
                            Log.d(TAG, "Other action: $action")
                        }
                    }
                }.launchIn(viewModelScope)

                channel.subscribe()
                Log.d(TAG, "🔔 Successfully subscribed to real-time donation updates")
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
                val userId = currentDonorId ?: return@launch
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
                val channel = supabase.channel("donor-messages-changes-${System.currentTimeMillis()}")
                messagesChannel = channel

                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "messages"
                }

                changeFlow.onEach { action ->
                    Log.d(TAG, "✅ Message real-time update received: ${action.javaClass.simpleName}")

                    if (currentDonorId == null) {
                        Log.d(TAG, "⚠️ Skipping message update - donor ID not set yet")
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
                val channel = supabase.channel("donor-notifications-changes-${System.currentTimeMillis()}")
                notificationsChannel = channel

                val changeFlow = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "notifications"
                }

                changeFlow.onEach { action ->
                    Log.d(TAG, "✅ Notification real-time update received: ${action.javaClass.simpleName}")

                    if (currentDonorId == null) {
                        Log.d(TAG, "⚠️ Skipping notification update - donor ID not set yet")
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
