package com.contextiq.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.contextiq.app.domain.UiState
import com.contextiq.app.network.ContextIQClient
import com.contextiq.app.network.dto.CitationRequest
import com.contextiq.app.ui.components.pressScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitationScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    var doiInput by remember { mutableStateOf("") }
    var selectedStyle by remember { mutableStateOf("apa") }
    var state by remember { mutableStateOf<UiState<String>>(UiState.Idle) }

    val citationStyles = listOf("apa" to "APA", "ieee" to "IEEE", "mla" to "MLA")

    fun fetchCitation(doi: String, style: String) {
        if (doi.isBlank()) return
        state = UiState.Loading

        scope.launch(Dispatchers.IO) {
            try {
                val cleanDoi = doi
                    .replace("https://doi.org/", "")
                    .replace("http://dx.doi.org/", "")
                    .trim()
                val response = ContextIQClient.api.citation(
                    CitationRequest(doi = cleanDoi, style = style),
                )
                if (response.isSuccessful) {
                    val citation = response.body()?.citation ?: "No citation generated."
                    state = UiState.Success(citation)
                } else {
                    state = UiState.Error("Could not fetch citation. Check the DOI.")
                }
            } catch (e: Exception) {
                state = UiState.Error("Network error: ${e.message}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CITATION GENERATOR", letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onPrimary) },
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
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Enter a DOI to generate an instant, perfectly formatted citation.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = doiInput,
                onValueChange = { doiInput = it },
                label = { Text("DOI (e.g., 10.1038/s41586-020-2649-2)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                citationStyles.forEach { (id, name) ->
                    FilterChip(
                        selected = selectedStyle == id,
                        onClick = { selectedStyle = id },
                        label = { Text(name, fontWeight = FontWeight.W700) },
                        shape = RoundedCornerShape(12.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { fetchCitation(doiInput, selectedStyle) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = state !is UiState.Loading && doiInput.isNotBlank(),
                shape = RoundedCornerShape(20.dp),
            ) {
                if (state is UiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text("GENERATE CITATION", fontWeight = FontWeight.W700, letterSpacing = 1.sp)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            when (val s = state) {
                is UiState.Error -> {
                    Text(s.message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
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
                                Text(
                                    "Formatted Citation ($selectedStyle)",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Box(modifier = Modifier.pressScale(0.92f)) {
                                    IconButton(onClick = { clipboardManager.setText(AnnotatedString(s.data)) }) {
                                        Icon(Icons.Default.ContentCopy, "Copy", modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
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
