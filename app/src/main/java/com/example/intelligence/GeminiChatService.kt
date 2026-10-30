package com.example.intelligence

import android.content.Context
import com.example.BuildConfig
import com.example.data.DocumentEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import java.util.concurrent.TimeUnit

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val attachedDocTitle: String? = null,
    val isLiveGemini: Boolean = false
)

class GeminiChatService(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val apiKey: String
        get() = try {
            BuildConfig.GEMINI_API_KEY
        } catch (_: Exception) {
            ""
        }

    suspend fun sendMessage(
        history: List<ChatMessage>,
        userMessage: String,
        attachedDoc: DocumentEntity? = null
    ): ChatMessage = withContext(Dispatchers.IO) {
        val promptWithContext = buildString {
            if (attachedDoc != null) {
                appendLine("[ATTACHED DOCUMENT CONTEXT]")
                appendLine("Title: ${attachedDoc.title}")
                appendLine("Category: ${attachedDoc.category}")
                appendLine("Organization: ${attachedDoc.organization ?: "Unknown"}")
                appendLine("Amount: ${attachedDoc.amount ?: "N/A"} ${attachedDoc.currency ?: ""}")
                appendLine("Due Date: ${attachedDoc.dueDate ?: "None"}")
                appendLine("Extracted Text: ${attachedDoc.extractedText.take(600)}")
                appendLine("---------------------------")
            }
            appendLine(userMessage)
        }

        if (apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY") {
            try {
                val liveResponse = callLiveGeminiApi(history, promptWithContext)
                if (liveResponse != null) {
                    return@withContext ChatMessage(
                        isUser = false,
                        text = liveResponse,
                        attachedDocTitle = attachedDoc?.title,
                        isLiveGemini = true
                    )
                }
            } catch (_: Exception) {
                // Fallback to offline intelligence engine
            }
        }

        // Intelligent local document advisor fallback
        val offlineAnswer = generateOfflineAdvisorResponse(userMessage, attachedDoc)
        ChatMessage(
            isUser = false,
            text = offlineAnswer,
            attachedDocTitle = attachedDoc?.title,
            isLiveGemini = false
        )
    }

    private fun callLiveGeminiApi(history: List<ChatMessage>, currentPrompt: String): String? {
        val modelName = "gemini-2.5-flash"
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        val contentsArray = JSONArray()

        // System Instruction / Context framing
        val systemInstruction = "You are PaperTrail AI Advisor, a world-class expert on personal documents, billing statements, invoices, tax records, warranty claims, and deadline management. Provide structured, actionable, and encouraging answers with clear bullet points, monetary tips, and legal preservation advice."

        val firstContent = JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().put(JSONObject().put("text", "System directive: $systemInstruction")))
        }
        contentsArray.put(firstContent)

        // Previous turns
        val recentHistory = history.takeLast(6)
        for (msg in recentHistory) {
            val role = if (msg.isUser) "user" else "model"
            contentsArray.put(
                JSONObject().apply {
                    put("role", role)
                    put("parts", JSONArray().put(JSONObject().put("text", msg.text)))
                }
            )
        }

        // Current user prompt
        contentsArray.put(
            JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().put(JSONObject().put("text", currentPrompt)))
            }
        )

        val requestBody = JSONObject().apply {
            put("contents", contentsArray)
        }.toString()

        val request = Request.Builder()
            .url(url)
            .post(requestBody.toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            val bodyString = response.body?.string() ?: return null
            val json = JSONObject(bodyString)
            val candidates = json.optJSONArray("candidates") ?: return null
            if (candidates.length() == 0) return null
            val content = candidates.getJSONObject(0).optJSONObject("content") ?: return null
            val parts = content.optJSONArray("parts") ?: return null
            if (parts.length() == 0) return null
            return parts.getJSONObject(0).optString("text")
        }
    }

    private fun generateOfflineAdvisorResponse(prompt: String, doc: DocumentEntity?): String {
        val query = prompt.lowercase()

        if (doc != null) {
            val amountStr = doc.amount?.let { "$$it" } ?: "the listed charge"
            val orgStr = doc.organization ?: "the issuer"
            val dateStr = doc.dueDate ?: "the upcoming deadline"

            if (query.contains("dispute") || query.contains("wrong") || query.contains("charge") || query.contains("error")) {
                return """
                    ### Formal Billing Dispute Strategy for **${doc.title}**
                    
                    **Target Organization:** $orgStr
                    **Disputed Amount:** $amountStr
                    
                    Here is a structured draft you can send or use during a phone inquiry:
                    
                    > **Subject:** Billing Inconsistency Notice – Ref #${doc.referenceNumber ?: "DOC-VAULT"}
                    > 
                    > Dear Customer Support,
                    > 
                    > I am writing to formally contest the charges of **$amountStr** listed on the statement dated **$dateStr**. Upon auditing the attached documentation, the charge does not align with my records.
                    > 
                    > Please place an immediate administrative hold on collection while this discrepancy is reviewed.
                    
                    **Recommended Action Checklist:**
                    * Keep a digital timestamped copy stored in your PaperTrail Google Drive Vault.
                    * Note the representative's employee badge ID and reference ticket number.
                    * Request an itemized ledger before authorizing any partial settlement.
                """.trimIndent()
            }

            if (query.contains("tax") || query.contains("deduct") || query.contains("write off")) {
                return """
                    ### Tax Record Guidance for **${doc.title}**
                    
                    * **Retention Window:** IRS and standard tax statutes recommend archiving business and deductible expense records for **3 to 7 years**.
                    * **Classification:** Classified under category **${doc.category}**.
                    * **Auditing Best Practice:** Ensure both the receipt and bank statement entry are paired in your PaperTrail Vault to withstand an audit.
                    * **Action:** Export this document to your Google Drive folder for safe annual tax compilation.
                """.trimIndent()
            }
        }

        return when {
            query.contains("deadline") || query.contains("remind") || query.contains("due") -> {
                """
                    ### PaperTrail Deadline Management Protocol
                    
                    1. **Triage by Urgency:**
                       * **Overdue:** Take immediate phone action or make settlement payment.
                       * **Due in 48 Hours:** Verify fund availability and set an alarm.
                       * **Upcoming (7+ Days):** Schedule automated payments or calendar alerts.
                    2. **Preserve Receipts:** Immediately mark items as *Action Completed* in PaperTrail after paying to stop overdue alerts.
                    3. **Auto-Backup:** Sync all proof-of-payment receipts into your Google Drive Vault folder.
                """.trimIndent()
            }
            query.contains("tax") || query.contains("receipt") -> {
                """
                    ### Document Archival & Retention Guidelines
                    
                    * **Tax Invoices & Receipts:** Keep for a minimum of **3 years** (or **7 years** if claiming asset depreciation or capital loss).
                    * **Warranty & Purchase Slips:** Keep for the duration of the manufacturer warranty plus 60 days.
                    * **Medical & Insurance Records:** Keep for **5 years** from date of service.
                    * **Property & Vehicle Titles:** Retain indefinitely until the asset is fully transferred or sold.
                    
                    *Tip:* PaperTrail scans documents into app-private storage and auto-uploads them to your personal Google Drive for permanence.
                """.trimIndent()
            }
            query.contains("dispute") || query.contains("letter") -> {
                """
                    ### Universal Bill Dispute Template
                    
                    **1. Essential Information to Include:**
                    * Account or Reference Number
                    * Disputed line-item amount and date of invoice
                    * Clear factual explanation of the error
                    * Request for written confirmation of charge adjustment
                    
                    **2. Next Steps:**
                    * Send via certified email or support ticket with attachment.
                    * Set a 14-day reminder in PaperTrail for follow-up response.
                """.trimIndent()
            }
            query.contains("organize") || query.contains("paperwork") || query.contains("tips") -> {
                """
                    ### 4-Step Paperless Organization Workflow
                    
                    1. **Capture Instantly:** Never let physical mail sit on tables. Snap it using PaperTrail's Camera OCR immediately upon arrival.
                    2. **Auto-Categorize:** Group into Bills, Receipts, Insurance, Tax, and Notices.
                    3. **Tag Critical Deadlines:** Ensure every bill has an extracted due date so notifications alert you before late fees apply.
                    4. **Cloud Vault Backup:** Keep your Google Drive Vault connected so you never lose documents when changing devices.
                """.trimIndent()
            }
            else -> {
                """
                    ### PaperTrail Document Assistant
                    
                    I can assist you with:
                    * **Analyzing Documents:** Attach any scanned document to audit terms, penalties, or payment methods.
                    * **Dispute Letters:** Generate formal claim notices for inaccurate bills or fees.
                    * **Tax & Retention:** Rules on how long to store receipts and certificates.
                    * **Cloud Sync:** Archiving your documents, custom visual stamps, and transcripts directly to your Google Drive.
                    
                    What document or financial task would you like help with today?
                """.trimIndent()
            }
        }
    }
}
