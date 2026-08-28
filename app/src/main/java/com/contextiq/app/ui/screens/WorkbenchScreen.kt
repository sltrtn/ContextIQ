package com.contextiq.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.contextiq.app.data.local.ChatDao
import com.contextiq.app.data.local.ChatMessageEntity
import com.contextiq.app.data.local.ChatSessionEntity
import com.contextiq.app.domain.UiState
import com.contextiq.app.network.ContextIQClient
import com.contextiq.app.network.dto.QueryRequest
import com.contextiq.app.network.dto.QueryResponse
import com.contextiq.app.network.dto.QuerySourceDto
import com.contextiq.app.ui.components.pressScale
import com.contextiq.app.ui.theme.ContextIQDesign
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

private val panelBorder = 2.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkbenchScreen(navController: NavController, chatDao: ChatDao) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var pdfFile by remember { mutableStateOf<File?>(null) }
    var uploadState by remember { mutableStateOf<UiState<String>>(UiState.Idle) }

    var question by remember { mutableStateOf("What does DPO stand for and what problem does it solve?") }
    var selectedPipeline by remember { mutableStateOf("hybrid_rerank") }

    var queryState by remember { mutableStateOf<UiState<QueryResponse>>(UiState.Idle) }
    var isQuerying by remember { mutableStateOf(false) }

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            scope.launch(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(it)
                    val tempFile = File(context.cacheDir, "workbench_paper.pdf")
                    FileOutputStream(tempFile).use { out -> inputStream?.copyTo(out) }
                    pdfFile = tempFile
                    uploadState = UiState.Loading

                    val requestFile = tempFile.asRequestBody("application/pdf".toMediaTypeOrNull())
                    val part = MultipartBody.Part.createFormData("file", tempFile.name, requestFile)
                    val response = ContextIQClient.api.uploadDocument(part)

                    uploadState = if (response.isSuccessful && response.body() != null) {
                        UiState.Success(response.body()!!.task_id)
                    } else {
                        UiState.Error("Upload failed: ${response.code()}")
                    }
                } catch (e: Exception) {
                    uploadState = UiState.Error("Upload error: ${e.message}")
                }
            }
        }
    }

    suspend fun runQuery(): QueryResponse? {
        return withContext(Dispatchers.IO) {
            try {
                val response = ContextIQClient.api.query(
                    QueryRequest(
                        question = question,
                        config = selectedPipeline,
                        top_k = 5,
                    ),
                )
                response.body()
            } catch (e: Exception) {
                null
            }
        }
    }

    fun ask() {
        if (question.isBlank() || uploadState !is UiState.Success) return
        isQuerying = true
        queryState = UiState.Loading
        scope.launch {
            val result = runQuery()
            if (result != null) {
                queryState = UiState.Success(result)
                // Persist session
                withContext(Dispatchers.IO) {
                    val session = ChatSessionEntity(
                        title = question.take(40),
                        pdfPath = pdfFile?.absolutePath,
                    )
                    val sessionId = chatDao.insertSession(session)
                    chatDao.insertMessage(
                        ChatMessageEntity(
                            sessionId = sessionId,
                            role = "user",
                            content = question,
                            isUser = true,
                        ),
                    )
                    chatDao.insertMessage(
                        ChatMessageEntity(
                            sessionId = sessionId,
                            role = "assistant",
                            content = result.answer,
                            isUser = false,
                        ),
                    )
                }
            } else {
                queryState = UiState.Error("Query failed. Is the backend running on port 8001?")
            }
            isQuerying = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "CONTEXTIQ",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.W900,
                            letterSpacing = (-0.05).em,
                        ),
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = ContextIQDesign.Space.Screen.dp),
            verticalArrangement = Arrangement.spacedBy(ContextIQDesign.Space.Xl.dp),
        ) {
            Spacer(modifier = Modifier.height(ContextIQDesign.Space.Lg.dp))

            // 01 / SOURCE
            WorkbenchPanel(index = "01", title = "SOURCE") {
                SourcePanelContent(
                    pdfFile = pdfFile,
                    uploadState = uploadState,
                    onPick = { pdfPicker.launch("application/pdf") },
                )
            }

            // 02 / INTERROGATE
            WorkbenchPanel(index = "02", title = "INTERROGATE") {
                InterrogatePanelContent(
                    question = question,
                    onQuestionChange = { question = it },
                    isQuerying = isQuerying,
                    canAsk = uploadState is UiState.Success && question.isNotBlank(),
                    onAsk = ::ask,
                )
            }

            // 03 / METHOD
            WorkbenchPanel(index = "03", title = "METHOD") {
                MethodPanelContent(
                    selected = selectedPipeline,
                    onSelect = { selectedPipeline = it },
                )
            }

            // 04 / RESPONSE
            if (queryState is UiState.Success || queryState is UiState.Error || queryState is UiState.Loading) {
                WorkbenchPanel(index = "04", title = "RESPONSE") {
                    ResponsePanelContent(
                        state = queryState,
                    )
                }
            }

            Spacer(modifier = Modifier.height(ContextIQDesign.Space.Xxl.dp))
        }
    }
}

@Composable
private fun WorkbenchPanel(
    index: String,
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(width = panelBorder, color = MaterialTheme.colorScheme.onBackground)
            .padding(ContextIQDesign.Space.Lg.dp),
    ) {
        Text(
            text = "$index / $title",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.W800,
                letterSpacing = 0.13.em,
            ),
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(modifier = Modifier.height(ContextIQDesign.Space.Md.dp))
        content()
    }
}

@Composable
private fun SourcePanelContent(
    pdfFile: File?,
    uploadState: UiState<String>,
    onPick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.onBackground,
                shape = RoundedCornerShape(ContextIQDesign.Radius.Card.dp),
            )
            .background(MaterialTheme.colorScheme.background)
            .pressScale(ContextIQDesign.Motion.CardPressScale),
        contentAlignment = Alignment.Center,
    ) {
        TextButton(onClick = onPick) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (pdfFile != null) pdfFile.name.uppercase() else "DROP THE\nEVIDENCE +",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.W900,
                        letterSpacing = (-0.05).em,
                        lineHeight = 0.9.em,
                    ),
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = when (uploadState) {
                        is UiState.Loading -> "INDEXING..."
                        is UiState.Success -> "INDEXED"
                        is UiState.Error -> (uploadState as UiState.Error).message
                        else -> "PDF / DOCX / TXT"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.W800,
                        letterSpacing = 0.12.em,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun InterrogatePanelContent(
    question: String,
    onQuestionChange: (String) -> Unit,
    isQuerying: Boolean,
    canAsk: Boolean,
    onAsk: () -> Unit,
) {
    Column {
        OutlinedTextField(
            value = question,
            onValueChange = onQuestionChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.W700,
                letterSpacing = (-0.04).em,
                lineHeight = 1.1.em,
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.background,
                unfocusedContainerColor = MaterialTheme.colorScheme.background,
                focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                unfocusedTextColor = MaterialTheme.colorScheme.onBackground,
            ),
            shape = RoundedCornerShape(ContextIQDesign.Radius.Field.dp),
            placeholder = {
                Text(
                    text = "Ask the document...",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
        )

        Spacer(modifier = Modifier.height(ContextIQDesign.Space.Lg.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (isQuerying) "RETRIEVING EVIDENCE..." else "ASK THE DOCUMENT",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.W800,
                    letterSpacing = 0.13.em,
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Box(modifier = Modifier.pressScale(ContextIQDesign.Motion.ButtonPressScale)) {
                Button(
                    onClick = onAsk,
                    enabled = canAsk && !isQuerying,
                    shape = RoundedCornerShape(ContextIQDesign.Radius.Action.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground,
                        contentColor = MaterialTheme.colorScheme.background,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                ) {
                    if (isQuerying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.background,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(
                            text = "ASK",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.W900,
                                letterSpacing = 0.1.em,
                            ),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    }
}

@Composable
private fun MethodPanelContent(
    selected: String,
    onSelect: (String) -> Unit,
) {
    val pipelines = listOf(
        "vector_rerank" to "VECTOR + RERANK" to "P@5 0.9933 · benchmark winner",
        "hybrid" to "HYBRID / RRF" to "MRR 1.0000 · speed / quality",
        "hybrid_rerank" to "FULL PIPELINE" to "Dense + BM25 + RRF + rerank",
    )

    Column(verticalArrangement = Arrangement.spacedBy(ContextIQDesign.Space.Sm.dp)) {
        pipelines.forEachIndexed { index, item ->
            val (id, label) = item.first
            val detail = item.second
            val isSelected = selected == id

            Box(modifier = Modifier.pressScale(ContextIQDesign.Motion.CardPressScale)) {
                Button(
                    onClick = { onSelect(id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(ContextIQDesign.Radius.Action.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.background,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground,
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.onBackground,
                    ),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "0${index + 1}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.W800,
                                ),
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.W800,
                                    letterSpacing = (-0.02).em,
                                ),
                            )
                        }
                        if (isSelected) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                        }
                    }
                }
            }

            if (isSelected) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.W700,
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = ContextIQDesign.Space.Sm.dp),
                )
            }
        }
    }
}

@Composable
private fun ResponsePanelContent(state: UiState<QueryResponse>) {
    when (state) {
        is UiState.Loading -> {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        is UiState.Error -> {
            Text(
                text = state.message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
            )
        }
        is UiState.Success -> {
            val data = state.data
            Column(verticalArrangement = Arrangement.spacedBy(ContextIQDesign.Space.Lg.dp)) {
                // Answer
                SelectionContainer {
                    Text(
                        text = data.answer,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.W800,
                            letterSpacing = (-0.04).em,
                            lineHeight = 1.15.em,
                        ),
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }

                // Metadata
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    MetadataItem(label = "FAITHFULNESS", value = data.faithfulness?.score?.let { "%.2f".format(it) } ?: "—")
                    MetadataItem(label = "SOURCES", value = "${data.sources.size}")
                    MetadataItem(label = "LATENCY", value = data.metadata?.latency_ms?.let { "%.1fs".format(it / 1000) } ?: "—")
                }

                // Sources
                if (data.sources.isNotEmpty()) {
                    Text(
                        text = "EVIDENCE",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.W800,
                            letterSpacing = 0.13.em,
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(ContextIQDesign.Space.Sm.dp)) {
                        data.sources.forEachIndexed { index, source: QuerySourceDto ->
                            SourceCard(index = index + 1, source = source)
                        }
                    }
                }
            }
        }
        else -> {}
    }
}

@Composable
private fun MetadataItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.W800,
                letterSpacing = 0.13.em,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.W800,
                letterSpacing = (-0.03).em,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

@Composable
private fun SourceCard(index: Int, source: QuerySourceDto) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.onBackground,
                shape = RoundedCornerShape(ContextIQDesign.Radius.Card.dp),
            )
            .padding(ContextIQDesign.Space.Lg.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "[%02d]".format(index),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.W800,
                ),
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "P. ${source.page ?: "?"}",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.W800,
                ),
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = source.filename ?: "UNTITLED SOURCE",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.W700,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = source.text.take(200),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
        )
    }
}
