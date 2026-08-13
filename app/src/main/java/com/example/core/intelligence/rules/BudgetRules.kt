package com.example.core.intelligence.rules

object BudgetRules {
    const val WARNING_THRESHOLD_RATIO = 0.90 // 90% of budget limit
    const val EXCEEDED_THRESHOLD_RATIO = 1.00 // 100% of budget limit

    fun isExceeded(spent: Double, limit: Double): Boolean {
        return limit > 0 && spent > limit
    }

    fun isNearLimit(spent: Double, limit: Double): Boolean {
        return limit > 0 && spent >= (limit * WARNING_THRESHOLD_RATIO) && spent <= limit
    }

    fun isRespected(spent: Double, limit: Double): Boolean {
        return limit > 0 && spent <= limit
    }
}
