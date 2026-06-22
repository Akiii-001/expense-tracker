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

    // Money LEFT the account.
    private val debitKeywords = listOf(
        "debited", "debit", "spent", "sent", "paid", "withdrawn", "purchase"
    )

    // Money CAME IN to the account.
    private val creditKeywords = listOf(
        "credited", "credit", "received", "deposited", "added to"
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

    // Merchant after "at" (card / wallet / POS) e.g. "at HKGN PARATH . Avl bal".
    private val atRegex = Regex(
        "\\bat\\s+([a-z0-9][a-z0-9 &._'\\-]*?)\\s*(?:\\.|\\bavl\\b|$)",
        RegexOption.IGNORE_CASE
    )

    fun parse(body: String): ParsedTxn? {
        val lower = body.lowercase()

        if (blocklist.any { lower.contains(it) }) return null
        if (bankSignals.none { lower.contains(it) }) return null

        // Decide direction. Debit wins if both somehow appear (rare), since a
        // debit alert is the one we most want to capture accurately.
        val type = when {
            debitKeywords.any { lower.contains(it) } -> TxnType.DEBIT
            creditKeywords.any { lower.contains(it) } -> TxnType.CREDIT
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
            .trim()
            .take(60)
    }
}
