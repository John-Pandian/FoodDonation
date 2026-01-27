package com.it.fooddonation.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object ForgotPassword : Screen("forgot_password")
    object ResetPassword : Screen("reset_password")
    object Home : Screen("home")
    object DonorDashboard : Screen("donor_dashboard")
    object ReceiverDashboard : Screen("receiver_dashboard")
    object DonateFood : Screen("donate_food")
    object Profile : Screen("profile")
    object Messages : Screen("messages")
    object Chat : Screen("chat/{otherUserId}/{otherUserName}") {
        fun createRoute(otherUserId: String, otherUserName: String) =
            "chat/$otherUserId/$otherUserName"
    }
}
