package com.it.fooddonation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.it.fooddonation.ui.auth.LoginScreen
import com.it.fooddonation.ui.auth.RegisterScreen
import com.it.fooddonation.ui.auth.viewmodel.AuthViewModel
import com.it.fooddonation.ui.home.HomeScreen

@Composable
fun NavGraph(
    navController: NavHostController,
    authViewModel: AuthViewModel = viewModel()
) {
    val authState by authViewModel.authState.collectAsState()

    val startDestination = if (authState.isAuthenticated) {
        Screen.Home.route
    } else {
        Screen.Login.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginClick = { email, password ->
                    authViewModel.signIn(email, password)
                },
                onRegisterClick = {
                    navController.navigate(Screen.Register.route)
                },
                isLoading = authState.isLoading,
                errorMessage = authState.errorMessage
            )

            if (authState.isAuthenticated) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegisterClick = { email, password, fullName, phoneNumber, role ->
                    authViewModel.signUp(email, password, fullName, phoneNumber, role)
                },
                onLoginClick = {
                    navController.popBackStack()
                },
                isLoading = authState.isLoading,
                errorMessage = authState.errorMessage
            )

            if (authState.isAuthenticated) {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Register.route) { inclusive = true }
                }
            }
        }

        composable(Screen.Home.route) {
            HomeScreen(
                onLogout = {
                    authViewModel.signOut()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                }
            )
        }
    }
}
