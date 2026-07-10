package com.wickedcoder.myadvisor.ui.components

import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Presentation-only number formatting shared across components. Pure Kotlin (no
 * platform `NumberFormat`) so it stays in `commonMain` and renders identically on
 * Android and iOS. Rounding here is display sugar only — the engine's own values
 * are authoritative.
 */

/** "7.5%", "2%", "1.33%" — trims trailing zeros, caps at 2 decimals. */
fun formatRatePct(pct: Double): String {
    val rounded = (pct * 100).roundToInt() / 100.0
    return if (rounded == rounded.toInt().toDouble()) "${rounded.toInt()}%" else "$rounded%"
}

/** Indian grouping: 1500 -> "1,500", 150000 -> "1,50,000". */
fun formatInr(amount: Number): String {
    val value = amount.toDouble().roundToLong()
    val negative = value < 0
    val digits = kotlin.math.abs(value).toString()
    if (digits.length <= 3) return (if (negative) "-" else "") + digits

    val last3 = digits.takeLast(3)
    val rest = digits.dropLast(3)
    val grouped = StringBuilder()
    // Group the remaining digits in pairs from the right (Indian lakh/crore style).
    val restChars = rest.reversed()
    for (i in restChars.indices) {
        grouped.append(restChars[i])
        if (i % 2 == 1 && i != restChars.lastIndex) grouped.append(',')
    }
    val head = grouped.reverse().toString()
    return (if (negative) "-" else "") + "$head,$last3"
}
