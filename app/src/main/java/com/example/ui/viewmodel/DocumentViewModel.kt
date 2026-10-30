package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DocumentCategory
import com.example.data.DocumentEntity
import com.example.data.DocumentRepository
import com.example.intelligence.DocumentIntelligence
import com.example.intelligence.ExtractedIntelligence
import com.example.ocr.OcrManager
import com.example.util.FileStorageHelper
import com.example.util.PdfHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

enum class AppScreen {
    DASHBOARD,
    DOCUMENTS,
    REMINDERS,
    SCAN,
    REVIEW_EXTRACTED,
    DETAIL
}

data class TempScanData(
    val savedFile: File,
    val previewBitmap: Bitmap,
    val fileType: String,
    val rawText: String,
    val intelligence: ExtractedIntelligence,
    val title: String,
    val category: DocumentCategory,
    val organization: String,
    val referenceNumber: String,
    val amountString: String,
    val currency: String,
    val dueDate: Long?,
    val isDueDateConfirmed: Boolean,
    val requiredAction: String,
    val notes: String
)

class DocumentViewModel(private val repository: DocumentRepository) : ViewModel() {

    private val _currentScreen = MutableStateFlow(AppScreen.DASHBOARD)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _screenHistory = mutableListOf(AppScreen.DASHBOARD)

    private val _selectedDocument = MutableStateFlow<DocumentEntity?>(null)
    val selectedDocument: StateFlow<DocumentEntity?> = _selectedDocument.asStateFlow()

    private val _tempScanData = MutableStateFlow<TempScanData?>(null)
    val tempScanData: StateFlow<TempScanData?> = _tempScanData.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow<String?>(null)
    val selectedCategoryFilter: StateFlow<String?> = _selectedCategoryFilter.asStateFlow()

    private val _isOcrProcessing = MutableStateFlow(false)
    val isOcrProcessing: StateFlow<Boolean> = _isOcrProcessing.asStateFlow()

    private val _ocrErrorMessage = MutableStateFlow<String?>(null)
    val ocrErrorMessage: StateFlow<String?> = _ocrErrorMessage.asStateFlow()

    val allDocuments: StateFlow<List<DocumentEntity>> = repository.allDocuments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeDeadlines: StateFlow<List<DocumentEntity>> = repository.activeDeadlines
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredDocuments: StateFlow<List<DocumentEntity>> = combine(
        allDocuments,
        _searchQuery,
        _selectedCategoryFilter
    ) { docs, query, category ->
        docs.filter { doc ->
            val matchesQuery = query.isBlank() ||
                    doc.title.contains(query, ignoreCase = true) ||
                    (doc.organization?.contains(query, ignoreCase = true) == true) ||
                    (doc.referenceNumber?.contains(query, ignoreCase = true) == true) ||
                    doc.extractedText.contains(query, ignoreCase = true) ||
                    doc.notes.contains(query, ignoreCase = true)

            val matchesCategory = category == null || doc.category.equals(category, ignoreCase = true)
            matchesQuery && matchesCategory
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun navigateTo(screen: AppScreen) {
        if (_currentScreen.value != screen) {
            _screenHistory.add(screen)
            _currentScreen.value = screen
        }
    }

    fun navigateBack(): Boolean {
        if (_screenHistory.size > 1) {
            _screenHistory.removeAt(_screenHistory.size - 1)
            _currentScreen.value = _screenHistory.last()
            return true
        }
        return false
    }

    fun selectDocument(doc: DocumentEntity) {
        _selectedDocument.value = doc
        navigateTo(AppScreen.DETAIL)
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setCategoryFilter(category: String?) {
        _selectedCategoryFilter.value = category
    }

    fun clearOcrError() {
        _ocrErrorMessage.value = null
    }

    fun getDocumentFlow(docId: Long) = repository.getDocumentById(docId)

    fun processCapturedBitmap(bitmap: Bitmap, context: Context, onExtracted: () -> Unit = {}) {
        viewModelScope.launch {
            _isOcrProcessing.value = true
            _ocrErrorMessage.value = null
            try {
                val savedFile = FileStorageHelper.saveBitmapToFile(context, bitmap, prefix = "scan")
                val extractedText = OcrManager.recognizeTextFromBitmap(bitmap)
                val intelligence = DocumentIntelligence.analyzeText(extractedText)

                _tempScanData.value = TempScanData(
                    savedFile = savedFile,
                    previewBitmap = bitmap,
                    fileType = "IMAGE",
                    rawText = extractedText,
                    intelligence = intelligence,
                    title = intelligence.suggestedTitle,
                    category = intelligence.category,
                    organization = intelligence.organization ?: "",
                    referenceNumber = intelligence.referenceNumber ?: "",
                    amountString = if (intelligence.amount != null) String.format("%.2f", intelligence.amount) else "",
                    currency = intelligence.currency ?: "$",
                    dueDate = intelligence.suggestedDueDate,
                    isDueDateConfirmed = false, // Always requires explicit confirmation
                    requiredAction = intelligence.suggestedAction ?: "",
                    notes = ""
                )
                navigateTo(AppScreen.REVIEW_EXTRACTED)
                onExtracted()
            } catch (e: Exception) {
                e.printStackTrace()
                _ocrErrorMessage.value = "Failed to process image: ${e.localizedMessage ?: "Unknown error"}"
            } finally {
                _isOcrProcessing.value = false
            }
        }
    }

    fun processImageUri(uri: Uri, context: Context, onExtracted: () -> Unit = {}) {
        viewModelScope.launch {
            _isOcrProcessing.value = true
            _ocrErrorMessage.value = null
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }
                if (bitmap == null) {
                    _ocrErrorMessage.value = "Could not decode selected image file."
                    return@launch
                }
                processCapturedBitmap(bitmap, context, onExtracted)
            } catch (e: Exception) {
                e.printStackTrace()
                _ocrErrorMessage.value = "Failed to load image: ${e.localizedMessage}"
                _isOcrProcessing.value = false
            }
        }
    }

    fun processPdfUri(uri: Uri, context: Context, onExtracted: () -> Unit = {}) {
        viewModelScope.launch {
            _isOcrProcessing.value = true
            _ocrErrorMessage.value = null
            try {
                val savedFile = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        FileStorageHelper.savePdfStream(context, input)
                    }
                }
                if (savedFile == null) {
                    _ocrErrorMessage.value = "Failed to import PDF file."
                    return@launch
                }

                val renderedBitmaps = PdfHelper.renderPdfPagesToBitmaps(savedFile, maxPages = 2)
                if (renderedBitmaps.isEmpty()) {
                    _ocrErrorMessage.value = "Could not render PDF pages."
                    return@launch
                }

                val extractedText = OcrManager.recognizeTextFromBitmaps(renderedBitmaps)
                val intelligence = DocumentIntelligence.analyzeText(extractedText)

                _tempScanData.value = TempScanData(
                    savedFile = savedFile,
                    previewBitmap = renderedBitmaps.first(),
                    fileType = "PDF",
                    rawText = extractedText,
                    intelligence = intelligence,
                    title = intelligence.suggestedTitle,
                    category = intelligence.category,
                    organization = intelligence.organization ?: "",
                    referenceNumber = intelligence.referenceNumber ?: "",
                    amountString = if (intelligence.amount != null) String.format("%.2f", intelligence.amount) else "",
                    currency = intelligence.currency ?: "$",
                    dueDate = intelligence.suggestedDueDate,
                    isDueDateConfirmed = false,
                    requiredAction = intelligence.suggestedAction ?: "",
                    notes = ""
                )
                navigateTo(AppScreen.REVIEW_EXTRACTED)
                onExtracted()
            } catch (e: Exception) {
                e.printStackTrace()
                _ocrErrorMessage.value = "Failed to process PDF: ${e.localizedMessage}"
            } finally {
                _isOcrProcessing.value = false
            }
        }
    }

    fun updateTempScanData(updater: (TempScanData) -> TempScanData) {
        _tempScanData.value?.let { current ->
            _tempScanData.value = updater(current)
        }
    }

    fun saveCurrentScan(reminderTriggerDaysBefore: Int? = null, onSaved: (Long) -> Unit = {}) {
        val data = _tempScanData.value ?: return
        viewModelScope.launch {
            val amountParsed = data.amountString.toDoubleOrNull()
            val reminderTime = if (reminderTriggerDaysBefore != null && data.dueDate != null) {
                val trigger = data.dueDate - TimeUnit.DAYS.toMillis(reminderTriggerDaysBefore.toLong())
                if (trigger > System.currentTimeMillis()) trigger else null
            } else null

            val doc = DocumentEntity(
                title = data.title.ifBlank { "Untitled Document" },
                category = data.category.name,
                organization = data.organization.ifBlank { null },
                referenceNumber = data.referenceNumber.ifBlank { null },
                amount = amountParsed,
                currency = data.currency,
                dueDate = data.dueDate,
                isDueDateConfirmed = data.isDueDateConfirmed,
                detectedDateCandidatesJson = "[]",
                requiredAction = data.requiredAction.ifBlank { null },
                isActionCompleted = false,
                extractedText = data.rawText,
                filePath = data.savedFile.absolutePath,
                fileType = data.fileType,
                notes = data.notes,
                hasReminder = reminderTime != null,
                reminderTime = reminderTime,
                createdAt = System.currentTimeMillis()
            )

            val insertedId = repository.insertDocument(doc, reminderTime)
            val insertedDoc = doc.copy(id = insertedId)
            _selectedDocument.value = insertedDoc
            _tempScanData.value = null
            navigateTo(AppScreen.DETAIL)
            onSaved(insertedId)
        }
    }

    fun toggleActionCompleted(doc: DocumentEntity) {
        viewModelScope.launch {
            val nextState = !doc.isActionCompleted
            repository.toggleActionCompleted(doc.id, nextState)
            _selectedDocument.value = doc.copy(isActionCompleted = nextState)
        }
    }

    fun deleteDocument(doc: DocumentEntity, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            repository.deleteDocument(doc)
            if (_selectedDocument.value?.id == doc.id) {
                _selectedDocument.value = null
            }
            navigateBack()
            onDeleted()
        }
    }

    fun snoozeReminder(doc: DocumentEntity, days: Int) {
        viewModelScope.launch {
            val newTrigger = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(days.toLong())
            repository.setReminder(doc.id, newTrigger)
            _selectedDocument.value = doc.copy(hasReminder = true, reminderTime = newTrigger)
        }
    }

    fun setReminder(doc: DocumentEntity, triggerMillis: Long) {
        viewModelScope.launch {
            repository.setReminder(doc.id, triggerMillis)
            _selectedDocument.value = doc.copy(hasReminder = true, reminderTime = triggerMillis)
        }
    }

    fun cancelReminder(doc: DocumentEntity) {
        viewModelScope.launch {
            repository.cancelReminder(doc.id)
            _selectedDocument.value = doc.copy(hasReminder = false, reminderTime = null)
        }
    }

    fun updateDocumentDetails(updatedDoc: DocumentEntity) {
        viewModelScope.launch {
            repository.updateDocument(updatedDoc)
            _selectedDocument.value = updatedDoc
        }
    }
}

class DocumentViewModelFactory(private val repository: DocumentRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DocumentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DocumentViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
