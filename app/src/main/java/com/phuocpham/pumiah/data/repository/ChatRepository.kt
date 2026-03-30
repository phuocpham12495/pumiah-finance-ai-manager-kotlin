package com.phuocpham.pumiah.data.repository

import com.phuocpham.pumiah.BuildConfig
import com.phuocpham.pumiah.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor() {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun sendMessage(
        userMessage: String,
        conversationHistory: List<ChatMessage>,
        financialContext: String
    ): Result<String> = runCatching {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank()) error("Chưa cấu hình Gemini API key")

        val systemPrompt = buildSystemPrompt(financialContext)

        // Build contents array for Gemini
        val contents = buildString {
            append("[")
            // System instruction as first user turn
            append("""{"role":"user","parts":[{"text":${json.encodeToString(systemPrompt)}}]}""")
            append(",")
            append("""{"role":"model","parts":[{"text":"Xin chào! Tôi là PFAM AI, trợ lý tài chính của bạn. Tôi có thể giúp bạn quản lý thu chi, phân tích tài chính và đưa ra lời khuyên. Bạn cần giúp gì không?"}]}""")

            // Conversation history
            for (msg in conversationHistory.takeLast(20)) {
                val role = if (msg.role == "user") "user" else "model"
                append(",")
                append("""{"role":"$role","parts":[{"text":${json.encodeToString(msg.content)}}]}""")
            }

            // Current user message
            append(",")
            append("""{"role":"user","parts":[{"text":${json.encodeToString(userMessage)}}]}""")
            append("]")
        }

        val requestBody = """{"contents":$contents,"generationConfig":{"temperature":0.5,"maxOutputTokens":2048}}"""

        val responseText = withContext(Dispatchers.IO) {
            val url = URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
            val connection = url.openConnection() as HttpURLConnection
            connection.apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 30_000
                readTimeout = 60_000
                doOutput = true
                outputStream.use { it.write(requestBody.toByteArray()) }
            }
            val response = connection.inputStream.bufferedReader().readText()
            connection.disconnect()
            response
        }

        parseGeminiResponse(responseText)
    }

    private fun parseGeminiResponse(responseText: String): String {
        // Navigate: candidates[0].content.parts[0].text
        val jsonObj = json.parseToJsonElement(responseText)
        val candidates = jsonObj.jsonObject["candidates"]?.jsonArray ?: error("No candidates")
        val content = candidates[0].jsonObject["content"] ?: error("No content")
        val parts = content.jsonObject["parts"]?.jsonArray ?: error("No parts")
        return parts[0].jsonObject["text"]?.toString()?.removeSurrounding("\"")
            ?.replace("\\n", "\n")?.replace("\\\"", "\"") ?: error("No text")
    }

    private fun buildSystemPrompt(financialContext: String): String {
        val today = java.time.LocalDate.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        return """Bạn là trợ lý AI tài chính thông minh tên Pumiah FAM, giúp người dùng quản lý tài chính cá nhân bằng tiếng Việt.

Ngày hôm nay: $today

Bạn có thể:
- Báo cáo tài chính theo tuần / tháng / năm
- Phân tích điểm nổi bật, xu hướng chi tiêu
- Báo cáo chi tiêu & thu nhập theo danh mục theo tuần / tháng / năm
- Báo cáo giao dịch trong một ngày cụ thể
- Báo cáo ngân sách theo ví cụ thể
- Báo cáo mục tiêu tiết kiệm
- Đưa ra lời khuyên tài chính cá nhân

$financialContext

Hãy trả lời chính xác dựa trên dữ liệu trên. Nếu người dùng hỏi về ngày/tuần/tháng/năm cụ thể, hãy lọc đúng theo khoảng thời gian đó từ dữ liệu giao dịch. Trả lời bằng tiếng Việt, ngắn gọn, rõ ràng và có số liệu cụ thể."""
    }
}
