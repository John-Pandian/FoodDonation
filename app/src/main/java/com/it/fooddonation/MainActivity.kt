package com.it.fooddonation

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.it.fooddonation.data.remote.SupabaseClient
import com.it.fooddonation.navigation.NavGraph
import com.it.fooddonation.navigation.Screen
import com.it.fooddonation.ui.theme.FoodDonationTheme
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private var shouldNavigateToResetPassword by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if this activity was opened via deep link
        handleDeepLink(intent)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )
        setContent {
            FoodDonationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // Handle deep link navigation
                    LaunchedEffect(shouldNavigateToResetPassword) {
                        if (shouldNavigateToResetPassword) {
                            Log.d(TAG, "Navigating to ResetPassword screen from deep link")
                            navController.navigate(Screen.ResetPassword.route) {
                                // Clear the back stack so user can't go back to splash/login
                                popUpTo(0) { inclusive = true }
                            }
                            shouldNavigateToResetPassword = false
                        }
                    }

                    NavGraph(navController = navController)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data
        Log.d(TAG, "handleDeepLink: data=$data, action=${intent?.action}")

        if (intent?.action == Intent.ACTION_VIEW && data != null) {
            // Check if this is the password reset deep link
            if (data.scheme == "fooddonation" && data.host == "reset-password") {
                Log.d(TAG, "Password reset deep link detected: $data")

                // Supabase sends tokens in the URL fragment (after #), not as query parameters
                // The URL will be like: fooddonation://reset-password#access_token=XXX&refresh_token=YYY&type=recovery
                val fragment = data.fragment
                Log.d(TAG, "URL fragment: $fragment")

                // Parse the fragment to extract tokens
                var accessToken: String? = null
                var refreshToken: String? = null
                var type: String? = null

                fragment?.split("&")?.forEach { param ->
                    val parts = param.split("=")
                    if (parts.size == 2) {
                        when (parts[0]) {
                            "access_token" -> accessToken = parts[1]
                            "refresh_token" -> refreshToken = parts[1]
                            "type" -> type = parts[1]
                        }
                    }
                }

                // Also check query parameters as fallback
                if (accessToken == null) accessToken = data.getQueryParameter("access_token")
                if (refreshToken == null) refreshToken = data.getQueryParameter("refresh_token")
                if (type == null) type = data.getQueryParameter("type")

                Log.d(TAG, "Deep link params - type: $type, accessToken present: ${accessToken != null}, refreshToken present: ${refreshToken != null}")

                if (accessToken != null && refreshToken != null && type == "recovery") {
                    // Handle the deep link with Supabase to establish the recovery session
                    lifecycleScope.launch {
                        try {
                            Log.d(TAG, "Importing auth token for recovery session")

                            // Import the auth token to establish the session
                            SupabaseClient.client.auth.importAuthToken(accessToken)

                            Log.d(TAG, "Recovery session established successfully")
                            Log.d(TAG, "Current user: ${SupabaseClient.client.auth.currentUserOrNull()?.email}")

                            // Now navigate to the reset password screen
                            shouldNavigateToResetPassword = true
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to import auth token from deep link: ${e.message}", e)
                        }
                    }
                } else {
                    Log.e(TAG, "Missing required parameters in deep link. accessToken: ${accessToken != null}, refreshToken: ${refreshToken != null}, type: $type")
                }
            }
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}