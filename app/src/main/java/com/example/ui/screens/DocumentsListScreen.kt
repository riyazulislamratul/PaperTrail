package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.DocumentEntity
import com.example.ui.components.CategoryFilterRow
import com.example.ui.components.DocumentCard
import com.example.ui.viewmodel.DocumentViewModel

enum class DocumentSortOption(val displayName: String) {
    DATE_ADDED_DESC("Newest First"),
    DUE_DATE_ASC("Closest Deadline"),
    AMOUNT_DESC("Highest Amount"),
    TITLE_ASC("Alphabetical")
}

@Composable
fun DocumentsListScreen(
    viewModel: DocumentViewModel,
    documents: List<DocumentEntity>,
    searchQuery: String,
    selectedCategory: String?,
    onScanClick: () -> Unit,
    onDocumentClick: (DocumentEntity) -> Unit = { viewModel.selectDocument(it) },
    modifier: Modifier = Modifier
) {
    var sortOption by remember { mutableStateOf(DocumentSortOption.DATE_ADDED_DESC) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val sortedDocuments = remember(documents, sortOption) {
        when (sortOption) {
            DocumentSortOption.DATE_ADDED_DESC -> documents.sortedByDescending { it.createdAt }
            DocumentSortOption.DUE_DATE_ASC -> documents.sortedWith(
                compareBy<DocumentEntity> { it.dueDate == null }.thenBy { it.dueDate }
            )
            DocumentSortOption.AMOUNT_DESC -> documents.sortedWith(
                compareBy<DocumentEntity> { it.amount == null }.thenByDescending { it.amount }
            )
            DocumentSortOption.TITLE_ASC -> documents.sortedBy { it.title.lowercase() }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("documents_list_screen"),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Search and Sort Bar
        item {
            Column {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_text_field"),
                    placeholder = { Text("Search title, organization, text...") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Search, contentDescription = "Search")
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                CategoryFilterRow(
                    selectedCategory = selectedCategory,
                    onSelectCategory = { viewModel.setCategoryFilter(it) }
                )
            }
        }

        // Subheader: Document Count & Sort Selector
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${sortedDocuments.size} ${if (sortedDocuments.size == 1) "document" else "documents"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )

                Box {
                    OutlinedButton(
                        onClick = { sortMenuExpanded = true },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.size(6.dp))
                        Text(sortOption.displayName, style = MaterialTheme.typography.labelMedium)
                    }

                    DropdownMenu(
                        expanded = sortMenuExpanded,
                        onDismissRequest = { sortMenuExpanded = false }
                    ) {
                        DocumentSortOption.entries.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.displayName) },
                                onClick = {
                                    sortOption = option
                                    sortMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // Empty state or Document List
        if (sortedDocuments.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedCategory != null) {
                                "No matching documents found"
                            } else {
                                "No documents saved yet"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Try refining your search terms or filters"
                            else "Capture bills, receipts or letters with OCR scanning",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onScanClick) {
                            Text("Scan Document")
                        }
                    }
                }
            }
        } else {
            items(sortedDocuments, key = { it.id }) { doc ->
                DocumentCard(
                    document = doc,
                    onClick = { onDocumentClick(doc) },
                    onToggleDone = { viewModel.toggleActionCompleted(doc) }
                )
            }
        }
    }
}
