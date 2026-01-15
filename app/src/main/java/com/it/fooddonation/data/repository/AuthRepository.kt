package com.it.fooddonation.data.repository

import android.util.Log
import com.it.fooddonation.data.model.UserProfile
import com.it.fooddonation.data.model.UserRole
import com.it.fooddonation.data.remote.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from

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
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }

            val currentSession = supabase.auth.currentSessionOrNull()
            Log.d("AuthRepository", "Current session: $currentSession")

            if (currentSession == null) {
                throw Exception("Registration successful! Please check your email to confirm your account, then login.")
            }

            val userId = currentSession.user?.id ?: throw Exception("User ID not found after signup")

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
                throw Exception("Failed to create profile: ${e.message}")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepository", "Signup failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
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

    fun isUserLoggedIn(): Boolean {
        return supabase.auth.currentUserOrNull() != null
    }
}
