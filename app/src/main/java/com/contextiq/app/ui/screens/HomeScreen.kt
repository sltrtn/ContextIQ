package com.contextiq.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.contextiq.app.ui.components.pressScale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "CONTEXTIQ",
                        fontWeight = FontWeight.W700,
                        letterSpacing = 6.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            SectionHeader("ANALYZE PAPERS")
            FeatureCard(
                title = "Analyze Research Paper",
                description = "Upload PDF, extract text, ask AI",
                icon = Icons.Default.TravelExplore,
                onClick = { navController.navigate("analyze_paper") },
            )
            FeatureCard(
                title = "Analysis History",
                description = "View your past AI explanations",
                icon = Icons.Default.History,
                onClick = { navController.navigate("history") },
            )

            SectionHeader("AGENTIC AI TOOLS")
            FeatureCard(
                title = "Autonomous Literature Reviewer",
                description = "Upload up to 3 PDFs — get a synthesized review",
                icon = Icons.AutoMirrored.Filled.LibraryBooks,
                onClick = { navController.navigate("lit_reviewer") },
            )
            FeatureCard(
                title = "Claim Verifier & Citer",
                description = "Paste a claim. AI verifies against literature.",
                icon = Icons.Default.FactCheck,
                onClick = { navController.navigate("claim_verifier") },
            )

            SectionHeader("QUICK TOOLS")
            FeatureCard(
                title = "Journal Matcher",
                description = "Find best-fit Q1/Q2 journals for your manuscript",
                icon = Icons.Default.Publish,
                onClick = { navController.navigate("journal_matcher") },
            )
            FeatureCard(
                title = "Rebuttal Drafter",
                description = "Cross-reference reviewer comments with your manuscript",
                icon = Icons.Default.RateReview,
                onClick = { navController.navigate("rebuttal_drafter") },
            )
            FeatureCard(
                title = "Citation Generator",
                description = "Convert any DOI into APA, IEEE, or MLA",
                icon = Icons.Default.FormatQuote,
                onClick = { navController.navigate("citation_generator") },
            )
            FeatureCard(
                title = "Find Related Papers",
                description = "Discover literature by topic or title",
                icon = Icons.Default.Search,
                onClick = { navController.navigate("related_papers") },
            )
            FeatureCard(
                title = "Open Access Finder",
                description = "Find free, legal PDF versions",
                icon = Icons.Default.LockOpen,
                onClick = { navController.navigate("open_access") },
            )
            FeatureCard(
                title = "Abstract TL;DR",
                description = "Extract core findings from dense abstracts",
                icon = Icons.Default.AutoAwesome,
                onClick = { navController.navigate("abstract_summary") },
            )
            FeatureCard(
                title = "AI Paper Reviewer",
                description = "Upload a draft — get a structured peer review",
                icon = Icons.Default.RateReview,
                onClick = { navController.navigate("paper_reviewer") },
            )
            FeatureCard(
                title = "LaTeX Generator",
                description = "Snap a photo of an equation — get LaTeX code",
                icon = Icons.Default.Functions,
                onClick = { navController.navigate("latex_generator") },
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        letterSpacing = 2.sp,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
fun FeatureCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(0.96f),
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.W700,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
