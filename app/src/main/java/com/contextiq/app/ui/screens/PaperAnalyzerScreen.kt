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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.contextiq.app.data.local.ChatMessageEntity
import com.contextiq.app.data.local.ChatSessionEntity
import com.contextiq.app.domain.ChatMessage
import com.contextiq.app.domain.UiState
import com.contextiq.app.ui.components.ChatSheet
import com.contextiq.app.ui.components.RegionSelectionDialog
import com.contextiq.app.ui.components.SelectionOverlay
import com.contextiq.app.ui.components.pressScale
import com.contextiq.app.utils.PdfUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.ByteArrayOutputStream
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

    var showDialog by remember { mutableStateOf(false) }
    var currentCroppedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageViewSize by remember { mutableStateOf(androidx.compose.ui.geometry.Size.Zero) }

    var isProcessing by remember { mutableStateOf(false) }
    var selectionResetKey by remember { mutableIntStateOf(0) }

    val chatHistory = remember { mutableStateListOf<ChatMessage>() }
    var showChatSheet by remember { mutableStateOf(false) }
    var isAiThinking by remember { mutableStateOf(false) }
    var currentSessionId by remember { mutableStateOf<Long?>(existingSessionId) }
    var chatState by remember { mutableStateOf<UiState<String>>(UiState.Idle) }

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
                            android.widget.Toast.makeText(context, "Original PDF was moved or deleted.", android.widget.Toast.LENGTH_LONG).show()
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
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    suspend fun analyzeImageWithBackend(bitmap: Bitmap, type: String): String {
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            try {
                val outputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
                val tempFile = File(context.cacheDir, "crop_${System.currentTimeMillis()}.jpg")
                FileOutputStream(tempFile).use { it.write(outputStream.toByteArray()) }

                val requestFile = tempFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", tempFile.name, requestFile)
                val typePart = okhttp3.RequestBody.Companion.create("text/plain".toMediaTypeOrNull(), type)

                val response = com.contextiq.app.network.ContextIQClient.api.analyzeImage(filePart, typePart)
                if (response.isSuccessful && response.body() != null) {
                    response.body()!!.analysis
                } else {
                    "API Error: ${response.code()}"
                }
            } catch (e: Exception) {
                "Network Error: ${e.message}"
            }
        }
    }

    suspend fun continueChatWithBackend(
        history: List<ChatMessage>,
        newMessage: String,
    ): String {
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            try {
                val chatHistory = history.map {
                    com.contextiq.app.network.dto.ChatMessageDto(
                        role = if (it.isUser) "user" else "model",
                        content = it.content,
                    )
                }
                val response = com.contextiq.app.network.ContextIQClient.api.chatStream(
                    com.contextiq.app.network.dto.ChatRequest(
                        message = newMessage,
                        history = chatHistory,
                        session_id = currentSessionId?.toString(),
                    ),
                )
                if (response.isSuccessful && response.body() != null) {
                    response.body()!!.string()
                } else {
                    "API Error: ${response.code()}"
                }
            } catch (e: Exception) {
                "Network Error: ${e.message}"
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
                    modifier = Modifier
                        .fillMaxSize()
                        .onGloballyPositioned { coordinates ->
                            imageViewSize = coordinates.size.toSize()
                        },
                )

                key(selectionResetKey) {
                    SelectionOverlay(
                        modifier = Modifier.fillMaxSize(),
                        onSelectionFinished = { rect: Rect ->
                            if (imageViewSize.width > 0 && imageViewSize.height > 0) {
                                val cropped = PdfUtils.cropBitmap(
                                    original = pdfBitmap!!,
                                    cropRect = rect,
                                    viewWidth = imageViewSize.width,
                                    viewHeight = imageViewSize.height,
                                )
                                if (cropped != null) {
                                    currentCroppedBitmap = cropped
                                    showDialog = true
                                }
                            }
                        },
                    )
                }

                if (showDialog && currentCroppedBitmap != null) {
                    RegionSelectionDialog(
                        croppedBitmap = currentCroppedBitmap!!,
                        onDismiss = {
                            showDialog = false
                            selectionResetKey++
                        },
                        onConfirm = { type ->
                            showDialog = false
                            selectionResetKey++
                            isProcessing = true

                            scope.launch {
                                val aiResponse = analyzeImageWithBackend(currentCroppedBitmap!!, type)

                                var activeSessionId = currentSessionId
                                if (activeSessionId == null || activeSessionId == -1L) {
                                    val newSession = ChatSessionEntity(
                                        title = "Analysis: $type",
                                        pdfPath = pdfFile?.absolutePath,
                                    )
                                    activeSessionId = chatDao.insertSession(newSession)
                                    currentSessionId = activeSessionId
                                }

                                val userPrompt = "[Image Cropped] Please analyze this $type."
                                chatDao.insertMessage(ChatMessageEntity(sessionId = activeSessionId!!, role = "user", content = userPrompt, isUser = true))
                                chatDao.insertMessage(ChatMessageEntity(sessionId = activeSessionId!!, role = "assistant", content = aiResponse, isUser = false))

                                chatHistory.add(ChatMessage(role = "user", content = userPrompt, isUser = true))
                                chatHistory.add(ChatMessage(role = "assistant", content = aiResponse, isUser = false))

                                showChatSheet = true
                                isProcessing = false
                            }
                        },
                    )
                }

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

                                currentSessionId?.let { id ->
                                    chatDao.insertMessage(ChatMessageEntity(sessionId = id, role = "user", content = userText, isUser = true))
                                }

                                val aiResponse = continueChatWithBackend(currentHistory, userText)

                                currentSessionId?.let { id ->
                                    chatDao.insertMessage(ChatMessageEntity(sessionId = id, role = "assistant", content = aiResponse, isUser = false))
                                }

                                chatHistory.add(ChatMessage(role = "assistant", content = aiResponse, isUser = false))
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
                    Text("Tap + to analyze a paper.", color = Color.Gray, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
