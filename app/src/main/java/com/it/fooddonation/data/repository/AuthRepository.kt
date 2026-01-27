package com.it.fooddonation.data.repository

import android.util.Log
import com.it.fooddonation.data.model.UserProfile
import com.it.fooddonation.data.model.UserRole
import com.it.fooddonation.data.remote.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull

class AuthRepository {
    private val supabase = SupabaseClient.client

    suspend fun signUp(
        email: String,
        password: String,
        fullName: String,
        phoneNumber: String,
        role: UserRole
    ): Result<Unit> {
        return try {
            Log.d("AuthRepository", "Starting signup for: $email")

            // This will throw an exception if rate limited or other errors occur
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }

            val currentSession = supabase.auth.currentSessionOrNull()
            val currentUser = supabase.auth.currentUserOrNull()
            Log.d("AuthRepository", "After signup - session: $currentSession")
            Log.d("AuthRepository", "After signup - user: ${currentUser?.email}, emailConfirmedAt: ${currentUser?.emailConfirmedAt}")

            // If no session but user exists, email confirmation is required
            if (currentSession == null && currentUser != null) {
                Log.d("AuthRepository", "Email confirmation required")
                return Result.failure(Exception("Registration successful! Please check your email ($email) to confirm your account before logging in."))
            }

            // If no session and no user, something went wrong
            if (currentSession == null) {
                Log.d("AuthRepository", "No session or user after signup")
                return Result.failure(Exception("Registration successful! Please check your email to confirm your account, then login."))
            }

            val userId = currentSession.user?.id ?: run {
                Log.e("AuthRepository", "User ID not found after signup")
                return Result.failure(Exception("User ID not found after signup"))
            }

            Log.d("AuthRepository", "Inserting profile for user: $userId")

            try {
                supabase.from("profiles").insert(
                    mapOf(
                        "id" to userId,
                        "email" to email,
                        "full_name" to fullName,
                        "phone_number" to phoneNumber,
                        "role" to role.name.lowercase()
                    )
                )
                Log.d("AuthRepository", "Profile inserted successfully")
            } catch (e: Exception) {
                Log.e("AuthRepository", "Profile insertion failed: ${e.message}", e)
                // Sign out to clean up the auth state since profile creation failed
                try {
                    supabase.auth.signOut()
                } catch (signOutError: Exception) {
                    Log.e("AuthRepository", "Failed to sign out after profile error", signOutError)
                }
                return Result.failure(Exception("Failed to create profile: ${e.message}"))
            }

            Log.d("AuthRepository", "Signup completed successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Signup failed: ${e.message}", e)

            // Parse the error message to provide better feedback
            val errorMessage = when {
                e.message?.contains("rate", ignoreCase = true) == true ->
                    "Too many registration attempts. Please wait a few minutes and try again."
                e.message?.contains("already registered", ignoreCase = true) == true ||
                e.message?.contains("already exists", ignoreCase = true) == true ->
                    "This email is already registered. Please try logging in instead."
                else -> e.message ?: "Registration failed. Please try again."
            }

            Result.failure(Exception(errorMessage))
        }
    }

    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }

            // Check if email is confirmed
            val currentUser = supabase.auth.currentUserOrNull()
            val emailConfirmedAt = currentUser?.emailConfirmedAt

            Log.d("AuthRepository", "User email confirmed at: $emailConfirmedAt")

            // Note: We allow login even if email is not confirmed
            // The dashboard will show a verification prompt

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Sign in failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signOut(): Result<Unit> {
        return try {
            supabase.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isEmailConfirmed(): Boolean {
        return try {
            val currentUser = supabase.auth.currentUserOrNull()
            val emailConfirmedAt = currentUser?.emailConfirmedAt
            Log.d("AuthRepository", "Email confirmed check: $emailConfirmedAt")
            emailConfirmedAt != null
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to check email confirmation: ${e.message}", e)
            false
        }
    }

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            // Send password reset email
            // The redirect URL is configured in Supabase Dashboard → Authentication → URL Configuration
            // Set it to: fooddonation://reset-password
            supabase.auth.resetPasswordForEmail(email)
            Log.d("AuthRepository", "Password reset email sent to: $email")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to send password reset email: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun updatePassword(newPassword: String): Result<Unit> {
        return try {
            // The user must be authenticated with the recovery token from the deep link
            supabase.auth.updateUser {
                password = newPassword
            }
            Log.d("AuthRepository", "Password updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Failed to update password: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getCurrentUser(): UserProfile? {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id ?: return null
            val response = supabase.from("profiles")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingle<UserProfile>()
            response
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Wait for session to load from storage and check if user is logged in
     * This is a suspend function because we need to wait for the session to be loaded
     */
    suspend fun waitForSessionAndCheck(): Boolean {
        Log.d(TAG, "waitForSessionAndCheck: Waiting for session status...")

        // Wait for the session status to be initialized
        val sessionStatus = withTimeoutOrNull(5000) {
            supabase.auth.sessionStatus.first { status ->
                Log.d(TAG, "waitForSessionAndCheck: Current status = $status")
                // Wait until we get a definitive status (not loading)
                status is SessionStatus.Authenticated || status is SessionStatus.NotAuthenticated
            }
        }

        Log.d(TAG, "waitForSessionAndCheck: Final status = $sessionStatus")

        // Give it a tiny bit more time for the session to be fully loaded
        delay(100)

        val session = supabase.auth.currentSessionOrNull()
        val user = supabase.auth.currentUserOrNull()
        Log.d(TAG, "waitForSessionAndCheck: session=$session, user=$user")

        val isLoggedIn = session != null
        Log.d(TAG, "waitForSessionAndCheck: Result = $isLoggedIn")
        return isLoggedIn
    }

    fun isUserLoggedIn(): Boolean {
        val session = supabase.auth.currentSessionOrNull()
        val user = supabase.auth.currentUserOrNull()
        Log.d(TAG, "isUserLoggedIn: Checking session - session=$session, user=$user")
        val isLoggedIn = session != null
        Log.d(TAG, "isUserLoggedIn: Result = $isLoggedIn")
        return isLoggedIn
    }

    fun getSessionStatus(): Flow<SessionStatus> {
        return supabase.auth.sessionStatus
    }

    /**
     * Update user profile information
     */
    suspend fun updateProfile(
        fullName: String,
        phoneNumber: String,
        address: String,
        city: String
    ): Result<Unit> {
        return try {
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("User not authenticated"))

            Log.d(TAG, "updateProfile: Updating profile for user: $userId")

            supabase.from("profiles")
                .update(
                    mapOf(
                        "full_name" to fullName,
                        "phone_number" to phoneNumber,
                        "address" to address,
                        "city" to city
                    )
                ) {
                    filter {
                        eq("id", userId)
                    }
                }

            Log.d(TAG, "updateProfile: Profile updated successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "updateProfile: Update failed", e)
            Result.failure(e)
        }
    }

    companion object {
        private const val TAG = "AuthRepository"
    }
}
