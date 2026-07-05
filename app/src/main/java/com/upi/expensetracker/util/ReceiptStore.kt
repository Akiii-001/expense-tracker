package com.upi.expensetracker.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.util.UUID

/**
 * Provides a temporary file for capturing a receipt photo. The photo is only
 * used to run OCR and is deleted afterwards, so no image is kept.
 */
object ReceiptStore {

    private fun tempDir(context: Context): File =
        File(context.cacheDir, "capture").apply { mkdirs() }

    fun tempCaptureFile(context: Context): File =
        File(tempDir(context), "cap_${System.currentTimeMillis()}_${UUID.randomUUID()}.jpg")

    fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun delete(file: File?) {
        if (file != null) runCatching { file.delete() }
    }
}
