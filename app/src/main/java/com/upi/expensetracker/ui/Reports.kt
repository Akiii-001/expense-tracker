package com.upi.expensetracker.ui

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.upi.expensetracker.data.CategoryTotal
import com.upi.expensetracker.ui.theme.IncomeGreen
import com.upi.expensetracker.ui.theme.SpendRed
import kotlinx.coroutines.launch

@Composable
fun ReportsScreen(viewModel: ExpenseViewModel, modifier: Modifier = Modifier) {
    val data by viewModel.reportData.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showPicker by remember { mutableStateOf(false) }

    fun share(pdf: Boolean) {
        scope.launch {
            val uri = viewModel.export(pdf) ?: return@launch
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = if (pdf) "application/pdf" else "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share report"))
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Selector: month nav, or a custom range with a way back to months.
        if (data.isRange) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(data.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                TextButton(onClick = { viewModel.showMonthView() }) { Text("Month view") }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.changeReportMonth(-1) }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
                }
                Text(data.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                IconButton(onClick = { viewModel.changeReportMonth(1) }, enabled = data.offset < 0) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Next month")
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Pick custom dates")
        }

        Spacer(Modifier.height(12.dp))
        SummaryCard(data)

        Spacer(Modifier.height(16.dp))
        SpendingCard(data)

        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = { share(false) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.TableChart, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Export CSV")
            }
            OutlinedButton(onClick = { share(true) }, modifier = Modifier.weight(1f)) {
                Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Export PDF")
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showPicker) {
        DateRangePickerDialog(
            onDismiss = { showPicker = false },
            onConfirm = { start, end ->
                viewModel.setCustomRange(start, end)
                showPicker = false
            }
        )
    }
}

@Composable
private fun SummaryCard(data: ReportData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            SummaryRow("Received", money(data.income), IncomeGreen, bold = false)
            Spacer(Modifier.height(10.dp))
            SummaryRow("Spent", money(data.spend), SpendRed, bold = false)
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
            SummaryRow(
                "Balance",
                money(data.opening + data.income - data.spend),
                MaterialTheme.colorScheme.primary,
                bold = true
            )
            if (data.opening != 0.0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Includes opening balance of ${money(data.opening)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String, accent: Color, bold: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Text(
            value,
            style = if (bold) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.SemiBold,
            color = accent,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun SpendingCard(data: ReportData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("Where it went", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (data.byCategory.isEmpty()) {
                Text("No spends this month.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                val slices = data.byCategory.map { row ->
                    row.total.toFloat() to categoryStyleFor(row.category).color
                }
                Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                    DonutChart(slices = slices, modifier = Modifier.size(170.dp))
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Spent", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(money(data.spend), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(8.dp))
                data.byCategory.forEach { row ->
                    CategoryRow(row, data.spend, data.budgets[row.category])
                }
            }
        }
    }
}

@Composable
private fun DonutChart(slices: List<Pair<Float, Color>>, modifier: Modifier = Modifier) {
    val total = slices.sumOf { it.first.toDouble() }.toFloat()
    Canvas(modifier = modifier) {
        val stroke = size.minDimension * 0.16f
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        var startAngle = -90f
        slices.forEach { (value, color) ->
            val sweep = if (total > 0f) value / total * 360f else 0f
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke)
            )
            startAngle += sweep
        }
    }
}

@Composable
private fun CategoryRow(row: CategoryTotal, totalSpend: Double, budget: Double?) {
    val style = categoryStyleFor(row.category)
    // Bar fraction is vs budget if a budget exists, else vs total spend.
    val fraction = when {
        budget != null && budget > 0 -> (row.total / budget).toFloat().coerceIn(0f, 1f)
        totalSpend > 0 -> (row.total / totalSpend).toFloat().coerceIn(0f, 1f)
        else -> 0f
    }
    val over = budget != null && row.total > budget
    val barColor = if (over) SpendRed else style.color

    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(style.color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(style.icon, contentDescription = null, tint = style.color, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(row.category, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    if (budget != null) "${money(row.total)} / ${money(budget)}" else money(row.total),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (over) SpendRed else MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(barColor)
                )
            }
            if (over) {
                Text(
                    "Over by ${money(row.total - (budget ?: 0.0))}",
                    style = MaterialTheme.typography.labelSmall,
                    color = SpendRed
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateRangePickerDialog(
    onDismiss: () -> Unit,
    onConfirm: (Long, Long) -> Unit
) {
    val state = rememberDateRangePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = state.selectedStartDateMillis
                    val end = state.selectedEndDateMillis
                    if (start != null && end != null) onConfirm(start, end)
                },
                enabled = state.selectedStartDateMillis != null && state.selectedEndDateMillis != null
            ) { Text("Apply") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    ) {
        DateRangePicker(
            state = state,
            modifier = Modifier.heightIn(max = 520.dp),
            title = {
                Text(
                    "Select a date range",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        )
    }
}
