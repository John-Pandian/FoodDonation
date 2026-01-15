package com.it.fooddonation.ui.auth.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.it.fooddonation.data.model.UserRole
import com.it.fooddonation.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val errorMessage: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    init {
        checkAuthStatus()
    }

    private fun checkAuthStatus() {
        _authState.value = _authState.value.copy(
            isAuthenticated = authRepository.isUserLoggedIn()
        )
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = _authState.value.copy(isLoading = true, errorMessage = null)

            val result = authRepository.signIn(email, password)

            result.onSuccess {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    isAuthenticated = true,
                    errorMessage = null
                )
            }.onFailure { error ->
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    isAuthenticated = false,
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
            _authState.value = _authState.value.copy(isLoading = true, errorMessage = null)

            val result = authRepository.signUp(email, password, fullName, phoneNumber, role)

            result.onSuccess {
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    isAuthenticated = true,
                    errorMessage = null
                )
            }.onFailure { error ->
                _authState.value = _authState.value.copy(
                    isLoading = false,
                    isAuthenticated = false,
                    errorMessage = error.message ?: "Registration failed"
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
            _authState.value = _authState.value.copy(isAuthenticated = false)
        }
    }

    fun clearError() {
        _authState.value = _authState.value.copy(errorMessage = null)
    }
}
