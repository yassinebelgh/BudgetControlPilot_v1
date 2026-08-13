package com.example.core.intelligence.analyzers

import com.example.data.model.BudgetEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.core.intelligence.models.Insight
import com.example.core.intelligence.models.InsightType
import com.example.core.intelligence.rules.BudgetRules
import com.example.ui.util.formatCurrency
import java.util.Locale

class BudgetAnalyzer {

    fun analyze(
        budgets: List<BudgetEntity>,
        transactions: List<TransactionEntity>,
        currencySymbol: String = "€",
        isEn: Boolean = false
    ): List<Insight> {
        val insights = mutableListOf<Insight>()

        if (budgets.isEmpty()) return insights

        val expensesByCategory = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        budgets.forEach { budget ->
            val spent = expensesByCategory[budget.category] ?: 0.0
            val limit = budget.monthlyLimit

            if (BudgetRules.isExceeded(spent, limit)) {
                val excess = spent - limit
                insights.add(
                    Insight(
                        title = if (isEn) "Budget Exceeded: ${budget.category}" else "Budget dépassé : ${budget.category}",
                        description = if (isEn)
                            "You exceeded your monthly limit for ${budget.category} by ${excess.formatCurrency(currencySymbol)} (${spent.formatCurrency(currencySymbol)} / ${limit.formatCurrency(currencySymbol)})."
                        else
                            "Vous avez dépassé votre budget ${budget.category} de ${excess.formatCurrency(currencySymbol)} (${spent.formatCurrency(currencySymbol)} / ${limit.formatCurrency(currencySymbol)}).",
                        priority = 1,
                        icon = "warning",
                        colorHex = "#EF4444", // Red
                        type = InsightType.BUDGET
                    )
                )
            } else if (BudgetRules.isNearLimit(spent, limit)) {
                val pct = (spent / limit) * 100.0
                insights.add(
                    Insight(
                        title = if (isEn) "Budget Warning (>90%): ${budget.category}" else "Attention Budget (>90%) : ${budget.category}",
                        description = if (isEn)
                            "You have used ${String.format(Locale.getDefault(), "%.0f", pct)}% of your ${budget.category} budget (${spent.formatCurrency(currencySymbol)} / ${limit.formatCurrency(currencySymbol)})."
                        else
                            "Vous avez consommé ${String.format(Locale.getDefault(), "%.0f", pct)}% de votre budget ${budget.category} (${spent.formatCurrency(currencySymbol)} / ${limit.formatCurrency(currencySymbol)}).",
                        priority = 2,
                        icon = "pie_chart",
                        colorHex = "#F59E0B", // Amber
                        type = InsightType.BUDGET
                    )
                )
            } else if (limit > 0 && spent > 0) {
                val remaining = limit - spent
                insights.add(
                    Insight(
                        title = if (isEn) "Budget Respected: ${budget.category}" else "Budget maitrisé : ${budget.category}",
                        description = if (isEn)
                            "Remaining budget: ${remaining.formatCurrency(currencySymbol)} out of ${limit.formatCurrency(currencySymbol)}."
                        else
                            "Reste disponible : ${remaining.formatCurrency(currencySymbol)} sur un total de ${limit.formatCurrency(currencySymbol)}.",
                        priority = 4,
                        icon = "check",
                        colorHex = "#10B981", // Green
                        type = InsightType.SUCCESS
                    )
                )
            }
        }

        return insights
    }
}
