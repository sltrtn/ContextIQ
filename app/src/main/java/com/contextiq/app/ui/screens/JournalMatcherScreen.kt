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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.contextiq.app.domain.UiState
import com.contextiq.app.network.ContextIQClient
import com.contextiq.app.network.dto.JournalRecommendationDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalMatcherScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    var pdfFile by remember { mutableStateOf<File?>(null) }
    var state by remember { mutableStateOf<UiState<List<JournalRecommendationDto>>>(UiState.Idle) }

    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val tempFile = File(context.cacheDir, "journal_match.pdf")
                FileOutputStream(tempFile).use { out -> inputStream?.copyTo(out) }
                pdfFile = tempFile
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun findJournals() {
        val file = pdfFile ?: return
        state = UiState.Loading

        scope.launch(Dispatchers.IO) {
            try {
                val requestFile = file.asRequestBody("application/pdf".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", file.name, requestFile)
                val response = ContextIQClient.api.journalMatch(part)

                if (response.isSuccessful && response.body() != null) {
                    state = UiState.Success(response.body()!!.recommendations)
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
                title = { Text("JOURNAL MATCHER", letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onPrimary) },
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
                "Upload your draft manuscript. AI will analyze your methodology and recommend 7-8 best-fit journals.",
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
                    if (pdfFile != null) "SELECT DIFFERENT DRAFT" else "UPLOAD MANUSCRIPT",
                    fontWeight = FontWeight.W700,
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { findJournals() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = state !is UiState.Loading && pdfFile != null,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            ) {
                if (state is UiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onSecondary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("ANALYZING...", color = MaterialTheme.colorScheme.onSecondary, fontWeight = FontWeight.W700)
                } else {
                    Icon(Icons.Default.AutoAwesome, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("FIND TARGET JOURNALS", fontWeight = FontWeight.W700, letterSpacing = 1.sp)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            when (val s = state) {
                is UiState.Error -> {
                    Text(s.message, color = MaterialTheme.colorScheme.error)
                }
                is UiState.Success -> {
                    Text("Top Recommendations", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.W700, modifier = Modifier.align(Alignment.Start))
                    Spacer(modifier = Modifier.height(16.dp))
                    SelectionContainer {
                        Column {
                            s.data.forEachIndexed { index, journal ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.MenuBook, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("${index + 1}. ${journal.name}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.W700)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text("Publisher: ${journal.publisher}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Surface(
                                            color = MaterialTheme.colorScheme.secondaryContainer,
                                            shape = RoundedCornerShape(8.dp),
                                        ) {
                                            Text(journal.metrics, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
                                        }
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(journal.justification, style = MaterialTheme.typography.bodyMedium)
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}
