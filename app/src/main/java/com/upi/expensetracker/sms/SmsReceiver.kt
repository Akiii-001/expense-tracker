package com.upi.expensetracker.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.upi.expensetracker.data.AppDatabase
import com.upi.expensetracker.data.Categories
import com.upi.expensetracker.data.Transaction
import com.upi.expensetracker.data.TxnType
import com.upi.expensetracker.notify.Notifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Listens for incoming SMS. For each message it asks SmsParser whether it is a
 * UPI debit or credit. Non-transaction messages (OTPs, promos) are ignored.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        val bySender = messages.groupBy { it.originatingAddress ?: "" }

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val dao = AppDatabase.get(context).transactionDao()
                for ((sender, parts) in bySender) {
                    val body = parts.joinToString("") { it.messageBody ?: "" }
                    val txn = SmsParser.parse(body) ?: continue

                    val category = resolveCategory(dao, txn.payee, txn.type)

                    // Suggest the note used last time for the same payee + amount
                    // (e.g. "Tea"). It's a prefill the user can edit.
                    val suggestedNote = dao.lastNoteForPayeeAmount(txn.payee, txn.amount) ?: ""

                    val id = dao.insert(
                        Transaction(
                            amount = txn.amount,
                            payee = txn.payee,
                            type = txn.type,
                            category = category,
                            note = suggestedNote,
                            timestamp = System.currentTimeMillis(),
                            sender = sender
                        )
                    )

                    // Only nudge for spends that still need a reason. Credits
                    // (salary etc.) are auto-tagged as Income and stay quiet.
                    if (txn.type == TxnType.DEBIT && category == Categories.UNCATEGORIZED) {
                        Notifications.showCategorizePrompt(
                            context = context,
                            transactionId = id,
                            amount = txn.amount,
                            payee = txn.payee
                        )
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    /**
     * Decide the starting category.
     * - Credits default to "Income".
     * - Debits are only auto-categorized when the payee has been CONSISTENT
     *   (exactly one category used before). If the payee has had different
     *   categories, we leave it Uncategorized so the user decides, avoiding
     *   wrong guesses.
     */
    private suspend fun resolveCategory(
        dao: com.upi.expensetracker.data.TransactionDao,
        payee: String,
        type: String
    ): String {
        if (type == TxnType.CREDIT) return Categories.INCOME

        val distinct = dao.distinctCategoriesForPayee(payee)
        return if (distinct == 1) {
            dao.lastCategoryForPayee(payee) ?: Categories.UNCATEGORIZED
        } else {
            Categories.UNCATEGORIZED
        }
    }
}
