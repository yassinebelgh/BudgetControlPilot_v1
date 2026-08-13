package com.example.core.intelligence.analyzers

import com.example.data.model.BudgetEntity
import com.example.data.model.RealAccountEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.TransactionType
import com.example.core.intelligence.models.Insight
import com.example.core.intelligence.models.InsightType
import java.util.Locale

class FinancialScoreAnalyzer {

    fun analyze(
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        realAccounts: List<RealAccountEntity>,
        theoreticalBalance: Double,
        isEn: Boolean = false
    ): Pair<Int, List<Insight>> {
        var score = 70 // Base baseline

        val expenses = transactions.filter { it.type == TransactionType.EXPENSE }
        val income = transactions.filter { it.type == TransactionType.INCOME }
        val totalSpent = expenses.sumOf { it.amount }
        val totalIncome = income.sumOf { it.amount }

        // 1. Savings Rate Impact (+15 max or -20 max)
        if (totalIncome > 0) {
            val savingsRate = ((totalIncome - totalSpent) / totalIncome) * 100.0
            if (savingsRate >= 20.0) score += 15
            else if (savingsRate >= 10.0) score += 8
            else if (savingsRate < 0) score -= 20
        }

        // 2. Budget Discipline Impact (+15 or -15)
        if (budgets.isNotEmpty()) {
            val spentByCat = expenses.groupBy { it.category }.mapValues { it.value.sumOf { tx -> tx.amount } }
            val exceededCount = budgets.count { b -> (spentByCat[b.category] ?: 0.0) > b.monthlyLimit }
            if (exceededCount == 0) score += 15
            else score -= (exceededCount * 10)
        }

        // 3. Reality Match Impact (+10 or -10)
        val totalRealBalance = realAccounts.sumOf { a -> if (a.accountType == "DEBT") -kotlin.math.abs(a.balance) else a.balance }
        if (realAccounts.isNotEmpty()) {
            val gap = kotlin.math.abs(totalRealBalance - theoreticalBalance)
            if (gap < 1.0) score += 10
            else if (gap > 50.0) score -= 10
        }

        score = score.coerceIn(0, 100)

        val (badgeTitle, colorHex) = when {
            score >= 85 -> Pair(if (isEn) "Financial Score: $score/100 (Excellent)" else "Score Financier : $score/100 (Excellent)", "#10B981")
            score >= 65 -> Pair(if (isEn) "Financial Score: $score/100 (Good)" else "Score Financier : $score/100 (Bon)", "#3B82F6")
            score >= 45 -> Pair(if (isEn) "Financial Score: $score/100 (Average)" else "Score Financier : $score/100 (Moyen)", "#F59E0B")
            else -> Pair(if (isEn) "Financial Score: $score/100 (Requires Attention)" else "Score Financier : $score/100 (À Surveiller)", "#EF4444")
        }

        val insight = Insight(
            title = badgeTitle,
            description = if (isEn)
                "Calculated based on your savings rate, budget compliance, and account accuracy."
            else
                "Calculé selon votre taux d'épargne, le respect de vos budgets et l'exactitude de vos comptes.",
            priority = 2,
            icon = "star",
            colorHex = colorHex,
            type = InsightType.SUCCESS
        )

        return Pair(score, listOf(insight))
    }
}
