package com.contextiq.app.network

import com.contextiq.app.network.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ContextIQApi {

    @Multipart
    @POST("analyze/image")
    suspend fun analyzeImage(
        @Part file: MultipartBody.Part,
        @Part("type") type: RequestBody,
    ): Response<AnalyzeImageResponse>

    @Multipart
    @POST("analyze/literature-review")
    suspend fun literatureReview(
        @Part files: List<MultipartBody.Part>,
    ): Response<LiteratureReviewResponse>

    @POST("analyze/abstract")
    suspend fun analyzeAbstract(
        @Body request: AbstractRequest,
    ): Response<AbstractResponse>

    @POST("analyze/claim-verify")
    suspend fun claimVerify(
        @Body request: ClaimVerifyRequest,
    ): Response<ClaimVerifyResponse>

    @Multipart
    @POST("analyze/journal-match")
    suspend fun journalMatch(
        @Part file: MultipartBody.Part,
    ): Response<JournalMatchResponse>

    @Multipart
    @POST("analyze/paper-review")
    suspend fun paperReview(
        @Part file: MultipartBody.Part,
    ): Response<PaperReviewResponse>

    @Multipart
    @POST("analyze/latex")
    suspend fun latexGenerate(
        @Part file: MultipartBody.Part,
    ): Response<LatexResponse>

    @Multipart
    @POST("analyze/rebuttal")
    suspend fun rebuttalDraft(
        @Part file: MultipartBody.Part,
        @Part("reviewer_comments") reviewerComments: RequestBody,
    ): Response<RebuttalResponse>

    @POST("chat/stream")
    suspend fun chatStream(
        @Body request: ChatRequest,
    ): Response<okhttp3.ResponseBody>

    @POST("tools/citation")
    suspend fun citation(
        @Body request: CitationRequest,
    ): Response<CitationResponse>

    @POST("tools/related-papers")
    suspend fun relatedPapers(
        @Body request: RelatedPapersRequest,
    ): Response<RelatedPapersResponse>

    @POST("tools/open-access")
    suspend fun openAccess(
        @Body request: OpenAccessRequest,
    ): Response<OpenAccessResponse>
}
