package com.contextiq.app.network.dto

data class AnalyzeImageRequest(
    val type: String,
    val session_id: String? = null,
)

data class AbstractRequest(
    val abstract: String,
)

data class ClaimVerifyRequest(
    val claim: String,
)

data class ChatRequest(
    val session_id: String? = null,
    val message: String,
    val history: List<ChatMessageDto> = emptyList(),
)

data class ChatMessageDto(
    val role: String,
    val content: String,
)

data class CitationRequest(
    val doi: String,
    val style: String = "apa",
)

data class RelatedPapersRequest(
    val query: String,
    val limit: Int = 5,
)

data class OpenAccessRequest(
    val doi: String,
)

data class RebuttalRequest(
    val reviewer_comments: String,
)
