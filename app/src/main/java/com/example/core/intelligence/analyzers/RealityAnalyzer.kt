package com.example.core.intelligence.analyzers

import com.example.data.model.RealAccountEntity
import com.example.core.intelligence.models.Insight
import com.example.core.intelligence.models.InsightType
import com.example.core.intelligence.rules.RealityRules
import com.example.ui.util.formatCurrency
import java.util.Locale

class RealityAnalyzer {

    fun analyze(
        theoreticalBalance: Double,
        realAccounts: List<RealAccountEntity>,
        currencySymbol: String = "€",
        isEn: Boolean = false
    ): List<Insight> {
        val insights = mutableListOf<Insight>()

        val totalRealBalance = realAccounts.sumOf { account ->
            if (account.accountType == "DEBT") -kotlin.math.abs(account.balance) else account.balance
        }

        if (realAccounts.isEmpty()) {
            insights.add(
                Insight(
                    title = if (isEn) "No Real Accounts Configured" else "Aucun compte réel configuré",
                    description = if (isEn)
                        "Add your real bank accounts & cash balances in Tools tab to compare theory vs reality."
                    else
                        "Ajoutez vos comptes bancaires et cash réels dans l'onglet Outils pour suivre l'écart théorie/réalité.",
                    priority = 3,
                    icon = "account_balance",
                    colorHex = "#6B7280", // Gray
                    type = InsightType.REALITY
                )
            )
            return insights
        }

        val isMatch = RealityRules.isPerfectMatch(theoreticalBalance, totalRealBalance)
        val gap = RealityRules.getGap(theoreticalBalance, totalRealBalance)

        if (isMatch) {
            insights.add(
                Insight(
                    title = if (isEn) "Perfect Reality Match" else "Concordance Parfaite Théorie/Réalité",
                    description = if (isEn)
                        "Your real accounts match your theoretical balance perfectly (${totalRealBalance.formatCurrency(currencySymbol)})."
                    else
                        "Vos comptes réels correspondent exactement à votre solde théorique (${totalRealBalance.formatCurrency(currencySymbol)}).",
                    priority = 4,
                    icon = "check",
                    colorHex = "#10B981", // Green
                    type = InsightType.SUCCESS
                )
            )
        } else {
            val gapAbs = kotlin.math.abs(gap)
            val isPositiveGap = gap > 0

            insights.add(
                Insight(
                    title = if (isEn) "Theory/Reality Gap: ${gapAbs.formatCurrency(currencySymbol)}" else "Écart Théorie/Réalité : ${gapAbs.formatCurrency(currencySymbol)}",
                    description = if (isEn) {
                        if (isPositiveGap) "Your real cash balance is higher than theoretical by ${gapAbs.formatCurrency(currencySymbol)}."
                        else "Your real cash balance is lower than theoretical by ${gapAbs.formatCurrency(currencySymbol)}. Check unrecorded expenses."
                    } else {
                        if (isPositiveGap) "Votre disponible réel dépasse votre théorie de +${gapAbs.formatCurrency(currencySymbol)}."
                        else "Il manque ${gapAbs.formatCurrency(currencySymbol)} dans vos comptes réels par rapport aux calculs théoriques."
                    },
                    priority = 1,
                    icon = "warning",
                    colorHex = "#F59E0B", // Orange/Amber
                    type = InsightType.REALITY
                )
            )
        }

        return insights
    }
}
