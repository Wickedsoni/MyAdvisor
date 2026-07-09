package com.wickedcoder.myadvisor.domain

import com.wickedcoder.myadvisor.domain.engine.PurchaseQuery
import com.wickedcoder.myadvisor.domain.engine.matches
import com.wickedcoder.myadvisor.domain.engine.resolveQueryContext
import com.wickedcoder.myadvisor.domain.model.Condition
import com.wickedcoder.myadvisor.domain.model.Merchant
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Phase 1 DoD: the merchant → family → category precedence chain must
 * resolve Amazon vs Amazon Fresh vs Amazon Pay differently.
 */
class ConditionMatcherTest {

    private val merchants = listOf(
        Merchant("amazon_in", familyId = "amazon", name = "Amazon.in", categoryId = "online_shopping"),
        Merchant("amazon_fresh", familyId = "amazon", name = "Amazon Fresh", categoryId = "groceries"),
        Merchant("amazon_pay_gc", familyId = "amazon", name = "Amazon Pay Gift Cards", categoryId = "online_shopping"),
        Merchant("swiggy", familyId = null, name = "Swiggy", categoryId = "dining"),
    ).associateBy { it.id }

    private fun contextFor(merchantId: String) =
        resolveQueryContext(PurchaseQuery(merchantId = merchantId), merchants)

    @Test
    fun `merchant condition matches only the exact merchant`() {
        val condition = Condition.MerchantIs("amazon_fresh")
        assertTrue(condition.matches(contextFor("amazon_fresh")))
        assertFalse(condition.matches(contextFor("amazon_in")))
        assertFalse(condition.matches(contextFor("amazon_pay_gc")))
    }

    @Test
    fun `family condition matches every merchant in the family`() {
        val condition = Condition.MerchantFamilyIs("amazon")
        assertTrue(condition.matches(contextFor("amazon_in")))
        assertTrue(condition.matches(contextFor("amazon_fresh")))
        assertTrue(condition.matches(contextFor("amazon_pay_gc")))
        assertFalse(condition.matches(contextFor("swiggy")))
    }

    @Test
    fun `category condition matches via the merchant's default category`() {
        val groceries = Condition.CategoryIs("groceries")
        assertTrue(groceries.matches(contextFor("amazon_fresh")))
        assertFalse(groceries.matches(contextFor("amazon_in")))
    }

    @Test
    fun `category-only query resolves without merchant or family`() {
        val context = resolveQueryContext(PurchaseQuery(categoryId = "dining"), merchants)
        assertTrue(Condition.CategoryIs("dining").matches(context))
        assertFalse(Condition.MerchantIs("swiggy").matches(context))
        assertFalse(Condition.MerchantFamilyIs("amazon").matches(context))
    }

    @Test
    fun `always matches everything`() {
        assertTrue(Condition.Always.matches(contextFor("swiggy")))
        assertTrue(
            Condition.Always.matches(resolveQueryContext(PurchaseQuery(categoryId = "travel"), merchants)),
        )
    }

    @Test
    fun `allOf requires every part to match`() {
        val condition = Condition.AllOf(
            listOf(Condition.MerchantFamilyIs("amazon"), Condition.CategoryIs("groceries")),
        )
        assertTrue(condition.matches(contextFor("amazon_fresh")))
        assertFalse(condition.matches(contextFor("amazon_in"))) // family yes, category no
        assertFalse(condition.matches(contextFor("swiggy")))
    }
}
