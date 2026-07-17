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
    private const val BUDGET_CHANNEL_ID = "budget_alerts"
    private const val SIP_CHANNEL_ID = "sip_alerts"

    fun ensureChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Categorize spending",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Prompts to tag the reason for a UPI payment"
        }
        val budgetChannel = NotificationChannel(
            BUDGET_CHANNEL_ID,
            "Budget alerts",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Warns when a category is near or over its monthly budget"
        }
        val sipChannel = NotificationChannel(
            SIP_CHANNEL_ID,
            "SIP alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Warns before an SIP date if your balance may be insufficient"
        }
        val mgr = context.getSystemService(NotificationManager::class.java)
        mgr.createNotificationChannel(channel)
        mgr.createNotificationChannel(budgetChannel)
        mgr.createNotificationChannel(sipChannel)
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

    /** Dismiss a previously shown notification (e.g. after the user categorizes in-app). */
    fun cancel(context: Context, id: Int) {
        NotificationManagerCompat.from(context).cancel(id)
    }

    fun showSipAlert(context: Context, title: String, message: String) {
        ensureChannel(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context, 90001, openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, SIP_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(90001, notification)
        } catch (_: SecurityException) {
        }
    }

    fun showBudgetAlert(
        context: Context,
        category: String,
        spent: Double,
        budget: Double,
        over: Boolean
    ) {
        ensureChannel(context)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            category.hashCode(),
            openIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = if (over) "Over budget: $category" else "Nearing budget: $category"
        val text = "Spent \u20B9%.0f of \u20B9%.0f this month".format(spent, budget)

        val notification = NotificationCompat.Builder(context, BUDGET_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(text)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                "budget_$category".hashCode(),
                notification
            )
        } catch (_: SecurityException) {
            // Notification permission not granted.
        }
    }
}
