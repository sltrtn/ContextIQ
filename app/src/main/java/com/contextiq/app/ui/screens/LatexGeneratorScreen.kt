package com.contextiq.app.ui.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.webkit.WebView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.contextiq.app.domain.UiState
import com.contextiq.app.network.ContextIQClient
import com.contextiq.app.ui.components.pressScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

@Composable
fun LatexRenderer(latexCode: String, modifier: Modifier = Modifier) {
    val htmlContent = """
        <!DOCTYPE html>
        <html>
        <head>
            <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.css">
            <script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/katex.min.js"></script>
            <script defer src="https://cdn.jsdelivr.net/npm/katex@0.16.8/dist/contrib/auto-render.min.js"
                onload="renderMathInElement(document.body);"></script>
            <style>
                body { display: flex; justify-content: center; align-items: center; min-height: 100vh; margin: 0; font-size: 20px; background-color: transparent; color: #000000; }
            </style>
        </head>
        <body>$$ $latexCode $$</body>
        </html>
    """.trimIndent()

    AndroidView(
        modifier = modifier.fillMaxWidth().heightIn(min = 100.dp, max = 200.dp),
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            webView.loadDataWithBaseURL(null, htmlContent, "text/html", "UTF-8", null)
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LatexGeneratorScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current
    val scrollState = rememberScrollState()

    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageFile by remember { mutableStateOf<File?>(null) }
    var latexResult by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    fun processImageUri(uri: Uri) {
        scope.launch(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                val maxDim = 1024f
                val scale = minOf(maxDim / originalBitmap.width, maxDim / originalBitmap.height)
                val scaledBitmap = if (scale < 1f) {
                    Bitmap.createScaledBitmap(
                        originalBitmap,
                        (originalBitmap.width * scale).toInt(),
                        (originalBitmap.height * scale).toInt(),
                        true,
                    )
                } else {
                    originalBitmap
                }

                val outputStream = ByteArrayOutputStream()
                scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)

                val tempFile = File(context.cacheDir, "latex_eq.jpg")
                FileOutputStream(tempFile).use { it.write(outputStream.toByteArray()) }

                launch(Dispatchers.Main) {
                    selectedBitmap = scaledBitmap
                    imageFile = tempFile
                    latexResult = ""
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? -> uri?.let { processImageUri(it) } }

    fun generateLatex() {
        val file = imageFile ?: return
        isLoading = true
        latexResult = ""

        scope.launch(Dispatchers.IO) {
            try {
                val requestFile = file.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val part = MultipartBody.Part.createFormData("file", file.name, requestFile)
                val response = ContextIQClient.api.latexGenerate(part)

                if (response.isSuccessful && response.body() != null) {
                    var code = response.body()!!.latex_code
                    code = code.replace("```latex", "").replace("```", "").trim()
                    launch(Dispatchers.Main) { latexResult = code }
                } else {
                    launch(Dispatchers.Main) { latexResult = "API Error: ${response.code()}" }
                }
            } catch (e: Exception) {
                launch(Dispatchers.Main) { latexResult = "Network Error: ${e.message}" }
            } finally {
                launch(Dispatchers.Main) { isLoading = false }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LATEX GENERATOR", letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onPrimary) },
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
                "Upload a photo of a handwritten or printed equation — AI converts it to LaTeX.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                if (selectedBitmap != null) {
                    Image(
                        bitmap = selectedBitmap!!.asImageBitmap(),
                        contentDescription = "Selected Equation",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(8.dp),
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Image, null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No image selected", color = Color.Gray)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(20.dp),
            ) {
                Icon(Icons.Default.CameraAlt, null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (selectedBitmap != null) "SELECT DIFFERENT IMAGE" else "CHOOSE IMAGE", fontWeight = FontWeight.W700)
            }
            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { generateLatex() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isLoading && imageFile != null,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimaryContainer)
                } else {
                    Text("GENERATE LATEX CODE", fontWeight = FontWeight.W700, letterSpacing = 1.sp)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))

            if (latexResult.isNotEmpty() && !isLoading) {
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
                            Text("AI Interpretation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.W700, color = MaterialTheme.colorScheme.primary)
                            Box(modifier = Modifier.pressScale(0.92f)) {
                                IconButton(onClick = { clipboardManager.setText(AnnotatedString(latexResult)) }) {
                                    Icon(Icons.Default.ContentCopy, "Copy", modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Preview:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .padding(8.dp),
                        ) {
                            LatexRenderer(latexCode = latexResult)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        Text("Raw Code:", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        SelectionContainer {
                            Text(latexResult, style = MaterialTheme.typography.bodyLarge, fontFamily = FontFamily.Monospace)
                        }
                    }
                }
            }
        }
    }
}
