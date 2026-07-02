package com.contextiq.app.domain

data class ChatMessage(
    val role: String,
    val content: String,
    val isUser: Boolean,
)
