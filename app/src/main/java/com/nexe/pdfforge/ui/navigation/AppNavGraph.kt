package com.nexe.pdfforge.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nexe.pdfforge.ui.screens.ComingSoonScreen
import com.nexe.pdfforge.ui.screens.HomeScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(Screen.Home.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(onToolClick = { tool -> navController.navigate(tool.route) })
            }
            composable(Screen.MyFiles.route) {
                ComingSoonScreen(
                    title = Screen.MyFiles.title,
                    subtitle = Screen.MyFiles.subtitle,
                    onBack = null
                )
            }
            composable(Screen.Settings.route) {
                ComingSoonScreen(
                    title = Screen.Settings.title,
                    subtitle = Screen.Settings.subtitle,
                    onBack = null
                )
            }
            toolSections.flatMap { it.second }.forEach { tool ->
                composable(tool.route) {
                    ComingSoonScreen(
                        title = tool.title,
                        subtitle = tool.subtitle,
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
