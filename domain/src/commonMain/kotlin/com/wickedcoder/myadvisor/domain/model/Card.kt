package com.wickedcoder.myadvisor.domain.model

import kotlinx.datetime.LocalDate

data class Card(
    val id: String,
    val issuerId: String,
    val name: String,
    val lastVerified: LocalDate,
    val baseRule: RewardRule,
    val rules: List<RewardRule>,
    val exclusions: List<Exclusion>,
)

data class RewardRule(
    val id: String,
    val priority: Int,                 // lower = higher precedence
    val condition: Condition,
    val reward: Reward,
    val cap: Cap? = null,
    val validity: Validity? = null,
    val paymentRoute: PaymentRoute? = null,
)

data class Reward(
    val effectiveRatePct: Double,      // the ONLY number the engine ranks on
    val earnDescription: String,       // "10 RP per ₹150 on SmartBuy"
    val valuationNote: String? = null, // "1 RP valued at ₹0.50 (conservative)"
)

data class Cap(val amountInr: Int, val period: CapPeriod)

enum class CapPeriod { PER_TRANSACTION, CALENDAR_MONTH, STATEMENT_CYCLE, QUARTER, YEAR }

data class Validity(val from: LocalDate?, val until: LocalDate?)

data class PaymentRoute(val id: String, val name: String, val instruction: String)
