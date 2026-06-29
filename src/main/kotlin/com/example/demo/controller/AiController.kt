package com.example.demo.controller

import org.springframework.web.bind.annotation.*
import org.springframework.http.ResponseEntity
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.web.client.RestTemplate
import org.json.JSONObject
import org.json.JSONArray

@RestController
@RequestMapping("/api/ai")
class AiController {

    private val restTemplate = RestTemplate()

    @PostMapping("/process")
    fun processAI(@RequestBody request: Map<String, String>): ResponseEntity<String> {
        val prompt = request["prompt"] ?: ""
        val apiKey = System.getenv("GEMINI_API_KEY")
        
        if (apiKey.isNullOrEmpty()) {
            return ResponseEntity.internalServerError().body("Lỗi: Server chưa cấu hình GEMINI_API_KEY")
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

        val headers = HttpHeaders()
        headers.contentType = MediaType.APPLICATION_JSON

        val systemInstruction = """
            You are an AI assistant designed to extract habit schedules from user commands.
            Analyze the user's input and current time. Extract the habits they want to add.
            For each habit, output a JSON object with the following fields:
            - "action": "ADD"
            - "name": The name of the habit (e.g. "Chạy bộ", "Đọc sách")
            - "icon": A single emoji representing the habit (e.g. "🏃", "📚")
            - "time": The time of day in "HH:mm" format (e.g. "08:00")
            - "minsBefore": Reminder time in minutes before (e.g. 5)
            - "date": The date of the habit in "yyyy-MM-dd" format (default is today)

            Return ONLY a valid JSON array of these objects. Do not include any explanations or markdown formatting like ```json.
        """.trimIndent()

        val fullPrompt = "$systemInstruction\n\n$prompt"

        val body = JSONObject()
        val contents = JSONArray()
        val part = JSONObject().put("text", fullPrompt)
        val parts = JSONArray().put(part)
        contents.put(JSONObject().put("parts", parts))
        body.put("contents", contents)

        val entity = HttpEntity(body.toString(), headers)

        return try {
            val response = restTemplate.postForEntity(url, entity, String::class.java)
            // Trích xuất văn bản từ kết quả Gemini trả về
            val jsonResponse = JSONObject(response.body)
            val candidates = jsonResponse.getJSONArray("candidates")
            val text = candidates.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
            
            ResponseEntity.ok(text)
        } catch (e: Exception) {
            ResponseEntity.internalServerError().body("Lỗi xử lý AI: ${e.message}")
        }
    }
}
