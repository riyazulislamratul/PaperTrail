package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DocumentCategory(val displayName: String, val iconName: String) {
    BILL("Bill & Utilities", "ReceiptLong"),
    INVOICE("Invoice", "RequestQuote"),
    RECEIPT("Receipt", "ShoppingBag"),
    BANK_LETTER("Bank Statement", "AccountBalance"),
    INSURANCE("Insurance Policy", "HealthAndSafety"),
    OFFICIAL_NOTICE("Official Notice", "Policy"),
    OTHER("Other Document", "Description");

    companion object {
        fun fromString(value: String): DocumentCategory {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OTHER
        }
    }
}

@Entity(tableName = "documents")
data class DocumentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val category: String = DocumentCategory.OTHER.name,
    val organization: String? = null,
    val referenceNumber: String? = null,
    val amount: Double? = null,
    val currency: String? = "$",
    val dueDate: Long? = null,
    val isDueDateConfirmed: Boolean = false,
    val detectedDateCandidatesJson: String = "[]",
    val requiredAction: String? = null,
    val isActionCompleted: Boolean = false,
    val extractedText: String = "",
    val filePath: String = "",
    val fileType: String = "IMAGE", // "IMAGE" or "PDF"
    val notes: String = "",
    val hasReminder: Boolean = false,
    val reminderTime: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
