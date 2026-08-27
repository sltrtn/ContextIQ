package com.contextiq.app.network.dto

// Backend-aligned DTOs for the ContextIQ FastAPI API.
// See backend/app/models/ for the source of truth.

data class UploadResponse(
    val task_id: String,
    val filename: String,
    val status: String,
)

data class QueryRequest(
    val question: String,
    val config: String = "hybrid_rerank",
    val top_k: Int = 5,
    val expand: Boolean = false,
)

data class QuerySourceDto(
    val text: String,
    val score: Double? = null,
    val chunk_id: String? = null,
    val filename: String? = null,
    val page: Int? = null,
)

data class FaithfulnessCheckDto(
    val score: Double,
    val supported_claims: Int,
    val total_claims: Int,
    val unsupported_claims: List<String> = emptyList(),
)

data class QueryMetadataDto(
    val config: String,
    val latency_ms: Double,
    val model: String,
    val num_queries: Int,
    val num_sources: Int,
)

data class QueryResponse(
    val answer: String,
    val sources: List<QuerySourceDto> = emptyList(),
    val metadata: QueryMetadataDto? = null,
    val faithfulness: FaithfulnessCheckDto? = null,
)

data class DocumentStatusResponse(
    val task_id: String,
    val status: String,
    val doc_id: String,
)
