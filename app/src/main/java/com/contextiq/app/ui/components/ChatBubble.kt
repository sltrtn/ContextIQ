package com.contextiq.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import com.contextiq.app.domain.ChatMessage
import com.contextiq.app.ui.theme.ContextIQDesign

@Composable
fun ChatBubble(message: ChatMessage) {
    val isUser = message.isUser
    val backgroundColor = if (isUser) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.background
    val contentColor = if (isUser) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .border(
                    width = 2.dp,
                    color = MaterialTheme.colorScheme.onBackground,
                    shape = RoundedCornerShape(ContextIQDesign.Radius.Card.dp),
                )
                .background(backgroundColor, RoundedCornerShape(ContextIQDesign.Radius.Card.dp))
                .padding(ContextIQDesign.Space.Lg.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
            Text(
                text = if (isUser) "YOU" else "CONTEXTIQ",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.W800,
                    letterSpacing = 0.13.em,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(ContextIQDesign.Space.Sm.dp))
            SelectionContainer {
                Text(
                    text = message.content,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontWeight = FontWeight.W600,
                        lineHeight = 1.35.em,
                    ),
                    color = contentColor,
                )
            }
        }
    }
}
