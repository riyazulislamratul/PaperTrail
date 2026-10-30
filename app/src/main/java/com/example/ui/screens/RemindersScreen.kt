package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AlarmOn
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DocumentCategory
import com.example.data.DocumentEntity
import com.example.ui.components.UrgencyBadge
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.StatusUrgent
import com.example.ui.theme.StatusWarning
import com.example.ui.viewmodel.DocumentViewModel
import com.example.util.DeadlineUrgency
import com.example.util.Formatters

@Composable
fun RemindersScreen(
    viewModel: DocumentViewModel,
    deadlines: List<DocumentEntity>,
    onScanClick: () -> Unit,
    onDocumentClick: (DocumentEntity) -> Unit = { viewModel.selectDocument(it) },
    modifier: Modifier = Modifier
) {
    val overdueList = remember(deadlines) {
        deadlines.filter {
            !it.isActionCompleted && Formatters.getDeadlineUrgency(it.dueDate, false) == DeadlineUrgency.OVERDUE
        }
    }

    val upcomingList = remember(deadlines) {
        deadlines.filter {
            !it.isActionCompleted && Formatters.getDeadlineUrgency(it.dueDate, false) != DeadlineUrgency.OVERDUE
        }
    }

    val completedList = remember(deadlines) {
        deadlines.filter { it.isActionCompleted }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("reminders_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Top Header
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Deadlines & Reminders",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Offline alarms and action deadlines",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }

        // Overdue Alert Section (if any)
        if (overdueList.isNotEmpty()) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = StatusUrgent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "OVERDUE ACTIONS (${overdueList.size})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = StatusUrgent,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            items(overdueList, key = { it.id }) { doc ->
                ReminderItemCard(
                    doc = doc,
                    onDocClick = { onDocumentClick(doc) },
                    onToggleDone = { viewModel.toggleActionCompleted(doc) },
                    onSnoozeDays = { days -> viewModel.snoozeReminder(doc, days) },
                    onCancelReminder = { viewModel.cancelReminder(doc) }
                )
            }
        }

        // Upcoming Deadlines Section
        item {
            Text(
                text = "UPCOMING DEADLINES (${upcomingList.size})",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 0.5.sp
            )
        }

        if (upcomingList.isEmpty() && overdueList.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(42.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "All deadlines settled!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "No pending bills or expiring policies detected.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(upcomingList, key = { it.id }) { doc ->
                ReminderItemCard(
                    doc = doc,
                    onDocClick = { onDocumentClick(doc) },
                    onToggleDone = { viewModel.toggleActionCompleted(doc) },
                    onSnoozeDays = { days -> viewModel.snoozeReminder(doc, days) },
                    onCancelReminder = { viewModel.cancelReminder(doc) }
                )
            }
        }

        // Completed Section
        if (completedList.isNotEmpty()) {
            item {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "COMPLETED (${completedList.size})",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 0.5.sp
                )
            }

            items(completedList.take(4), key = { it.id }) { doc ->
                ReminderItemCard(
                    doc = doc,
                    onDocClick = { onDocumentClick(doc) },
                    onToggleDone = { viewModel.toggleActionCompleted(doc) },
                    onSnoozeDays = { days -> viewModel.snoozeReminder(doc, days) },
                    onCancelReminder = { viewModel.cancelReminder(doc) }
                )
            }
        }
    }
}

@Composable
fun ReminderItemCard(
    doc: DocumentEntity,
    onDocClick: () -> Unit,
    onToggleDone: () -> Unit,
    onSnoozeDays: (Int) -> Unit,
    onCancelReminder: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val urgency = Formatters.getDeadlineUrgency(doc.dueDate, doc.isActionCompleted)
    val relativeText = Formatters.getRelativeDeadlineText(doc.dueDate, doc.isActionCompleted)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onDocClick)
            .testTag("reminder_item_${doc.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (doc.isActionCompleted) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (urgency == DeadlineUrgency.OVERDUE) StatusUrgent.copy(alpha = 0.5f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (urgency == DeadlineUrgency.OVERDUE) StatusUrgent.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.primaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getCategoryIcon(doc.category),
                            contentDescription = null,
                            tint = if (urgency == DeadlineUrgency.OVERDUE) StatusUrgent
                            else MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = doc.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = doc.organization ?: DocumentCategory.fromString(doc.category).displayName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                UrgencyBadge(urgency = urgency, relativeText = relativeText)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Details row: Due date, Amount, Action
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Deadline",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = Formatters.formatDate(doc.dueDate),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (doc.amount != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Amount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = Formatters.formatCurrency(doc.amount, doc.currency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (!doc.requiredAction.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Action: ${doc.requiredAction}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons: Done / Snooze / More
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Done toggle button
                FilledTonalButton(
                    onClick = onToggleDone,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = if (doc.isActionCompleted) Icons.Default.CheckCircle else Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = if (doc.isActionCompleted) Color(0xFF10B981) else MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (doc.isActionCompleted) "Completed" else "Mark as Done")
                }

                // Snooze / reminder quick menu
                if (!doc.isActionCompleted) {
                    Box {
                        OutlinedButton(
                            onClick = { menuExpanded = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Snooze, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Snooze")
                        }

                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Snooze 1 Day") },
                                onClick = {
                                    onSnoozeDays(1)
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Snooze 3 Days") },
                                onClick = {
                                    onSnoozeDays(3)
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Snooze 1 Week") },
                                onClick = {
                                    onSnoozeDays(7)
                                    menuExpanded = false
                                }
                            )
                            if (doc.hasReminder) {
                                DropdownMenuItem(
                                    text = { Text("Cancel Reminder") },
                                    onClick = {
                                        onCancelReminder()
                                        menuExpanded = false
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
