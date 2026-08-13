package com.example.core.intelligence.rules

object TrendRules {
    const val SIGNIFICANT_INCREASE_PERCENT = 15.0 // +15% spending surge
    const val SIGNIFICANT_DECREASE_PERCENT = -15.0 // -15% spending drop

    fun calculateVariation(current: Double, previous: Double): Double {
        if (previous <= 0) return 0.0
        return ((current - previous) / previous) * 100.0
    }
}
