package com.wickedcoder.myadvisor.domain.engine

import com.wickedcoder.myadvisor.domain.model.Cap
import com.wickedcoder.myadvisor.domain.model.CapPeriod
import com.wickedcoder.myadvisor.domain.model.Card
import com.wickedcoder.myadvisor.domain.model.Category
import com.wickedcoder.myadvisor.domain.model.Condition
import com.wickedcoder.myadvisor.domain.model.ExclusionScope
import com.wickedcoder.myadvisor.domain.model.ExclusionTarget
import com.wickedcoder.myadvisor.domain.model.Merchant
import com.wickedcoder.myadvisor.domain.model.MerchantFamily
import com.wickedcoder.myadvisor.domain.model.RewardRule
import kotlin.math.min
import kotlin.math.roundToInt
import kotlinx.datetime.LocalDate

/**
 * The Spec §5 evaluation algorithm. Pure and deterministic (D5, ADR-003):
 * all context data and `today` are injected; no I/O, no clock reads, no
 * randomness. Same inputs → same ordered output, always.
 */
class DefaultRecommendationEngine(
    private val merchantsById: Map<String, Merchant>,
    private val categoriesById: Map<String, Category>,
    private val familiesById: Map<String, MerchantFamily>,
    private val dataVersion: String,
    private val today: LocalDate,
) : RecommendationEngine {

    override fun recommend(query: PurchaseQuery, userCards: List<Card>): List<Recommendation> {
        val context = resolveQueryContext(query, merchantsById)                     // Step 1
        return userCards
            .map { evaluateCard(it, context, query.amountInr) }
            .sortedWith(ranking)                                                    // Step 6
    }

    private fun evaluateCard(card: Card, context: QueryContext, amountInr: Int?): Recommendation {
        val exclusionScope = card.exclusions.effectiveExclusionScope(context)       // Step 2

        if (exclusionScope == ExclusionScope.FULL) {
            return fullyExcludedRecommendation(card, context, amountInr)
        }

        val exclusionNotes = if (exclusionScope == ExclusionScope.ACCELERATED_ONLY) {
            listOf(
                "Accelerated rates excluded for this purchase " +
                    "(${describeMatchingExclusions(card, context)}); base rate applies",
            )
        } else {
            emptyList()
        }

        val candidates = if (exclusionScope == ExclusionScope.ACCELERATED_ONLY) {
            listOf(card.baseRule)
        } else {
            card.rules + card.baseRule
        }
        val eligible = candidates                                                   // Step 3
            .filter { it.condition.matches(context) && it.isValidOn(today) }
            .sortedWith(
                compareBy<RewardRule> { it.priority }                               // Step 4.1
                    .thenByDescending { it.reward.effectiveRatePct }                // Step 4.2
                    .thenBy { it.id },                                              // Step 4.3 backstop
            )

        val winner = eligible.firstOrNull()
            ?: return fullyExcludedRecommendation(card, context, amountInr, reason = "no applicable rule")

        // Step 7 — route variants. If the winning rule is route-gated but the card also
        // has an eligible route-free rule, keep the route rule primary (it ranked first,
        // so it's the better deal) and attach the best direct rule as the alternative.
        // If a route-free rule already ranked first, or none is eligible, there is no
        // alternative to show.
        val directAlternative = if (winner.paymentRoute != null) {
            eligible.firstOrNull { it.paymentRoute == null }?.let { routeFree ->
                buildRecommendation(card, routeFree, eligible.filter { it.paymentRoute == null }, amountInr, exclusionNotes)
            }
        } else {
            null
        }

        return buildRecommendation(card, winner, eligible, amountInr, exclusionNotes, directAlternative)
    }

    /** Assemble one [Recommendation] from a chosen rule (Step 5 value math + Step 8 explanation). */
    private fun buildRecommendation(
        card: Card,
        rule: RewardRule,
        provenanceEligible: List<RewardRule>,
        amountInr: Int?,
        exclusionNotes: List<String>,
        directAlternative: Recommendation? = null,
    ): Recommendation {
        val value = effectiveValue(rule, amountInr)                                 // Step 5
        return Recommendation(
            card = card,
            winningRule = rule,
            nominalRatePct = rule.reward.effectiveRatePct,
            effectiveValueInr = value.valueInr,
            effectiveRatePct = value.ratePct,
            capCaveat = rule.cap?.let(::capCaveatFor),
            routeInstruction = rule.paymentRoute?.instruction,                      // Step 7
            explanation = Explanation(                                              // Step 8
                ruleProvenance = provenance(rule, provenanceEligible),
                earnDescription = rule.reward.earnDescription,
                valuationNote = rule.reward.valuationNote,
                exclusionNotes = exclusionNotes,
                dataVerified = card.lastVerified,
                dataVersion = dataVersion,
            ),
            directAlternative = directAlternative,
        )
    }

    // ── Step 5: within-purchase cap math (D4 — full headroom assumed) ──

    private data class EffectiveValue(val ratePct: Double, val valueInr: Int?)

    private fun effectiveValue(rule: RewardRule, amountInr: Int?): EffectiveValue {
        val nominal = rule.reward.effectiveRatePct
        if (amountInr == null || amountInr <= 0) return EffectiveValue(nominal, null)
        val raw = nominal / 100.0 * amountInr
        val rewarded = rule.cap?.let { min(raw, it.amountInr.toDouble()) } ?: raw
        return EffectiveValue(ratePct = rewarded / amountInr * 100.0, valueInr = rewarded.roundToInt())
    }

    private fun capCaveatFor(cap: Cap): String? {
        val period = when (cap.period) {
            CapPeriod.PER_TRANSACTION -> return null // no prior-spend assumption involved
            CapPeriod.CALENDAR_MONTH -> "this month's"
            CapPeriod.STATEMENT_CYCLE -> "this statement cycle's"
            CapPeriod.QUARTER -> "this quarter's"
            CapPeriod.YEAR -> "this year's"
        }
        return "Assumes you haven't used $period ₹${formatInr(cap.amountInr)} cap"
    }

    // ── Step 6: deterministic ranking ──

    private val ranking =
        compareByDescending<Recommendation> { it.effectiveRatePct }        // 1. higher effective rate
            .thenBy { it.winningRule.cap != null }                         // 2. uncapped beats capped
            .thenByDescending { it.winningRule.cap?.amountInr ?: 0 }       // 3. higher cap
            .thenBy { it.card.id }                                         // 4. stable card order

    // ── Step 8: explanations ──

    private fun provenance(winner: RewardRule, eligible: List<RewardRule>): String {
        val winnerDesc = describeRule(winner)
        val runnerUp = eligible.firstOrNull { it.id != winner.id }
            ?: return "$winnerDesc applies"
        return "$winnerDesc overrides ${describeRule(runnerUp)}"
    }

    private fun describeRule(rule: RewardRule): String = describeCondition(rule.condition)

    private fun describeCondition(condition: Condition): String = when (condition) {
        is Condition.MerchantIs ->
            "${merchantsById[condition.merchantId]?.name ?: condition.merchantId} merchant rule"
        is Condition.MerchantFamilyIs ->
            "${familiesById[condition.familyId]?.name ?: condition.familyId} family rule"
        is Condition.CategoryIs ->
            "${categoriesById[condition.categoryId]?.name ?: condition.categoryId} category rule"
        Condition.Always -> "base rate"
        is Condition.AllOf -> condition.conditions.joinToString(" + ") { describeCondition(it) }
    }

    private fun fullyExcludedRecommendation(
        card: Card,
        context: QueryContext,
        amountInr: Int?,
        reason: String = describeMatchingExclusions(card, context),
    ): Recommendation = Recommendation(
        card = card,
        winningRule = card.baseRule,
        nominalRatePct = 0.0,
        effectiveValueInr = if (amountInr != null) 0 else null,
        effectiveRatePct = 0.0,
        capCaveat = null,
        routeInstruction = null,
        explanation = Explanation(
            ruleProvenance = "${card.name} earns nothing on this purchase ($reason)",
            earnDescription = "No rewards",
            valuationNote = null,
            exclusionNotes = listOf("Excluded: $reason"),
            dataVerified = card.lastVerified,
            dataVersion = dataVersion,
        ),
    )

    private fun describeMatchingExclusions(card: Card, context: QueryContext): String =
        card.exclusions
            .filter { it.target.matches(context) }
            .joinToString(", ") { describeExclusionTarget(it.target) }
            .ifEmpty { "excluded" }

    private fun describeExclusionTarget(target: ExclusionTarget): String = when (target) {
        is ExclusionTarget.MerchantIs -> merchantsById[target.merchantId]?.name ?: target.merchantId
        is ExclusionTarget.CategoryIs -> categoriesById[target.categoryId]?.name ?: target.categoryId
        is ExclusionTarget.TransactionTypeIs -> target.type.name.lowercase().replace('_', ' ') + "s"
    }
}

private fun RewardRule.isValidOn(today: LocalDate): Boolean {
    val v = validity ?: return true
    return (v.from == null || v.from <= today) && (v.until == null || today <= v.until)
}

internal fun formatInr(amount: Int): String =
    amount.toString().reversed().chunked(3).joinToString(",").reversed()
