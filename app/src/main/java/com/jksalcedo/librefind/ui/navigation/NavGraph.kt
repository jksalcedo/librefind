package com.jksalcedo.librefind.ui.navigation

import android.net.Uri
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.jksalcedo.librefind.ui.auth.AuthScreen
import com.jksalcedo.librefind.ui.auth.AuthViewModel
import com.jksalcedo.librefind.ui.auth.ProfileSetupScreen
import com.jksalcedo.librefind.ui.dashboard.DashboardScreen
import org.koin.androidx.compose.koinViewModel
import com.jksalcedo.librefind.ui.details.AlternativeDetailScreen
import com.jksalcedo.librefind.ui.details.DetailsScreen
import com.jksalcedo.librefind.ui.mysubmissions.MySubmissionsScreen
import com.jksalcedo.librefind.ui.reports.MyReportsScreen
import com.jksalcedo.librefind.ui.reports.ReportScreen
import com.jksalcedo.librefind.ui.settings.IgnoredAppsScreen
import com.jksalcedo.librefind.ui.submit.SubmitScreen

private const val NAV_DURATION = 300

@Composable
fun NavGraph(
    navController: NavHostController
) {
    NavHost(
        navController = navController,
        startDestination = Route.Dashboard.route,
        enterTransition = { slideInHorizontally(tween(NAV_DURATION)) { it / 4 } + fadeIn(tween(NAV_DURATION)) },
        exitTransition = { slideOutHorizontally(tween(NAV_DURATION)) { -it / 4 } + fadeOut(tween(NAV_DURATION)) },
        popEnterTransition = { slideInHorizontally(tween(NAV_DURATION)) { -it / 4 } + fadeIn(tween(NAV_DURATION)) },
        popExitTransition = { slideOutHorizontally(tween(NAV_DURATION)) { it / 4 } + fadeOut(tween(NAV_DURATION)) }
    ) {
        composable(Route.Dashboard.route) {
            val authViewModel: AuthViewModel = koinViewModel()
            val authState by authViewModel.uiState.collectAsState()
            
            DashboardScreen(
                onAppClick = { appName, packageName ->
                    navController.navigate(Route.Details.createRoute(appName, packageName))
                },
                onSubmitClick = {
                    if (authState.isSignedIn) {
                        navController.navigate(Route.Submit.createRoute())
                    } else {
                        navController.navigate(Route.Auth.route)
                    }
                },
                onMySubmissionsClick = {
                    navController.navigate(Route.MySubmissions.route)
                },
                onIgnoredAppsClick = {
                    navController.navigate(Route.IgnoredApps.route)
                },
                onSettingsClick = {
                    navController.navigate(Route.Settings.route)
                }
            )
        }

        composable(
            route = Route.Details.route,
            arguments = listOf(
                navArgument("packageName") { type = NavType.StringType },
                navArgument("appName") { type = NavType.StringType })
        ) { backStackEntry ->
            val packageName =
                backStackEntry.arguments?.getString("packageName") ?: return@composable
            val appName = backStackEntry.arguments?.getString("appName") ?: return@composable
            DetailsScreen(
                appName = appName,
                packageName = packageName,
                onBackClick = { navController.navigateUp() },
                onAlternativeClick = { altId ->
                    navController.navigate(Route.AlternativeDetail.createRoute(altId))
                },
                onSuggestAsFoss = { name, pkg ->
                    navController.navigate(Route.Submit.createRoute(name, pkg, "foss"))
                },
                onSuggestAsProprietary = { name, pkg ->
                    navController.navigate(Route.Submit.createRoute(name, pkg, "proprietary"))
                }
            )
        }

        composable(
            route = Route.AlternativeDetail.route,
            arguments = listOf(navArgument("altId") { type = NavType.StringType })
        ) { backStackEntry ->
            val altId = backStackEntry.arguments?.getString("altId")?.let { Uri.decode(it) } ?: return@composable
            AlternativeDetailScreen(
                altId = altId,
                onBackClick = { navController.navigateUp() }
            )
        }

        composable(Route.Auth.route) {
            AuthScreen(
                onAuthSuccess = { needsProfile ->
                    if (needsProfile) {
                        navController.navigate(Route.ProfileSetup.route) {
                            popUpTo(Route.Auth.route) { inclusive = true }
                        }
                    } else {
                        navController.navigate(Route.Submit.createRoute()) {
                            popUpTo(Route.Auth.route) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Route.ProfileSetup.route) {
            ProfileSetupScreen(
                onProfileComplete = {
                    navController.navigate(Route.Submit.createRoute()) {
                        popUpTo(Route.ProfileSetup.route) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Route.Submit.route,
            arguments = listOf(
                navArgument("appName") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("packageName") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("type") { 
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("submissionId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val prefilledAppName = backStackEntry.arguments?.getString("appName")
            val prefilledPackageName = backStackEntry.arguments?.getString("packageName")
            val prefilledType = backStackEntry.arguments?.getString("type")
            val submissionId = backStackEntry.arguments?.getString("submissionId")
            
            SubmitScreen(
                onBackClick = { navController.navigateUp() },
                onSuccess = {
                    navController.navigate(Route.Dashboard.route) {
                        popUpTo(Route.Dashboard.route) { inclusive = true }
                    }
                },
                onNavigateToTargetSubmission = { appName, packageName ->
                    navController.navigate(Route.Submit.createRoute(appName, packageName)) {
                        popUpTo(Route.Submit.route) { inclusive = true }
                    }
                },
                prefilledAppName = prefilledAppName,
                prefilledPackageName = prefilledPackageName,
                prefilledType = prefilledType,
                submissionId = submissionId
            )
        }

        composable(Route.MySubmissions.route) {
            MySubmissionsScreen(
                onBackClick = { navController.navigateUp() },
                onSubmissionClick = { submissionId ->
                    navController.navigate(Route.Submit.createRoute(submissionId = submissionId))
                }
            )
        }

        composable(Route.IgnoredApps.route) {
            IgnoredAppsScreen(
                onBackClick = { navController.navigateUp() }
            )
        }

        composable(Route.Settings.route) {
            val authViewModel: AuthViewModel = koinViewModel()
            val authState by authViewModel.uiState.collectAsState()

            com.jksalcedo.librefind.ui.settings.SettingsScreen(
                onBackClick = { navController.navigateUp() },
                onReportClick = {
                    if (authState.isSignedIn) {
                        navController.navigate(Route.Report.route)
                    } else {
                        navController.navigate(Route.Auth.route)
                    }
                },
                onMyReportsClick = {
                    if (authState.isSignedIn) {
                        navController.navigate(Route.MyReports.route)
                    } else {
                        navController.navigate(Route.Auth.route)
                    }
                },
                onPrivacyPolicyClick = {
                    navController.navigate(Route.PrivacyPolicy.route)
                }
            )
        }

        composable(Route.PrivacyPolicy.route) {
            com.jksalcedo.librefind.ui.settings.PrivacyPolicyScreen(
                onBackClick = { navController.navigateUp() }
            )
        }

        composable(Route.Report.route) {
            ReportScreen(
                onBackClick = { navController.navigateUp() },
                onSuccess = {
                    navController.navigate(Route.Dashboard.route) {
                        popUpTo(Route.Dashboard.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Route.MyReports.route) {
            MyReportsScreen(
                onBackClick = { navController.navigateUp() }
            )
        }
    }
}


