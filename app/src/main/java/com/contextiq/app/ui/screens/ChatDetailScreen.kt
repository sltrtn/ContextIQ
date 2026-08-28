package com.contextiq.app.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.contextiq.app.data.local.ChatDao
import com.contextiq.app.data.local.ChatMessageEntity
import com.contextiq.app.domain.ChatMessage
import com.contextiq.app.ui.components.ChatBubble
import com.contextiq.app.ui.theme.ContextIQDesign
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
                        "SESSION RECORD",
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
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("workbench") },
                icon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null) },
                text = {
                    Text(
                        "NEW INTERROGATION",
                        fontWeight = FontWeight.W900,
                        letterSpacing = 0.05.em,
                    )
                },
                containerColor = MaterialTheme.colorScheme.onBackground,
                contentColor = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(ContextIQDesign.Radius.Action.dp),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = ContextIQDesign.Space.Screen.dp),
            contentPadding = PaddingValues(vertical = ContextIQDesign.Space.Screen.dp),
            verticalArrangement = Arrangement.spacedBy(ContextIQDesign.Space.Lg.dp),
        ) {
            item {
                Text(
                    text = "${messages.filter { it.isUser }.size} QUESTIONS  •  ${messages.filter { !it.isUser }.size} RESPONSES",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.W800,
                        letterSpacing = 0.13.em,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
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
