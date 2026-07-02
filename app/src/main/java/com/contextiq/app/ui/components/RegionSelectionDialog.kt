package com.contextiq.app.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun RegionSelectionDialog(
    croppedBitmap: Bitmap,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = "CONFIRM SELECTION",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 2.sp,
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    bitmap = croppedBitmap.asImageBitmap(),
                    contentDescription = "Crop Preview",
                    modifier = Modifier
                        .height(150.dp)
                        .fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Content Type:",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = { onConfirm("Text") },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.pressScale(0.92f),
                    ) { Text("Text") }
                    FilledTonalButton(
                        onClick = { onConfirm("Figure") },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.pressScale(0.92f),
                    ) { Text("Fig") }
                    FilledTonalButton(
                        onClick = { onConfirm("Table") },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.pressScale(0.92f),
                    ) { Text("Table") }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.pressScale(0.94f),
            ) { Text("Cancel") }
        },
    )
}

private val Float.dp: androidx.compose.ui.unit.Dp
    get() = androidx.compose.ui.unit.Dp(this)
