package com.contextiq.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.contextiq.app.domain.UiState
import com.contextiq.app.network.ContextIQClient
import com.contextiq.app.network.dto.OpenAccessRequest
import com.contextiq.app.network.dto.OpenAccessResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OpenAccessScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    var doiInput by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<UiState<OpenAccessResponse>>(UiState.Idle) }

    fun checkOpenAccess(doi: String) {
        if (doi.isBlank()) return
        state = UiState.Loading

        scope.launch(Dispatchers.IO) {
            try {
                val cleanDoi = doi
                    .replace("https://doi.org/", "")
                    .replace("http://dx.doi.org/", "")
                    .trim()
                val response = ContextIQClient.api.openAccess(
                    OpenAccessRequest(doi = cleanDoi),
                )
                if (response.isSuccessful && response.body() != null) {
                    state = UiState.Success(response.body()!!)
                } else {
                    state = UiState.Error("DOI not found in Open Access databases.")
                }
            } catch (e: Exception) {
                state = UiState.Error("Network error: ${e.message}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("OPEN ACCESS FINDER", letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onPrimary) },
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
                "Hit a paywall? Paste the DOI below to check if a free, legal PDF exists.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = doiInput,
                onValueChange = { doiInput = it },
                label = { Text("DOI (e.g., 10.1038/nature12373)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
            )
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { checkOpenAccess(doiInput) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = state !is UiState.Loading && doiInput.isNotBlank(),
                shape = RoundedCornerShape(20.dp),
            ) {
                if (state is UiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("SEARCH FOR FREE PDF", fontWeight = FontWeight.W700, letterSpacing = 1.sp)
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
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(s.data.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.W700, textAlign = TextAlign.Center)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(s.data.journal, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))

                            if (s.data.is_open_access && s.data.pdf_url != null) {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFE8F5E9), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CheckCircle, "Success", tint = Color(0xFF2E7D32))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Open Access Available!", color = Color(0xFF2E7D32), fontWeight = FontWeight.W700)
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { uriHandler.openUri(s.data.pdf_url) },
                                    modifier = Modifier.fillMaxWidth().height(56.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                ) {
                                    Icon(Icons.Default.PictureAsPdf, "PDF")
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("READ PDF NOW", fontWeight = FontWeight.W700)
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFFEBEE), RoundedCornerShape(12.dp))
                                        .padding(12.dp)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ErrorOutline, "Paywall", tint = Color(0xFFC62828))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Behind a Paywall", color = Color(0xFFC62828), fontWeight = FontWeight.W700)
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No free version found in public repositories.", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                else -> {}
            }
        }
    }
}
