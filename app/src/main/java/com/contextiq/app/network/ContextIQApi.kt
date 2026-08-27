package com.contextiq.app.network

import com.contextiq.app.network.dto.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface ContextIQApi {

    /**
     * Upload a PDF/DOCX/TXT document. Ingests synchronously and returns a task_id.
     */
    @Multipart
    @POST("api/v1/documents/upload")
    suspend fun uploadDocument(
        @Part file: MultipartBody.Part,
    ): Response<UploadResponse>

    /**
     * Ask a question against the uploaded document(s).
     */
    @POST("api/v1/query")
    suspend fun query(
        @Body request: QueryRequest,
    ): Response<QueryResponse>

    /**
     * Ask a question and receive an SSE stream.
     */
    @Streaming
    @POST("api/v1/query/stream")
    suspend fun queryStream(
        @Body request: QueryRequest,
    ): Response<ResponseBody>

    /**
     * Health check. Returns status, version, and active model name.
     */
    @GET("api/v1/health")
    suspend fun health(): Response<Map<String, String>>
}
