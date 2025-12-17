package com.example.doanck // Đổi package này cho đúng với dự án của bạn

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Khi có tin nhắn đến, hàm này sẽ chạy
        remoteMessage.notification?.let {
            showNotification(it.title ?: "Thông báo mới", it.body ?: "Bạn có tin nhắn mới")
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Token này dùng để gửi thông báo riêng cho từng máy (nếu cần lưu lại)
        println("FCM Token mới: $token")
    }

    private fun showNotification(title: String, message: String) {
        val channelId = "sos_alert_channel"
        val notificationId = Random.nextInt()

        // Khi bấm vào thông báo thì mở App
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_alert) // Bạn có thể thay bằng R.drawable.ic_sos
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // Quan trọng: Rung và hiện popup
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Tạo Channel (Bắt buộc cho Android 8.0 trở lên)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Thông báo SOS Khẩn cấp",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(notificationId, notificationBuilder.build())
    }
}