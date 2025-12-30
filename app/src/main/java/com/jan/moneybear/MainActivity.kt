package com.jan.moneybear

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.jan.moneybear.ui.navigation.Screen
import com.jan.moneybear.ui.screen.AddEditTransactionScreen
import com.jan.moneybear.ui.screen.HomeScreen
import com.jan.moneybear.ui.screen.LoginScreen
import com.jan.moneybear.ui.screen.PaywallScreen
import com.jan.moneybear.ui.screen.SettingsScreen
import com.jan.moneybear.ui.theme.MoneyBearTheme
import com.jan.moneybear.util.LocaleUtils
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val app = application as MoneyBearApp
        val authRepository = app.authRepository
        val settingsStore = app.settingsStore
        val initialLanguage = runBlocking { settingsStore.languageCode.first() }
        LocaleUtils.applyAppLanguage(initialLanguage)
        
        // Determine start destination based on auth state
        val startDestination = if (authRepository.currentUser != null) {
            Screen.Home.route
        } else {
            Screen.Login.route
        }
        
        setContent {
            val languageCode by settingsStore.languageCode.collectAsState(initial = initialLanguage)
            LaunchedEffect(languageCode) {
                LocaleUtils.applyAppLanguage(languageCode)
            }
            val themeMode by settingsStore.themeMode.collectAsState(initial = "green")
            val accentId by settingsStore.accentColor.collectAsState(initial = "teal")
            MoneyBearTheme(themeMode = themeMode, accentId = accentId) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MoneyBearNavHost(startDestination = startDestination)
                }
            }
        }
    }
}

@Composable
fun MoneyBearNavHost(
    startDestination: String,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Home.route) {
            HomeScreen(
                onAddTransaction = {
                    navController.navigate(Screen.AddEdit.route())
                },
                onSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onEditTransaction = { transactionId ->
                    navController.navigate(Screen.AddEdit.route(transactionId))
                }
            )
        }
        
        composable(
            route = "${Screen.AddEdit.route}?${Screen.AddEdit.TRANSACTION_ID_ARG}={${Screen.AddEdit.TRANSACTION_ID_ARG}}",
            arguments = listOf(
                navArgument(Screen.AddEdit.TRANSACTION_ID_ARG) {
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val transactionId = backStackEntry.arguments?.getString(Screen.AddEdit.TRANSACTION_ID_ARG)
            AddEditTransactionScreen(
                transactionId = transactionId,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
        
        composable(Screen.Paywall.route) {
            PaywallScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
