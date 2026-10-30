package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.RequestQuote
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DocumentCategory
import com.example.data.DocumentEntity
import com.example.ui.theme.StatusUrgent
import com.example.ui.theme.StatusWarning
import com.example.util.DeadlineUrgency
import com.example.util.Formatters

@Composable
fun getCategoryIcon(categoryName: String): ImageVector {
    return when (DocumentCategory.fromString(categoryName)) {
        DocumentCategory.BILL -> Icons.Default.ReceiptLong
        DocumentCategory.INVOICE -> Icons.Default.RequestQuote
        DocumentCategory.RECEIPT -> Icons.Default.ShoppingBag
        DocumentCategory.BANK_LETTER -> Icons.Default.AccountBalance
        DocumentCategory.INSURANCE -> Icons.Default.HealthAndSafety
        DocumentCategory.OFFICIAL_NOTICE -> Icons.Default.Policy
        DocumentCategory.OTHER -> Icons.Default.Description
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.testTag("stat_card_${title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(contentColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = contentColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun UrgencyBadge(urgency: DeadlineUrgency, relativeText: String) {
    val (bgColor, textColor) = when (urgency) {
        DeadlineUrgency.OVERDUE -> Pair(StatusUrgent.copy(alpha = 0.15f), StatusUrgent)
        DeadlineUrgency.TODAY, DeadlineUrgency.TOMORROW -> Pair(StatusWarning.copy(alpha = 0.18f), StatusWarning)
        DeadlineUrgency.THIS_WEEK -> Pair(Color(0xFF38BDF8).copy(alpha = 0.15f), Color(0xFF0284C7))
        DeadlineUrgency.COMPLETED -> Pair(Color(0xFF10B981).copy(alpha = 0.15f), Color(0xFF059669))
        DeadlineUrgency.UPCOMING -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
        DeadlineUrgency.NO_DEADLINE -> Pair(Color.Transparent, Color.Transparent)
    }

    if (urgency != DeadlineUrgency.NO_DEADLINE) {
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = bgColor
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (urgency == DeadlineUrgency.OVERDUE || urgency == DeadlineUrgency.TODAY) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = textColor
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = relativeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun DocumentCard(
    document: DocumentEntity,
    onClick: () -> Unit,
    onToggleDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val urgency = Formatters.getDeadlineUrgency(document.dueDate, document.isActionCompleted)
    val relativeDateText = Formatters.getRelativeDeadlineText(document.dueDate, document.isActionCompleted)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("doc_card_${document.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (document.isActionCompleted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            1.dp,
            if (urgency == DeadlineUrgency.OVERDUE) StatusUrgent.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon glyph box
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (document.isActionCompleted) MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(document.category),
                    contentDescription = null,
                    tint = if (document.isActionCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Middle: Title, Org, Category, and Action
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = document.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (document.hasReminder) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = "Reminder active",
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (!document.organization.isNullOrBlank()) {
                    Text(
                        text = document.organization,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Category pill
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = DocumentCategory.fromString(document.category).displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    // Urgency / Due Date badge
                    if (document.dueDate != null) {
                        UrgencyBadge(urgency = urgency, relativeText = relativeDateText)
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right side: Financial Amount and Done toggle
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                if (document.amount != null) {
                    Text(
                        text = Formatters.formatCurrency(document.amount, document.currency),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (document.isActionCompleted) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                IconButton(
                    onClick = onToggleDone,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("toggle_done_${document.id}")
                ) {
                    Icon(
                        imageVector = if (document.isActionCompleted) Icons.Default.CheckCircle else Icons.Default.Check,
                        contentDescription = "Toggle status",
                        tint = if (document.isActionCompleted) Color(0xFF10B981)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryFilterRow(
    selectedCategory: String?,
    onSelectCategory: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            FilterChip(
                selected = selectedCategory == null,
                onClick = { onSelectCategory(null) },
                label = { Text("All") },
                modifier = Modifier.testTag("filter_all")
            )
        }
        items(DocumentCategory.entries.toTypedArray()) { category ->
            FilterChip(
                selected = selectedCategory.equals(category.name, ignoreCase = true),
                onClick = {
                    if (selectedCategory.equals(category.name, ignoreCase = true)) {
                        onSelectCategory(null)
                    } else {
                        onSelectCategory(category.name)
                    }
                },
                leadingIcon = {
                    Icon(
                        imageVector = getCategoryIcon(category.name),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                label = { Text(category.displayName) },
                modifier = Modifier.testTag("filter_${category.name.lowercase()}")
            )
        }
    }
}
