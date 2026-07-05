package com.upi.expensetracker.util

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/** On-device OCR for receipts (runs fully offline via the bundled ML Kit model). */
object ReceiptOcr {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** Extracts text from the image at [uri]; calls [onResult] with the text (or null on failure). */
    fun recognize(context: Context, uri: Uri, onResult: (String?) -> Unit) {
        try {
            val image = InputImage.fromFilePath(context, uri)
            recognizer.process(image)
                .addOnSuccessListener { result -> onResult(result.text.ifBlank { null }) }
                .addOnFailureListener { onResult(null) }
        } catch (e: Exception) {
            onResult(null)
        }
    }
}
