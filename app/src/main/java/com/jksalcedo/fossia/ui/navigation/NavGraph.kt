package com.jksalcedo.fossia.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.jksalcedo.fossia.ui.dashboard.DashboardScreen
import com.jksalcedo.fossia.ui.details.DetailsScreen

/**
 * Navigation graph for Fossia app
 */
@Composable
fun NavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Route.Dashboard.route
    ) {
        composable(Route.Dashboard.route) {
            DashboardScreen(
                onAppClick = { packageName ->
                    navController.navigate(Route.Details.createRoute(packageName))
                }
            )
        }

        composable(
            route = Route.Details.route,
            arguments = listOf(
                navArgument("packageName") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val packageName = backStackEntry.arguments?.getString("packageName") ?: return@composable
            DetailsScreen(
                packageName = packageName,
                onBackClick = { navController.navigateUp() }
            )
        }
    }
}
