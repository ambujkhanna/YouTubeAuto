package com.ambuj.youtubeauto

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder

class CastProjectionService : Service() {
    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(CHANNEL_ID, "Phone Cast", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("ParkPlay Phone Cast")
            .setContentText("Phone screen casting is active")
            .setSmallIcon(android.R.drawable.ic_menu_view)
            .setOngoing(true)
            .build()
        startForeground(NOTIFICATION_ID, notification, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION)
        sendBroadcast(Intent(ACTION_READY).setPackage(packageName))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_NOT_STICKY
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_READY = "com.ambuj.youtubeauto.CAST_SERVICE_READY"
        private const val CHANNEL_ID = "phone_cast"
        private const val NOTIFICATION_ID = 4202
    }
}