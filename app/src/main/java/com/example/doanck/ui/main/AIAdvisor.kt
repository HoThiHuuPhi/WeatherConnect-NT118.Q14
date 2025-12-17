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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.doanck.data.model.CurrentWeather
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AIWeatherTip(
    val emoji: String,
    val advice: String,
    val reason: String
)

// Service gọi Claude API
object WeatherAIService {
    suspend fun getAITips(
        tempC: Int,
        weatherCode: Int,
        weatherDesc: String,
        windSpeedKmh: Double?,
        humidity: Double?,
        uvIndex: Double?,
        rainMm: Double?
    ): List<AIWeatherTip> {
        return try {
            val prompt = buildPrompt(tempC, weatherCode, weatherDesc, windSpeedKmh, humidity, uvIndex, rainMm)
            val response = callClaudeAPI(prompt)
            parseAIResponse(response)
        } catch (e: Exception) {
            e.printStackTrace()
            getFallbackTips(tempC, weatherDesc, windSpeedKmh, humidity, uvIndex, rainMm)
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
Bạn là trợ lý “Lời khuyên từ AI” cho ứng dụng thời tiết ở Việt Nam.
Dựa trên dữ liệu thời tiết sau, hãy đưa ra lời khuyên ngắn gọn (quần áo + phụ kiện + lưu ý an toàn).

**Dữ liệu:**
- Nhiệt độ: ${tempC}°C
- Tình trạng: $weatherDesc (mã: $weatherCode)
- Gió: ${windSpeed ?: "N/A"} km/h
- Độ ẩm: ${humidity ?: "N/A"}%
- UV: ${uv ?: "N/A"}
- Mưa hiện tại: ${rain ?: 0.0} mm

**Yêu cầu output:**
Chỉ trả về JSON (không markdown, không backticks), dạng:
[
  { "emoji": "☔", "advice": "Sắp mưa, nhớ mang ô/áo mưa", "reason": "Có dấu hiệu mưa/ẩm ướt" },
  { "emoji": "🕶️", "advice": "UV cao, nên đeo kính râm", "reason": "Chỉ số UV cao" }
]

**Quy tắc:**
- 8–12 lời khuyên
- Mỗi advice tối đa ~70 ký tự, dễ hiểu, đúng kiểu người Việt nói
- Mỗi reason dưới 20 từ
- Mỗi lời khuyên phải có emoji phù hợp (mưa/UV/nắng/gió/lạnh/nóng/trơn trượt/đủ nước…)
- Không thêm chữ nào ngoài JSON
        """.trimIndent()
    }

    private suspend fun callClaudeAPI(prompt: String): String {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val url = URL("https://api.anthropic.com/v1/messages")
            val conn = url.openConnection() as HttpURLConnection

            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("anthropic-version", "2023-06-01")
            // API key sẽ được thêm tự động bởi hệ thống của bạn (nếu bạn có cơ chế inject)
            conn.doOutput = true

            val requestBody = JSONObject().apply {
                put("model", "claude-sonnet-4-20250514")
                put("max_tokens", 1200)
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

    private fun parseAIResponse(response: String): List<AIWeatherTip> {
        val json = JSONObject(response)
        val contentArray = json.getJSONArray("content")
        var textContent = ""

        for (i in 0 until contentArray.length()) {
            val item = contentArray.getJSONObject(i)
            if (item.optString("type") == "text") {
                textContent = item.optString("text")
                break
            }
        }

        val cleanJson = textContent
            .replace("```json", "")
            .replace("```", "")
            .trim()

        val arr = JSONArray(cleanJson)
        val result = mutableListOf<AIWeatherTip>()

        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            result.add(
                AIWeatherTip(
                    emoji = obj.getString("emoji"),
                    advice = obj.getString("advice"),
                    reason = obj.optString("reason", "")
                )
            )
        }
        return result
    }

    // Fallback khi API lỗi (vẫn đảm bảo mỗi dòng có icon)
    private fun getFallbackTips(
        tempC: Int,
        weatherDesc: String,
        windSpeed: Double?,
        humidity: Double?,
        uv: Double?,
        rain: Double?
    ): List<AIWeatherTip> {
        val tips = mutableListOf<AIWeatherTip>()

        // Nhiệt độ
        when {
            tempC <= 16 -> tips += AIWeatherTip("🧥", "Trời lạnh, mặc áo khoác/áo len", "Nhiệt độ thấp")
            tempC in 17..23 -> tips += AIWeatherTip("🧥", "Trời mát, mang áo khoác mỏng", "Dễ lạnh về tối")
            tempC >= 30 -> tips += AIWeatherTip("🧢", "Trời nóng, mặc đồ thoáng + đội nón", "Giảm sốc nhiệt")
            else -> tips += AIWeatherTip("👕", "Mặc đồ thoải mái, thấm mồ hôi", "Thời tiết dễ chịu")
        }

        // Mưa
        if ((rain ?: 0.0) > 0.1) {
            tips += AIWeatherTip("☔", "Có mưa/ẩm ướt, nhớ mang ô hoặc áo mưa", "Tránh bị ướt")
            tips += AIWeatherTip("👟", "Ưu tiên giày chống trơn, tránh dép trượt", "Đường dễ trơn")
        } else {
            tips += AIWeatherTip("🌤️", "Mang ô gấp phòng mưa bất chợt", "Thời tiết có thể đổi nhanh")
        }

        // UV
        when {
            (uv ?: 0.0) >= 8 -> {
                tips += AIWeatherTip("🕶️", "UV cao, đeo kính râm + áo chống nắng", "Bảo vệ da & mắt")
                tips += AIWeatherTip("🧴", "Bôi kem chống nắng khi ra ngoài", "Giảm cháy nắng")
            }
            (uv ?: 0.0) >= 5 -> tips += AIWeatherTip("🧴", "UV trung bình, nên bôi chống nắng nhẹ", "Hạn chế sạm da")
            else -> tips += AIWeatherTip("🙂", "UV thấp, vẫn nên che chắn nhẹ khi đi lâu", "Giữ da ổn định")
        }

        // Gió
        if ((windSpeed ?: 0.0) >= 25) tips += AIWeatherTip("🌬️", "Gió mạnh, mặc áo gió/đóng khuy áo", "Tránh lạnh & bụi")

        // Độ ẩm
        if ((humidity ?: 0.0) >= 80) tips += AIWeatherTip("💧", "Độ ẩm cao, mặc đồ thoáng, mau khô", "Giảm bí bách")
        if ((humidity ?: 100.0) <= 45) tips += AIWeatherTip("🫗", "Độ ẩm thấp, uống đủ nước", "Tránh khô da")

        tips += AIWeatherTip("🚶", "Nếu ra đường, xem trời trước khi đi xa", "Chủ động lịch trình")
        tips += AIWeatherTip("📌", "Theo dõi cảnh báo thời tiết trong ngày", "Tránh thay đổi đột ngột")

        return tips.take(12)
    }
}

@Composable
fun AIAdvisorDialog(
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

    val LightBlueSky = Color(0xFF87CEFA)

    var tips by remember { mutableStateOf<List<AIWeatherTip>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun reload() {
        scope.launch {
            try {
                isLoading = true
                error = null
                tips = WeatherAIService.getAITips(
                    tempC = tempC,
                    weatherCode = currentWeather.weatherCode,
                    weatherDesc = weatherDesc,
                    windSpeedKmh = windSpeedKmh,
                    humidity = currentWeather.humidity,
                    uvIndex = uvIndex,
                    rainMm = rainMm
                )
            } catch (e: Exception) {
                error = "Lỗi kết nối AI: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { reload() }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .heightIn(max = screenHeight * 0.6f)
                .wrapContentHeight(),
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
                                text = "🤖 Lời khuyên từ AI",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2D3748)
                            )
                            Text(
                                text = "Dựa trên dữ liệu thời tiết hôm nay",
                                fontSize = 12.sp,
                                color = Color(0xFF4A5568)
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!isLoading) {
                                IconButton(onClick = { reload() }) {
                                    Icon(Icons.Default.Refresh, "Làm mới", tint = Color(0xFF2D3748))
                                }
                            }
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, "Đóng", tint = Color(0xFF2D3748))
                            }
                        }
                    }
                }

                // --- CONTENT ---
                Box(modifier = Modifier.fillMaxWidth()) {
                    when {
                        isLoading -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(color = LightBlueSky)
                                Spacer(Modifier.height(16.dp))
                                Text("AI đang tổng hợp lời khuyên...", color = Color.Gray)
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

                        tips != null -> {
                            Column(
                                modifier = Modifier
                                    .verticalScroll(rememberScrollState())
                                    .padding(16.dp)
                            ) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(
                                        0xFFFFF2B6
                                    )
                                    ),
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
                                                "Thời tiết hiện tại",
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

                                tips!!.forEach { tip ->
                                    AITipItem(tip)
                                    Spacer(Modifier.height(12.dp))
                                }

                                Text(
                                    "✨ Lời khuyên từ AI (tham khảo)",
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    fontStyle = FontStyle.Italic
                                )

                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AITipItem(tip: AIWeatherTip) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFBFC)),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEDF2F7)),
                contentAlignment = Alignment.Center
            ) {
                Text(tip.emoji, fontSize = 22.sp)
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = tip.advice,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF2D3748),
                    lineHeight = 18.sp
                )
                if (tip.reason.isNotBlank()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = tip.reason,
                        fontSize = 12.sp,
                        color = Color(0xFF718096),
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}
