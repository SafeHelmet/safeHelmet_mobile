package com.safehelmet.safehelmet_mobile.notification

import android.content.pm.PackageManager

/// TODO: Reference https://developer.android.com/develop/ui/views/notifications/build-notification


import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.safehelmet.safehelmet_mobile.MainActivity
import android.Manifest

object PollingNotification {

    private const val CHANNEL_ID = "helmet_alert_channel"

    fun showNotification(context: Context, title: String, message: String) {
        // Controlla se il permesso è stato concesso
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        // Crea il canale di notifica (necessario per Android 8+)
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Helmet Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifiche per anomalie del casco"
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)

        // Intent per aprire l'app al tocco della notifica
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            // putExtra("from_notification", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Costruzione della notifica
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Mostra la notifica
//        with(NotificationManagerCompat.from(context)) {
//            notify(1, builder.build())
//        }
        notificationManager.notify(1, builder)
    }
}
