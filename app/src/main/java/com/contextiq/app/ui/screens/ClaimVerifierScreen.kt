package com.contextiq.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FactCheck
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
import com.contextiq.app.ui.components.pressScale
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClaimVerifierScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    var claimInput by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<UiState<String>>(UiState.Idle) }
    var loadingStatus by remember { mutableStateOf("") }

    fun verifyClaim(claim: String) {
        if (claim.isBlank()) return
        state = UiState.Error("Claim verification against external literature is not supported by the current ContextIQ backend. Upload a PDF in Paper Analyzer and ask whether the claim is supported by the paper.")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CLAIM VERIFIER", letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onPrimary) },
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
                "Paste a draft claim. AI will hunt down real papers on Crossref and verify the facts.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = claimInput,
                onValueChange = { claimInput = it },
                label = { Text("Enter your draft claim...") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                maxLines = 10,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { verifyClaim(claimInput) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = state !is UiState.Loading && claimInput.isNotBlank(),
                shape = RoundedCornerShape(20.dp),
            ) {
                if (state is UiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(loadingStatus, color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Icon(Icons.Default.FactCheck, null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("VERIFY & CITE", fontWeight = FontWeight.W700, letterSpacing = 1.sp)
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
                                Text("Verified Text & Bibliography", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.W700, color = MaterialTheme.colorScheme.primary)
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
