package com.example.ui.util

import java.util.Locale
import kotlin.math.abs
import kotlin.math.round

/**
 * Formats a Double number according to app rules:
 * - If the number has no decimal part (e.g. 200.0 or 200.00), returns integer string "200".
 * - If it has decimals (e.g. 28.233), returns up to 2 decimal places (e.g. "28.23").
 * - Trims unnecessary trailing zeros if any (e.g. 28.20 -> "28.2").
 */
fun Double.formatAmount(): String {
    if (this % 1.0 == 0.0 || abs(this - round(this)) < 0.00001) {
        return this.toLong().toString()
    }
    val formatted = String.format(Locale.US, "%.2f", this)
    return formatted.dropLastWhile { it == '0' }.dropLastWhile { it == '.' }
}

fun Float.formatAmount(): String = this.toDouble().formatAmount()

fun Double.formatCurrency(currencySymbol: String = "€"): String {
    val formatted = this.formatAmount()
    return if (currencySymbol.isBlank()) formatted else "$formatted $currencySymbol"
}

fun Float.formatCurrency(currencySymbol: String = "€"): String {
    return this.toDouble().formatCurrency(currencySymbol)
}
