package com.jan.moneybear.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object AddEdit : Screen("addEdit") {
        const val TRANSACTION_ID_ARG = "transactionId"
        fun route(transactionId: String? = null): String =
            if (transactionId.isNullOrBlank()) route else "$route?$TRANSACTION_ID_ARG=$transactionId"
    }
    object Settings : Screen("settings")
    object Paywall : Screen("paywall")
}




















