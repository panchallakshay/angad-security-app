package com.spidey.js.angad.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Home : Screen("home", "Home", Icons.Default.Home)
    object Apps : Screen("apps", "Apps", Icons.Default.List)
    object Threats : Screen("threats", "Threats", Icons.Default.Warning)
    object Blockchain : Screen("blockchain", "Ledger", Icons.Default.AccountBalance)
    object Traffic : Screen("traffic", "Traffic", Icons.Default.Analytics)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Apps,
    Screen.Threats,
    Screen.Blockchain,
    Screen.Settings
)