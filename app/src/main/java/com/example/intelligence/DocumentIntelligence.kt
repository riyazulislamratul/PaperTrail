package com.example.intelligence

import com.example.data.DocumentCategory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern

data class DateCandidate(
    val rawText: String,
    val epochMillis: Long,
    val label: String,
    val isDueLikely: Boolean
)

data class ExtractedIntelligence(
    val suggestedTitle: String,
    val category: DocumentCategory,
    val organization: String?,
    val referenceNumber: String?,
    val amount: Double?,
    val currency: String?,
    val suggestedDueDate: Long?,
    val dateCandidates: List<DateCandidate>,
    val suggestedAction: String?,
    val confidenceNotes: List<String>
)

object DocumentIntelligence {

    fun analyzeText(rawText: String): ExtractedIntelligence {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotBlank() }
        val fullTextLower = rawText.lowercase(Locale.ROOT)

        val category = detectCategory(fullTextLower)
        val organization = detectOrganization(lines)
        val referenceNumber = detectReferenceNumber(lines, rawText)
        val (amount, currency) = detectAmountAndCurrency(lines, rawText)
        val dateCandidates = detectDateCandidates(lines, rawText)

        // Select the most probable due date candidate
        val bestDueDate = dateCandidates.firstOrNull { it.isDueLikely }?.epochMillis
            ?: dateCandidates.firstOrNull()?.epochMillis

        val suggestedTitle = buildSuggestedTitle(category, organization, referenceNumber)
        val suggestedAction = buildSuggestedAction(category, amount, currency, bestDueDate)

        val confidenceNotes = mutableListOf<String>()
        if (amount != null) {
            confidenceNotes.add("Detected financial amount: $currency${String.format(Locale.US, "%.2f", amount)}")
        } else {
            confidenceNotes.add("No financial total detected")
        }

        if (bestDueDate != null) {
            val dateStr = SimpleDateFormat("MMM dd, yyyy", Locale.US).format(Date(bestDueDate))
            confidenceNotes.add("Suggested deadline: $dateStr (Requires confirmation)")
        } else {
            confidenceNotes.add("No explicit deadline detected")
        }

        if (!organization.isNullOrBlank()) {
            confidenceNotes.add("Detected organization: $organization")
        }

        return ExtractedIntelligence(
            suggestedTitle = suggestedTitle,
            category = category,
            organization = organization,
            referenceNumber = referenceNumber,
            amount = amount,
            currency = currency ?: "$",
            suggestedDueDate = bestDueDate,
            dateCandidates = dateCandidates,
            suggestedAction = suggestedAction,
            confidenceNotes = confidenceNotes
        )
    }

    private fun detectCategory(text: String): DocumentCategory {
        val scores = mutableMapOf<DocumentCategory, Int>()

        fun score(cat: DocumentCategory, keywords: List<String>, weight: Int = 1) {
            var s = 0
            for (kw in keywords) {
                if (text.contains(kw)) s += weight
            }
            scores[cat] = (scores[cat] ?: 0) + s
        }

        // Bill / Utilities
        score(DocumentCategory.BILL, listOf(
            "utility", "electric", "power", "water", "gas", "bill", "billing", "amount due",
            "due date", "minimum payment", "past due", "pay online", "account balance",
            "service period", "meter reading", "telecom", "broadband", "internet"
        ), 2)

        // Invoice
        score(DocumentCategory.INVOICE, listOf(
            "invoice", "invoice no", "invoice #", "bill to", "ship to", "purchase order",
            "po #", "subtotal", "vat #", "unit price", "payment terms", "net 30", "net 15",
            "remit to", "tax invoice"
        ), 3)

        // Receipt
        score(DocumentCategory.RECEIPT, listOf(
            "receipt", "cashier", "store #", "merchant", "total items", "visa ending",
            "mastercard", "amex", "change due", "sub total", "pos", "terminal #", "sales draft"
        ), 3)

        // Bank Letter / Statement
        score(DocumentCategory.BANK_LETTER, listOf(
            "bank", "checking", "savings", "statement period", "routing number",
            "direct deposit", "wire transfer", "overdraft", "annual percentage yield", "apy",
            "credit union", "available balance", "statement of account"
        ), 3)

        // Insurance
        score(DocumentCategory.INSURANCE, listOf(
            "insurance", "policy", "policyholder", "coverage", "premium", "deductible",
            "claim", "co-pay", "declaration page", "underwriter", "mutual assurance",
            "liability", "collision", "comprehensive"
        ), 3)

        // Official Notice
        score(DocumentCategory.OFFICIAL_NOTICE, listOf(
            "internal revenue", "irs", "department of", "official notice", "notice date",
            "form 1040", "form 1099", "form w-2", "penalty", "summons", "court",
            "treasury", "compliance", "government", "assessor", "property tax"
        ), 3)

        val bestEntry = scores.maxByOrNull { it.value }
        return if (bestEntry != null && bestEntry.value >= 2) {
            bestEntry.key
        } else {
            DocumentCategory.OTHER
        }
    }

    private fun detectOrganization(lines: List<String>): String? {
        val orgKeywords = listOf(
            "inc", "llc", "corp", "corporation", "ltd", "bank", "energy", "power",
            "insurance", "assurance", "health", "hospital", "city of", "department",
            "services", "store", "retail", "solutions", "group", "technologies"
        )

        // Look at top 6 lines first
        for (i in 0 until minOf(lines.size, 6)) {
            val line = lines[i]
            if (line.length in 3..50) {
                val lower = line.lowercase(Locale.ROOT)
                if (orgKeywords.any { lower.contains(it) }) {
                    return cleanOrgName(line)
                }
            }
        }

        // If no explicit keyword, check if the first non-empty line looks like a header/company name
        val firstLine = lines.firstOrNull { it.length in 4..40 && !it.any { c -> c.isDigit() } }
        return firstLine?.let { cleanOrgName(it) }
    }

    private fun cleanOrgName(raw: String): String {
        return raw.replace(Regex("^(statement of|welcome to|bill from|invoice from)\\s*", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    private fun detectReferenceNumber(lines: List<String>, fullText: String): String? {
        val pattern = Pattern.compile(
            "(?i)(?:account|acct|acc|invoice|inv|policy|pol|receipt|rec|order|notice|ref|reference)\\s*(?:#|no\\.?|number|id)?[:\\s]+([A-Z0-9\\-_/]{4,24})"
        )
        for (line in lines) {
            val matcher = pattern.matcher(line)
            if (matcher.find()) {
                val found = matcher.group(1)?.trim()
                if (!found.isNullOrBlank() && found.any { it.isDigit() }) {
                    return found
                }
            }
        }
        val matcher = pattern.matcher(fullText)
        if (matcher.find()) {
            return matcher.group(1)?.trim()
        }
        return null
    }

    private fun detectAmountAndCurrency(lines: List<String>, fullText: String): Pair<Double?, String?> {
        // High priority lines: containing "total", "amount due", "balance", "pay", "charge"
        val totalLineRegex = Regex("(?i)(amount due|total due|balance due|total amount|grand total|pay this amount|total|balance)")
        val currencySymbolRegex = Regex("([$€£¥])\\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{2})?)")
        val currencyWordRegex = Regex("(?i)([0-9]{1,3}(?:,[0-9]{3})*(?:\\.[0-9]{2}))\\s*(usd|eur|gbp)")

        var bestAmount: Double? = null
        var bestCurrency: String? = "$"

        // Check lines that specifically mention total / amount due first
        for (line in lines) {
            if (totalLineRegex.containsMatchIn(line)) {
                val match = currencySymbolRegex.find(line)
                if (match != null) {
                    val symbol = match.groupValues[1]
                    val numStr = match.groupValues[2].replace(",", "")
                    val parsed = numStr.toDoubleOrNull()
                    if (parsed != null && parsed > 0) {
                        return Pair(parsed, symbol)
                    }
                }
                val wordMatch = currencyWordRegex.find(line)
                if (wordMatch != null) {
                    val numStr = wordMatch.groupValues[1].replace(",", "")
                    val word = wordMatch.groupValues[2].uppercase()
                    val parsed = numStr.toDoubleOrNull()
                    if (parsed != null && parsed > 0) {
                        val symbol = when (word) {
                            "EUR" -> "€"
                            "GBP" -> "£"
                            else -> "$"
                        }
                        return Pair(parsed, symbol)
                    }
                }
            }
        }

        // Fallback: search anywhere in full text for amounts
        val matches = currencySymbolRegex.findAll(fullText)
        for (m in matches) {
            val symbol = m.groupValues[1]
            val numStr = m.groupValues[2].replace(",", "")
            val parsed = numStr.toDoubleOrNull()
            if (parsed != null && parsed > 0) {
                // Keep the largest amount found (often the total)
                if (bestAmount == null || parsed > bestAmount) {
                    bestAmount = parsed
                    bestCurrency = symbol
                }
            }
        }

        return Pair(bestAmount, bestCurrency)
    }

    private fun detectDateCandidates(lines: List<String>, fullText: String): List<DateCandidate> {
        val candidates = mutableListOf<DateCandidate>()
        val dueContextRegex = Regex("(?i)(due|payment due|pay by|deadline|expires|reply by|before|pay before)")
        val statementContextRegex = Regex("(?i)(statement date|billing date|issue date|invoice date|date of notice)")

        // Date formats:
        // 1. "Month DD, YYYY" e.g. "Nov 15, 2026", "November 15, 2026"
        val monthDayYear = Pattern.compile(
            "(?i)\\b(Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)\\s+(\\d{1,2})(?:st|nd|rd|th)?,?\\s+(20\\d\\d)\\b"
        )
        // 2. "DD Month YYYY" e.g. "15 Nov 2026"
        val dayMonthYear = Pattern.compile(
            "(?i)\\b(\\d{1,2})(?:st|nd|rd|th)?\\s+(Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?),?\\s+(20\\d\\d)\\b"
        )
        // 3. "YYYY-MM-DD" or "MM/DD/YYYY"
        val numericDate = Pattern.compile(
            "\\b(20\\d\\d)[-/.](0?[1-9]|1[0-2])[-/.](0?[1-9]|[12]\\d|3[01])\\b"
        )
        val usNumericDate = Pattern.compile(
            "\\b(0?[1-9]|1[0-2])[-/.](0?[1-9]|[12]\\d|3[01])[-/.](20\\d\\d)\\b"
        )

        for (line in lines) {
            val isDueContext = dueContextRegex.containsMatchIn(line)
            val isStatementContext = statementContextRegex.containsMatchIn(line)

            // Try Month Day Year
            val m1 = monthDayYear.matcher(line)
            while (m1.find()) {
                val raw = m1.group(0) ?: ""
                val millis = parseMonthDayYear(m1.group(1), m1.group(2), m1.group(3))
                if (millis != null && candidates.none { it.epochMillis == millis }) {
                    candidates.add(
                        DateCandidate(
                            rawText = raw,
                            epochMillis = millis,
                            label = if (isDueContext) "Due Date" else if (isStatementContext) "Statement Date" else "Date",
                            isDueLikely = isDueContext
                        )
                    )
                }
            }

            // Try Day Month Year
            val m2 = dayMonthYear.matcher(line)
            while (m2.find()) {
                val raw = m2.group(0) ?: ""
                val millis = parseMonthDayYear(m2.group(2), m2.group(1), m2.group(3))
                if (millis != null && candidates.none { it.epochMillis == millis }) {
                    candidates.add(
                        DateCandidate(
                            rawText = raw,
                            epochMillis = millis,
                            label = if (isDueContext) "Due Date" else if (isStatementContext) "Statement Date" else "Date",
                            isDueLikely = isDueContext
                        )
                    )
                }
            }

            // Try US Numeric MM/DD/YYYY
            val m3 = usNumericDate.matcher(line)
            while (m3.find()) {
                val raw = m3.group(0) ?: ""
                val millis = parseNumeric(m3.group(3), m3.group(1), m3.group(2))
                if (millis != null && candidates.none { it.epochMillis == millis }) {
                    candidates.add(
                        DateCandidate(
                            rawText = raw,
                            epochMillis = millis,
                            label = if (isDueContext) "Due Date" else if (isStatementContext) "Statement Date" else "Date",
                            isDueLikely = isDueContext
                        )
                    )
                }
            }

            // Try ISO Numeric YYYY-MM-DD
            val m4 = numericDate.matcher(line)
            while (m4.find()) {
                val raw = m4.group(0) ?: ""
                val millis = parseNumeric(m4.group(1), m4.group(2), m4.group(3))
                if (millis != null && candidates.none { it.epochMillis == millis }) {
                    candidates.add(
                        DateCandidate(
                            rawText = raw,
                            epochMillis = millis,
                            label = if (isDueContext) "Due Date" else if (isStatementContext) "Statement Date" else "Date",
                            isDueLikely = isDueContext
                        )
                    )
                }
            }
        }

        // Sort so due-likely candidates are first
        return candidates.sortedByDescending { it.isDueLikely }
    }

    private fun parseMonthDayYear(monthStr: String?, dayStr: String?, yearStr: String?): Long? {
        if (monthStr == null || dayStr == null || yearStr == null) return null
        return try {
            val month = when (monthStr.take(3).lowercase(Locale.ROOT)) {
                "jan" -> 0; "feb" -> 1; "mar" -> 2; "apr" -> 3; "may" -> 4; "jun" -> 5;
                "jul" -> 6; "aug" -> 7; "sep" -> 8; "oct" -> 9; "nov" -> 10; "dec" -> 11
                else -> return null
            }
            val day = dayStr.toIntOrNull() ?: return null
            val year = yearStr.toIntOrNull() ?: return null

            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, day)
            cal.set(Calendar.HOUR_OF_DAY, 9)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        } catch (e: Exception) {
            null
        }
    }

    private fun parseNumeric(yearStr: String?, monthStr: String?, dayStr: String?): Long? {
        if (yearStr == null || monthStr == null || dayStr == null) return null
        return try {
            val year = yearStr.toIntOrNull() ?: return null
            val month = (monthStr.toIntOrNull() ?: return null) - 1
            val day = dayStr.toIntOrNull() ?: return null

            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, day)
            cal.set(Calendar.HOUR_OF_DAY, 9)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.timeInMillis
        } catch (e: Exception) {
            null
        }
    }

    private fun buildSuggestedTitle(category: DocumentCategory, org: String?, ref: String?): String {
        return when {
            !org.isNullOrBlank() -> "$org ${category.displayName}"
            !ref.isNullOrBlank() -> "${category.displayName} #$ref"
            else -> category.displayName
        }
    }

    private fun buildSuggestedAction(category: DocumentCategory, amount: Double?, currency: String?, dueDate: Long?): String {
        val amountStr = if (amount != null) " $currency${String.format(Locale.US, "%.2f", amount)}" else ""
        val dueStr = if (dueDate != null) {
            " before " + SimpleDateFormat("MMM dd", Locale.US).format(Date(dueDate))
        } else ""

        return when (category) {
            DocumentCategory.BILL -> "Pay bill$amountStr$dueStr"
            DocumentCategory.INVOICE -> "Process payment$amountStr$dueStr"
            DocumentCategory.RECEIPT -> "Save for expense & tax deduction"
            DocumentCategory.INSURANCE -> "Review policy renewal coverage$dueStr"
            DocumentCategory.OFFICIAL_NOTICE -> "Review official notice requirements$dueStr"
            DocumentCategory.BANK_LETTER -> "Review statement & account activity"
            DocumentCategory.OTHER -> if (dueDate != null) "Complete action$dueStr" else "Review document"
        }
    }
}
