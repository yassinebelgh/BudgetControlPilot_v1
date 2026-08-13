package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.viewmodel.AppLanguage

import com.example.ui.util.formatCurrency

@Composable
fun CanvasBarChart(
    incomeAmount: Double,
    expenseAmount: Double,
    currencySymbol: String = "€",
    appLanguage: AppLanguage = AppLanguage.FR,
    modifier: Modifier = Modifier
) {
    val isEn = appLanguage == AppLanguage.EN
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (isEn) "Financial Flow (Income vs Expenses)" else "Flux Financier (Revenus vs Dépenses)",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            val maxVal = maxOf(incomeAmount, expenseAmount, 100.0)
            val incomeRatio = (incomeAmount / maxVal).toFloat().coerceIn(0.05f, 1f)
            val expenseRatio = (expenseAmount / maxVal).toFloat().coerceIn(0.05f, 1f)

            val incomeColor = Color(0xFF10B981)
            val expenseColor = Color(0xFFEF4444)

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                val barWidth = size.width * 0.28f
                val chartHeight = size.height - 30f

                // Income Bar
                val incomeBarHeight = chartHeight * incomeRatio
                val incomeX = size.width * 0.2f - barWidth / 2
                drawRoundRect(
                    color = incomeColor,
                    topLeft = Offset(incomeX, chartHeight - incomeBarHeight),
                    size = Size(barWidth, incomeBarHeight),
                    cornerRadius = CornerRadius(12f, 12f)
                )

                // Expense Bar
                val expenseBarHeight = chartHeight * expenseRatio
                val expenseX = size.width * 0.8f - barWidth / 2
                drawRoundRect(
                    color = expenseColor,
                    topLeft = Offset(expenseX, chartHeight - expenseBarHeight),
                    size = Size(barWidth, expenseBarHeight),
                    cornerRadius = CornerRadius(12f, 12f)
                )

                // Base Line
                drawLine(
                    color = Color.Gray.copy(alpha = 0.3f),
                    start = Offset(0f, chartHeight),
                    end = Offset(size.width, chartHeight),
                    strokeWidth = 3f
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isEn) "Income" else "Revenus",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = incomeAmount.formatCurrency(currencySymbol),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF10B981)
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (isEn) "Expenses" else "Dépenses",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = expenseAmount.formatCurrency(currencySymbol),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFFEF4444)
                    )
                }
            }
        }
    }
}
