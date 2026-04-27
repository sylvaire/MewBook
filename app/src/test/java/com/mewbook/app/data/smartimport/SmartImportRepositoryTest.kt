package com.mewbook.app.data.smartimport

import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartImportRepositoryTest {

    @Test
    fun convertFileToEnvelope_fallsBackToChatCompletionsWhenFileUploadReturns404() = runBlocking {
        val requestPaths = mutableListOf<String>()
        val chatRequestBodies = mutableListOf<String>()
        val client = OkHttpClient.Builder()
            .addInterceptor(
                Interceptor { chain ->
                    val request = chain.request()
                    requestPaths += request.url.encodedPath
                    when (request.url.encodedPath) {
                        "/files" -> responseFor(request = request, code = 404, body = """{"error":"not found"}""")
                        "/chat/completions" -> {
                            chatRequestBodies += request.readBodyAsUtf8()
                            responseFor(
                                request = request,
                                body = """
                                    {
                                      "choices": [
                                        {
                                          "message": {
                                            "content": "{\"records\":[{\"date\":\"2026-04-26\",\"type\":\"EXPENSE\",\"amount\":12.5,\"category\":\"餐饮\"}]}"
                                          }
                                        }
                                      ]
                                    }
                                """.trimIndent()
                            )
                        }

                        else -> responseFor(request = request, code = 500, body = """{"error":"unexpected"}""")
                    }
                }
            )
            .build()
        val repository = SmartImportRepository(
            okHttpClient = client,
            loadCredentials = {
                SmartImportCredentials(
                    baseUrl = "https://api.deepseek.com",
                    model = "deepseek-v4-flash",
                    apiKey = "test-key"
                )
            }
        )

        val result = repository.convertFileToEnvelope(
            fileName = "records.txt",
            mimeType = "text/plain",
            fileBytes = "日期,类型,金额,分类\n2026-04-26,支出,12.5,餐饮".toByteArray()
        )

        assertTrue(result.isSuccess)
        assertEquals(listOf("/files", "/chat/completions"), requestPaths)
        assertTrue(chatRequestBodies.single().contains("2026-04-26,支出,12.5,餐饮"))
    }

    @Test
    fun convertFileToEnvelope_usesLocalCsvParserBeforeAiRequests() = runBlocking {
        val requestPaths = mutableListOf<String>()
        val client = OkHttpClient.Builder()
            .addInterceptor(
                Interceptor { chain ->
                    requestPaths += chain.request().url.encodedPath
                    responseFor(chain.request(), code = 500, body = """{"error":"network should not be used"}""")
                }
            )
            .build()
        val repository = SmartImportRepository(
            okHttpClient = client,
            loadCredentials = {
                SmartImportCredentials(
                    baseUrl = "https://api.deepseek.com",
                    model = "deepseek-v4-flash",
                    apiKey = "test-key"
                )
            }
        )

        val result = repository.convertFileToEnvelope(
            fileName = "records.csv",
            mimeType = "text/csv",
            fileBytes = """
                UUID,金额,分类,类型,日期,时间戳（毫秒）,备注
                0EEC2148-C284-4C5D-B3ED-A39C4F55D2BB,112.00,学习,支出,2020年08月15日 14:11,1597471902285,社工报名
            """.trimIndent().toByteArray()
        )

        assertTrue(result.isSuccess)
        assertTrue(requestPaths.isEmpty())
        assertEquals(1, result.getOrThrow().payload.records.size)
    }

    @Test
    fun convertFileToEnvelope_splitsLongTextFallbackIntoMultipleChatRequests() = runBlocking {
        val requestPaths = mutableListOf<String>()
        var chatCallCount = 0
        val client = OkHttpClient.Builder()
            .addInterceptor(
                Interceptor { chain ->
                    val request = chain.request()
                    requestPaths += request.url.encodedPath
                    when (request.url.encodedPath) {
                        "/files" -> responseFor(request = request, code = 404, body = """{"error":"not found"}""")
                        "/chat/completions" -> {
                            chatCallCount += 1
                            responseFor(
                                request = request,
                                body = """
                                    {
                                      "choices": [
                                        {
                                          "message": {
                                            "content": "{\"records\":[{\"date\":\"2026-04-26\",\"type\":\"EXPENSE\",\"amount\":${chatCallCount}.0,\"category\":\"餐饮\"}]}"
                                          }
                                        }
                                      ]
                                    }
                                """.trimIndent()
                            )
                        }

                        else -> responseFor(request = request, code = 500, body = """{"error":"unexpected"}""")
                    }
                }
            )
            .build()
        val repository = SmartImportRepository(
            okHttpClient = client,
            loadCredentials = {
                SmartImportCredentials(
                    baseUrl = "https://api.deepseek.com",
                    model = "deepseek-v4-flash",
                    apiKey = "test-key"
                )
            }
        )
        val longCsv = buildString {
            appendLine("日期,类型,金额,分类,备注")
            repeat(4_000) { index ->
                appendLine("2026-04-26,支出,12.5,餐饮,这是一条比较长的导入备注用于触发分段-$index")
            }
        }

        val result = repository.convertFileToEnvelope(
            fileName = "records.txt",
            mimeType = "text/plain",
            fileBytes = longCsv.toByteArray()
        )

        assertTrue(result.isSuccess)
        assertTrue(chatCallCount > 1)
        assertEquals(1 + chatCallCount, requestPaths.size)
        assertEquals(chatCallCount, result.getOrThrow().payload.records.size)
    }

    private fun responseFor(
        request: okhttp3.Request,
        code: Int = 200,
        body: String
    ): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code in 200..299) "OK" else "Error")
            .body(body.toResponseBody("application/json; charset=utf-8".toMediaType()))
            .build()
    }

    private fun okhttp3.Request.readBodyAsUtf8(): String {
        val buffer = Buffer()
        body?.writeTo(buffer)
        return buffer.readUtf8()
    }
}
