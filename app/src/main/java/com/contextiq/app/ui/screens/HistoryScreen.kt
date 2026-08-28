package com.contextiq.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.contextiq.app.data.local.ChatDao
import com.contextiq.app.data.local.ChatSessionEntity
import com.contextiq.app.ui.components.pressScale
import com.contextiq.app.ui.theme.ContextIQDesign
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
                        "PAST SESSIONS",
                        letterSpacing = 0.08.em,
                        fontWeight = FontWeight.W900,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                },
                navigationIcon = {
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text(
                            text = "BACK",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.W800,
                                letterSpacing = 0.1.em,
                            ),
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
                modifier = Modifier.border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.onBackground,
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
                EmptyHistoryState()
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(ContextIQDesign.Space.Screen.dp),
                    verticalArrangement = Arrangement.spacedBy(ContextIQDesign.Space.Md.dp),
                ) {
                    item {
                        Text(
                            text = "${sessions.size} INTERROGATIONS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.W800,
                                letterSpacing = 0.13.em,
                            ),
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = ContextIQDesign.Space.Sm.dp),
                        )
                    }
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
private fun EmptyHistoryState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.History,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.outlineVariant,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "NO PAST SESSIONS.",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.W800,
                letterSpacing = 0.05.em,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Upload a paper to start an interrogation.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun SessionCard(session: ChatSessionEntity, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy  •  HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(session.timestamp))

    Box(modifier = Modifier.pressScale(ContextIQDesign.Motion.CardPressScale)) {
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 80.dp),
            shape = RoundedCornerShape(ContextIQDesign.Radius.Card.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                color = MaterialTheme.colorScheme.onBackground,
            ),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.title.uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.W800,
                            letterSpacing = (-0.02).em,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = dateString.uppercase(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.W800,
                            letterSpacing = 0.12.em,
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground,
                )
            }
        }
    }
}
