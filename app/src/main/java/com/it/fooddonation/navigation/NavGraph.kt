package com.it.fooddonation.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.it.fooddonation.data.model.UserRole
import com.it.fooddonation.ui.auth.ForgotPasswordScreen
import com.it.fooddonation.ui.auth.LoginScreen
import com.it.fooddonation.ui.auth.RegisterScreen
import com.it.fooddonation.ui.auth.ResetPasswordScreen
import com.it.fooddonation.ui.auth.viewmodel.AuthViewModel
import com.it.fooddonation.ui.chat.ChatScreen
import com.it.fooddonation.ui.donor.DonateFoodScreen
import com.it.fooddonation.ui.donor.DonorDashboardScreen
import com.it.fooddonation.ui.home.HomeScreen
import com.it.fooddonation.ui.messages.MessagesListScreen
import com.it.fooddonation.ui.profile.ProfileScreen
import com.it.fooddonation.ui.receiver.ReceiverDashboardScreen
import com.it.fooddonation.ui.splash.SplashScreen

private const val TAG = "NavGraph"

@Composable
fun NavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel()
) {
    val authState by authViewModel.authState.collectAsState()

    Log.d(TAG, "NavGraph: Composing with authState - isCheckingAuth=${authState.isCheckingAuth}, isAuthenticated=${authState.isAuthenticated}")

    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
    ) {
        composable(Screen.Splash.route) {
            Log.d(TAG, "Splash Screen: Composing")
            SplashScreen(
                onCheckComplete = {
                    // Navigation happens after auth check is complete
                }
            )

            // Navigate based on auth state after checking
            LaunchedEffect(authState.isCheckingAuth, authState.isAuthenticated, authState.userProfile) {
                Log.d(TAG, "Splash LaunchedEffect: isCheckingAuth=${authState.isCheckingAuth}, isAuthenticated=${authState.isAuthenticated}, role=${authState.userProfile?.role}")
                if (!authState.isCheckingAuth) {
                    val destination = if (authState.isAuthenticated) {
                        // Route based on user role
                        when (authState.userProfile?.role?.lowercase()) {
                            "donor" -> Screen.DonorDashboard.route
                            "receiver" -> Screen.ReceiverDashboard.route
                            else -> Screen.DonorDashboard.route // Default to donor if role unclear
                        }
                    } else {
                        Screen.Login.route
                    }

                    Log.d(TAG, "Splash LaunchedEffect: Navigating to $destination")
                    navController.navigate(destination) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            }
        }

        composable(Screen.Login.route) {
            Log.d(TAG, "Login Screen: Composing")
            LoginScreen(
                onLoginClick = { email, password ->
                    authViewModel.signIn(email, password)
                },
                onRegisterClick = {
                    navController.navigate(Screen.Register.route)
                },
                onForgotPasswordClick = {
                    navController.navigate(Screen.ForgotPassword.route)
                },
                isLoading = authState.isLoading,
                errorMessage = authState.errorMessage
            )

            LaunchedEffect(authState.isAuthenticated, authState.userProfile) {
                Log.d(TAG, "Login LaunchedEffect: isAuthenticated=${authState.isAuthenticated}, isLoading=${authState.isLoading}, role=${authState.userProfile?.role}")
                if (authState.isAuthenticated && !authState.isLoading) {
                    val destination = when (authState.userProfile?.role?.lowercase()) {
                        "donor" -> Screen.DonorDashboard.route
                        "receiver" -> Screen.ReceiverDashboard.route
                        else -> Screen.DonorDashboard.route
                    }
                    Log.d(TAG, "Login LaunchedEffect: Navigating to $destination")
                    navController.navigate(destination) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            }
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterClick = { email, password, fullName, phoneNumber, role ->
                    authViewModel.signUp(email, password, fullName, phoneNumber, role)
                },
                onLoginClick = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                },
                isLoading = authState.isLoading,
                errorMessage = authState.errorMessage
            )

            LaunchedEffect(authState.isAuthenticated, authState.userProfile) {
                if (authState.isAuthenticated && !authState.isLoading) {
                    val destination = when (authState.userProfile?.role?.lowercase()) {
                        "donor" -> Screen.DonorDashboard.route
                        "receiver" -> Screen.ReceiverDashboard.route
                        else -> Screen.DonorDashboard.route
                    }
                    navController.navigate(destination) {
                        popUpTo(Screen.Register.route) { inclusive = true }
                    }
                }
            }
        }

        composable(Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onResetPassword = { email, onSuccess, onError ->
                    authViewModel.resetPassword(email, onSuccess, onError)
                },
                isLoading = authState.isLoading
            )
        }

        composable(Screen.ResetPassword.route) {
            ResetPasswordScreen(
                onUpdatePassword = { newPassword, onSuccess, onError ->
                    authViewModel.updatePassword(newPassword, onSuccess, onError)
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                isLoading = authState.isLoading
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onLogout = {
                    authViewModel.signOut()
                },
                onDonorDashboardClick = {
                    navController.navigate(Screen.DonorDashboard.route)
                }
            )

            // Navigate to login when user logs out
            LaunchedEffect(authState.isAuthenticated) {
                Log.d(TAG, "Home LaunchedEffect: isAuthenticated=${authState.isAuthenticated}")
                if (!authState.isAuthenticated && !authState.isCheckingAuth) {
                    Log.d(TAG, "Home LaunchedEffect: User logged out, navigating to Login")
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }

        composable(Screen.DonorDashboard.route) {
            DonorDashboardScreen(
                authViewModel = authViewModel,
                onDonateFoodClick = {
                    navController.navigate(Screen.DonateFood.route)
                },
                onProfileClick = {
                    navController.navigate(Screen.Profile.route)
                },
                onMessagesClick = {
                    navController.navigate(Screen.Messages.route)
                },
                onChatClick = { receiverId, receiverName ->
                    navController.navigate(
                        Screen.Chat.createRoute(receiverId, receiverName)
                    )
                }
            )

            // Navigate to login if user logs out
            LaunchedEffect(authState.isAuthenticated) {
                Log.d(TAG, "DonorDashboard LaunchedEffect: isAuthenticated=${authState.isAuthenticated}")
                if (!authState.isAuthenticated && !authState.isCheckingAuth) {
                    Log.d(TAG, "DonorDashboard LaunchedEffect: User logged out, navigating to Login")
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }

        composable(Screen.ReceiverDashboard.route) {
            ReceiverDashboardScreen(
                authViewModel = authViewModel,
                onProfileClick = {
                    navController.navigate(Screen.Profile.route)
                },
                onMessagesClick = {
                    navController.navigate(Screen.Messages.route)
                },
                onChatClick = { donorId, donorName ->
                    navController.navigate(
                        Screen.Chat.createRoute(donorId, donorName)
                    )
                }
            )

            // Navigate to login if user logs out
            LaunchedEffect(authState.isAuthenticated) {
                Log.d(TAG, "ReceiverDashboard LaunchedEffect: isAuthenticated=${authState.isAuthenticated}")
                if (!authState.isAuthenticated && !authState.isCheckingAuth) {
                    Log.d(TAG, "ReceiverDashboard LaunchedEffect: User logged out, navigating to Login")
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }

        composable(Screen.DonateFood.route) {
            DonateFoodScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onProfileClick = {
                    navController.navigate(Screen.Profile.route)
                },
                authViewModel = authViewModel
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                authViewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    authViewModel.signOut()
                }
            )

            // Navigate to login when user logs out
            LaunchedEffect(authState.isAuthenticated) {
                Log.d(TAG, "Profile LaunchedEffect: isAuthenticated=${authState.isAuthenticated}")
                if (!authState.isAuthenticated && !authState.isCheckingAuth) {
                    Log.d(TAG, "Profile LaunchedEffect: User logged out, navigating to Login")
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            }
        }

        composable(Screen.Messages.route) {
            MessagesListScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onConversationClick = { otherUserId, otherUserName ->
                    navController.navigate(
                        Screen.Chat.createRoute(otherUserId, otherUserName)
                    )
                }
            )
        }

        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("otherUserId") { type = NavType.StringType },
                navArgument("otherUserName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val otherUserId = backStackEntry.arguments?.getString("otherUserId") ?: ""
            val otherUserName = backStackEntry.arguments?.getString("otherUserName") ?: ""

            ChatScreen(
                otherUserId = otherUserId,
                otherUserName = otherUserName,
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }
    }
}
