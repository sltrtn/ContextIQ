package com.contextiq.app.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.contextiq.app.domain.UiState
import com.contextiq.app.network.ContextIQClient
import com.contextiq.app.ui.components.pressScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewRebuttalScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var pdfFile by remember { mutableStateOf<File?>(null) }
    var reviewerComments by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<UiState<String>>(UiState.Idle) }
    var loadingStatus by remember { mutableStateOf("") }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val tempFile = File(context.cacheDir, "rebuttal_manuscript.pdf")
                FileOutputStream(tempFile).use { out -> inputStream?.copyTo(out) }
                selectedUri = it
                pdfFile = tempFile
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun generateRebuttal() {
        if (pdfFile == null || reviewerComments.isBlank()) return
        state = UiState.Loading
        loadingStatus = "Scanning manuscript..."

        scope.launch(Dispatchers.IO) {
            try {
                val file = pdfFile!!
                val requestFile = file.asRequestBody("application/pdf".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)
                val commentsPart = reviewerComments.toRequestBody("text/plain".toMediaTypeOrNull())

                val response = ContextIQClient.api.rebuttalDraft(filePart, commentsPart)
                if (response.isSuccessful && response.body() != null) {
                    state = UiState.Success(response.body()!!.rebuttal)
                } else {
                    state = UiState.Error("API Error: ${response.code()}")
                }
            } catch (e: Exception) {
                state = UiState.Error("Network error: ${e.message}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("REBUTTAL DRAFTER", letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onPrimary) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Upload your manuscript and paste the reviewer's critiques. AI will draft professional rebuttals.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { pdfPickerLauncher.launch("application/pdf") },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = state !is UiState.Loading,
                shape = RoundedCornerShape(20.dp),
            ) {
                Icon(Icons.Default.UploadFile, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (selectedUri != null) "MANUSCRIPT SELECTED (TAP TO CHANGE)" else "UPLOAD MANUSCRIPT (PDF)",
                    fontWeight = FontWeight.W700,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = reviewerComments,
                onValueChange = { reviewerComments = it },
                label = { Text("Paste Reviewer Comments here...") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                maxLines = 10,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { generateRebuttal() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = state !is UiState.Loading && selectedUri != null && reviewerComments.isNotBlank(),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            ) {
                if (state is UiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onSecondary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(loadingStatus, color = MaterialTheme.colorScheme.onSecondary)
                } else {
                    Icon(Icons.Default.RateReview, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("DRAFT RESPONSE", fontWeight = FontWeight.W700, letterSpacing = 1.sp)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            when (val s = state) {
                is UiState.Error -> {
                    Text(s.message, color = MaterialTheme.colorScheme.error)
                }
                is UiState.Success -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text("Rebuttal Draft", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.W700, color = MaterialTheme.colorScheme.primary)
                                Box(modifier = Modifier.pressScale(0.92f)) {
                                    IconButton(onClick = { clipboardManager.setText(AnnotatedString(s.data)) }) {
                                        Icon(Icons.Default.ContentCopy, "Copy", modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            SelectionContainer {
                                Text(s.data, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}
