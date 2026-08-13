package com.example.core.intelligence.analyzers

import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.core.intelligence.models.Insight
import com.example.core.intelligence.models.InsightType
import com.example.ui.util.formatCurrency
import java.util.Locale

class ExpenseAnalyzer {

    fun analyze(
        transactions: List<TransactionEntity>,
        currencySymbol: String = "€",
        isEn: Boolean = false
    ): List<Insight> {
        val insights = mutableListOf<Insight>()

        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        if (expenses.isEmpty()) return insights

        val totalSpent = expenses.sumOf { it.amount }
        val categoryTotals = expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
            .toList()
            .sortedByDescending { it.second }

        // Top Spending Category
        val topCategory = categoryTotals.firstOrNull()
        if (topCategory != null && totalSpent > 0) {
            val pct = (topCategory.second / totalSpent) * 100.0
            insights.add(
                Insight(
                    title = if (isEn) "Top Expense: ${topCategory.first}" else "Poste principal : ${topCategory.first}",
                    description = if (isEn)
                        "${topCategory.first} represents ${String.format(Locale.getDefault(), "%.1f", pct)}% of your total expenses (${topCategory.second.formatCurrency(currencySymbol)})."
                    else
                        "${topCategory.first} représente ${String.format(Locale.getDefault(), "%.1f", pct)}% de vos dépenses totales (${topCategory.second.formatCurrency(currencySymbol)}).",
                    priority = 3,
                    icon = "pie_chart",
                    colorHex = "#8B5CF6", // Purple
                    type = InsightType.INFO
                )
            )
        }

        // Unusual single transaction spike (> 25% of total monthly expenses)
        val largestTx = expenses.maxByOrNull { it.amount }
        if (largestTx != null && totalSpent > 0 && largestTx.amount > (totalSpent * 0.25) && largestTx.amount >= 50.0) {
            insights.add(
                Insight(
                    title = if (isEn) "Significant Expense Detected" else "Dépense majeure détectée",
                    description = if (isEn)
                        "'${largestTx.title}' (${largestTx.amount.formatCurrency(currencySymbol)}) accounts for a large portion of this month's spending."
                    else
                        "'${largestTx.title}' (${largestTx.amount.formatCurrency(currencySymbol)}) représente une part très importante de vos sorties.",
                    priority = 2,
                    icon = "warning",
                    colorHex = "#3B82F6", // Blue
                    type = InsightType.INFO
                )
            )
        }

        return insights
    }
}
