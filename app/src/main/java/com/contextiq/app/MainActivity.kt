package com.contextiq.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.contextiq.app.data.local.AppDatabase
import com.contextiq.app.ui.screens.*
import com.contextiq.app.ui.theme.ContextIQTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = AppDatabase.getDatabase(applicationContext)
        val chatDao = database.chatDao()

        setContent {
            ContextIQTheme(darkTheme = true) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "landing") {

                        composable("landing") {
                            LandingScreen(navController = navController)
                        }

                        composable("workbench") {
                            WorkbenchScreen(navController = navController, chatDao = chatDao)
                        }

                        composable("history") {
                            HistoryScreen(navController = navController, chatDao = chatDao)
                        }

                        composable("chat_detail/{sessionId}") { backStackEntry ->
                            val sessionIdString = backStackEntry.arguments?.getString("sessionId")
                            val sessionId = sessionIdString?.toLongOrNull() ?: 0L
                            ChatDetailScreen(
                                navController = navController,
                                chatDao = chatDao,
                                sessionId = sessionId,
                            )
                        }
                    }
                }
            }
        }
    }
}
