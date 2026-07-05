package com.upi.expensetracker.util

import com.google.mlkit.vision.text.Text
import kotlin.math.abs

/**
 * Turns raw OCR into a clean summary by using each line's position on screen.
 * Labels (e.g. "Total Amount") are paired with the value on the same row, which
 * survives the two-column layouts common in order/receipt screens.
 *
 * This is a best-effort heuristic: it works well for standard order screens but
 * won't perfectly parse every format, so the result stays editable.
 */
object ReceiptParser {

    data class Info(val summary: String, val amount: Double?)

    private data class Ln(val text: String, val left: Int, val cy: Int, val h: Int)

    private val amountRegex = Regex(
        "(?:rs\\.?|inr|\u20B9)\\s*([0-9][0-9,]*(?:\\.[0-9]{1,2})?)",
        RegexOption.IGNORE_CASE
    )

    private val brands = listOf(
        "Amazon", "Flipkart", "Myntra", "Ajio", "Meesho", "Savana", "Nykaa",
        "Snapdeal", "Zomato", "Swiggy", "BigBasket", "Blinkit", "Zepto"
    )

    private val labelWords = listOf(
        "order", "total", "amount", "delivery", "payment", "shipping",
        "discount", "mrp", "track", "estimated", "date", "method", "details", "updated"
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
        val date = valueFor(lines, Regex("order date", RegexOption.IGNORE_CASE))
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

    /** Value for a label: text after ':' on the same line, else the line on the same row to the right. */
    private fun valueFor(lines: List<Ln>, label: Regex): String? {
        val labelLine = lines.firstOrNull { label.containsMatchIn(it.text) } ?: return null
        val after = labelLine.text.substringAfter(':', "").trim()
        if (after.isNotBlank()) return after
        val tol = (labelLine.h * 0.7).toInt().coerceAtLeast(8)
        return lines
            .filter { it !== labelLine && abs(it.cy - labelLine.cy) <= tol && it.left >= labelLine.left }
            .minByOrNull { it.left }
            ?.text
    }

    private fun extractAmount(lines: List<Ln>): Double? {
        valueFor(lines, Regex("total amount", RegexOption.IGNORE_CASE))
            ?.let { parseAmount(it)?.let { v -> return v } }
        // Fallback: the largest currency amount seen.
        return lines.mapNotNull { parseAmount(it.text) }.maxOrNull()
    }

    private fun parseAmount(s: String): Double? {
        val m = amountRegex.find(s) ?: return null
        return m.groupValues[1].replace(",", "").toDoubleOrNull()
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
