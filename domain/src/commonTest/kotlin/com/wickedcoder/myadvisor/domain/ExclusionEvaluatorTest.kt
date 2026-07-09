package com.wickedcoder.myadvisor.domain

import com.wickedcoder.myadvisor.domain.engine.PurchaseQuery
import com.wickedcoder.myadvisor.domain.engine.effectiveExclusionScope
import com.wickedcoder.myadvisor.domain.engine.resolveQueryContext
import com.wickedcoder.myadvisor.domain.model.Exclusion
import com.wickedcoder.myadvisor.domain.model.ExclusionScope
import com.wickedcoder.myadvisor.domain.model.ExclusionTarget
import com.wickedcoder.myadvisor.domain.model.Merchant
import com.wickedcoder.myadvisor.domain.model.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Phase 1 DoD: exclusions evaluate before positive rules. This pins the
 * evaluator's semantics; the engine (Phase 2) consults it as Step 2, before
 * collecting any positive rule.
 */
class ExclusionEvaluatorTest {

    private val merchants = mapOf(
        "amazon_pay_gc" to Merchant("amazon_pay_gc", "amazon", "Amazon Pay Gift Cards", "online_shopping"),
    )

    @Test
    fun `FULL exclusion matches transaction-type hint`() {
        val exclusions = listOf(
            Exclusion(ExclusionTarget.TransactionTypeIs(TransactionType.WALLET_LOAD), ExclusionScope.FULL),
        )
        val context = resolveQueryContext(
            PurchaseQuery(categoryId = "online_shopping", transactionType = TransactionType.WALLET_LOAD),
            merchants,
        )
        assertEquals(ExclusionScope.FULL, exclusions.effectiveExclusionScope(context))
    }

    @Test
    fun `merchant-targeted ACCELERATED_ONLY exclusion falls back to base`() {
        val exclusions = listOf(
            Exclusion(ExclusionTarget.MerchantIs("amazon_pay_gc"), ExclusionScope.ACCELERATED_ONLY),
        )
        val context = resolveQueryContext(PurchaseQuery(merchantId = "amazon_pay_gc"), merchants)
        assertEquals(ExclusionScope.ACCELERATED_ONLY, exclusions.effectiveExclusionScope(context))
    }

    @Test
    fun `category exclusion matches the category resolved from a merchant`() {
        val exclusions = listOf(
            Exclusion(ExclusionTarget.CategoryIs("online_shopping"), ExclusionScope.ACCELERATED_ONLY),
        )
        val context = resolveQueryContext(PurchaseQuery(merchantId = "amazon_pay_gc"), merchants)
        assertEquals(ExclusionScope.ACCELERATED_ONLY, exclusions.effectiveExclusionScope(context))
    }

    @Test
    fun `FULL beats ACCELERATED_ONLY when both match`() {
        val exclusions = listOf(
            Exclusion(ExclusionTarget.MerchantIs("amazon_pay_gc"), ExclusionScope.ACCELERATED_ONLY),
            Exclusion(ExclusionTarget.TransactionTypeIs(TransactionType.GIFT_CARD), ExclusionScope.FULL),
        )
        val context = resolveQueryContext(
            PurchaseQuery(merchantId = "amazon_pay_gc", transactionType = TransactionType.GIFT_CARD),
            merchants,
        )
        assertEquals(ExclusionScope.FULL, exclusions.effectiveExclusionScope(context))
    }

    @Test
    fun `no matching exclusion returns null`() {
        val exclusions = listOf(
            Exclusion(ExclusionTarget.TransactionTypeIs(TransactionType.RENT), ExclusionScope.FULL),
        )
        val context = resolveQueryContext(PurchaseQuery(categoryId = "dining"), merchants)
        assertNull(exclusions.effectiveExclusionScope(context))
    }
}
