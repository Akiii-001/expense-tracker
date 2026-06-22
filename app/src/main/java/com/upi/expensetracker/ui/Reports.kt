package com.upi.expensetracker.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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

    fun share(pdf: Boolean) {
        scope.launch {
            val uri = viewModel.export(data.offset, pdf) ?: return@launch
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
        // Month selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.changeReportMonth(-1) }) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
            }
            Text(data.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(
                onClick = { viewModel.changeReportMonth(1) },
                enabled = data.offset < 0
            ) {
                Icon(Icons.Filled.ChevronRight, contentDescription = "Next month")
            }
        }

        Spacer(Modifier.height(8.dp))
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
}

@Composable
private fun SummaryCard(data: ReportData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Pill("Received", money(data.income), IncomeGreen, Modifier.weight(1f))
                Pill("Spent", money(data.spend), SpendRed, Modifier.weight(1f))
                Pill("Balance", money(data.opening + data.income - data.spend), MaterialTheme.colorScheme.primary, Modifier.weight(1f))
            }
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
                data.byCategory.forEach { row ->
                    CategoryRow(row, data.spend, data.budgets[row.category])
                }
            }
        }
    }
}

@Composable
private fun Pill(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(accent.copy(alpha = 0.12f))
            .padding(vertical = 10.dp, horizontal = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = accent)
        }
    }
}

@Composable
private fun CategoryRow(row: CategoryTotal, totalSpend: Double, budget: Double?) {
    val style = CategoryStyle.of(row.category)
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
