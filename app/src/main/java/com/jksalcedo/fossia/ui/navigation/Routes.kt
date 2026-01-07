package com.jksalcedo.fossia.ui.navigation

/**
 * Navigation routes for Fossia app
 */
sealed class Route(val route: String) {
    data object Dashboard : Route("dashboard")
    data object Details : Route("details/{packageName}") {
        fun createRoute(packageName: String) = "details/$packageName"
    }
}
