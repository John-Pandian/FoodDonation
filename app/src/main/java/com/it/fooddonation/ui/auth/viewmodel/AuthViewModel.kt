package com.it.fooddonation.ui.auth.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.it.fooddonation.data.model.UserProfile
import com.it.fooddonation.data.model.UserRole
import com.it.fooddonation.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val isCheckingAuth: Boolean = true,
    val userProfile: UserProfile? = null,
    val errorMessage: String? = null,
    val isEmailConfirmed: Boolean = true,
    val showEmailVerificationPrompt: Boolean = false
)

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        Log.d(TAG, "AuthViewModel initialized - starting auth check")
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            Log.d(TAG, "checkAuthStatus: Starting auth check")
            _authState.value = _authState.value.copy(isCheckingAuth = true)

            // Wait for session to load from storage and check if user is logged in
            val isLoggedIn = authRepository.waitForSessionAndCheck()
            Log.d(TAG, "checkAuthStatus: waitForSessionAndCheck = $isLoggedIn")

            // Load user profile if logged in
            val userProfile = if (isLoggedIn) {
                Log.d(TAG, "checkAuthStatus: User is logged in, fetching profile")
                val profile = authRepository.getCurrentUser()
                Log.d(TAG, "checkAuthStatus: Profile fetched = ${profile?.fullName} (${profile?.email})")
                profile
            } else {
                Log.d(TAG, "checkAuthStatus: User is not logged in")
                null
            }

            _authState.value = _authState.value.copy(
                isAuthenticated = isLoggedIn,
                userProfile = userProfile,
                isCheckingAuth = false
            )
            Log.d(TAG, "checkAuthStatus: Auth check completed - isAuthenticated=$isLoggedIn, isCheckingAuth=false")
        }
    }

    companion object {
        private const val TAG = "AuthViewModel"
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            Log.d(TAG, "signIn: Attempting to sign in user: $email")
            _authState.value = _authState.value.copy(isLoading = true, errorMessage = null)

            val result = authRepository.signIn(email, password)

            result.onSuccess {
                Log.d(TAG, "signIn: Sign in successful, fetching profile")
                val userProfile = authRepository.getCurrentUser()
                Log.d(TAG, "signIn: Profile fetched = ${userProfile?.fullName}")
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    isAuthenticated = true,
                    userProfile = userProfile,
                    errorMessage = null
                )
                Log.d(TAG, "signIn: State updated - isAuthenticated=true")
            }.onFailure { error ->
                Log.e(TAG, "signIn: Sign in failed - ${error.message}", error)
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    isAuthenticated = false,
                    userProfile = null,
                    errorMessage = error.message ?: "Login failed"
                )
            }
        }
    }

    fun signUp(
        email: String,
        password: String,
        fullName: String,
        phoneNumber: String,
        role: UserRole
    ) {
        viewModelScope.launch {
            Log.d(TAG, "signUp: Attempting to register user: $email")
            _authState.value = _authState.value.copy(isLoading = true, errorMessage = null)

            val result = authRepository.signUp(email, password, fullName, phoneNumber, role)

            result.onSuccess {
                Log.d(TAG, "signUp: Registration successful, fetching profile")
                val userProfile = authRepository.getCurrentUser()
                Log.d(TAG, "signUp: Profile fetched = ${userProfile?.fullName}")
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    isAuthenticated = true,
                    userProfile = userProfile,
                    errorMessage = null
                )
                Log.d(TAG, "signUp: State updated - isAuthenticated=true")
            }.onFailure { error ->
                Log.e(TAG, "signUp: Registration failed - ${error.message}", error)
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    isAuthenticated = false,
                    userProfile = null,
                    errorMessage = error.message ?: "Registration failed"
                )
                Log.d(TAG, "signUp: State updated - isAuthenticated=false, error shown")
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            Log.d(TAG, "signOut: Signing out user")
            authRepository.signOut()
            _authState.value = _authState.value.copy(
                isAuthenticated = false,
                userProfile = null
            )
            Log.d(TAG, "signOut: User signed out, state updated")
        }
    }

    fun clearError() {
        _authState.value = _authState.value.copy(errorMessage = null)
    }

    fun checkEmailVerification() {
        viewModelScope.launch {
            val isConfirmed = authRepository.isEmailConfirmed()
            Log.d(TAG, "checkEmailVerification: isConfirmed = $isConfirmed")

            _authState.value = _authState.value.copy(
                isEmailConfirmed = isConfirmed,
                showEmailVerificationPrompt = !isConfirmed
            )
        }
    }

    fun dismissEmailVerificationPrompt() {
        _authState.value = _authState.value.copy(showEmailVerificationPrompt = false)
    }

    fun resetPassword(email: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            Log.d(TAG, "resetPassword: Sending password reset email to: $email")
            _authState.value = _authState.value.copy(isLoading = true, errorMessage = null)

            val result = authRepository.resetPassword(email)

            result.onSuccess {
                Log.d(TAG, "resetPassword: Password reset email sent successfully")
                _authState.value = _authState.value.copy(isLoading = false)
                onSuccess()
            }.onFailure { error ->
                Log.e(TAG, "resetPassword: Failed - ${error.message}", error)
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Failed to send password reset email"
                )
                onError(error.message ?: "Failed to send password reset email")
            }
        }
    }

    fun updatePassword(newPassword: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            Log.d(TAG, "updatePassword: Updating password")
            _authState.value = _authState.value.copy(isLoading = true, errorMessage = null)

            val result = authRepository.updatePassword(newPassword)

            result.onSuccess {
                Log.d(TAG, "updatePassword: Password updated successfully")
                _authState.value = _authState.value.copy(isLoading = false)
                onSuccess()
            }.onFailure { error ->
                Log.e(TAG, "updatePassword: Failed - ${error.message}", error)
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    errorMessage = error.message ?: "Failed to update password"
                )
                onError(error.message ?: "Failed to update password")
            }
        }
    }


    fun updateProfile(
        fullName: String,
        phoneNumber: String,
        address: String,
        city: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            Log.d(TAG, "updateProfile: Updating profile")

            val result = authRepository.updateProfile(fullName, phoneNumber, address, city)

            result.onSuccess {
                Log.d(TAG, "updateProfile: Update successful, fetching updated profile")
                // Fetch updated profile
                val updatedProfile = authRepository.getCurrentUser()
                _authState.value = _authState.value.copy(
                    userProfile = updatedProfile
                )
                onSuccess()
            }.onFailure { error ->
                Log.e(TAG, "updateProfile: Update failed", error)
                onError(error.message ?: "Failed to update profile")
            }
        }
    }
}
