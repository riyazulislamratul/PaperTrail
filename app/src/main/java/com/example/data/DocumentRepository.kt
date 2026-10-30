package com.example.data

import android.content.Context
import com.example.PaperTrailApplication
import com.example.util.FileStorageHelper
import kotlinx.coroutines.flow.Flow

class DocumentRepository(
    private val documentDao: DocumentDao,
    private val context: Context
) {

    val allDocuments: Flow<List<DocumentEntity>> = documentDao.getAllDocuments()
    val activeDeadlines: Flow<List<DocumentEntity>> = documentDao.getActiveDeadlines()

    fun getDocumentById(id: Long): Flow<DocumentEntity?> = documentDao.getDocumentById(id)

    suspend fun getDocumentByIdDirect(id: Long): DocumentEntity? = documentDao.getDocumentByIdDirect(id)

    fun searchDocuments(query: String): Flow<List<DocumentEntity>> = documentDao.searchDocuments(query)

    fun getDocumentsByCategory(category: String): Flow<List<DocumentEntity>> = documentDao.getDocumentsByCategory(category)

    fun getUpcomingReminders(currentTime: Long): Flow<List<DocumentEntity>> = documentDao.getUpcomingReminders(currentTime)

    suspend fun insertDocument(doc: DocumentEntity, reminderTriggerMillis: Long? = null): Long {
        val insertedId = documentDao.insertDocument(doc)
        if (reminderTriggerMillis != null && reminderTriggerMillis > System.currentTimeMillis()) {
            val app = context.applicationContext as? PaperTrailApplication
            val amountStr = if (doc.amount != null) "${doc.currency ?: "$"}${doc.amount}" else null
            app?.reminderManager?.scheduleReminder(
                docId = insertedId,
                title = doc.title,
                amountText = amountStr,
                triggerTimeMillis = reminderTriggerMillis,
                dueDateMillis = doc.dueDate
            )
            documentDao.updateDocument(
                doc.copy(
                    id = insertedId,
                    hasReminder = true,
                    reminderTime = reminderTriggerMillis
                )
            )
        }
        return insertedId
    }

    suspend fun updateDocument(doc: DocumentEntity) {
        documentDao.updateDocument(doc.copy(updatedAt = System.currentTimeMillis()))
        val app = context.applicationContext as? PaperTrailApplication
        if (doc.hasReminder && doc.reminderTime != null && doc.reminderTime > System.currentTimeMillis()) {
            val amountStr = if (doc.amount != null) "${doc.currency ?: "$"}${doc.amount}" else null
            app?.reminderManager?.scheduleReminder(
                docId = doc.id,
                title = doc.title,
                amountText = amountStr,
                triggerTimeMillis = doc.reminderTime,
                dueDateMillis = doc.dueDate
            )
        } else if (!doc.hasReminder) {
            app?.reminderManager?.cancelReminder(doc.id)
        }
    }

    suspend fun deleteDocument(doc: DocumentEntity) {
        val app = context.applicationContext as? PaperTrailApplication
        app?.reminderManager?.cancelReminder(doc.id)
        FileStorageHelper.deleteFile(doc.filePath)
        documentDao.deleteDocument(doc)
    }

    suspend fun toggleActionCompleted(docId: Long, completed: Boolean) {
        val doc = documentDao.getDocumentByIdDirect(docId) ?: return
        val app = context.applicationContext as? PaperTrailApplication
        if (completed) {
            app?.reminderManager?.cancelReminder(docId)
        }
        documentDao.updateDocument(
            doc.copy(
                isActionCompleted = completed,
                hasReminder = if (completed) false else doc.hasReminder,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun setReminder(docId: Long, reminderMillis: Long) {
        val doc = documentDao.getDocumentByIdDirect(docId) ?: return
        val app = context.applicationContext as? PaperTrailApplication
        val amountStr = if (doc.amount != null) "${doc.currency ?: "$"}${doc.amount}" else null
        app?.reminderManager?.scheduleReminder(
            docId = doc.id,
            title = doc.title,
            amountText = amountStr,
            triggerTimeMillis = reminderMillis,
            dueDateMillis = doc.dueDate
        )
        documentDao.updateDocument(
            doc.copy(
                hasReminder = true,
                reminderTime = reminderMillis,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun cancelReminder(docId: Long) {
        val doc = documentDao.getDocumentByIdDirect(docId) ?: return
        val app = context.applicationContext as? PaperTrailApplication
        app?.reminderManager?.cancelReminder(docId)
        documentDao.updateDocument(
            doc.copy(
                hasReminder = false,
                reminderTime = null,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}
