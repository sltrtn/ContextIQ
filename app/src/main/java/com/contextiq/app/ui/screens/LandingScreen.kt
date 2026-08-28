package com.contextiq.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.contextiq.app.ui.components.pressScale
import com.contextiq.app.ui.theme.ContextIQDesign

@Composable
fun LandingScreen(navController: NavController) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = ContextIQDesign.Space.Screen.dp),
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Wordmark
        Text(
            text = "CONTEXT",
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.W900,
                letterSpacing = (-0.09).em,
                lineHeight = 0.85.em,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "IQ",
            style = MaterialTheme.typography.displayLarge.copy(
                fontWeight = FontWeight.W900,
                letterSpacing = (-0.13).em,
                lineHeight = 0.85.em,
            ),
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "RESEARCH, INTERROGATED.",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.W800,
                letterSpacing = 0.22.em,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Hero statement
        Text(
            text = "READ BETWEEN\nTHE LINES.",
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.W900,
                letterSpacing = (-0.08).em,
                lineHeight = 0.9.em,
            ),
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Upload a research paper. Interrogate the evidence. Follow every answer to its source.",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.W600,
                lineHeight = 1.35.em,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.End),
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Marquee strip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.onBackground)
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "QUESTION EVERYTHING — QUESTION EVERYTHING — QUESTION EVERYTHING",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.W900,
                    letterSpacing = (-0.02).em,
                ),
                color = MaterialTheme.colorScheme.background,
                maxLines = 1,
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // CTA
        Box(modifier = Modifier.pressScale(ContextIQDesign.Motion.ButtonPressScale)) {
            Button(
                onClick = { navController.navigate("workbench") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ContextIQDesign.Control.ProminentHeight.dp),
                shape = RoundedCornerShape(ContextIQDesign.Radius.Action.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onBackground,
                    contentColor = MaterialTheme.colorScheme.background,
                ),
            ) {
                Text(
                    text = "START INTERROGATING",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.W900,
                        letterSpacing = 0.1.em,
                    ),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // History link
        TextButton(
            onClick = { navController.navigate("history") },
            modifier = Modifier.align(Alignment.CenterHorizontally),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onBackground,
            ),
        ) {
            Text(
                text = "PAST SESSIONS",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.W800,
                    letterSpacing = 0.13.em,
                ),
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
