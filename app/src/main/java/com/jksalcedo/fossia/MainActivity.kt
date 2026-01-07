package com.jksalcedo.fossia

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.jksalcedo.fossia.ui.navigation.NavGraph
import com.jksalcedo.fossia.ui.theme.FossiaTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main activity - entry point for Fossia app
 * 
 * Annotated with @AndroidEntryPoint to enable Hilt dependency injection
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FossiaTheme {
                val navController = rememberNavController()
                NavGraph(navController = navController)
            }
        }
    }
}