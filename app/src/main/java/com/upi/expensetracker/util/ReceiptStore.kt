package com.upi.expensetracker.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/** Stores receipt images in the app's private storage and loads them back. */
object ReceiptStore {

    private fun dir(context: Context): File =
        File(context.filesDir, "receipts").apply { mkdirs() }

    /** A fresh empty file for a new receipt (used as the camera output target). */
    fun newFile(context: Context): File =
        File(dir(context), "rcpt_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    /** Copy an image from a picked content Uri into private storage; returns the path. */
    fun copyFrom(context: Context, source: Uri): String? = runCatching {
        val target = newFile(context)
        context.contentResolver.openInputStream(source)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        target.absolutePath
    }.getOrNull()

    /** Load a downscaled bitmap (null if the file is missing). */
    fun load(path: String?, maxPx: Int = 1200): Bitmap? {
        if (path.isNullOrBlank()) return null
        val file = File(path)
        if (!file.exists()) return null
        return runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            var sample = 1
            val largest = maxOf(bounds.outWidth, bounds.outHeight)
            while (largest / sample > maxPx) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeFile(path, opts)
        }.getOrNull()
    }

    fun delete(path: String?) {
        if (!path.isNullOrBlank()) runCatching { File(path).delete() }
    }
}
