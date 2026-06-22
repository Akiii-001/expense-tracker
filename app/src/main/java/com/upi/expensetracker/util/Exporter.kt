package com.upi.expensetracker.util

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.upi.expensetracker.data.Transaction
import com.upi.expensetracker.data.TxnType
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Writes a month's transactions to a CSV or PDF file and returns a shareable Uri. */
object Exporter {

    private val dateFmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    private fun exportsDir(context: Context): File =
        File(context.cacheDir, "exports").apply { mkdirs() }

    private fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    private fun safe(label: String) = label.replace(" ", "-")

    private fun csvCell(value: String): String =
        "\"" + value.replace("\"", "\"\"") + "\""

    fun writeCsv(context: Context, label: String, txns: List<Transaction>): Uri {
        val file = File(exportsDir(context), "UPI-${safe(label)}.csv")
        file.bufferedWriter().use { w ->
            w.write("Date,Type,Amount,Category,What for,Paid to/from")
            w.newLine()
            txns.forEach { t ->
                val type = if (t.type == TxnType.CREDIT) "Received" else "Spent"
                w.write(
                    listOf(
                        csvCell(dateFmt.format(Date(t.timestamp))),
                        csvCell(type),
                        "%.2f".format(t.amount),
                        csvCell(t.category),
                        csvCell(t.note),
                        csvCell(t.payee)
                    ).joinToString(",")
                )
                w.newLine()
            }
        }
        return uriFor(context, file)
    }

    fun writePdf(
        context: Context,
        label: String,
        income: Double,
        spend: Double,
        txns: List<Transaction>
    ): Uri {
        val pageWidth = 595
        val pageHeight = 842
        val margin = 40f
        val lineHeight = 18f

        val doc = PdfDocument()
        val title = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val head = Paint().apply { textSize = 12f; isFakeBoldText = true }
        val body = Paint().apply { textSize = 11f }

        var pageNo = 1
        var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNo).create())
        var canvas = page.canvas
        var y = margin

        canvas.drawText("Expense report - $label", margin, y, title); y += 28f
        canvas.drawText("Received: %.2f    Spent: %.2f    Balance: %.2f".format(income, spend, income - spend), margin, y, body)
        y += 24f
        canvas.drawText("Date", margin, y, head)
        canvas.drawText("Amount", margin + 120, y, head)
        canvas.drawText("Category", margin + 200, y, head)
        canvas.drawText("What for", margin + 320, y, head)
        y += lineHeight

        txns.forEach { t ->
            if (y > pageHeight - margin) {
                doc.finishPage(page)
                pageNo++
                page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNo).create())
                canvas = page.canvas
                y = margin
            }
            val sign = if (t.type == TxnType.CREDIT) "+" else "-"
            canvas.drawText(dateFmt.format(Date(t.timestamp)), margin, y, body)
            canvas.drawText("$sign%.2f".format(t.amount), margin + 120, y, body)
            canvas.drawText(t.category.take(14), margin + 200, y, body)
            canvas.drawText(t.note.take(18), margin + 320, y, body)
            y += lineHeight
        }

        doc.finishPage(page)

        val file = File(exportsDir(context), "UPI-${safe(label)}.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return uriFor(context, file)
    }
}
