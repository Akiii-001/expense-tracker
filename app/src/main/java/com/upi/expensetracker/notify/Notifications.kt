package com.upi.expensetracker.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.upi.expensetracker.ui.MainActivity

object Notifications {

    private const val CHANNEL_ID = "categorize"

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Categorize spending",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Prompts to tag the reason for a UPI payment"
        }
        val mgr = context.getSystemService(NotificationManager::class.java)
        mgr.createNotificationChannel(channel)
    }

    fun showCategorizePrompt(
        context: Context,
        transactionId: Long,
        amount: Double,
        payee: String
    ) {
        ensureChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_TXN_ID, transactionId)
        }
        val pending = PendingIntent.getActivity(
            context,
            transactionId.toInt(),
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentTitle("What was \u20B9%.0f for?".format(amount))
            .setContentText("Paid to $payee \u2014 tap to set a category")
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(transactionId.toInt(), notification)
        } catch (_: SecurityException) {
            // Notification permission not granted; the transaction is still saved.
        }
    }
}
