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
import androidx.compose.material.icons.filled.LibraryBooks
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
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LitReviewerScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    var selectedUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var pdfFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var state by remember { mutableStateOf<UiState<String>>(UiState.Idle) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris: List<Uri> ->
        selectedUris = uris.take(3)
        pdfFiles = uris.take(3).mapNotNull { uri ->
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val tempFile = File(context.cacheDir, "lit_review_${uri.hashCode()}.pdf")
                FileOutputStream(tempFile).use { out -> inputStream?.copyTo(out) }
                tempFile
            } catch (e: Exception) { null }
        }
    }

    fun runLiteratureReview() {
        if (pdfFiles.isEmpty()) return
        state = UiState.Loading

        scope.launch(Dispatchers.IO) {
            try {
                val parts = pdfFiles.map { file ->
                    val requestFile = file.asRequestBody("application/pdf".toMediaTypeOrNull())
                    MultipartBody.Part.createFormData("files", file.name, requestFile)
                }
                val response = ContextIQClient.api.literatureReview(parts)
                if (response.isSuccessful && response.body() != null) {
                    state = UiState.Success(response.body()!!.review)
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
                title = { Text("LITERATURE REVIEWER", letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onPrimary) },
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
                "Select up to 3 related research papers. AI will read all of them and write a synthesized literature review.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { pdfPickerLauncher.launch(arrayOf("application/pdf")) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = state !is UiState.Loading,
                shape = RoundedCornerShape(20.dp),
            ) {
                Icon(Icons.Default.LibraryBooks, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SELECT PDFS (MAX 3)", fontWeight = FontWeight.W700)
            }

            if (selectedUris.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "${selectedUris.size} PDF(s) selected.",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { runLiteratureReview() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = state !is UiState.Loading && pdfFiles.isNotEmpty(),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            ) {
                if (state is UiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onSecondary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("SYNTHESIZING...", color = MaterialTheme.colorScheme.onSecondary, fontWeight = FontWeight.W700)
                } else {
                    Text("GENERATE LITERATURE REVIEW", fontWeight = FontWeight.W700, letterSpacing = 1.sp)
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
                                Text("Synthesized Review", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.W700, color = MaterialTheme.colorScheme.primary)
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
