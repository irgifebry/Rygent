package com.rygent.monitor.ui.navigation

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Dashboard : Screen("dashboard")
    object Detail : Screen("detail/{deviceId}") {
        fun createRoute(deviceId: String) = "detail/$deviceId"
    }
    object AddDevice : Screen("add_device?code={code}") {
        fun createRoute(code: String? = null) = "add_device?code=${java.net.URLEncoder.encode(code ?: "", "UTF-8")}"
    }
    object Settings : Screen("settings")
}
