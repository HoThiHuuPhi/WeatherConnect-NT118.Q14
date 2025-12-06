package com.example.doanck.utils

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import com.google.android.gms.location.CurrentLocationRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeoutException

data class LocationData(
    val lat: Double,
    val lon: Double,
    val cityName: String
)

object LocationHelper {

    private val CITIES_TO_PREFIX_TP = setOf(
        "thủ đức", "thủy nguyên", "phúc yên", "vĩnh yên", "bắc ninh", "từ sơn",
        "hạ long", "uông bí", "cẩm phả", "móng cái", "đông triều", "hải dương",
        "hưng yên", "thái bình", "phủ lý", "nam định", "hoa lư", "tam điệp",
        "hà giang", "cao bằng", "bắc kạn", "tuyên quang", "lào cai", "yên bái",
        "thái nguyên", "sông công", "phổ yên", "lạng sơn", "bắc giang", "việt trì",
        "điện biên phủ", "lai châu", "sơn la", "hòa bình", "thanh hóa", "sầm sơn",
        "vinh", "hà tĩnh", "đồng hới", "đông hà", "tam kỳ", "hội an",
        "quảng ngãi", "quy nhơn", "tuy hòa", "nha trang", "cam ranh", "phan rang – tháp chàm",
        "phan thiết", "kon tum", "pleiku", "buôn ma thuột", "gia nghĩa", "đà lạt",
        "bảo lộc", "đồng xoài", "tây ninh", "thủ dầu một", "dĩ an", "thuận an",
        "tân uyên", "bến cát", "biên hòa", "long khánh", "vũng tàu", "bà rịa",
        "phú mỹ", "tân an", "mỹ tho", "gò công", "bến tre", "trà vinh",
        "vĩnh long", "cao lãnh", "sa đéc", "hồng ngự", "long xuyên", "châu đốc",
        "rạch giá", "phú quốc", "hà tiên", "vị thanh", "ngã bảy", "sóc trăng",
        "bạc liêu", "cà mau"
    )

    @SuppressLint("MissingPermission")
    suspend fun fetchLocation(client: FusedLocationProviderClient): Location = withContext(Dispatchers.IO) {
        try {
            val request = CurrentLocationRequest.Builder()
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build()

            client.getCurrentLocation(request, null).await()
                ?: client.lastLocation.await()
                ?: throw TimeoutException("Không thể lấy vị trí GPS hiện tại.")

        } catch (e: Exception) {
            e.printStackTrace()
            // Trả về vị trí mặc định (TP.HCM)
            Location("default").apply {
                latitude = 10.8231
                longitude = 106.6297
            }
        }
    }

    suspend fun getCityName(context: Context, lat: Double, lon: Double): String = withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context, Locale("vi", "VN"))

        try {
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            if (addresses.isNullOrEmpty()) return@withContext "Việt Nam (GPS)"

            val address = addresses[0]
            val subLocality = address.subLocality?.takeIf { it.trim().length > 2 }
            val locality = address.locality ?: address.subAdminArea
            val smallestUnit = subLocality ?: locality

            val finalProvinceName = AdminMap.getNewProvinceName(address.adminArea)
            var formattedUnit = smallestUnit?.trim()

            // Thêm tiền tố "TP." nếu cần
            formattedUnit?.let {
                val normalized = it.lowercase(Locale.ROOT)
                if (CITIES_TO_PREFIX_TP.any { city -> normalized.contains(city) } &&
                    !it.startsWith("TP.", true) &&
                    !it.startsWith("Thành phố", true) &&
                    !it.startsWith("Huyện", true) &&
                    !it.startsWith("Quận", true)
                ) formattedUnit = "TP. $it"
            }

            return@withContext formattedUnit ?: finalProvinceName ?: "Vị trí hiện tại"

        } catch (e: IOException) {
            "Lỗi mạng (Vị trí)"
        } catch (e: Exception) {
            e.printStackTrace()
            "Vị trí không xác định"
        }
    }
}
