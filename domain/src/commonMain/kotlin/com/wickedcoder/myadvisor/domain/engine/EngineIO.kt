package com.wickedcoder.myadvisor.domain.engine

import com.wickedcoder.myadvisor.domain.model.Card
import com.wickedcoder.myadvisor.domain.model.RewardRule
import com.wickedcoder.myadvisor.domain.model.TransactionType
import kotlinx.datetime.LocalDate

data class PurchaseQuery(
    val merchantId: String? = null,    // exactly one of merchantId / categoryId set
    val categoryId: String? = null,
    val amountInr: Int? = null,
    val transactionType: TransactionType? = null, // optional user hint, e.g. "this is a gift card"
)

data class Recommendation(
    val card: Card,
    val winningRule: RewardRule,       // may be the base rule
    val nominalRatePct: Double,
    val effectiveValueInr: Int?,       // null when amount absent
    val effectiveRatePct: Double,      // == nominal when uncapped or amount absent
    val capCaveat: String?,            // "Assumes you haven't used this month's ₹1,500 cap"
    val routeInstruction: String?,     // Phase 3 UI; populated when winningRule has a paymentRoute
    val explanation: Explanation,
)

data class Explanation(
    val ruleProvenance: String,        // "Swiggy merchant rule overrides online-shopping category rule"
    val earnDescription: String,
    val valuationNote: String?,
    val exclusionNotes: List<String>,  // "Accelerated rate excluded for gift cards; base 1% applies"
    val dataVerified: LocalDate,
    val dataVersion: String,
)
