package com.wickedcoder.myadvisor.data

import com.wickedcoder.myadvisor.data.importer.CapDto
import com.wickedcoder.myadvisor.data.importer.CardDto
import com.wickedcoder.myadvisor.data.importer.CategoryDto
import com.wickedcoder.myadvisor.data.importer.DatasetDto
import com.wickedcoder.myadvisor.data.importer.DatasetValidator
import com.wickedcoder.myadvisor.data.importer.ExclusionDto
import com.wickedcoder.myadvisor.data.importer.ExclusionTargetDto
import com.wickedcoder.myadvisor.data.importer.IssuerDto
import com.wickedcoder.myadvisor.data.importer.MerchantDto
import com.wickedcoder.myadvisor.data.importer.MerchantFamilyDto
import com.wickedcoder.myadvisor.data.importer.RewardDto
import com.wickedcoder.myadvisor.data.importer.RewardRuleDto
import com.wickedcoder.myadvisor.data.importer.SemVer
import com.wickedcoder.myadvisor.domain.model.Condition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Spec §7.10: each §3.1 rejection rule has a failing-fixture test. The
 * validator is the integrity gate for everything Room FKs can't see
 * (ADR-006), so these are non-optional.
 */
class DatasetValidatorTest {

    private fun rule(
        id: String,
        priority: Int = 10,
        condition: Condition = Condition.CategoryIs("dining"),
        ratePct: Double = 5.0,
        cap: CapDto? = null,
    ) = RewardRuleDto(
        id = id,
        priority = priority,
        condition = condition,
        reward = RewardDto(effectiveRatePct = ratePct, earnDescription = "test"),
        cap = cap,
    )

    private fun baseRule(id: String = "card_base") =
        rule(id, priority = 1000, condition = Condition.Always, ratePct = 1.0)

    private fun card(
        id: String = "card",
        baseRule: RewardRuleDto = baseRule("${id}_base"),
        rules: List<RewardRuleDto> = emptyList(),
        exclusions: List<ExclusionDto> = emptyList(),
        researchRef: String = "research/test/$id/",
        lastVerified: String = "2026-07-10",
    ) = CardDto(
        id = id,
        issuerId = "issuer",
        name = "Test Card",
        lastVerified = lastVerified,
        researchRef = researchRef,
        baseRule = baseRule,
        rules = rules,
        exclusions = exclusions,
    )

    private fun dataset(
        cards: List<CardDto> = listOf(card()),
        merchants: List<MerchantDto> = listOf(
            MerchantDto("swiggy", familyId = null, name = "Swiggy", categoryId = "dining"),
            MerchantDto("amazon_in", familyId = "amazon", name = "Amazon.in", categoryId = "online_shopping"),
        ),
    ) = DatasetDto(
        schemaVersion = 1,
        dataVersion = "1.0.0",
        generatedAt = "2026-07-10",
        issuers = listOf(IssuerDto("issuer", "Test Bank")),
        categories = listOf(CategoryDto("dining", "Dining"), CategoryDto("online_shopping", "Online")),
        merchantFamilies = listOf(MerchantFamilyDto("amazon", "Amazon")),
        merchants = merchants,
        cards = cards,
    )

    private fun assertRejected(dataset: DatasetDto, fragment: String, ownedCardIds: Set<String> = emptySet()) {
        val errors = DatasetValidator.validate(dataset, ownedCardIds)
        assertTrue(
            errors.any { it.contains(fragment) },
            "expected an error containing '$fragment', got: $errors",
        )
    }

    @Test
    fun `valid dataset passes`() {
        assertEquals(emptyList(), DatasetValidator.validate(dataset()))
    }

    @Test
    fun `duplicate ids are rejected`() {
        assertRejected(dataset(cards = listOf(card("a"), card("a"))), "duplicate card id 'a'")
    }

    @Test
    fun `dangling merchant reference in a condition is rejected`() {
        val bad = card(rules = listOf(rule("r1", condition = Condition.MerchantIs("nope"))))
        assertRejected(dataset(cards = listOf(bad)), "unknown merchant 'nope'")
    }

    @Test
    fun `dangling reference inside AllOf is rejected (recursion)`() {
        val nested = Condition.AllOf(
            listOf(Condition.CategoryIs("dining"), Condition.AllOf(listOf(Condition.MerchantFamilyIs("ghost")))),
        )
        val bad = card(rules = listOf(rule("r1", condition = nested)))
        assertRejected(dataset(cards = listOf(bad)), "unknown family 'ghost'")
    }

    @Test
    fun `base rule must have an always condition`() {
        val bad = card(baseRule = rule("card_base", priority = 1000, condition = Condition.CategoryIs("dining")))
        assertRejected(dataset(cards = listOf(bad)), "baseRule condition must be 'always'")
    }

    @Test
    fun `rate outside sanity bound is rejected`() {
        assertRejected(dataset(cards = listOf(card(rules = listOf(rule("r1", ratePct = 51.0))))), "sanity bound")
        assertRejected(dataset(cards = listOf(card(rules = listOf(rule("r2", ratePct = 0.0))))), "sanity bound")
    }

    @Test
    fun `non-positive cap is rejected`() {
        val bad = card(rules = listOf(rule("r1", cap = CapDto(0, "CALENDAR_MONTH"))))
        assertRejected(dataset(cards = listOf(bad)), "cap amountInr must be > 0")
    }

    @Test
    fun `unknown cap period is rejected`() {
        val bad = card(rules = listOf(rule("r1", cap = CapDto(500, "FORTNIGHT"))))
        assertRejected(dataset(cards = listOf(bad)), "unknown cap period 'FORTNIGHT'")
    }

    @Test
    fun `equal-priority co-matching rules are rejected`() {
        val bad = card(
            rules = listOf(
                rule("r1", priority = 10, condition = Condition.MerchantIs("swiggy")),
                rule("r2", priority = 10, condition = Condition.CategoryIs("dining")), // swiggy's default category
            ),
        )
        assertRejected(dataset(cards = listOf(bad)), "share priority 10")
    }

    @Test
    fun `equal-priority non-overlapping rules are allowed`() {
        val ok = card(
            rules = listOf(
                rule("r1", priority = 10, condition = Condition.MerchantIs("swiggy")),
                rule("r2", priority = 10, condition = Condition.CategoryIs("online_shopping")),
            ),
        )
        assertEquals(emptyList(), DatasetValidator.validate(dataset(cards = listOf(ok))))
    }

    @Test
    fun `missing researchRef is rejected`() {
        assertRejected(dataset(cards = listOf(card(researchRef = ""))), "missing researchRef")
    }

    @Test
    fun `malformed lastVerified is rejected`() {
        assertRejected(dataset(cards = listOf(card(lastVerified = "10/07/2026"))), "not a valid ISO date")
    }

    @Test
    fun `unknown transaction type in exclusion is rejected`() {
        val bad = card(
            exclusions = listOf(
                ExclusionDto(ExclusionTargetDto.TransactionTypeTarget("CRYPTO"), "FULL"),
            ),
        )
        assertRejected(dataset(cards = listOf(bad)), "unknown transaction type 'CRYPTO'")
    }

    @Test
    fun `dataset dropping a user-owned card is rejected`() {
        assertRejected(dataset(), "drops card 'owned_card'", ownedCardIds = setOf("owned_card"))
    }

    @Test
    fun `semver parses and orders correctly`() {
        assertNull(SemVer.parseOrNull("1.0"))
        assertNull(SemVer.parseOrNull("1.0.x"))
        assertTrue(SemVer.parseOrNull("1.0.10")!! > SemVer.parseOrNull("1.0.9")!!)
        assertTrue(SemVer.parseOrNull("2.0.0")!! > SemVer.parseOrNull("1.9.9")!!)
    }
}
