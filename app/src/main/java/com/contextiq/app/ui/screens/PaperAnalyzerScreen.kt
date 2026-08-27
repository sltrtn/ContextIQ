package com.contextiq.app.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.contextiq.app.data.local.ChatMessageEntity
import com.contextiq.app.data.local.ChatSessionEntity
import com.contextiq.app.domain.ChatMessage
import com.contextiq.app.domain.UiState
import com.contextiq.app.network.ContextIQClient
import com.contextiq.app.network.dto.QueryRequest
import com.contextiq.app.network.dto.QueryResponse
import com.contextiq.app.network.dto.QuerySourceDto
import com.contextiq.app.ui.components.ChatSheet
import com.contextiq.app.ui.components.pressScale
import com.contextiq.app.utils.PdfUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaperAnalyzerScreen(
    chatDao: com.contextiq.app.data.local.ChatDao,
    existingSessionId: Long? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pdfFile by remember { mutableStateOf<File?>(null) }
    var pdfBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentPageIndex by remember { mutableIntStateOf(0) }
    var totalPageCount by remember { mutableIntStateOf(0) }

    var isProcessing by remember { mutableStateOf(false) }
    var uploadState by remember { mutableStateOf<UiState<String>>(UiState.Idle) }

    val chatHistory = remember { mutableStateListOf<ChatMessage>() }
    var showChatSheet by remember { mutableStateOf(false) }
    var isAiThinking by remember { mutableStateOf(false) }
    var currentSessionId by remember { mutableStateOf<Long?>(existingSessionId) }

    fun loadPage(pageIndex: Int) {
        if (pdfFile != null && pageIndex in 0 until totalPageCount) {
            scope.launch(Dispatchers.IO) {
                val bitmap = PdfUtils.pdfToBitmap(pdfFile!!, pageIndex)
                launch(Dispatchers.Main) {
                    pdfBitmap = bitmap
                    currentPageIndex = pageIndex
                }
            }
        }
    }

    LaunchedEffect(existingSessionId) {
        if (existingSessionId != null && existingSessionId != -1L) {
            scope.launch(Dispatchers.IO) {
                val session = chatDao.getSession(existingSessionId)
                val pastMessages = chatDao.getMessagesForSession(existingSessionId)
                launch(Dispatchers.Main) {
                    if (session?.pdfPath != null) {
                        val file = File(session.pdfPath)
                        if (file.exists()) {
                            pdfFile = file
                            totalPageCount = PdfUtils.getPageCount(file)
                            loadPage(0)
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                "Original PDF was moved or deleted.",
                                android.widget.Toast.LENGTH_LONG,
                            ).show()
                        }
                    }
                    chatHistory.clear()
                    pastMessages.forEach { msg ->
                        if (msg.role != "system") {
                            chatHistory.add(ChatMessage(msg.role, msg.content, msg.isUser))
                        }
                    }
                    if (chatHistory.isNotEmpty()) showChatSheet = true
                }
            }
        }
    }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val tempFile = File(context.cacheDir, "analyzer_paper.pdf")
                    FileOutputStream(tempFile).use { out -> inputStream?.copyTo(out) }
                    val count = PdfUtils.getPageCount(tempFile)

                    launch(Dispatchers.Main) {
                        pdfFile = tempFile
                        totalPageCount = count
                        loadPage(0)
                        currentSessionId = null
                        chatHistory.clear()
                        showChatSheet = true
                    }

                    uploadPdfToBackend(context, tempFile) { result ->
                        scope.launch(Dispatchers.Main) {
                            uploadState = result
                            when (result) {
                                is UiState.Success -> {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Document indexed: ${result.data}",
                                        android.widget.Toast.LENGTH_SHORT,
                                    ).show()
                                }
                                is UiState.Error -> {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Upload failed: ${result.message}",
                                        android.widget.Toast.LENGTH_LONG,
                                    ).show()
                                }
                                else -> {}
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    suspend fun queryBackend(question: String): QueryResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val response = ContextIQClient.api.query(
                    QueryRequest(
                        question = question,
                        config = "hybrid_rerank",
                        top_k = 5,
                    ),
                )
                if (response.isSuccessful) response.body() else null
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CONTEXTIQ", letterSpacing = 4.sp, fontWeight = FontWeight.W700, color = MaterialTheme.colorScheme.onPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary),
            )
        },
        floatingActionButton = {
            Box(modifier = Modifier.pressScale(0.92f)) {
                FloatingActionButton(
                    onClick = { pdfPickerLauncher.launch("application/pdf") },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Icon(Icons.Default.Add, "Upload PDF", tint = Color.White)
                }
            }
        },
        bottomBar = {
            if (pdfBitmap != null) {
                BottomAppBar(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 0.dp,
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(modifier = Modifier.pressScale(0.92f)) {
                            IconButton(
                                onClick = { loadPage(currentPageIndex - 1) },
                                enabled = currentPageIndex > 0,
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Previous")
                            }
                        }
                        Text(
                            "Page ${currentPageIndex + 1} of $totalPageCount",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.W700,
                        )
                        Box(modifier = Modifier.pressScale(0.92f)) {
                            IconButton(
                                onClick = { loadPage(currentPageIndex + 1) },
                                enabled = currentPageIndex < totalPageCount - 1,
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, "Next")
                            }
                        }
                    }
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (pdfBitmap != null) {
                Image(
                    bitmap = pdfBitmap!!.asImageBitmap(),
                    contentDescription = "PDF Page",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize(),
                )

                if (showChatSheet) {
                    ChatSheet(
                        messages = chatHistory,
                        isThinking = isAiThinking,
                        onDismiss = { showChatSheet = false },
                        onSendMessage = { userText ->
                            isAiThinking = true
                            scope.launch {
                                val currentHistory = chatHistory.toList()
                                chatHistory.add(ChatMessage(role = "user", content = userText, isUser = true))

                                var activeSessionId = currentSessionId
                                if (activeSessionId == null || activeSessionId == -1L) {
                                    val newSession = ChatSessionEntity(
                                        title = "Paper Analysis",
                                        pdfPath = pdfFile?.absolutePath,
                                    )
                                    activeSessionId = chatDao.insertSession(newSession)
                                    currentSessionId = activeSessionId
                                }

                                chatDao.insertMessage(
                                    ChatMessageEntity(
                                        sessionId = activeSessionId!!,
                                        role = "user",
                                        content = userText,
                                        isUser = true,
                                    ),
                                )

                                val result = queryBackend(userText)
                                val answer = result?.answer ?: "No response from backend."
                                val sources = result?.sources?.mapIndexed { index, source: QuerySourceDto ->
                                    "[${index + 1}] ${source.filename ?: "source"} (p.${source.page ?: "?"})"
                                }?.joinToString("\n") ?: ""
                                val faithfulness = result?.faithfulness?.score?.let {
                                    "\n\nFaithfulness: ${String.format("%.2f", it)}"
                                } ?: ""
                                val fullResponse = "$answer\n\nSources:\n$sources$faithfulness"

                                chatDao.insertMessage(
                                    ChatMessageEntity(
                                        sessionId = activeSessionId,
                                        role = "assistant",
                                        content = fullResponse,
                                        isUser = false,
                                    ),
                                )

                                chatHistory.add(ChatMessage(role = "assistant", content = fullResponse, isUser = false))
                                isAiThinking = false
                            }
                        },
                    )
                }

                if (isProcessing) {
                    Box(
                        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.AutoMirrored.Filled.List,
                        contentDescription = null,
                        modifier = Modifier.size(100.dp),
                        tint = MaterialTheme.colorScheme.outlineVariant,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No papers yet",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.Gray,
                    )
                    Text("Tap + to upload and analyze a paper.", color = Color.Gray, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

private fun uploadPdfToBackend(
    context: android.content.Context,
    file: File,
    onResult: (UiState<String>) -> Unit,
) {
    kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
        try {
            val requestFile = file.asRequestBody("application/pdf".toMediaTypeOrNull())
            val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)
            val response = ContextIQClient.api.uploadDocument(filePart)
            if (response.isSuccessful && response.body() != null) {
                onResult(UiState.Success(response.body()!!.task_id))
            } else {
                onResult(UiState.Error("Upload failed: ${response.code()}"))
            }
        } catch (e: Exception) {
            onResult(UiState.Error("Network error: ${e.message}"))
        }
    }
}
