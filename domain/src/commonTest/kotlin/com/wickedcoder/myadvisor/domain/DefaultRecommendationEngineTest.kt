package com.wickedcoder.myadvisor.domain

import com.wickedcoder.myadvisor.domain.engine.DefaultRecommendationEngine
import com.wickedcoder.myadvisor.domain.engine.PurchaseQuery
import com.wickedcoder.myadvisor.domain.model.Cap
import com.wickedcoder.myadvisor.domain.model.CapPeriod
import com.wickedcoder.myadvisor.domain.model.Card
import com.wickedcoder.myadvisor.domain.model.Category
import com.wickedcoder.myadvisor.domain.model.Condition
import com.wickedcoder.myadvisor.domain.model.Exclusion
import com.wickedcoder.myadvisor.domain.model.ExclusionScope
import com.wickedcoder.myadvisor.domain.model.ExclusionTarget
import com.wickedcoder.myadvisor.domain.model.Merchant
import com.wickedcoder.myadvisor.domain.model.MerchantFamily
import com.wickedcoder.myadvisor.domain.model.PaymentRoute
import com.wickedcoder.myadvisor.domain.model.Reward
import com.wickedcoder.myadvisor.domain.model.RewardRule
import com.wickedcoder.myadvisor.domain.model.TransactionType
import com.wickedcoder.myadvisor.domain.model.Validity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.datetime.LocalDate

/**
 * Required engine test suite (Rule Engine Spec §7, items 1-9; item 10 —
 * validator fixtures — lives in :data's DatasetValidatorTest).
 */
class DefaultRecommendationEngineTest {

    private val today = LocalDate.parse("2026-07-10")

    private val merchants = listOf(
        Merchant("bigmart", familyId = null, name = "BigMart", categoryId = "groceries"),
        Merchant("swiggy", familyId = null, name = "Swiggy", categoryId = "dining"),
        Merchant("amazon_in", familyId = "amazon", name = "Amazon.in", categoryId = "online_shopping"),
        Merchant("amazon_fresh", familyId = "amazon", name = "Amazon Fresh", categoryId = "groceries"),
    )
    private val categories = listOf(
        Category("groceries", "Groceries"),
        Category("dining", "Dining"),
        Category("online_shopping", "Online Shopping"),
    )
    private val families = listOf(MerchantFamily("amazon", "Amazon"))

    private fun engine() = DefaultRecommendationEngine(
        merchantsById = merchants.associateBy { it.id },
        categoriesById = categories.associateBy { it.id },
        familiesById = families.associateBy { it.id },
        dataVersion = "1.0.0",
        today = today,
    )

    private fun rule(
        id: String,
        priority: Int = 10,
        condition: Condition = Condition.CategoryIs("groceries"),
        ratePct: Double = 5.0,
        cap: Cap? = null,
        validity: Validity? = null,
        route: PaymentRoute? = null,
    ) = RewardRule(
        id = id,
        priority = priority,
        condition = condition,
        reward = Reward(effectiveRatePct = ratePct, earnDescription = "$ratePct%"),
        cap = cap,
        validity = validity,
        paymentRoute = route,
    )

    private fun card(
        id: String,
        baseRatePct: Double = 1.0,
        rules: List<RewardRule> = emptyList(),
        exclusions: List<Exclusion> = emptyList(),
    ) = Card(
        id = id,
        issuerId = "bank",
        name = id,
        lastVerified = today,
        baseRule = rule("${id}_base", priority = 1000, condition = Condition.Always, ratePct = baseRatePct),
        rules = rules,
        exclusions = exclusions,
    )

    private val monthlyCap1000 = Cap(1000, CapPeriod.CALENDAR_MONTH)

    // Spec §6 example A: 5% capped ₹1,000/mo vs unlimited 2%
    private val cappedCard = card(
        "card_a",
        rules = listOf(rule("a_groceries", ratePct = 5.0, cap = monthlyCap1000)),
    )
    private val uncappedCard = card("card_b", baseRatePct = 2.0)

    // §7.1 — cap crossover flips the winner between two amounts
    @Test
    fun `cap crossover flips winner with amount`() {
        // ₹45,000: A = min(2250, 1000) = ₹1,000 → 2.22% beats B ₹900 → 2.0%
        val at45k = engine().recommend(
            PurchaseQuery(categoryId = "groceries", amountInr = 45_000),
            listOf(cappedCard, uncappedCard),
        )
        assertEquals("card_a", at45k[0].card.id)
        assertEquals(1000, at45k[0].effectiveValueInr)
        assertEquals(900, at45k[1].effectiveValueInr)

        // ₹60,000: B ₹1,200 → 2.0% beats A ₹1,000 → 1.67%
        val at60k = engine().recommend(
            PurchaseQuery(categoryId = "groceries", amountInr = 60_000),
            listOf(cappedCard, uncappedCard),
        )
        assertEquals("card_b", at60k[0].card.id)
        assertEquals(1200, at60k[0].effectiveValueInr)
        assertEquals(1000, at60k[1].effectiveValueInr)
    }

    // §7.2 — FULL exclusion zeroes an otherwise-winning card
    @Test
    fun `FULL exclusion zeroes an otherwise-winning card`() {
        val excluded = card(
            "excluder",
            rules = listOf(rule("e_groceries", ratePct = 10.0)),
            exclusions = listOf(
                Exclusion(ExclusionTarget.TransactionTypeIs(TransactionType.WALLET_LOAD), ExclusionScope.FULL),
            ),
        )
        val results = engine().recommend(
            PurchaseQuery(categoryId = "groceries", transactionType = TransactionType.WALLET_LOAD),
            listOf(excluded, uncappedCard),
        )
        assertEquals("card_b", results[0].card.id)
        val zeroed = results.first { it.card.id == "excluder" }
        assertEquals(0.0, zeroed.effectiveRatePct)
        assertTrue(zeroed.explanation.exclusionNotes.single().contains("wallet loads"))
    }

    // §7.3 — ACCELERATED_ONLY falls back to the base rule
    @Test
    fun `ACCELERATED_ONLY exclusion falls back to base rule`() {
        val cardWithExclusion = card(
            "acc_only",
            baseRatePct = 1.5,
            rules = listOf(rule("acc_family", condition = Condition.MerchantFamilyIs("amazon"), ratePct = 5.0)),
            exclusions = listOf(
                Exclusion(ExclusionTarget.MerchantIs("amazon_in"), ExclusionScope.ACCELERATED_ONLY),
            ),
        )
        val result = engine().recommend(
            PurchaseQuery(merchantId = "amazon_in"),
            listOf(cardWithExclusion),
        ).single()
        assertEquals("acc_only_base", result.winningRule.id)
        assertEquals(1.5, result.effectiveRatePct)
        assertTrue(result.explanation.exclusionNotes.single().contains("base rate applies"))
    }

    // §7.4 — priority resolves overlap; equal-priority backstop is deterministic
    @Test
    fun `priority resolves overlapping rules`() {
        val overlapping = card(
            "overlap",
            rules = listOf(
                rule("merchant_rule", priority = 10, condition = Condition.MerchantIs("swiggy"), ratePct = 10.0),
                rule("category_rule", priority = 20, condition = Condition.CategoryIs("dining"), ratePct = 5.0),
            ),
        )
        val result = engine().recommend(PurchaseQuery(merchantId = "swiggy"), listOf(overlapping)).single()
        assertEquals("merchant_rule", result.winningRule.id)
        assertEquals("Swiggy merchant rule overrides Dining category rule", result.explanation.ruleProvenance)
    }

    @Test
    fun `equal-priority backstop is deterministic - rate then id`() {
        val samePriority = card(
            "backstop",
            rules = listOf(
                rule("z_rule", priority = 10, ratePct = 5.0),
                rule("a_rule", priority = 10, ratePct = 5.0), // same rate → lexicographic id
                rule("mid_rule", priority = 10, ratePct = 7.0), // higher rate wins first
            ),
        )
        val result = engine().recommend(PurchaseQuery(categoryId = "groceries"), listOf(samePriority)).single()
        assertEquals("mid_rule", result.winningRule.id)

        val sameRate = card(
            "backstop2",
            rules = listOf(rule("z_rule", priority = 10), rule("a_rule", priority = 10)),
        )
        val result2 = engine().recommend(PurchaseQuery(categoryId = "groceries"), listOf(sameRate)).single()
        assertEquals("a_rule", result2.winningRule.id)
    }

    // §7.5 — merchant → family → category → base fallback, each level
    @Test
    fun `merchant to family to category to base fallback chain`() {
        val chainCard = card(
            "chain",
            rules = listOf(
                rule("chain_merchant", priority = 10, condition = Condition.MerchantIs("amazon_fresh"), ratePct = 10.0),
                rule("chain_family", priority = 20, condition = Condition.MerchantFamilyIs("amazon"), ratePct = 7.0),
                rule("chain_category", priority = 30, condition = Condition.CategoryIs("groceries"), ratePct = 5.0),
            ),
        )

        fun winner(query: PurchaseQuery) =
            engine().recommend(query, listOf(chainCard)).single().winningRule.id

        // merchant-specific beats everything
        assertEquals("chain_merchant", winner(PurchaseQuery(merchantId = "amazon_fresh")))
        // different amazon merchant: family rule wins
        assertEquals("chain_family", winner(PurchaseQuery(merchantId = "amazon_in")))
        // non-family groceries merchant: category rule wins
        assertEquals("chain_category", winner(PurchaseQuery(merchantId = "bigmart")))
        // nothing matches: base
        assertEquals("chain_base", winner(PurchaseQuery(merchantId = "swiggy")))
    }

    // §7.6 — missing amount: nominal rate + cap caveat present
    @Test
    fun `missing amount gives nominal rate and cap caveat`() {
        val result = engine().recommend(
            PurchaseQuery(categoryId = "groceries"),
            listOf(cappedCard),
        ).single()
        assertEquals(5.0, result.effectiveRatePct)
        assertNull(result.effectiveValueInr)
        assertEquals("Assumes you haven't used this month's ₹1,000 cap", result.capCaveat)
    }

    // §7.7 — expired validity rule never wins
    @Test
    fun `expired validity rule never wins`() {
        val expired = card(
            "expired",
            rules = listOf(
                rule(
                    "old_promo",
                    ratePct = 20.0,
                    validity = Validity(from = null, until = LocalDate.parse("2026-06-30")),
                ),
            ),
        )
        val result = engine().recommend(PurchaseQuery(categoryId = "groceries"), listOf(expired)).single()
        assertEquals("expired_base", result.winningRule.id)

        val active = card(
            "active",
            rules = listOf(
                rule(
                    "live_promo",
                    ratePct = 20.0,
                    validity = Validity(from = LocalDate.parse("2026-07-01"), until = LocalDate.parse("2026-07-31")),
                ),
            ),
        )
        val result2 = engine().recommend(PurchaseQuery(categoryId = "groceries"), listOf(active)).single()
        assertEquals("live_promo", result2.winningRule.id)
    }

    // §7.8 — route-bearing rule emits routeInstruction
    @Test
    fun `route-bearing rule emits routeInstruction`() {
        val routed = card(
            "routed",
            rules = listOf(
                rule(
                    "smartbuy_rule",
                    ratePct = 6.65,
                    route = PaymentRoute("smartbuy", "SmartBuy", "Book via the SmartBuy portal"),
                ),
            ),
        )
        val result = engine().recommend(PurchaseQuery(categoryId = "groceries"), listOf(routed)).single()
        assertEquals("Book via the SmartBuy portal", result.routeInstruction)
    }

    // A1 — route variants (Spec §5 Step 7): route rule wins, direct rule rides along.
    @Test
    fun `route rule wins and attaches the direct route-free rule as alternative`() {
        val regalia = card(
            "regalia",
            baseRatePct = 1.33,
            rules = listOf(
                rule(
                    "smartbuy_travel",
                    priority = 30,
                    condition = Condition.CategoryIs("groceries"),
                    ratePct = 6.65,
                    route = PaymentRoute("smartbuy", "SmartBuy", "Book via the SmartBuy portal"),
                ),
            ),
        )
        val result = engine().recommend(PurchaseQuery(categoryId = "groceries"), listOf(regalia)).single()

        // primary = the route rule
        assertEquals("smartbuy_travel", result.winningRule.id)
        assertEquals(6.65, result.effectiveRatePct)
        assertEquals("Book via the SmartBuy portal", result.routeInstruction)

        // alternative = the direct (base) rule, route-free and un-nested
        val alt = assertNotNull(result.directAlternative)
        assertEquals("regalia_base", alt.winningRule.id)
        assertEquals(1.33, alt.effectiveRatePct)
        assertNull(alt.routeInstruction)
        assertNull(alt.directAlternative)
    }

    // A1 — a route-free rule ranking first yields no alternative, even if a (worse)
    // route rule is also eligible ("if the route-free rule would rank better, it stays
    // primary and no alternative is attached").
    @Test
    fun `route-free winner keeps no alternative even when a route rule is eligible`() {
        val mixed = card(
            "mixed",
            rules = listOf(
                rule("direct_cat", priority = 30, condition = Condition.CategoryIs("groceries"), ratePct = 5.0),
                rule(
                    "portal_cat",
                    priority = 40,
                    condition = Condition.CategoryIs("groceries"),
                    ratePct = 6.0,
                    route = PaymentRoute("portal", "Portal", "Pay via Portal"),
                ),
            ),
        )
        val result = engine().recommend(PurchaseQuery(categoryId = "groceries"), listOf(mixed)).single()
        assertEquals("direct_cat", result.winningRule.id) // route-free wins on priority
        assertNull(result.routeInstruction)
        assertNull(result.directAlternative)
    }

    // A1 — a route-only card (no route-free rule eligible at all) emits no alternative.
    @Test
    fun `route-only card emits no direct alternative`() {
        val wallet = PaymentRoute("wallet", "Wallet", "Pay via the Wallet app")
        val base = card("portal_only")
        val routeOnly = base.copy(
            baseRule = base.baseRule.copy(paymentRoute = wallet), // even base is route-gated
            rules = listOf(rule("portal_bonus", ratePct = 6.0, route = wallet)),
        )
        val result = engine().recommend(PurchaseQuery(categoryId = "groceries"), listOf(routeOnly)).single()
        assertEquals("portal_bonus", result.winningRule.id)
        assertNotNull(result.routeInstruction)
        assertNull(result.directAlternative)
    }

    // A1 — determinism holds with nested route variants.
    @Test
    fun `route variant output is deterministic`() {
        val regalia = card(
            "regalia",
            baseRatePct = 1.33,
            rules = listOf(
                rule("smartbuy", ratePct = 6.65, route = PaymentRoute("sb", "SmartBuy", "Book via SmartBuy")),
            ),
        )
        val query = PurchaseQuery(categoryId = "groceries", amountInr = 10_000)
        val first = engine().recommend(query, listOf(regalia))
        repeat(5) { assertEquals(first, engine().recommend(query, listOf(regalia))) }
    }

    // §7.9 — full determinism, including input-order independence
    @Test
    fun `identical query and cards give identical ordered output repeatedly`() {
        val cards = listOf(cappedCard, uncappedCard, card("card_c", baseRatePct = 2.0))
        val query = PurchaseQuery(categoryId = "groceries", amountInr = 45_000)

        val first = engine().recommend(query, cards)
        repeat(5) { assertEquals(first, engine().recommend(query, cards)) }
        // shuffled input order must not change the ranked output
        assertEquals(first, engine().recommend(query, cards.reversed()))
    }

    // Tie-breakers 2-4: uncapped beats capped, then higher cap, then card id
    @Test
    fun `tie-breakers - uncapped beats capped, then higher cap, then card id`() {
        val capped = card("t_capped", rules = listOf(rule("tc", ratePct = 5.0, cap = monthlyCap1000)))
        val cappedHigher = card("t_capped_hi", rules = listOf(rule("tch", ratePct = 5.0, cap = Cap(2000, CapPeriod.CALENDAR_MONTH))))
        val uncapped = card("t_uncapped", rules = listOf(rule("tu", ratePct = 5.0)))
        val uncapped2 = card("z_uncapped", rules = listOf(rule("tz", ratePct = 5.0)))

        // no amount → all 5.0% nominal; tie-breakers decide
        val results = engine().recommend(
            PurchaseQuery(categoryId = "groceries"),
            listOf(capped, cappedHigher, uncapped2, uncapped),
        )
        assertEquals(listOf("t_uncapped", "z_uncapped", "t_capped_hi", "t_capped"), results.map { it.card.id })
    }

    @Test
    fun `points valuation note is surfaced in the explanation`() {
        val pointsCard = card("points").let {
            it.copy(
                baseRule = it.baseRule.copy(
                    reward = Reward(1.33, "4 RP per ₹150", "1 RP valued at ₹0.50 (conservative)"),
                ),
            )
        }
        val result = engine().recommend(PurchaseQuery(categoryId = "dining"), listOf(pointsCard)).single()
        assertNotNull(result.explanation.valuationNote)
        assertEquals("4 RP per ₹150", result.explanation.earnDescription)
    }
}
