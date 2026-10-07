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
import com.nexe.pdfforge.ui.screens.HomeScreen
import com.nexe.pdfforge.ui.screens.ImageToPdfScreen
import com.nexe.pdfforge.ui.screens.MergePdfScreen
import com.nexe.pdfforge.ui.screens.MyFilesScreen
import com.nexe.pdfforge.ui.screens.PdfInfoScreen
import com.nexe.pdfforge.ui.screens.PdfToImageScreen
import com.nexe.pdfforge.ui.screens.SettingsScreen
import com.nexe.pdfforge.ui.screens.SplitPdfScreen
import com.nexe.pdfforge.ui.screens.TextConverterScreen
import com.nexe.pdfforge.ui.screens.TextToPdfScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = bottomNavItems.any { it.route == currentRoute }
    val goBack: () -> Unit = { navController.popBackStack() }

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
            composable(Screen.MyFiles.route) { MyFilesScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }

            composable(Screen.TextToPdf.route) { TextToPdfScreen(onBack = goBack) }
            composable(Screen.TextConverter.route) { TextConverterScreen(onBack = goBack) }
            composable(Screen.ImageToPdf.route) { ImageToPdfScreen(onBack = goBack) }
            composable(Screen.MergePdf.route) { MergePdfScreen(onBack = goBack) }
            composable(Screen.SplitPdf.route) { SplitPdfScreen(onBack = goBack) }
            composable(Screen.PdfToImage.route) { PdfToImageScreen(onBack = goBack) }
            composable(Screen.PdfInfo.route) { PdfInfoScreen(onBack = goBack) }
        }
    }
}
