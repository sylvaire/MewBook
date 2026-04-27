package com.mewbook.app.data.smartimport

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartImportApiPolicyTest {

    @Test
    fun fileUploadUrl_usesSameV1BaseAsChatCompletionsUrl() {
        assertEquals(
            "https://api.openai.com/v1/files",
            SmartImportApiPolicy.fileUploadUrl("https://api.openai.com/v1")
        )
        assertEquals(
            "https://api.example.com/v1/files",
            SmartImportApiPolicy.fileUploadUrl("https://api.example.com/v1/chat/completions")
        )
    }

    @Test
    fun responsesUrl_usesSameV1BaseAsChatCompletionsUrl() {
        assertEquals(
            "https://api.openai.com/v1/responses",
            SmartImportApiPolicy.responsesUrl("https://api.openai.com/v1")
        )
        assertEquals(
            "https://api.example.com/v1/responses",
            SmartImportApiPolicy.responsesUrl("https://api.example.com/v1/chat/completions")
        )
    }

    @Test
    fun smartImportFileSupport_acceptsTxtCsvAndJson() {
        assertTrue(SmartImportApiPolicy.isSupportedImportFile("records.txt", "text/plain"))
        assertTrue(SmartImportApiPolicy.isSupportedImportFile("records.csv", "text/csv"))
        assertTrue(SmartImportApiPolicy.isSupportedImportFile("records.json", "application/json"))
        assertTrue(SmartImportApiPolicy.isSupportedImportFile("records.TXT", null))
        assertTrue(SmartImportApiPolicy.isSupportedImportFile("records.CSV", null))

        assertFalse(SmartImportApiPolicy.isSupportedImportFile("records.xlsx", "application/vnd.ms-excel"))
        assertFalse(SmartImportApiPolicy.isSupportedImportFile("records.pdf", "application/pdf"))
    }

    @Test
    fun importFileMediaType_preservesTxtCsvAndJsonContentTypes() {
        assertEquals("text/plain", SmartImportApiPolicy.importFileMediaType("records.txt", "text/plain"))
        assertEquals("text/csv", SmartImportApiPolicy.importFileMediaType("records.csv", "text/plain"))
        assertEquals("application/json", SmartImportApiPolicy.importFileMediaType("records.json", "application/octet-stream"))
    }
}
