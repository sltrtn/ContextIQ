package com.contextiq.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
            ContextIQTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "home") {

                        composable("home") {
                            HomeScreen(navController = navController)
                        }

                        composable(
                            route = "analyze_paper?sessionId={sessionId}",
                            arguments = listOf(navArgument("sessionId") {
                                defaultValue = -1L
                                type = NavType.LongType
                            }),
                        ) { backStackEntry ->
                            val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: -1L
                            val actualSessionId = if (sessionId == -1L) null else sessionId
                            PaperAnalyzerScreen(
                                chatDao = chatDao,
                                existingSessionId = actualSessionId,
                            )
                        }

                        composable("history") {
                            HistoryScreen(navController = navController, chatDao = chatDao)
                        }

                        composable("citation_generator") {
                            CitationScreen(navController = navController)
                        }

                        composable("open_access") {
                            OpenAccessScreen(navController = navController)
                        }

                        composable("abstract_summary") {
                            AbstractSummaryScreen(navController = navController)
                        }

                        composable("paper_reviewer") {
                            PaperReviewerScreen(navController = navController)
                        }

                        composable("latex_generator") {
                            LatexGeneratorScreen(navController = navController)
                        }

                        composable("lit_reviewer") {
                            LitReviewerScreen(navController = navController)
                        }

                        composable("claim_verifier") {
                            ClaimVerifierScreen(navController = navController)
                        }

                        composable("journal_matcher") {
                            JournalMatcherScreen(navController = navController)
                        }

                        composable("rebuttal_drafter") {
                            ReviewRebuttalScreen(navController = navController)
                        }

                        composable("related_papers") {
                            RelatedPapersScreen(navController = navController)
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
