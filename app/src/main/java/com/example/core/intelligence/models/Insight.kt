package com.example.core.intelligence.models

import java.util.UUID

data class Insight(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val priority: Int = 3, // 1 = Critical/Danger, 2 = Warning, 3 = Info, 4 = Success/Positive
    val icon: String = "info", // "warning", "check", "trending_up", "pie_chart", "account_balance", "info", "star"
    val colorHex: String = "#3B82F6",
    val type: InsightType = InsightType.INFO,
    val createdAt: Long = System.currentTimeMillis(),
    val resolved: Boolean = false
)
