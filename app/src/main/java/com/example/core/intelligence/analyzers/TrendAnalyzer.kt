package com.example.core.intelligence.analyzers

import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.core.intelligence.models.Insight
import com.example.core.intelligence.models.InsightType
import com.example.core.intelligence.rules.TrendRules
import com.example.ui.util.formatCurrency
import java.util.Locale

class TrendAnalyzer {

    fun analyze(
        transactions: List<TransactionEntity>,
        currencySymbol: String = "€",
        isEn: Boolean = false
    ): List<Insight> {
        val insights = mutableListOf<Insight>()

        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        val income = transactions.filter { it.type == TransactionType.INCOME }

        val totalExpense = expenses.sumOf { it.amount }
        val totalIncome = income.sumOf { it.amount }

        if (totalIncome > 0) {
            val savingsRatio = ((totalIncome - totalExpense) / totalIncome) * 100.0
            if (savingsRatio >= 20.0) {
                insights.add(
                    Insight(
                        title = if (isEn) "Strong Savings Rate: ${String.format(Locale.getDefault(), "%.0f", savingsRatio)}%" else "Excellent Taux d'Épargne : ${String.format(Locale.getDefault(), "%.0f", savingsRatio)}%",
                        description = if (isEn)
                            "You are saving ${String.format(Locale.getDefault(), "%.0f", savingsRatio)}% of your income this month. Great financial management!"
                        else
                            "Vous conservez ${String.format(Locale.getDefault(), "%.0f", savingsRatio)}% de vos revenus ce mois-ci. Excellente gestion !",
                        priority = 4,
                        icon = "trending_up",
                        colorHex = "#10B981", // Green
                        type = InsightType.TREND
                    )
                )
            } else if (savingsRatio < 0) {
                insights.add(
                    Insight(
                        title = if (isEn) "Deficit Trend Detected" else "Tendance Déficitaire",
                        description = if (isEn)
                            "Expenses exceed income by ${(totalExpense - totalIncome).formatCurrency(currencySymbol)}."
                        else
                            "Vos dépenses dépassent vos revenus de ${(totalExpense - totalIncome).formatCurrency(currencySymbol)}.",
                        priority = 1,
                        icon = "warning",
                        colorHex = "#EF4444", // Red
                        type = InsightType.TREND
                    )
                )
            }
        }

        return insights
    }
}
