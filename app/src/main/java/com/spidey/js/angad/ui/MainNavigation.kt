package com.spidey.js.angad.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.spidey.js.angad.ui.screens.*
import com.spidey.js.angad.ui.theme.*

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    
    Scaffold(
        containerColor = DeepEarth, // Matches the theme background to remove black gaps
        bottomBar = {
            NavigationBar(
                containerColor = DeepEarth,
                contentColor = RoyalGold,
                windowInsets = WindowInsets.navigationBars // Ensures it spans correctly
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                bottomNavItems.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.title, style = MaterialTheme.typography.labelSmall) },
                        selected = selected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = DeepEarth,
                            selectedTextColor = RoyalGold,
                            unselectedIconColor = MutedGold,
                            unselectedTextColor = TextMuted,
                            indicatorColor = RoyalGold
                        ),
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
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
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { HomeScreen() }
            composable(Screen.Apps.route) { AppControlScreen() }
            composable(Screen.Threats.route) { ThreatLogScreen() }
            composable(Screen.Traffic.route) { TrafficMonitorScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}