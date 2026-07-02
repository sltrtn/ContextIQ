package com.contextiq.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.contextiq.app.data.local.ChatDao
import com.contextiq.app.data.local.ChatSessionEntity
import com.contextiq.app.ui.components.pressScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController, chatDao: ChatDao) {
    val scope = rememberCoroutineScope()
    var sessions by remember { mutableStateOf<List<ChatSessionEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            val pastSessions = chatDao.getAllSessions()
            launch(Dispatchers.Main) {
                sessions = pastSessions
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "PAST ANALYSES",
                        letterSpacing = 2.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary,
                )
            } else if (sessions.isEmpty()) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No past analyses found.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(sessions) { session ->
                        SessionCard(
                            session = session,
                            onClick = {
                                navController.navigate("chat_detail/${session.sessionId}")
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SessionCard(session: ChatSessionEntity, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy  •  hh:mm a", Locale.getDefault())
    val dateString = dateFormat.format(Date(session.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(0.96f),
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = session.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.W700,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dateString,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
