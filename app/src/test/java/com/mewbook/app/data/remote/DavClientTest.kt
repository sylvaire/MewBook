package com.mewbook.app.data.remote

import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DavClientTest {

    private val davClient = DavClient(OkHttpClient())

    @Test
    fun buildConnectionProbeRequest_usesPropfindWithDepthZero() {
        val request = davClient.buildConnectionProbeRequest(
            serverUrl = "https://dav.jianguoyun.com/dav",
            username = "user@example.com",
            password = "secret"
        )

        assertEquals("PROPFIND", request.method)
        assertEquals("https://dav.jianguoyun.com/dav", request.url.toString())
        assertEquals("0", request.header("Depth"))
        assertNotNull(request.header("Authorization"))
        assertNotNull(request.body)
    }

    @Test
    fun buildDeleteRequest_usesDeleteWithBasicAuth() {
        val request = davClient.buildDeleteRequest(
            serverUrl = "https://dav.example.com/MewBook/mewbook_backup_20260401_120000.json",
            username = "user@example.com",
            password = "secret"
        )

        assertEquals("DELETE", request.method)
        assertEquals(
            "https://dav.example.com/MewBook/mewbook_backup_20260401_120000.json",
            request.url.toString()
        )
        assertNotNull(request.header("Authorization"))
    }
}
