package com.upi.expensetracker.util

import com.google.mlkit.vision.text.Text
import kotlin.math.abs

/**
 * Turns raw OCR into a clean summary by using each line's position on screen.
 * Labels (e.g. "Total Amount") are paired with the value on the same row, which
 * survives the two-column layouts common in order/receipt screens.
 *
 * Fully on-device, no network. Best-effort heuristic: works well for standard
 * order/receipt screens; the result stays editable for anything it misreads.
 */
object ReceiptParser {

    data class Info(val summary: String, val amount: Double?)

    private data class Ln(val text: String, val left: Int, val cy: Int, val h: Int)

    // Currency-prefixed amount (₹ / Rs / INR).
    private val currencyRegex = Regex(
        "(?:rs\\.?|inr|\u20B9)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)",
        RegexOption.IGNORE_CASE
    )

    // A bare number that looks like a price (used only for trusted label values).
    private val bareNumberRegex = Regex("^\\s*([0-9][0-9,]{0,6}(?:\\.[0-9]{1,2})?)\\s*$")

    // Amount labels in priority order (most specific first).
    private val amountLabels = listOf(
        "total amount", "amount payable", "amount paid", "grand total",
        "order total", "net payable", "total payable", "you pay", "net amount"
    )

    private val dateLabels = listOf(
        "order date", "ordered on", "purchased on", "invoice date", "transaction date", "date"
    )

    private val brands = listOf(
        "Amazon", "Flipkart", "Myntra", "Ajio", "Meesho", "Savana", "Nykaa",
        "Snapdeal", "Zomato", "Swiggy", "BigBasket", "Blinkit", "Zepto",
        "Tata Cliq", "FirstCry", "Croma", "Reliance", "JioMart", "Dunzo"
    )

    private val labelWords = listOf(
        "order", "total", "amount", "delivery", "payment", "shipping",
        "discount", "mrp", "track", "estimated", "date", "method", "details",
        "updated", "revised", "initial", "number", "charges"
    )

    fun summarize(vt: Text): Info {
        val lines = vt.textBlocks
            .flatMap { it.lines }
            .mapNotNull { ln ->
                val b = ln.boundingBox ?: return@mapNotNull null
                val text = ln.text.trim()
                if (text.isEmpty()) null else Ln(text, b.left, b.centerY(), b.height())
            }
        if (lines.isEmpty()) return Info(vt.text, null)

        val amount = extractAmount(lines)
        val date = extractDate(lines)
        val store = extractStore(lines)
        val item = extractItem(lines)

        val sb = StringBuilder()
        store?.let { sb.append("Store: ").append(it).append('\n') }
        item?.let { sb.append("Item: ").append(it).append('\n') }
        amount?.let { sb.append("Amount: \u20B9").append(fmt(it)).append('\n') }
        date?.let { sb.append("Order date: ").append(it).append('\n') }

        val summary = sb.toString().trim()
        return if (summary.isNotBlank()) Info(summary, amount) else Info(vt.text, amount)
    }

    private fun fmt(v: Double) = if (v % 1.0 == 0.0) "%,.0f".format(v) else "%,.2f".format(v)

    /** Value for a label: text after ':' on the same line, else the nearest line on the same row to the right. */
    private fun valueFor(lines: List<Ln>, label: String): String? {
        val rx = Regex(Regex.escape(label), RegexOption.IGNORE_CASE)
        val labelLine = lines.firstOrNull { rx.containsMatchIn(it.text) } ?: return null
        val after = labelLine.text.substringAfter(':', "").trim()
        if (after.isNotBlank()) return after
        val tol = (labelLine.h * 0.7).toInt().coerceAtLeast(8)
        return lines
            .filter { it !== labelLine && abs(it.cy - labelLine.cy) <= tol && it.left >= labelLine.left }
            .minByOrNull { it.left }
            ?.text
    }

    private fun extractAmount(lines: List<Ln>): Double? {
        for (label in amountLabels) {
            val value = valueFor(lines, label) ?: continue
            parseAmountValue(value)?.let { return it }
        }
        // Fallback: the largest currency-prefixed amount seen anywhere.
        return lines.mapNotNull { currencyValue(it.text) }.maxOrNull()
    }

    private fun parseAmountValue(value: String): Double? {
        currencyValue(value)?.let { return it }
        bareNumberRegex.find(value)?.let { return it.groupValues[1].replace(",", "").toDoubleOrNull() }
        return null
    }

    private fun currencyValue(s: String): Double? {
        val m = currencyRegex.find(s) ?: return null
        return m.groupValues[1].replace(",", "").toDoubleOrNull()
    }

    private fun extractDate(lines: List<Ln>): String? {
        for (label in dateLabels) {
            val value = valueFor(lines, label) ?: continue
            if (value.any { it.isDigit() }) return value.take(40)
        }
        return null
    }

    private fun extractStore(lines: List<Ln>): String? {
        val joined = lines.joinToString(" ") { it.text }
        Regex("from the ([A-Za-z][A-Za-z ]+?) dispatch", RegexOption.IGNORE_CASE)
            .find(joined)?.let { return it.groupValues[1].trim() }
        return brands.firstOrNull { joined.contains(it, ignoreCase = true) }
    }

    /** Best-guess product name: a short Title-Case line with no digits/labels. */
    private fun extractItem(lines: List<Ln>): String? {
        return lines.map { it.text }.firstOrNull { t ->
            val words = t.split(Regex("\\s+")).filter { it.isNotBlank() }
            words.size in 2..6 &&
                t.none { it.isDigit() } &&
                !t.contains(':') &&
                words.all { w -> !w.first().isLetter() || w.first().isUpperCase() } &&
                labelWords.none { lw -> t.contains(lw, ignoreCase = true) }
        }
    }
}
