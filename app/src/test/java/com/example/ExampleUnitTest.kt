package com.example

import com.example.data.DocumentCategory
import com.example.intelligence.DocumentIntelligence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testUtilityBillIntelligenceExtraction() {
        val ocrSample = """
            PACIFIC POWER & LIGHT
            STATEMENT OF ELECTRIC SERVICE
            Account Number: ACC-892401-09
            Billing Date: Oct 28, 2026
            Payment Due Date: Nov 15, 2026
            TOTAL AMOUNT DUE: $142.75
            Please remit payment before the due date.
        """.trimIndent()

        val intelligence = DocumentIntelligence.analyzeText(ocrSample)

        assertEquals(DocumentCategory.BILL, intelligence.category)
        assertEquals(142.75, intelligence.amount ?: 0.0, 0.01)
        assertEquals("$", intelligence.currency)
        assertNotNull(intelligence.suggestedDueDate)
        assertEquals("ACC-892401-09", intelligence.referenceNumber)
        assertTrue(intelligence.suggestedTitle.contains("Pacific Power") || intelligence.suggestedTitle.contains("Bill"))
    }

    @Test
    fun testReceiptIntelligenceExtraction() {
        val receiptSample = """
            APEX TECH RETAIL #044
            100 Tech Blvd, Suite 200
            Date: 10/28/2026 14:22
            Receipt #: REC-441029
            Item: Ergonomic Vertical Wireless Mouse $84.90
            Tax (6.0%): $5.09
            Total: $89.99
            Card: VISA ENDING IN 9012
            Thank you for shopping with Apex Tech!
        """.trimIndent()

        val intelligence = DocumentIntelligence.analyzeText(receiptSample)

        assertEquals(DocumentCategory.RECEIPT, intelligence.category)
        assertEquals(89.99, intelligence.amount ?: 0.0, 0.01)
        assertEquals("REC-441029", intelligence.referenceNumber)
    }

    @Test
    fun testInsuranceIntelligenceExtraction() {
        val insuranceSample = """
            SUMMIT MUTUAL ASSURANCE
            AUTO POLICY RENEWAL DECLARATION
            Policy Number: POL-9923841
            Policyholder: Personal Vehicles
            Renewal Premium Due: $389.00
            Payment Deadline: Nov 22, 2026
        """.trimIndent()

        val intelligence = DocumentIntelligence.analyzeText(insuranceSample)

        assertEquals(DocumentCategory.INSURANCE, intelligence.category)
        assertEquals(389.00, intelligence.amount ?: 0.0, 0.01)
        assertEquals("POL-9923841", intelligence.referenceNumber)
        assertNotNull(intelligence.suggestedDueDate)
    }
}
