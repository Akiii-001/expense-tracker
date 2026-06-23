package com.upi.expensetracker.sms

import com.upi.expensetracker.data.TxnType

/**
 * Parses bank SMS into a transaction (debit or credit).
 *
 * PRIVACY: This is the gatekeeper. A message is only kept if it clearly looks
 * like a bank money movement (debit or credit) and contains an amount.
 * OTP / verification / promotional messages do not match and are returned as
 * null, so they never reach the database.
 */
object SmsParser {

    data class ParsedTxn(
        val amount: Double,
        val payee: String,
        val type: String
    )

    // Money LEFT the account. NOTE: we use action words only. We deliberately
    // do NOT include the bare word "debit", because phrases like "Debit Card"
    // would wrongly match (it's the card name, not the action).
    private val debitKeywords = listOf(
        "debited", "spent", "sent", "paid", "withdrawn", "purchase"
    )

    // Money CAME IN to the account (including reversals/refunds). Again, action
    // words only, not the bare word "credit" ("Credit Card" is not a credit).
    private val creditKeywords = listOf(
        "credited", "reversal", "reversed", "refund", "refunded",
        "received", "deposited", "added to", "cashback"
    )

    // Strong signal this is a banking/UPI/wallet message rather than a random text.
    private val bankSignals = listOf(
        "upi", "a/c", "acct", "account", "bank", "vpa", "ref no", "ref:", "txn",
        "imps", "neft", "wallet", "card", "avl bal", "pluxee", "meal card"
    )

    // Hard "ignore this" list, even if it contains a number (OTPs, etc.).
    private val blocklist = listOf(
        "otp", "one time password", "verification code", "do not share", "is your code"
    )

    private val amountRegex = Regex(
        "(?:rs\\.?|inr)\\s*([0-9]+(?:,[0-9]{2,3})*(?:\\.[0-9]{1,2})?)",
        RegexOption.IGNORE_CASE
    )

    // Payee after "to" (for debits) e.g. "to swiggy@ybl".
    private val toRegex = Regex(
        "to\\s+([a-z0-9@._\\-]+(?:\\s[a-z0-9._\\-]+){0,2})",
        RegexOption.IGNORE_CASE
    )

    // Source after "from" (for credits) e.g. "from EMPLOYER PVT LTD".
    private val fromRegex = Regex(
        "from\\s+([a-z0-9@._\\-]+(?:\\s[a-z0-9._\\-]+){0,2})",
        RegexOption.IGNORE_CASE
    )

    // Merchant after "at" (card / wallet / POS) e.g. "at ORACLE SINGAPORE on ..."
    // or "at HKGN PARATH . Avl bal". Stops at "on", a period, "avl", or end.
    private val atRegex = Regex(
        "\\bat\\s+([a-z0-9][a-z0-9 &._'\\-]*?)(?:\\s+on\\b|\\s*\\.|\\s+avl\\b|$)",
        RegexOption.IGNORE_CASE
    )

    fun parse(body: String): ParsedTxn? {
        val lower = body.lowercase()

        if (blocklist.any { lower.contains(it) }) return null
        if (bankSignals.none { lower.contains(it) }) return null

        // Decide direction. Check CREDIT/reversal first, so a reversal message
        // that mentions "Debit Card" isn't mistaken for a debit.
        val type = when {
            creditKeywords.any { lower.contains(it) } -> TxnType.CREDIT
            debitKeywords.any { lower.contains(it) } -> TxnType.DEBIT
            else -> return null
        }

        val amount = amountRegex.find(body)
            ?.groupValues?.get(1)
            ?.replace(",", "")
            ?.toDoubleOrNull()
            ?: return null
        if (amount <= 0.0) return null

        val matcher = if (type == TxnType.CREDIT) fromRegex else toRegex
        val payeeRaw = matcher.find(body)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }
            ?: atRegex.find(body)?.groupValues?.get(1)?.trim()?.takeIf { it.isNotBlank() }

        val payee = payeeRaw?.let { cleanPayee(it) }
            ?: if (type == TxnType.CREDIT) "Unknown source" else "Unknown"

        return ParsedTxn(amount = amount, payee = payee, type = type)
    }

    private fun cleanPayee(raw: String): String {
        return raw
            .split(Regex("\\s+(on|via|ref|upi|dated)\\b", RegexOption.IGNORE_CASE))
            .first()
            .replace(Regex("\\s+"), " ")
            .trim()
            .take(60)
    }
}
