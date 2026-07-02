package com.contextiq.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.contextiq.app.data.local.ChatDao
import com.contextiq.app.data.local.ChatMessageEntity
import com.contextiq.app.domain.ChatMessage
import com.contextiq.app.ui.components.ChatBubble
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(navController: NavController, chatDao: ChatDao, sessionId: Long) {
    var messages by remember { mutableStateOf<List<ChatMessageEntity>>(emptyList()) }

    LaunchedEffect(sessionId) {
        val pastMessages = withContext(Dispatchers.IO) {
            chatDao.getMessagesForSession(sessionId)
        }
        messages = pastMessages
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "ANALYSIS RECORD",
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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("analyze_paper?sessionId=$sessionId") },
                icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Continue") },
                text = { Text("Continue Chat", fontWeight = androidx.compose.ui.text.font.FontWeight.W700) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(messages.filter { it.role != "system" }) { msg ->
                ChatBubble(
                    message = ChatMessage(
                        role = msg.role,
                        content = msg.content,
                        isUser = msg.isUser,
                    ),
                )
            }
        }
    }
}
