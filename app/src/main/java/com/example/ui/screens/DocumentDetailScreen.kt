package com.example.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOff
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.DocumentCategory
import com.example.data.DocumentEntity
import com.example.ui.components.GoogleGroundingCard
import com.example.ui.components.UrgencyBadge
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.StatusUrgent
import com.example.ui.theme.StatusWarning
import com.example.ui.viewmodel.DocumentViewModel
import com.example.util.DeadlineUrgency
import com.example.util.FileStorageHelper
import com.example.util.Formatters
import java.io.File
import java.util.concurrent.TimeUnit

@Composable
fun DocumentDetailScreen(
    viewModel: DocumentViewModel,
    docId: Long,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val document by viewModel.getDocumentFlow(docId).collectAsStateWithLifecycle(initialValue = null)
    if (document == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        DocumentDetailScreen(
            viewModel = viewModel,
            document = document!!,
            onBackClick = onBackClick,
            modifier = modifier
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DocumentDetailScreen(
    viewModel: DocumentViewModel,
    document: DocumentEntity,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var showSnoozeMenu by remember { mutableStateOf(false) }
    var showOcrTextExpanded by remember { mutableStateOf(false) }

    val urgency = Formatters.getDeadlineUrgency(document.dueDate, document.isActionCompleted)
    val relativeText = Formatters.getRelativeDeadlineText(document.dueDate, document.isActionCompleted)

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = document.dueDate ?: System.currentTimeMillis()
    )

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete Document?") },
            text = { Text("This will permanently remove '${document.title}' and any local image/PDF files from your device.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteDocument(document) {
                            onBackClick()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Date Picker Dialog for changing deadline
    if (showDatePickerDialog) {
        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selected = datePickerState.selectedDateMillis
                        if (selected != null) {
                            viewModel.updateDocumentDetails(
                                document.copy(
                                    dueDate = selected,
                                    isDueDateConfirmed = true
                                )
                            )
                        }
                        showDatePickerDialog = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePickerDialog = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Edit Metadata Dialog
    if (showEditDialog) {
        EditDocumentDialog(
            document = document,
            onDismiss = { showEditDialog = false },
            onSave = { updated ->
                viewModel.updateDocumentDetails(updated)
                showEditDialog = false
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("document_detail_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Toolbar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.testTag("detail_back_button")
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }

                Row {
                    // Export / Share Button
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                val file = File(document.filePath)
                                if (file.exists()) {
                                    val uri = FileStorageHelper.getShareableUri(context, file)
                                    type = if (document.fileType == "PDF") "application/pdf" else "image/jpeg"
                                    putExtra(Intent.EXTRA_STREAM, uri)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                } else {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_SUBJECT, document.title)
                                    putExtra(Intent.EXTRA_TEXT, "${document.title}\n${document.extractedText}")
                                }
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Document"))
                        },
                        modifier = Modifier.testTag("share_button")
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Export or Share")
                    }

                    // Edit button
                    IconButton(
                        onClick = { showEditDialog = true },
                        modifier = Modifier.testTag("edit_button")
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit details")
                    }

                    // Delete button
                    IconButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.testTag("delete_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete document",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Document Image or Visual Representation Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val file = File(document.filePath)
                if (file.exists()) {
                    AsyncImage(
                        model = file,
                        contentDescription = "Scanned Document",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    // Crisp generated vector placeholder for documents
                    val placeholder = remember(document) {
                        FileStorageHelper.createPlaceholderDocumentBitmap(
                            title = document.title,
                            category = document.category,
                            organization = document.organization
                        )
                    }
                    Image(
                        bitmap = placeholder.asImageBitmap(),
                        contentDescription = "Document placeholder",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Header: Category, Urgency, Title
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = getCategoryIcon(document.category),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = DocumentCategory.fromString(document.category).displayName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    if (document.dueDate != null) {
                        UrgencyBadge(urgency = urgency, relativeText = relativeText)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = document.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )

                if (!document.organization.isNullOrBlank()) {
                    Text(
                        text = document.organization,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Primary Metric Cards: Amount Due & Due Date
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Due Date Card
                Card(
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (urgency == DeadlineUrgency.OVERDUE) StatusUrgent.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (urgency == DeadlineUrgency.OVERDUE) StatusUrgent else MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (document.isDueDateConfirmed) "Confirmed Due" else "Suggested Due",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = Formatters.formatDate(document.dueDate),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Amount Card (if any)
                if (document.amount != null) {
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "Total Amount",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = Formatters.formatCurrency(document.amount, document.currency),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }

        // Action Status & Reminder Controls
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (document.isActionCompleted) Color(0xFF10B981).copy(alpha = 0.1f)
                    else MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (document.isActionCompleted) Color(0xFF10B981).copy(alpha = 0.3f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (document.isActionCompleted) "Status: Completed / Paid" else "Action Required",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (document.isActionCompleted) Color(0xFF059669) else MaterialTheme.colorScheme.primary
                            )
                            if (!document.requiredAction.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = document.requiredAction,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }

                        // Toggle Action Completed Button
                        Button(
                            onClick = { viewModel.toggleActionCompleted(document) },
                            shape = RoundedCornerShape(10.dp),
                            colors = if (document.isActionCompleted) ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                            else ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = if (document.isActionCompleted) Icons.Default.CheckCircle else Icons.Default.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (document.isActionCompleted) "Done" else "Mark Done")
                        }
                    }

                    // Scheduled Reminder row
                    if (!document.isActionCompleted) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (document.hasReminder) Icons.Default.NotificationsActive else Icons.Default.Alarm,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = if (document.hasReminder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = if (document.hasReminder) "Reminder Scheduled" else "No reminder set",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (document.hasReminder && document.reminderTime != null) {
                                        Text(
                                            text = Formatters.formatDateTime(document.reminderTime),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Quick Snooze / Setup menu
                            Box {
                                OutlinedButton(
                                    onClick = { showSnoozeMenu = true },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Snooze / Alert")
                                }

                                DropdownMenu(
                                    expanded = showSnoozeMenu,
                                    onDismissRequest = { showSnoozeMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Snooze 1 Day") },
                                        onClick = {
                                            viewModel.snoozeReminder(document, 1)
                                            showSnoozeMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Snooze 3 Days") },
                                        onClick = {
                                            viewModel.snoozeReminder(document, 3)
                                            showSnoozeMenu = false
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Snooze 1 Week") },
                                        onClick = {
                                            viewModel.snoozeReminder(document, 7)
                                            showSnoozeMenu = false
                                        }
                                    )
                                    if (document.hasReminder) {
                                        DropdownMenuItem(
                                            text = { Text("Turn Off Reminder") },
                                            onClick = {
                                                viewModel.cancelReminder(document)
                                                showSnoozeMenu = false
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

        // Additional Metadata Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Document Information",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (!document.referenceNumber.isNullOrBlank()) {
                        MetadataRow("Reference #", document.referenceNumber)
                    }

                    MetadataRow("Date Added", Formatters.formatDate(document.createdAt))

                    if (document.notes.isNotBlank()) {
                        MetadataRow("Notes", document.notes)
                    }
                }
            }
        }

        // Google Search & Google Maps Intelligence
        item {
            GoogleGroundingCard(document = document)
        }

        // Extracted OCR Text Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showOcrTextExpanded = !showOcrTextExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Extracted OCR Text (${document.extractedText.length} characters)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            imageVector = if (showOcrTextExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }

                    AnimatedVisibility(visible = showOcrTextExpanded) {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                TextButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(document.extractedText))
                                        Toast.makeText(context, "Copied OCR text", Toast.LENGTH_SHORT).show()
                                    }
                                ) {
                                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy")
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = document.extractedText.ifBlank { "No text extracted." },
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.padding(12.dp)
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
fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditDocumentDialog(
    document: DocumentEntity,
    onDismiss: () -> Unit,
    onSave: (DocumentEntity) -> Unit
) {
    var title by remember { mutableStateOf(document.title) }
    var selectedCategory by remember { mutableStateOf(DocumentCategory.fromString(document.category)) }
    var organization by remember { mutableStateOf(document.organization ?: "") }
    var referenceNumber by remember { mutableStateOf(document.referenceNumber ?: "") }
    var amountString by remember { mutableStateOf(if (document.amount != null) "${document.amount}" else "") }
    var requiredAction by remember { mutableStateOf(document.requiredAction ?: "") }
    var notes by remember { mutableStateOf(document.notes) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Document Details") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Title") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Text("Category", style = MaterialTheme.typography.labelSmall)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DocumentCategory.entries.forEach { cat ->
                            FilterChip(
                                selected = selectedCategory == cat,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat.displayName, fontSize = 12.sp) }
                            )
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        value = organization,
                        onValueChange = { organization = it },
                        label = { Text("Organization") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = referenceNumber,
                        onValueChange = { referenceNumber = it },
                        label = { Text("Reference / Account #") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = amountString,
                        onValueChange = { amountString = it },
                        label = { Text("Amount ($)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = requiredAction,
                        onValueChange = { requiredAction = it },
                        label = { Text("Required Action") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notes") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        document.copy(
                            title = title.trim(),
                            category = selectedCategory.name,
                            organization = organization.ifBlank { null },
                            referenceNumber = referenceNumber.ifBlank { null },
                            amount = amountString.toDoubleOrNull(),
                            requiredAction = requiredAction.ifBlank { null },
                            notes = notes.trim(),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
