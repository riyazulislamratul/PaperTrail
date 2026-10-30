package com.example.cloud

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.auth.GoogleAuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class DriveFileType {
    IMAGE,
    CHAT_TRANSCRIPT,
    DOCUMENT_EXPORT
}

data class DriveVaultItem(
    val id: String,
    val title: String,
    val type: DriveFileType,
    val sizeBytes: Long,
    val timestamp: Long,
    val localFilePath: String,
    val driveCloudPath: String,
    val userEmail: String,
    val isSynced: Boolean = true
) {
    val formattedDate: String
        get() = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))

    val formattedSize: String
        get() = when {
            sizeBytes >= 1024 * 1024 -> String.format("%.1f MB", sizeBytes / (1024.0 * 1024.0))
            sizeBytes >= 1024 -> String.format("%.0f KB", sizeBytes / 1024.0)
            else -> "$sizeBytes B"
        }
}

class GoogleDriveVaultManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("papertrail_drive_vault", Context.MODE_PRIVATE)
    private val vaultDir = File(context.filesDir, "google_drive_vault").apply { mkdirs() }
    private val imagesDir = File(vaultDir, "images").apply { mkdirs() }
    private val chatsDir = File(vaultDir, "chats").apply { mkdirs() }

    private val _syncedItems = MutableStateFlow<List<DriveVaultItem>>(emptyList())
    val syncedItems: StateFlow<List<DriveVaultItem>> = _syncedItems.asStateFlow()

    init {
        loadItems()
    }

    private fun loadItems() {
        val jsonStr = prefs.getString("vault_items_json", "[]") ?: "[]"
        val list = mutableListOf<DriveVaultItem>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    DriveVaultItem(
                        id = obj.getString("id"),
                        title = obj.getString("title"),
                        type = DriveFileType.valueOf(obj.getString("type")),
                        sizeBytes = obj.getLong("sizeBytes"),
                        timestamp = obj.getLong("timestamp"),
                        localFilePath = obj.getString("localFilePath"),
                        driveCloudPath = obj.getString("driveCloudPath"),
                        userEmail = obj.optString("userEmail", GoogleAuthManager.DEFAULT_EMAIL),
                        isSynced = obj.optBoolean("isSynced", true)
                    )
                )
            }
        } catch (_: Exception) {}
        _syncedItems.value = list.sortedByDescending { it.timestamp }
    }

    private fun saveItems(items: List<DriveVaultItem>) {
        val array = JSONArray()
        for (item in items) {
            val obj = JSONObject().apply {
                put("id", item.id)
                put("title", item.title)
                put("type", item.type.name)
                put("sizeBytes", item.sizeBytes)
                put("timestamp", item.timestamp)
                put("localFilePath", item.localFilePath)
                put("driveCloudPath", item.driveCloudPath)
                put("userEmail", item.userEmail)
                put("isSynced", item.isSynced)
            }
            array.put(obj)
        }
        prefs.edit().putString("vault_items_json", array.toString()).apply()
        _syncedItems.value = items.sortedByDescending { it.timestamp }
    }

    suspend fun saveImageToDrive(
        bitmap: Bitmap,
        title: String,
        userEmail: String
    ): Result<DriveVaultItem> = withContext(Dispatchers.IO) {
        try {
            val safeTitle = title.replace(Regex("[^a-zA-Z0-9_]"), "_").take(30)
            val fileName = "img_${System.currentTimeMillis()}_$safeTitle.png"
            val file = File(imagesDir, fileName)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val item = DriveVaultItem(
                id = UUID.randomUUID().toString(),
                title = title,
                type = DriveFileType.IMAGE,
                sizeBytes = file.length(),
                timestamp = System.currentTimeMillis(),
                localFilePath = file.absolutePath,
                driveCloudPath = "Google Drive / PaperTrail Vault / Images / $fileName",
                userEmail = userEmail,
                isSynced = true
            )

            val current = _syncedItems.value.toMutableList()
            current.add(0, item)
            saveItems(current)

            Result.success(item)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveChatTranscriptToDrive(
        transcriptText: String,
        title: String,
        userEmail: String
    ): Result<DriveVaultItem> = withContext(Dispatchers.IO) {
        try {
            val safeTitle = title.replace(Regex("[^a-zA-Z0-9_]"), "_").take(30)
            val fileName = "chat_${System.currentTimeMillis()}_$safeTitle.txt"
            val file = File(chatsDir, fileName)

            file.writeText(transcriptText)

            val item = DriveVaultItem(
                id = UUID.randomUUID().toString(),
                title = title,
                type = DriveFileType.CHAT_TRANSCRIPT,
                sizeBytes = file.length(),
                timestamp = System.currentTimeMillis(),
                localFilePath = file.absolutePath,
                driveCloudPath = "Google Drive / PaperTrail Vault / Chats / $fileName",
                userEmail = userEmail,
                isSynced = true
            )

            val current = _syncedItems.value.toMutableList()
            current.add(0, item)
            saveItems(current)

            Result.success(item)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteItem(id: String) = withContext(Dispatchers.IO) {
        val current = _syncedItems.value.toMutableList()
        val item = current.find { it.id == id }
        if (item != null) {
            try {
                File(item.localFilePath).delete()
            } catch (_: Exception) {}
            current.remove(item)
            saveItems(current)
        }
    }
}
