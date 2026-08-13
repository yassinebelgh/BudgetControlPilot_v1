package com.example.core.intelligence

import com.example.data.model.BudgetEntity
import com.example.data.model.RealAccountEntity
import com.example.data.model.TransactionEntity
import com.example.core.intelligence.analyzers.*
import com.example.core.intelligence.models.Insight

class CoachEngine {

    private val budgetAnalyzer = BudgetAnalyzer()
    private val expenseAnalyzer = ExpenseAnalyzer()
    private val realityAnalyzer = RealityAnalyzer()
    private val trendAnalyzer = TrendAnalyzer()
    private val cashFlowAnalyzer = CashFlowAnalyzer()
    private val financialScoreAnalyzer = FinancialScoreAnalyzer()

    data class EngineResult(
        val score: Int,
        val topInsights: List<Insight>,
        val allInsights: List<Insight>
    )

    fun evaluate(
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        realAccounts: List<RealAccountEntity>,
        theoreticalBalance: Double,
        currencySymbol: String = "€",
        isEn: Boolean = false
    ): EngineResult {
        val budgetInsights = budgetAnalyzer.analyze(budgets, transactions, currencySymbol, isEn)
        val expenseInsights = expenseAnalyzer.analyze(transactions, currencySymbol, isEn)
        val realityInsights = realityAnalyzer.analyze(theoreticalBalance, realAccounts, currencySymbol, isEn)
        val trendInsights = trendAnalyzer.analyze(transactions, currencySymbol, isEn)
        val cashFlowInsights = cashFlowAnalyzer.analyze(transactions, currencySymbol, isEn)
        val (score, scoreInsights) = financialScoreAnalyzer.analyze(transactions, budgets, realAccounts, theoreticalBalance, isEn)

        val merged = mutableListOf<Insight>()
        merged.addAll(scoreInsights)
        merged.addAll(budgetInsights)
        merged.addAll(realityInsights)
        merged.addAll(expenseInsights)
        merged.addAll(trendInsights)
        merged.addAll(cashFlowInsights)

        // Sort by priority (1 is highest priority)
        val sorted = merged.sortedBy { it.priority }

        return EngineResult(
            score = score,
            topInsights = sorted.take(6),
            allInsights = sorted
        )
    }
}
