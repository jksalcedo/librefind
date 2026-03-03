package com.jksalcedo.librefind

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.jksalcedo.librefind.ui.navigation.NavGraph
import com.jksalcedo.librefind.ui.navigation.Route
import com.jksalcedo.librefind.ui.theme.LibreFindTheme
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import org.koin.android.ext.android.inject


class MainActivity : ComponentActivity() {

    private val supabase: SupabaseClient by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        supabase.handleDeeplinks(intent)
        setContent {
            LibreFindTheme {
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                val showBottomBar =
                    currentRoute == Route.Dashboard.route || currentRoute == Route.Discover.route

                Scaffold(
                    bottomBar = {
                        if (showBottomBar) {
                            NavigationBar {
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_home),
                                            contentDescription = "Dashboard"
                                        )
                                    },
                                    selected = currentRoute == Route.Dashboard.route,
                                    onClick = {
                                        if (currentRoute != Route.Dashboard.route) {
                                            navController.navigate(Route.Dashboard.route) {
                                                popUpTo(Route.Dashboard.route) { inclusive = false }
                                                launchSingleTop = true
                                            }
                                        }
                                    }
                                )
                                NavigationBarItem(
                                    icon = {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_discover),
                                            contentDescription = "Discover"
                                        )
                                    },
                                    selected = currentRoute == Route.Discover.route,
                                    onClick = {
                                        if (currentRoute != Route.Discover.route) {
                                            navController.navigate(Route.Discover.route) {
                                                popUpTo(Route.Dashboard.route) { inclusive = false }
                                                launchSingleTop = true
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding())) {
                        NavGraph(navController = navController)
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        supabase.handleDeeplinks(intent)
    }

    override fun onResume() {
        super.onResume()
        supabase.handleDeeplinks(intent)
    }
}