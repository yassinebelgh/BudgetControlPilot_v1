package com.example.core.intelligence.analyzers

import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.core.intelligence.models.Insight
import com.example.core.intelligence.models.InsightType
import com.example.ui.util.formatCurrency
import java.util.Calendar
import java.util.Locale

class CashFlowAnalyzer {

    fun analyze(
        transactions: List<TransactionEntity>,
        currencySymbol: String = "€",
        isEn: Boolean = false
    ): List<Insight> {
        val insights = mutableListOf<Insight>()

        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        val totalSpent = expenses.sumOf { it.amount }

        val cal = Calendar.getInstance()
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        if (currentDay > 0 && totalSpent > 0) {
            val dailyBurnRate = totalSpent / currentDay
            val projectedTotal = dailyBurnRate * daysInMonth

            insights.add(
                Insight(
                    title = if (isEn) "Daily Pace: ${dailyBurnRate.formatCurrency(currencySymbol)}/day" else "Rythme Quotidien : ${dailyBurnRate.formatCurrency(currencySymbol)}/jour",
                    description = if (isEn)
                        "At this rate, month-end spending is projected at ~${String.format(Locale.getDefault(), "%.0f", projectedTotal)} $currencySymbol."
                    else
                        "À ce rythme, vos dépenses totales du mois atteindront environ ${String.format(Locale.getDefault(), "%.0f", projectedTotal)} $currencySymbol.",
                    priority = 3,
                    icon = "info",
                    colorHex = "#6366F1", // Indigo
                    type = InsightType.INFO
                )
            )
        }

        return insights
    }
}
