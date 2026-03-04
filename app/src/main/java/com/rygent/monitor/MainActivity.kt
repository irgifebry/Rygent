package com.rygent.monitor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import com.rygent.monitor.ui.navigation.Screen
import com.rygent.monitor.ui.screens.adddevice.AddDeviceScreen
import com.rygent.monitor.ui.screens.detail.DetailScreen
import com.rygent.monitor.ui.screens.main.MainScreen
import com.rygent.monitor.ui.theme.SystemMonitorTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContent {
            com.rygent.monitor.worker.WorkScheduler.schedulePolling(this)
            
            SystemMonitorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

private val slideAnimSpec = tween<androidx.compose.ui.unit.IntOffset>(
    durationMillis = 400,
    easing = FastOutSlowInEasing
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController, 
        startDestination = Screen.Splash.route,
        enterTransition = {
            slideInHorizontally(animationSpec = slideAnimSpec) { fullWidth -> fullWidth }
        },
        exitTransition = {
            slideOutHorizontally(animationSpec = slideAnimSpec) { fullWidth -> -fullWidth }
        },
        popEnterTransition = {
            slideInHorizontally(animationSpec = slideAnimSpec) { fullWidth -> -fullWidth }
        },
        popExitTransition = {
            slideOutHorizontally(animationSpec = slideAnimSpec) { fullWidth -> fullWidth }
        }
    ) {
        
        composable(Screen.Splash.route) {
            com.rygent.monitor.ui.screens.splash.SplashScreen(
                onSplashFinished = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            MainScreen(
                onAddDeviceClick = { code -> navController.navigate(Screen.AddDevice.createRoute(code)) },
                onDeviceClick = { deviceId -> navController.navigate(Screen.Detail.createRoute(deviceId)) }
            )
        }

        composable(
            route = Screen.AddDevice.route,
            arguments = listOf(navArgument("code") { 
                type = NavType.StringType 
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val code = backStackEntry.arguments?.getString("code")
            AddDeviceScreen(
                code = code,
                onBackClick = { navController.popBackStack() },
                onSaveClick = { deviceId -> 
                    navController.popBackStack()
                    navController.navigate(Screen.Detail.createRoute(deviceId))
                }
            )
        }

        composable(
            route = Screen.Detail.route,
            arguments = listOf(navArgument("deviceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
            DetailScreen(
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            com.rygent.monitor.ui.screens.settings.SettingsScreen(
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
