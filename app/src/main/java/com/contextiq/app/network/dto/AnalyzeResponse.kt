package com.contextiq.app.network.dto

data class AnalyzeImageResponse(
    val analysis: String,
    val session_id: String? = null,
)

data class LiteratureReviewResponse(
    val review: String,
    val sources: List<SourceDto> = emptyList(),
)

data class SourceDto(
    val title: String,
    val doi: String? = null,
)

data class AbstractResponse(
    val problem: String,
    val method: String,
    val result: String,
)

data class ClaimVerifyResponse(
    val verified_text: String,
    val bibliography: List<BibliographyEntryDto> = emptyList(),
    val status: String,
)

data class BibliographyEntryDto(
    val title: String,
    val url: String,
    val relevance: String,
)

data class JournalRecommendationDto(
    val name: String,
    val publisher: String,
    val metrics: String,
    val justification: String,
)

data class JournalMatchResponse(
    val recommendations: List<JournalRecommendationDto>,
)

data class RelatedPapersResponse(
    val papers: List<PaperDto>,
)

data class PaperDto(
    val title: String,
    val authors: String,
    val year: String,
    val abstract: String,
    val url: String,
    val doi: String? = null,
)

data class OpenAccessResponse(
    val title: String,
    val journal: String,
    val is_open_access: Boolean,
    val pdf_url: String? = null,
    val landing_url: String? = null,
)

data class CitationResponse(
    val citation: String,
    val doi: String,
)

data class PaperReviewResponse(
    val review: String,
    val strengths: List<String> = emptyList(),
    val weaknesses: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
)

data class LatexResponse(
    val latex_code: String,
)

data class RebuttalResponse(
    val rebuttal: String,
)

data class ErrorResponse(
    val detail: String,
)
