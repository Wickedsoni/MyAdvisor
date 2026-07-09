package com.wickedcoder.myadvisor.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * What transaction a [RewardRule] applies to.
 *
 * Persisted as a polymorphic JSON column (ADR-006) and shared with the
 * dataset JSON format, so subtypes carry stable [SerialName] discriminators.
 * Sealed hierarchy = closed polymorphism; kotlinx.serialization registers
 * subtypes automatically.
 */
@Serializable
sealed interface Condition {

    @Serializable
    @SerialName("merchant")
    data class MerchantIs(val merchantId: String) : Condition

    @Serializable
    @SerialName("merchantFamily")
    data class MerchantFamilyIs(val familyId: String) : Condition

    @Serializable
    @SerialName("category")
    data class CategoryIs(val categoryId: String) : Condition

    @Serializable
    @SerialName("always")
    data object Always : Condition

    @Serializable
    @SerialName("allOf")
    data class AllOf(val conditions: List<Condition>) : Condition
}
