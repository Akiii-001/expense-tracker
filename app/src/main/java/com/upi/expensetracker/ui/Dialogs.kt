package com.upi.expensetracker.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.upi.expensetracker.util.ReceiptOcr
import com.upi.expensetracker.util.ReceiptStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.unit.dp
import com.upi.expensetracker.data.Categories
import com.upi.expensetracker.data.CustomCategory
import com.upi.expensetracker.data.Transaction
import com.upi.expensetracker.data.TxnType

/** Predefined categories for the type, plus any custom ones the user has saved. */
private fun categoriesFor(type: String, custom: List<CustomCategory>): List<String> {
    val base = Categories.forType(type)
    val extras = custom.filter { it.type == type }.map { it.name }.filter { it !in base }
    return base + extras
}

/** Keep only digits and a single decimal point. */
internal fun sanitizeAmount(input: String): String {
    val filtered = input.filter { it.isDigit() || it == '.' }
    val dot = filtered.indexOf('.')
    return if (dot < 0) filtered
    else filtered.substring(0, dot + 1) + filtered.substring(dot + 1).replace(".", "")
}

/** Format a stored amount for editing (no trailing .0 for whole numbers). */
internal fun amountText(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CategoryChips(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    var showCustom by remember { mutableStateOf(false) }
    var custom by remember { mutableStateOf("") }

    FlowRow(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
        options.forEach { cat ->
            FilterChip(
                selected = cat == selected,
                onClick = { onSelect(cat) },
                label = { Text(cat) },
                modifier = Modifier.padding(end = 8.dp, bottom = 4.dp)
            )
        }
        FilterChip(
            selected = showCustom,
            onClick = { showCustom = !showCustom },
            label = { Text("+ Custom") },
            modifier = Modifier.padding(end = 8.dp, bottom = 4.dp)
        )
    }
    if (showCustom) {
        OutlinedTextField(
            value = custom,
            onValueChange = {
                custom = it
                if (it.isNotBlank()) onSelect(it.trim())
            },
            label = { Text("New category name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    customCategories: List<CustomCategory>,
    onDismiss: () -> Unit,
    onSetCategoryIcon: (String, String) -> Unit,
    onSetCategoryColor: (String, String) -> Unit,
    onAdd: (Double, String, String, String, String, Long, String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amount by remember { mutableStateOf("") }
    var payee by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(TxnType.DEBIT) }
    var showIconPicker by remember { mutableStateOf(false) }
    var dateMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    var receiptText by remember { mutableStateOf("") }

    val options = categoriesFor(type, customCategories)
    var category by remember { mutableStateOf(options.first()) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp)
        ) {
            Text("Add transaction", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = type == TxnType.DEBIT,
                    onClick = { type = TxnType.DEBIT; category = categoriesFor(TxnType.DEBIT, customCategories).first() },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text("Spent") }
                SegmentedButton(
                    selected = type == TxnType.CREDIT,
                    onClick = { type = TxnType.CREDIT; category = categoriesFor(TxnType.CREDIT, customCategories).first() },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text("Received") }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = sanitizeAmount(it) },
                label = { Text("Amount (\u20B9)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            DateField(dateMillis = dateMillis, onPick = { dateMillis = it })
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text(if (type == TxnType.CREDIT) "What for? (e.g. salary)" else "What for? (e.g. biscuit)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = payee,
                onValueChange = { payee = it },
                label = { Text(if (type == TxnType.CREDIT) "Received from (optional)" else "Paid to (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Category", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.weight(1f))
                if (category.isNotBlank()) CategoryIconButton(category) { showIconPicker = true }
            }
            CategoryChips(options = options, selected = category, onSelect = { category = it })

            Spacer(Modifier.height(16.dp))
            ReceiptSection(currentText = receiptText, onText = { receiptText = it })

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val value = amount.toDoubleOrNull()
                    if (value != null && value > 0) {
                        onAdd(value, payee, note, type, category, dateMillis, receiptText.ifBlank { null })
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Add transaction") }
        }
    }

    if (showIconPicker && category.isNotBlank()) {
        CategoryStyleDialog(
            category = category,
            onDismiss = { showIconPicker = false },
            onPickIcon = { key -> onSetCategoryIcon(category, key) },
            onPickColor = { hex -> onSetCategoryColor(category, hex) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionSheet(
    transaction: Transaction,
    customCategories: List<CustomCategory>,
    onDismiss: () -> Unit,
    onSave: (String, String, String?) -> Unit,
    onDelete: () -> Unit,
    onSetCategoryIcon: (String, String) -> Unit,
    onSetCategoryColor: (String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isCredit = transaction.type == TxnType.CREDIT
    var note by remember { mutableStateOf(transaction.note) }
    var receiptText by remember { mutableStateOf(transaction.receiptText ?: "") }
    var showIconPicker by remember { mutableStateOf(false) }
    val options = categoriesFor(transaction.type, customCategories)
    var category by remember {
        mutableStateOf(if (transaction.category in options) transaction.category else options.first())
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 32.dp)
        ) {
            Text(
                if (isCredit) "Where did this come from?" else "What did you pay for?",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                (if (isCredit) "+" else "-") + money(transaction.amount) +
                    (if (transaction.payee.isNotBlank()) "  \u00B7  ${transaction.payee}" else ""),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) },
                label = { Text(if (isCredit) "What for? (e.g. salary)" else "What for? (e.g. biscuit)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Category", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.weight(1f))
                CategoryIconButton(category) { showIconPicker = true }
            }
            CategoryChips(options = options, selected = category, onSelect = { category = it })

            Spacer(Modifier.height(16.dp))
            ReceiptSection(currentText = receiptText, onText = { receiptText = it })

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onSave(category, note, receiptText.ifBlank { null }) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Save") }
            Spacer(Modifier.height(4.dp))
            TextButton(
                onClick = onDelete,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Delete, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Delete this transaction")
            }
        }
    }

    if (showIconPicker) {
        CategoryStyleDialog(
            category = category,
            onDismiss = { showIconPicker = false },
            onPickIcon = { key -> onSetCategoryIcon(category, key) },
            onPickColor = { hex -> onSetCategoryColor(category, hex) }
        )
    }
}

@Composable
fun OpeningBalanceDialog(
    current: Double,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var text by remember { mutableStateOf(if (current != 0.0) amountText(current) else "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onSave(text.toDoubleOrNull() ?: 0.0) }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Opening balance") },
        text = {
            Column {
                Text(
                    "Carried over from last month automatically. Edit it here if " +
                        "the exact amount is different.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = sanitizeAmount(it) },
                    label = { Text("Amount (\u20B9)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    )
}

/** A tappable circular avatar showing a category's current icon. */
@Composable
internal fun CategoryIconButton(category: String, onClick: () -> Unit) {
    val style = categoryStyleFor(category)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(style.color.copy(alpha = 0.15f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(style.icon, contentDescription = "Change icon", tint = style.color, modifier = Modifier.size(22.dp))
    }
}

/** Color swatches + icon grid to customize a category's look. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CategoryStyleDialog(
    category: String,
    onDismiss: () -> Unit,
    onPickIcon: (String) -> Unit,
    onPickColor: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Done") } },
        title = { Text("Style for $category") },
        text = {
            Column {
                Text("Color", style = MaterialTheme.typography.labelLarge)
                FlowRow(modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)) {
                    Palette.colors.forEach { hex ->
                        val c = Palette.parse(hex) ?: MaterialTheme.colorScheme.primary
                        Box(
                            modifier = Modifier
                                .padding(4.dp)
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(c)
                                .clickable { onPickColor(hex) }
                        )
                    }
                }
                Text("Icon", style = MaterialTheme.typography.labelLarge)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    modifier = Modifier.heightIn(max = 240.dp).padding(top = 4.dp)
                ) {
                    items(IconPack.keys, key = { it }) { key ->
                        val vector = IconPack.icon(key)
                        if (vector != null) {
                            Box(
                                modifier = Modifier
                                    .padding(6.dp)
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { onPickIcon(key) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(vector, contentDescription = key, modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    )
}

/** Scan a receipt, extract its text with on-device OCR, and store/edit that text. */
@Composable
internal fun ReceiptSection(
    currentText: String?,
    onText: (String) -> Unit
) {
    val context = LocalContext.current
    var text by remember { mutableStateOf(currentText ?: "") }
    var scanning by remember { mutableStateOf(false) }
    var pendingFile by remember { mutableStateOf<File?>(null) }

    fun runOcr(uri: android.net.Uri, cleanup: (() -> Unit)?) {
        scanning = true
        ReceiptOcr.recognize(context, uri) { result ->
            scanning = false
            cleanup?.invoke()
            if (result != null) {
                text = if (text.isBlank()) result else text + "\n" + result
                onText(text)
            }
        }
    }

    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = pendingFile
        if (ok && f != null) {
            runOcr(ReceiptStore.uriFor(context, f)) { ReceiptStore.delete(f) }
        } else {
            ReceiptStore.delete(f)
        }
        pendingFile = null
    }
    val pickMedia = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) runOcr(uri, null)
    }

    Text("Receipt details", style = MaterialTheme.typography.labelLarge)
    Spacer(Modifier.height(4.dp))
    Row {
        OutlinedButton(
            onClick = {
                val f = ReceiptStore.tempCaptureFile(context)
                pendingFile = f
                takePicture.launch(ReceiptStore.uriFor(context, f))
            },
            enabled = !scanning
        ) {
            Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Scan")
        }
        Spacer(Modifier.width(8.dp))
        OutlinedButton(
            onClick = { pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
            enabled = !scanning
        ) {
            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Gallery")
        }
    }
    if (scanning) {
        Text(
            "Reading receipt\u2026",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
    Spacer(Modifier.height(8.dp))
    OutlinedTextField(
        value = text,
        onValueChange = { text = it; onText(it) },
        label = { Text("Extracted text (editable)") },
        minLines = 3,
        maxLines = 10,
        modifier = Modifier.fillMaxWidth()
    )
    if (text.isNotBlank()) {
        TextButton(
            onClick = { text = ""; onText("") },
            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Clear receipt text")
        }
    }
}

/** Button that shows the chosen date and opens a date picker. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateField(dateMillis: Long, onPick: (Long) -> Unit) {
    var show by remember { mutableStateOf(false) }
    val fmt = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    OutlinedButton(onClick = { show = true }, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("Date: ${fmt.format(Date(dateMillis))}")
    }
    if (show) {
        val state = rememberDatePickerState(initialSelectedDateMillis = dateMillis)
        DatePickerDialog(
            onDismissRequest = { show = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onPick(it + 12L * 60 * 60 * 1000) }
                    show = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { show = false }) { Text("Cancel") } }
        ) { DatePicker(state = state) }
    }
}
