package com.contextiq.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.contextiq.app.domain.UiState
import com.contextiq.app.network.ContextIQClient
import com.contextiq.app.network.dto.PaperDto
import com.contextiq.app.network.dto.RelatedPapersRequest
import com.contextiq.app.ui.components.pressScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RelatedPapersScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    var searchQuery by remember { mutableStateOf("") }
    var state by remember { mutableStateOf<UiState<List<PaperDto>>>(UiState.Idle) }

    fun searchPapers(query: String) {
        if (query.isBlank()) return
        state = UiState.Loading

        scope.launch(Dispatchers.IO) {
            try {
                val response = ContextIQClient.api.relatedPapers(
                    RelatedPapersRequest(query = query, limit = 10),
                )
                if (response.isSuccessful && response.body() != null) {
                    val papers = response.body()!!.papers
                    state = if (papers.isEmpty()) {
                        UiState.Error("No related papers found.")
                    } else {
                        UiState.Success(papers)
                    }
                } else {
                    state = UiState.Error("Could not fetch results.")
                }
            } catch (e: Exception) {
                state = UiState.Error("Network error: ${e.message}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RELATED PAPERS", letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onPrimary) },
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
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Enter a topic, keyword, or paper title") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                ),
                trailingIcon = {
                    IconButton(
                        onClick = { searchPapers(searchQuery) },
                        enabled = state !is UiState.Loading && searchQuery.isNotBlank(),
                    ) {
                        Icon(Icons.Default.Search, "Search")
                    }
                },
            )
            Spacer(modifier = Modifier.height(16.dp))

            when (val s = state) {
                is UiState.Loading -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is UiState.Error -> {
                    Text(s.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
                }
                is UiState.Success -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 16.dp),
                    ) {
                        items(s.data) { paper ->
                            PaperResultCard(paper = paper, onOpenUrl = {
                                if (paper.url.isNotEmpty()) uriHandler.openUri(paper.url)
                            })
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
fun PaperResultCard(paper: PaperDto, onOpenUrl: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(0.97f)
            .clickable { expanded = !expanded },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(paper.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.W700)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "${paper.year}  •  ${paper.authors}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(paper.abstract, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (paper.url.isNotEmpty()) {
                TextButton(
                    onClick = onOpenUrl,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Read Source")
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.AutoMirrored.Filled.OpenInNew, "Open Link", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}
