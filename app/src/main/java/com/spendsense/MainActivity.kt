package com.spendsense

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.spendsense.ui.budgets.BudgetsScreen
import com.spendsense.ui.dashboard.DashboardScreen
import com.spendsense.ui.debug.DebugScreen
import com.spendsense.ui.history.HistoryScreen
import com.spendsense.ui.settings.SettingsScreen
import com.spendsense.ui.theme.SpendSenseTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // POST_NOTIFICATIONS is a runtime permission on Android 13+ (API 33+).
    // We register the launcher once here and request immediately on start.
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied — no action needed, notifications silently no-op if denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestNotificationPermission()
        enableEdgeToEdge()
        setContent {
            SpendSenseTheme {
                SpendSenseApp()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

sealed class Screen(val route: String, val label: String) {
    object Dashboard : Screen("dashboard", "Dashboard")
    object History : Screen("history", "History")
    object Budgets : Screen("budgets", "Budgets")
    object Settings : Screen("settings", "Settings")
    object Debug : Screen("debug", "Debug")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendSenseApp() {
    val navController = rememberNavController()

    val navItems = buildList {
        add(Screen.Dashboard to Icons.Default.Dashboard)
        add(Screen.History to Icons.Default.History)
        add(Screen.Budgets to Icons.Default.PieChart)
        add(Screen.Settings to Icons.Default.Settings)
        if (BuildConfig.DEBUG) add(Screen.Debug to Icons.Default.BugReport)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SpendSense") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                navItems.forEach { (screen, icon) ->
                    NavigationBarItem(
                        icon = { Icon(icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen() }
            composable(Screen.History.route) { HistoryScreen() }
            composable(Screen.Budgets.route) { BudgetsScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
            composable(Screen.Debug.route) { DebugScreen() }
        }
    }
}
