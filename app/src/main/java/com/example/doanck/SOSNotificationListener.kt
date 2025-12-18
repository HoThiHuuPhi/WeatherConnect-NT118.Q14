package com.example.doanck

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.doanck.data.model.SOSRequest // ⚠️ Kiểm tra lại đường dẫn model SOSRequest của bạn
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlin.random.Random

object SOSNotificationListener {

    private var isListening = false

    fun startListening(context: Context) {
        if (isListening) return // Đã nghe rồi thì thôi
        isListening = true

        val db = Firebase.firestore

        // Mẹo quan trọng: Chỉ nhận tin mới hơn lúc mở App (để không bị spam tin cũ)
        val startTime = System.currentTimeMillis()

        db.collection("sos_requests")
            .whereGreaterThan("timestamp", startTime)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                for (dc in snapshots!!.documentChanges) {
                    // Nếu có tin mới được THÊM vào (ADDED)
                    if (dc.type == DocumentChange.Type.ADDED) {
                        try {
                            val sos = dc.document.toObject(SOSRequest::class.java)

                            // Lấy dữ liệu thật: Tỉnh nào? Nhắn gì? SĐT bao nhiêu?
                            val province = sos.province ?: "Chưa xác định"
                            val message = sos.message ?: "Cần hỗ trợ gấp"
                            val phone = sos.phone ?: "Không có SĐT"

                            // Tiêu đề chung (để bao quát)
                            val title = "🔴 CẢNH BÁO KHẨN CẤP (SOS)"
                            // Nội dung chi tiết
                            val content = "Tại $province: $message - SĐT: $phone"

                            showNotification(context, title, content)
                        } catch (ex: Exception) {
                            ex.printStackTrace()
                        }
                    }
                }
            }
    }

    private fun showNotification(context: Context, title: String, message: String) {
        val channelId = "sos_realtime_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Tạo kênh thông báo (Bắt buộc cho Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Cảnh báo SOS",
                NotificationManager.IMPORTANCE_HIGH // Quan trọng: Mức cao nhất để Rung + Chuông
            ).apply {
                description = "Nhận tin SOS khẩn cấp"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Khi bấm vào thông báo thì mở MainActivity
        val intent = Intent(context, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // Thay bằng icon app của bạn nếu muốn
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message)) // Cho phép hiện tin dài
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(Random.nextInt(), notification)
    }
}