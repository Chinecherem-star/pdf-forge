package com.nexe.pdfforge.ui.navigation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nexe.pdfforge.data.remote.RemoteConfig
import com.nexe.pdfforge.ui.components.FloatingNavBar
import com.nexe.pdfforge.ui.screens.HomeScreen
import com.nexe.pdfforge.ui.screens.ImageToPdfScreen
import com.nexe.pdfforge.ui.screens.LockPdfScreen
import com.nexe.pdfforge.ui.screens.MergePdfScreen
import com.nexe.pdfforge.ui.screens.MyFilesScreen
import com.nexe.pdfforge.ui.screens.PdfEditorScreen
import com.nexe.pdfforge.ui.screens.PdfInfoScreen
import com.nexe.pdfforge.ui.screens.PdfToImageScreen
import com.nexe.pdfforge.ui.screens.SettingsScreen
import com.nexe.pdfforge.ui.screens.SplitPdfScreen
import com.nexe.pdfforge.ui.screens.TextConverterScreen
import com.nexe.pdfforge.ui.screens.TextToPdfScreen
import com.nexe.pdfforge.ui.screens.UnavailableScreen
import com.nexe.pdfforge.ui.screens.WordEditorScreen
import com.nexe.pdfforge.util.FileHandoff

@Composable
fun AppNavGraph(
    config: RemoteConfig,
    onToolOpened: (String) -> Unit
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = bottomNavItems.any { it.route == currentRoute }
    val goBack: () -> Unit = { navController.popBackStack() }
    val toolRoutes = remember { toolSections.flatMap { it.second }.map { it.route }.toSet() }

    // Anonymous usage count: which tool was opened.
    LaunchedEffect(currentRoute) {
        val route = currentRoute
        if (route != null && route in toolRoutes && route !in config.disabledTools) {
            onToolOpened(route)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                FloatingNavBar(
                    items = bottomNavItems,
                    currentRoute = currentRoute,
                    onSelect = { item ->
                        navController.navigate(item.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(320)) +
                    slideInHorizontally(
                        animationSpec = tween(380, easing = FastOutSlowInEasing),
                        initialOffsetX = { it / 10 }
                    )
            },
            exitTransition = { fadeOut(animationSpec = tween(200)) },
            popEnterTransition = { fadeIn(animationSpec = tween(320)) },
            popExitTransition = {
                fadeOut(animationSpec = tween(220)) +
                    slideOutHorizontally(
                        animationSpec = tween(300, easing = FastOutSlowInEasing),
                        targetOffsetX = { it / 10 }
                    )
            }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    config = config,
                    onToolClick = { tool -> navController.navigate(tool.route) }
                )
            }
            composable(Screen.MyFiles.route) {
                MyFilesScreen(
                    onOpenWith = { route, file ->
                        FileHandoff.put(route, file)
                        navController.navigate(route)
                    }
                )
            }
            composable(Screen.Settings.route) { SettingsScreen() }

            composable(Screen.TextToPdf.route) {
                ToolGate(Screen.TextToPdf, config, goBack) { TextToPdfScreen(onBack = goBack) }
            }
            composable(Screen.TextConverter.route) {
                ToolGate(Screen.TextConverter, config, goBack) { TextConverterScreen(onBack = goBack) }
            }
            composable(Screen.ImageToPdf.route) {
                ToolGate(Screen.ImageToPdf, config, goBack) { ImageToPdfScreen(onBack = goBack) }
            }
            composable(Screen.PdfEditor.route) {
                ToolGate(Screen.PdfEditor, config, goBack) { PdfEditorScreen(onBack = goBack) }
            }
            composable(Screen.WordEditor.route) {
                ToolGate(Screen.WordEditor, config, goBack) { WordEditorScreen(onBack = goBack) }
            }
            composable(Screen.LockPdf.route) {
                ToolGate(Screen.LockPdf, config, goBack) { LockPdfScreen(onBack = goBack) }
            }
            composable(Screen.MergePdf.route) {
                ToolGate(Screen.MergePdf, config, goBack) { MergePdfScreen(onBack = goBack) }
            }
            composable(Screen.SplitPdf.route) {
                ToolGate(Screen.SplitPdf, config, goBack) { SplitPdfScreen(onBack = goBack) }
            }
            composable(Screen.PdfToImage.route) {
                ToolGate(Screen.PdfToImage, config, goBack) { PdfToImageScreen(onBack = goBack) }
            }
            composable(Screen.PdfInfo.route) {
                ToolGate(Screen.PdfInfo, config, goBack) { PdfInfoScreen(onBack = goBack) }
            }
        }
    }
}

/** Shows the tool, or an "unavailable" screen if the admin switched it off. */
@Composable
private fun ToolGate(
    screen: Screen,
    config: RemoteConfig,
    onBack: () -> Unit,
    content: @Composable () -> Unit
) {
    if (screen.route in config.disabledTools) {
        UnavailableScreen(title = screen.title, onBack = onBack)
    } else {
        content()
    }
}
