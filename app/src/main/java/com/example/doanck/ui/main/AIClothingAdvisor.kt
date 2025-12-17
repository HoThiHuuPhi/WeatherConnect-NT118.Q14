package com.example.doanck.ui.main

import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.doanck.data.model.CurrentWeather
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AIClothingAdvice(
    val category: String,
    val items: List<String>,
    val emoji: String,
    val reason: String
)

// Service gọi Claude API
object ClothingAIService {
    suspend fun getAIAdvice(
        tempC: Int,
        weatherCode: Int,
        weatherDesc: String,
        windSpeedKmh: Double?,
        humidity: Double?,
        uvIndex: Double?,
        rainMm: Double?
    ): List<AIClothingAdvice> {
        return try {
            val prompt = buildPrompt(tempC, weatherCode, weatherDesc, windSpeedKmh, humidity, uvIndex, rainMm)
            val response = callClaudeAPI(prompt)
            parseAIResponse(response)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback về logic cũ nếu API lỗi
            getFallbackAdvice(tempC, weatherCode, windSpeedKmh, humidity, uvIndex, rainMm)
        }
    }

    private fun buildPrompt(
        tempC: Int,
        weatherCode: Int,
        weatherDesc: String,
        windSpeed: Double?,
        humidity: Double?,
        uv: Double?,
        rain: Double?
    ): String {
        return """
Bạn là chuyên gia tư vấn trang phục thời tiết tại Việt Nam. Dựa trên dữ liệu thời tiết sau, hãy đưa ra lời khuyên về trang phục:

**Thời tiết hiện tại:**
- Nhiệt độ: ${tempC}°C
- Tình trạng: $weatherDesc (mã: $weatherCode)
- Tốc độ gió: ${windSpeed ?: "N/A"} km/h
- Độ ẩm: ${humidity ?: "N/A"}%
- Chỉ số UV: ${uv ?: "N/A"}
- Lượng mưa: ${rain ?: 0.0} mm

**Yêu cầu:**
Trả về ĐÚNG format JSON sau (không thêm markdown backticks):
[
  {
    "category": "Lớp ngoài",
    "items": ["Áo khoác", "Áo gió"],
    "emoji": "🧥",
    "reason": "Lý do ngắn gọn"
  },
  {
    "category": "Quần",
    "items": ["Quần dài", "Quần jean"],
    "emoji": "👖",
    "reason": "Lý do"
  }
]

**Quy tắc:**
- Từ 5-7 danh mục: Lớp ngoài, Quần, Phụ kiện chống mưa/nắng/gió, Giày dép, Lưu ý đặc biệt
- Mỗi item ngắn gọn, phù hợp người Việt
- Reason dưới 20 từ
- Chỉ trả về JSON, không text khác
        """.trimIndent()
    }

    private suspend fun callClaudeAPI(prompt: String): String {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val url = URL("https://api.anthropic.com/v1/messages")
            val conn = url.openConnection() as HttpURLConnection

            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("anthropic-version", "2023-06-01")
            // API key sẽ được thêm tự động bởi hệ thống
            conn.doOutput = true

            val requestBody = JSONObject().apply {
                put("model", "claude-sonnet-4-20250514")
                put("max_tokens", 2000)
                put("messages", JSONArray().apply {
                    put(JSONObject().apply {
                        put("role", "user")
                        put("content", prompt)
                    })
                })
            }

            conn.outputStream.use { it.write(requestBody.toString().toByteArray()) }

            val responseCode = conn.responseCode
            if (responseCode == 200) {
                conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                throw Exception("API error: $responseCode")
            }
        }
    }

    private fun parseAIResponse(response: String): List<AIClothingAdvice> {
        try {
            val json = JSONObject(response)
            val contentArray = json.getJSONArray("content")
            var textContent = ""

            for (i in 0 until contentArray.length()) {
                val item = contentArray.getJSONObject(i)
                if (item.getString("type") == "text") {
                    textContent = item.getString("text")
                    break
                }
            }

            // Loại bỏ markdown backticks nếu có
            val cleanJson = textContent
                .replace("```json", "")
                .replace("```", "")
                .trim()

            val adviceArray = JSONArray(cleanJson)
            val result = mutableListOf<AIClothingAdvice>()

            for (i in 0 until adviceArray.length()) {
                val obj = adviceArray.getJSONObject(i)
                val itemsArray = obj.getJSONArray("items")
                val items = mutableListOf<String>()
                for (j in 0 until itemsArray.length()) {
                    items.add(itemsArray.getString(j))
                }

                result.add(AIClothingAdvice(
                    category = obj.getString("category"),
                    items = items,
                    emoji = obj.getString("emoji"),
                    reason = obj.getString("reason")
                ))
            }

            return result
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    // Fallback khi API lỗi
    private fun getFallbackAdvice(
        tempC: Int,
        weatherCode: Int,
        windSpeed: Double?,
        humidity: Double?,
        uv: Double?,
        rain: Double?
    ): List<AIClothingAdvice> {
        val advice = mutableListOf<AIClothingAdvice>()

        // Logic đơn giản
        when {
            tempC < 15 -> advice.add(AIClothingAdvice("Lớp ngoài", listOf("Áo khoác dày", "Áo len"), "🧥", "Nhiệt độ dưới 15°C"))
            tempC < 22 -> advice.add(AIClothingAdvice("Lớp ngoài", listOf("Áo khoác mỏng", "Hoodie"), "🧥", "Trời mát"))
            else -> advice.add(AIClothingAdvice("Áo", listOf("Áo thun", "Áo ba lỗ"), "👕", "Trời nóng"))
        }

        if ((rain ?: 0.0) > 0.1) {
            advice.add(AIClothingAdvice("Chống mưa", listOf("Áo mưa", "Ô"), "☔", "Có mưa"))
        }

        return advice
    }
}

@Composable
fun AIClothingAdvisorDialog(
    currentWeather: CurrentWeather,
    tempC: Int,
    weatherDesc: String,
    uvIndex: Double?,
    windSpeedKmh: Double?,
    rainMm: Double?,
    onDismiss: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp

    // Define màu LightBlueSky thủ công vì không có sẵn trong Color
    val LightBlueSky = Color(0xFF87CEFA)

    var advice by remember { mutableStateOf<List<AIClothingAdvice>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        scope.launch {
            try {
                isLoading = true
                error = null
                advice = ClothingAIService.getAIAdvice(
                    tempC, currentWeather.weatherCode, weatherDesc,
                    windSpeedKmh, currentWeather.humidity, uvIndex, rainMm
                )
            } catch (e: Exception) {
                error = "Lỗi kết nối AI: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = screenHeight * 0.85f) // Giới hạn chiều cao max
                .wrapContentHeight(), // QUAN TRỌNG: Tự co lại nếu nội dung ngắn
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column {
                // --- HEADER ---
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    LightBlueSky,
                                    Color(0xFFB0E0E6),
                                    Color(0xFFFFFACD)
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "🤖 AI Tư Vấn Trang Phục",
                                fontSize = 18.sp, // Giảm size chút cho đỡ bị tràn
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2D3748) // Đổi sang màu tối để nổi trên nền sáng
                            )
                            Text(
                                text = "Powered by Claude AI",
                                fontSize = 12.sp,
                                color = Color(0xFF4A5568) // Màu tối
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!isLoading) {
                                IconButton(onClick = {
                                    scope.launch {
                                        isLoading = true
                                        advice = null // Reset để hiện loading view
                                        try {
                                            advice = ClothingAIService.getAIAdvice(
                                                tempC, currentWeather.weatherCode, weatherDesc,
                                                windSpeedKmh, currentWeather.humidity, uvIndex, rainMm
                                            )
                                        } catch (e: Exception) {
                                            error = "Lỗi: ${e.message}"
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                }) {
                                    // Icon màu tối
                                    Icon(Icons.Default.Refresh, "Làm mới", tint = Color(0xFF2D3748))
                                }
                            }
                            // Nút Close trên Header
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, "Đóng", tint = Color(0xFF2D3748))
                            }
                        }
                    }
                }

                // --- CONTENT ---
                // Không dùng weight(1f) ở đây để tránh bị kéo giãn
                Box(modifier = Modifier.fillMaxWidth()) {
                    when {
                        isLoading -> {
                            // Set chiều cao cố định cho lúc loading để nó gọn gàng
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = LightBlueSky)
                                Spacer(Modifier.height(16.dp))
                                Text("AI đang suy nghĩ...", color = Color.Gray)
                            }
                        }
                        error != null -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("⚠️", fontSize = 40.sp)
                                Spacer(Modifier.height(8.dp))
                                Text(error!!, color = Color.Red, fontSize = 14.sp)
                                Spacer(Modifier.height(16.dp))
                                Button(onClick = onDismiss) { Text("Đóng") }
                            }
                        }
                        advice != null -> {
                            Column(
                                modifier = Modifier
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp)
                            ) {
                                // Weather Info Card
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF7FAFC)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("🌡️", fontSize = 24.sp)
                                        Spacer(Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                "Thời tiết hôm nay",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = Color(0xFF2D3748)
                                            )
                                            Text(
                                                "${tempC}°C • $weatherDesc",
                                                fontSize = 13.sp,
                                                color = Color(0xFF718096)
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(16.dp))

                                // List Advice
                                advice!!.forEach { item ->
                                    AIClothingAdviceItem(item)
                                    Spacer(Modifier.height(12.dp))
                                }

                                Text(
                                    "✨ Lời khuyên từ AI (tham khảo)",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )

                                Spacer(Modifier.height(8.dp))

                                // --- NÚT ĐÓNG TO Ở DƯỚI ---
                                Button(
                                    onClick = onDismiss,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFFEDF2F7),
                                        contentColor = Color(0xFF2D3748)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Đóng", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AIClothingAdviceItem(advice: AIClothingAdvice) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFAFBFC)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emoji background
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEDF2F7)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(advice.emoji, fontSize = 22.sp)
                }
                Spacer(Modifier.width(12.dp))
                Text(
                    text = advice.category,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2D3748)
                )
            }

            Spacer(Modifier.height(12.dp))

            advice.items.forEach { item ->
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF667EEA))
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = item,
                        fontSize = 14.sp,
                        color = Color(0xFF4A5568),
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Reason với background nhẹ
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF7FAFC)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💭", fontSize = 14.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = advice.reason,
                        fontSize = 13.sp,
                        color = Color(0xFF718096),
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}