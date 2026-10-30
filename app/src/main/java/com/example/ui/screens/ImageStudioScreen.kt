package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.auth.GoogleAuthManager
import com.example.cloud.DriveFileType
import com.example.cloud.DriveVaultItem
import com.example.cloud.GoogleDriveVaultManager
import com.example.data.DocumentEntity
import com.example.intelligence.GenerationResult
import com.example.intelligence.ImageStudioService
import com.example.ui.components.TimeLimitBanner
import com.example.util.LimitedFeature
import com.example.util.UsageLimitTracker
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageStudioScreen(
    documents: List<DocumentEntity>,
    authManager: GoogleAuthManager,
    vaultManager: GoogleDriveVaultManager,
    onBackClick: () -> Unit,
    onOpenVault: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onBackClick)

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val userProfile by authManager.currentUser.collectAsState()
    val driveItems by vaultManager.syncedItems.collectAsState()

    val imageStudioService = remember { ImageStudioService(context) }
    val limitTracker = remember { UsageLimitTracker(LimitedFeature.IMAGE_STUDIO, scope) }
    val limitState by limitTracker.state.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Text-to-Image, 1 = Prompt Image Editing
    var promptText by remember { mutableStateOf("") }
    var isProcessing by remember { mutableStateOf(false) }
    var currentResult by remember { mutableStateOf<GenerationResult?>(null) }
    var isSavedToDrive by remember { mutableStateOf(false) }

    // Selected document for editing
    var selectedDocForEdit by remember { mutableStateOf<DocumentEntity?>(documents.firstOrNull { it.filePath.isNotBlank() }) }
    var isDocDropdownExpanded by remember { mutableStateOf(false) }

    val presetPrompts = remember(selectedTab) {
        if (selectedTab == 0) {
            listOf(
                "Official PAID Stamp in emerald ink with date and security code",
                "Expense breakdown donut chart with utilities and rent",
                "Official receipt for $249.00 payment to TechCorp Inc",
                "Certificate of Warranty guarantee with gold border",
                "Urgent Overdue notice seal with red border"
            )
        } else {
            listOf(
                "Add bold PAID IN FULL green stamp in top right corner",
                "Highlight payment due date and amount in bright yellow box",
                "Enhance contrast and sharpen document text",
                "Add CONFIDENTIAL diagonal watermark across page",
                "Add official audit approved verification badge"
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "AI Image Studio",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (userProfile.isDriveConnected) Icons.Default.CloudDone else Icons.Default.CloudUpload,
                                contentDescription = null,
                                tint = if (userProfile.isDriveConnected) Color(0xFF0F9D58) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (userProfile.isDriveConnected) "Google Drive: ${userProfile.email}" else "Drive Disconnected",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick, modifier = Modifier.testTag("btn_back_image_studio")) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenVault, modifier = Modifier.testTag("btn_open_vault_header")) {
                        Icon(imageVector = Icons.Default.CloudDone, contentDescription = "Google Drive Vault")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("image_studio_screen"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Time Limit & Cooldown Banner (Mandatory requirement)
            item {
                TimeLimitBanner(
                    state = limitState,
                    onRefreshSession = { limitTracker.refreshSession() },
                    onAddBonusTime = { limitTracker.addBonusMinutes(3) }
                )
            }

            // 2. Mode Selector: Text-to-Image vs Prompt Image Editing
            item {
                TabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            promptText = ""
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Create Image", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            promptText = ""
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Edit Document", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                }
            }

            // 3. Document selector if in "Edit Document" mode
            if (selectedTab == 1) {
                item {
                    Column {
                        Text(
                            text = "Select Document to Edit with Prompt:",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val docsWithImages = documents.filter { it.filePath.isNotBlank() }
                        if (docsWithImages.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Text(
                                    text = "No scanned documents with images found yet. You can scan or import a document first, or create a new visual using 'Create Image'.",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        } else {
                            ExposedDropdownMenuBox(
                                expanded = isDocDropdownExpanded,
                                onExpandedChange = { isDocDropdownExpanded = !isDocDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = selectedDocForEdit?.title ?: "Select a document",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDocDropdownExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                ExposedDropdownMenu(
                                    expanded = isDocDropdownExpanded,
                                    onDismissRequest = { isDocDropdownExpanded = false }
                                ) {
                                    docsWithImages.forEach { doc ->
                                        DropdownMenuItem(
                                            text = { Text(doc.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                            onClick = {
                                                selectedDocForEdit = doc
                                                isDocDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. Prompt Input & Presets
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = if (selectedTab == 0) "Text-to-Image Prompt" else "Document Edit Directive",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = promptText,
                            onValueChange = { promptText = it },
                            placeholder = {
                                Text(
                                    if (selectedTab == 0)
                                        "Describe the visual to create (e.g. Paid stamp, Invoice receipt, Expense chart)..."
                                    else
                                        "Describe edits (e.g. Add PAID stamp in green, highlight due date, sharpen text)..."
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp)
                                .testTag("input_image_prompt"),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick prompt inspiration chips
                        Text(
                            text = "Suggestions:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(presetPrompts) { prompt ->
                                FilterChip(
                                    selected = promptText == prompt,
                                    onClick = { promptText = prompt },
                                    label = { Text(prompt.take(32) + "...", fontSize = 12.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Button(
                            onClick = {
                                if (promptText.isBlank()) {
                                    Toast.makeText(context, "Please enter a prompt", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                if (!limitState.canPerformAction) {
                                    Toast.makeText(context, "Time limit or cooldown active. Please wait.", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                scope.launch {
                                    isProcessing = true
                                    isSavedToDrive = false
                                    limitTracker.triggerActionConsumed()

                                    try {
                                        val result = if (selectedTab == 0) {
                                            imageStudioService.generateImageFromPrompt(promptText)
                                        } else {
                                            val docImage: Bitmap = selectedDocForEdit?.filePath?.let { path ->
                                                if (path.isNotBlank()) BitmapFactory.decodeFile(path) else null
                                            } ?: Bitmap.createBitmap(800, 1000, Bitmap.Config.ARGB_8888).apply {
                                                eraseColor(android.graphics.Color.WHITE)
                                            }
                                            imageStudioService.editImageWithPrompt(docImage, promptText)
                                        }
                                        currentResult = result

                                        // Auto-save to Google Drive if connected and auto-sync is on
                                        if (userProfile.isDriveConnected && userProfile.autoSyncEnabled) {
                                            vaultManager.saveImageToDrive(
                                                bitmap = result.bitmap,
                                                title = result.title,
                                                userEmail = userProfile.email
                                            )
                                            isSavedToDrive = true
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    } finally {
                                        isProcessing = false
                                    }
                                }
                            },
                            enabled = !isProcessing && limitState.canPerformAction && promptText.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("btn_generate_image"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isProcessing) {
                                CircularProgressIndicator(
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Synthesizing Visual...")
                            } else {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (selectedTab == 0) "Generate Visual (${limitState.remainingSessionSeconds / 60}m limit)"
                                    else "Apply Edits (${limitState.remainingSessionSeconds / 60}m limit)"
                                )
                            }
                        }
                    }
                }
            }

            // 5. Result Preview & Cloud Drive Actions
            currentResult?.let { result ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("image_preview_card"),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = result.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Style: ${result.styleCategory} • 1080x1350 HD",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (isSavedToDrive) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFE8F5E9)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CloudDone,
                                                contentDescription = null,
                                                tint = Color(0xFF2E7D32),
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Drive Synced",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF2E7D32)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // High quality Image Preview
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(340.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = result.bitmap.asImageBitmap(),
                                    contentDescription = result.title,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Action buttons: Save to Google Drive, Export to Device, Share
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val driveResult = vaultManager.saveImageToDrive(
                                                bitmap = result.bitmap,
                                                title = result.title,
                                                userEmail = userProfile.email
                                            )
                                            if (driveResult.isSuccess) {
                                                isSavedToDrive = true
                                                Toast.makeText(context, "Saved to Google Drive: PaperTrail Vault!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Drive save failed", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSavedToDrive) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .testTag("btn_save_to_drive")
                                ) {
                                    Icon(
                                        imageVector = if (isSavedToDrive) Icons.Default.CloudDone else Icons.Default.CloudUpload,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(if (isSavedToDrive) "In Drive Vault" else "Save to Drive", maxLines = 1)
                                }

                                OutlinedButton(
                                    onClick = {
                                        scope.launch {
                                            saveImageToGallery(context, result.bitmap, result.title)
                                            Toast.makeText(context, "Saved to device storage!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Device", maxLines = 1)
                                }

                                OutlinedButton(
                                    onClick = {
                                        shareGeneratedImage(context, result.bitmap, result.title)
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(0.9f)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Share", maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }

            // 6. Recent Images in Google Drive
            val driveImages = driveItems.filter { it.type == DriveFileType.IMAGE }
            if (driveImages.isNotEmpty()) {
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Saved in Your Google Drive Vault",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "View All (${driveImages.size})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.clickable { onOpenVault() }
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(driveImages) { item ->
                                DriveImageCard(
                                    item = item,
                                    onClick = {
                                        val bmp = BitmapFactory.decodeFile(item.localFilePath)
                                        if (bmp != null) {
                                            currentResult = GenerationResult(
                                                bitmap = bmp,
                                                title = item.title,
                                                promptUsed = "Loaded from Drive",
                                                styleCategory = "Vault Archive"
                                            )
                                            isSavedToDrive = true
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DriveImageCard(
    item: DriveVaultItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column {
            val bitmap = remember(item.localFilePath) {
                BitmapFactory.decodeFile(item.localFilePath)
            }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(95.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(95.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }

            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = item.formattedSize,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun saveImageToGallery(context: Context, bitmap: Bitmap, title: String) {
    val picturesDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES) ?: context.filesDir
    val file = File(picturesDir, "papertrail_${System.currentTimeMillis()}.png")
    FileOutputStream(file).use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
    }
}

private fun shareGeneratedImage(context: Context, bitmap: Bitmap, title: String) {
    try {
        val cachePath = File(context.cacheDir, "images").apply { mkdirs() }
        val stream = File(cachePath, "share_image.png")
        FileOutputStream(stream).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", stream)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, title)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Image via"))
    } catch (_: Exception) {}
}
