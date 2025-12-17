package com.example.doanck

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.doanck.data.model.SOSRequest // ⚠️ Kiểm tra import
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlin.random.Random

class SOSService : Service() {

    private var listenerRegistration: ListenerRegistration? = null

    // Hàm này chạy khi Service bắt đầu
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 1. Tạo thông báo "Đang chạy ngầm" để giữ App sống
        startForegroundServiceNotification()

        // 2. Bắt đầu nghe tin SOS
        startListeningSOS()

        // START_STICKY: Nếu hệ thống giết Service, nó sẽ tự hồi sinh lại
        return START_STICKY
    }

    private fun startForegroundServiceNotification() {
        val channelId = "sos_background_service"
        val channelName = "SOS Monitor"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Weather Connect")
            .setContentText("Đang giám sát tín hiệu SOS khẩn cấp...")
            .setSmallIcon(android.R.drawable.ic_menu_compass) // Thay icon của bạn
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        // Bắt buộc phải gọi dòng này để Service không bị Android giết
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(this, 999, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(999, notification)
        }
    }

    private fun startListeningSOS() {
        val db = Firebase.firestore
        val currentTime = System.currentTimeMillis()

        // Lưu registration để lát hủy nếu cần
        listenerRegistration = db.collection("sos_requests")
            .whereGreaterThan("timestamp", currentTime)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener

                for (dc in snapshots!!.documentChanges) {
                    if (dc.type == DocumentChange.Type.ADDED) {
                        val sos = dc.document.toObject(SOSRequest::class.java)
                        showAlertNotification(sos.province, sos.message, sos.phone)
                    }
                }
            }
    }

    private fun showAlertNotification(province: String?, message: String?, phone: String?) {
        val channelId = "sos_alert_realtime"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "CẢNH BÁO KHẨN CẤP", NotificationManager.IMPORTANCE_HIGH).apply {
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val finalTitle = "🔴 SOS TẠI ${province?.uppercase() ?: "KHU VỰC LẠ"}"
        val finalContent = "$message - SĐT: $phone"

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(finalTitle)
            .setContentText(finalContent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(finalContent))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(Random.nextInt(), notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove() // Dọn dẹp khi tắt hẳn
    }

    override fun onBind(intent: Intent?): IBinder? = null
}