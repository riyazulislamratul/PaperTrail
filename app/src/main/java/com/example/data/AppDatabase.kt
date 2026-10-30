package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

@Database(entities = [DocumentEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun documentDao(): DocumentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "papertrail_database"
                )
                .addCallback(DatabaseCallback(context.applicationContext))
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialDocuments(database.documentDao(), context)
                    }
                }
            }
        }

        private suspend fun populateInitialDocuments(dao: DocumentDao, context: Context) {
            val now = System.currentTimeMillis()
            val fiveDaysAhead = now + TimeUnit.DAYS.toMillis(5)
            val twelveDaysAhead = now + TimeUnit.DAYS.toMillis(12)
            val twoDaysAgo = now - TimeUnit.DAYS.toMillis(2)

            val seedDocs = listOf(
                DocumentEntity(
                    title = "Electric Utility Statement",
                    category = DocumentCategory.BILL.name,
                    organization = "Pacific Power & Light",
                    referenceNumber = "ACC-892401-09",
                    amount = 142.75,
                    currency = "$",
                    dueDate = fiveDaysAhead,
                    isDueDateConfirmed = true,
                    detectedDateCandidatesJson = "[{\"date\":\"Nov 15, 2026\",\"type\":\"DUE DATE\",\"confidence\":0.95}]",
                    requiredAction = "Pay bill before due date",
                    isActionCompleted = false,
                    extractedText = "PACIFIC POWER & LIGHT\nSTATEMENT OF ELECTRIC SERVICE\nAccount Number: ACC-892401-09\nBilling Date: Oct 28, 2026\nPayment Due Date: Nov 15, 2026\nTOTAL AMOUNT DUE: $142.75\nPlease pay online or remit payment before the due date to avoid a late fee.",
                    filePath = "",
                    fileType = "IMAGE",
                    notes = "Monthly residential electricity statement.",
                    hasReminder = true,
                    reminderTime = fiveDaysAhead - TimeUnit.DAYS.toMillis(1),
                    createdAt = now - TimeUnit.DAYS.toMillis(3)
                ),
                DocumentEntity(
                    title = "Vehicle Insurance Policy Renewal",
                    category = DocumentCategory.INSURANCE.name,
                    organization = "Summit Mutual Assurance",
                    referenceNumber = "POL-9923841",
                    amount = 389.00,
                    currency = "$",
                    dueDate = twelveDaysAhead,
                    isDueDateConfirmed = true,
                    detectedDateCandidatesJson = "[{\"date\":\"Nov 22, 2026\",\"type\":\"DUE DATE\",\"confidence\":0.92}]",
                    requiredAction = "Review policy coverage & renew",
                    isActionCompleted = false,
                    extractedText = "SUMMIT MUTUAL ASSURANCE\nAUTO POLICY RENEWAL DECLARATION\nPolicy Number: POL-9923841\nPolicyholder: Personal Vehicles\nTerm Effective: Dec 01, 2026\nRenewal Premium Due: $389.00\nPayment Deadline: Nov 22, 2026\nPlease review your updated comprehensive and collision deductible limits.",
                    filePath = "",
                    fileType = "IMAGE",
                    notes = "Semi-annual auto insurance renewal terms.",
                    hasReminder = false,
                    reminderTime = null,
                    createdAt = now - TimeUnit.DAYS.toMillis(1)
                ),
                DocumentEntity(
                    title = "Office Equipment Hardware Receipt",
                    category = DocumentCategory.RECEIPT.name,
                    organization = "Apex Tech Retail",
                    referenceNumber = "REC-441029",
                    amount = 89.99,
                    currency = "$",
                    dueDate = null,
                    isDueDateConfirmed = false,
                    detectedDateCandidatesJson = "[]",
                    requiredAction = "Save for business tax deduction",
                    isActionCompleted = true,
                    extractedText = "APEX TECH RETAIL #044\n100 Tech Blvd, Suite 200\nDate: Oct 28, 2026 14:22\nReceipt #: REC-441029\nItem: Ergonomic Vertical Wireless Mouse\nPrice: $84.90\nTax (6.0%): $5.09\nTotal: $89.99\nCard: VISA ENDING IN 9012\nThank you for shopping with Apex Tech!",
                    filePath = "",
                    fileType = "IMAGE",
                    notes = "Hardware purchase receipt for home workspace.",
                    hasReminder = false,
                    reminderTime = null,
                    createdAt = twoDaysAgo
                )
            )

            for (doc in seedDocs) {
                dao.insertDocument(doc)
            }
        }
    }
}
