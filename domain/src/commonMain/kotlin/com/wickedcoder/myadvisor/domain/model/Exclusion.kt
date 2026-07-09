package com.wickedcoder.myadvisor.domain.model

/**
 * Negative rules, evaluated BEFORE positive rules (Spec D3).
 */
data class Exclusion(val target: ExclusionTarget, val scope: ExclusionScope)

sealed interface ExclusionTarget {
    data class MerchantIs(val merchantId: String) : ExclusionTarget
    data class CategoryIs(val categoryId: String) : ExclusionTarget
    data class TransactionTypeIs(val type: TransactionType) : ExclusionTarget
}

enum class ExclusionScope {
    /** The card earns nothing on the transaction. */
    FULL,

    /** Accelerated rules don't apply; the base rule still does. */
    ACCELERATED_ONLY,
}

/**
 * Curated approximation of MCC-class transaction types (Spec §8 —
 * real MCC behavior varies by network and is not modeled in v1).
 */
enum class TransactionType {
    RENT, WALLET_LOAD, GIFT_CARD, FUEL, EMI, INSURANCE,
    GOVERNMENT, EDUCATION, UTILITY, JEWELLERY,
}
