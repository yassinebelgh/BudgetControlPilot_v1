package com.example.core.intelligence.rules

object RealityRules {
    const val MATCH_TOLERANCE_EUROS = 1.0 // Gap < 1€ is considered perfect match

    fun isPerfectMatch(theoreticalBalance: Double, realBalance: Double): Boolean {
        return kotlin.math.abs(theoreticalBalance - realBalance) < MATCH_TOLERANCE_EUROS
    }

    fun getGap(theoreticalBalance: Double, realBalance: Double): Double {
        return realBalance - theoreticalBalance
    }
}
