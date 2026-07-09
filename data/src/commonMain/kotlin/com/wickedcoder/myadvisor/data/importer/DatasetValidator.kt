package com.wickedcoder.myadvisor.data.importer

import com.wickedcoder.myadvisor.domain.model.CapPeriod
import com.wickedcoder.myadvisor.domain.model.Condition
import com.wickedcoder.myadvisor.domain.model.ExclusionScope
import com.wickedcoder.myadvisor.domain.model.TransactionType
import kotlinx.datetime.LocalDate

/**
 * The pipeline gate (Rule Engine Spec §3.1, ADR-004): bad data must never
 * reach users. Runs in CI against `data/cards.json` and again before every
 * import. Returns ALL problems, not just the first — a useful error report
 * is the point.
 */
object DatasetValidator {

    const val SUPPORTED_SCHEMA_VERSION = 1

    /**
     * @param ownedCardIds cards the user owns; a dataset that drops one is
     *   rejected (removing a catalog card is a deliberate, versioned act —
     *   data-model.md user zone).
     */
    fun validate(dataset: DatasetDto, ownedCardIds: Set<String> = emptySet()): List<String> {
        val errors = mutableListOf<String>()

        if (dataset.schemaVersion != SUPPORTED_SCHEMA_VERSION) {
            errors += "schemaVersion ${dataset.schemaVersion} unsupported (expected $SUPPORTED_SCHEMA_VERSION)"
        }
        if (SemVer.parseOrNull(dataset.dataVersion) == null) {
            errors += "dataVersion '${dataset.dataVersion}' is not a valid semver (x.y.z)"
        }
        errors += checkDate(dataset.generatedAt, "generatedAt")

        checkDuplicates(dataset, errors)
        checkReferences(dataset, errors)
        checkCards(dataset, errors)
        checkOwnedCards(dataset, ownedCardIds, errors)

        return errors
    }

    private fun checkDuplicates(dataset: DatasetDto, errors: MutableList<String>) {
        fun dupes(kind: String, ids: List<String>) {
            ids.groupBy { it }.filterValues { it.size > 1 }.keys.forEach {
                errors += "duplicate $kind id '$it'"
            }
        }
        dupes("issuer", dataset.issuers.map { it.id })
        dupes("category", dataset.categories.map { it.id })
        dupes("merchant family", dataset.merchantFamilies.map { it.id })
        dupes("merchant", dataset.merchants.map { it.id })
        dupes("card", dataset.cards.map { it.id })
        dupes("rule", dataset.cards.flatMap { card -> card.allRules().map { it.id } })
    }

    private fun checkReferences(dataset: DatasetDto, errors: MutableList<String>) {
        val categoryIds = dataset.categories.map { it.id }.toSet()
        val familyIds = dataset.merchantFamilies.map { it.id }.toSet()
        val merchantIds = dataset.merchants.map { it.id }.toSet()
        val issuerIds = dataset.issuers.map { it.id }.toSet()

        dataset.merchants.forEach { merchant ->
            merchant.familyId?.let {
                if (it !in familyIds) errors += "merchant '${merchant.id}' references unknown family '$it'"
            }
            if (merchant.categoryId !in categoryIds) {
                errors += "merchant '${merchant.id}' references unknown category '${merchant.categoryId}'"
            }
        }

        dataset.cards.forEach { card ->
            if (card.issuerId !in issuerIds) {
                errors += "card '${card.id}' references unknown issuer '${card.issuerId}'"
            }
            card.allRules().forEach { rule ->
                // ADR-006 action item 2: recurse into AllOf
                checkConditionRefs(rule.condition, "rule '${rule.id}'", merchantIds, familyIds, categoryIds, errors)
            }
            card.exclusions.forEach { exclusion ->
                when (val target = exclusion.target) {
                    is ExclusionTargetDto.MerchantTarget ->
                        if (target.merchantId !in merchantIds) {
                            errors += "card '${card.id}' exclusion references unknown merchant '${target.merchantId}'"
                        }
                    is ExclusionTargetDto.CategoryTarget ->
                        if (target.categoryId !in categoryIds) {
                            errors += "card '${card.id}' exclusion references unknown category '${target.categoryId}'"
                        }
                    is ExclusionTargetDto.TransactionTypeTarget ->
                        if (enumNames<TransactionType>().none { it == target.value }) {
                            errors += "card '${card.id}' exclusion has unknown transaction type '${target.value}'"
                        }
                }
                if (enumNames<ExclusionScope>().none { it == exclusion.scope }) {
                    errors += "card '${card.id}' exclusion has unknown scope '${exclusion.scope}'"
                }
            }
        }
    }

    private fun checkConditionRefs(
        condition: Condition,
        where: String,
        merchantIds: Set<String>,
        familyIds: Set<String>,
        categoryIds: Set<String>,
        errors: MutableList<String>,
    ) {
        when (condition) {
            is Condition.MerchantIs ->
                if (condition.merchantId !in merchantIds) {
                    errors += "$where references unknown merchant '${condition.merchantId}'"
                }
            is Condition.MerchantFamilyIs ->
                if (condition.familyId !in familyIds) {
                    errors += "$where references unknown family '${condition.familyId}'"
                }
            is Condition.CategoryIs ->
                if (condition.categoryId !in categoryIds) {
                    errors += "$where references unknown category '${condition.categoryId}'"
                }
            Condition.Always -> Unit
            is Condition.AllOf -> condition.conditions.forEach {
                checkConditionRefs(it, where, merchantIds, familyIds, categoryIds, errors)
            }
        }
    }

    private fun checkCards(dataset: DatasetDto, errors: MutableList<String>) {
        val merchantsById = dataset.merchants.associateBy { it.id }

        dataset.cards.forEach { card ->
            if (card.baseRule.condition != Condition.Always) {
                errors += "card '${card.id}' baseRule condition must be 'always'"
            }
            if (card.lastVerified.isBlank()) {
                errors += "card '${card.id}' is missing lastVerified"
            } else {
                errors += checkDate(card.lastVerified, "card '${card.id}' lastVerified")
            }
            if (card.researchRef.isBlank()) {
                errors += "card '${card.id}' is missing researchRef"
            }

            card.allRules().forEach { rule ->
                val where = "rule '${rule.id}'"
                if (rule.reward.effectiveRatePct <= 0 || rule.reward.effectiveRatePct > 50) {
                    errors += "$where effectiveRatePct ${rule.reward.effectiveRatePct} outside sanity bound (0, 50]"
                }
                rule.cap?.let { cap ->
                    if (cap.amountInr <= 0) errors += "$where cap amountInr must be > 0"
                    if (enumNames<CapPeriod>().none { it == cap.period }) {
                        errors += "$where has unknown cap period '${cap.period}'"
                    }
                }
                rule.validity?.let { validity ->
                    validity.from?.let { errors += checkDate(it, "$where validity.from") }
                    validity.until?.let { errors += checkDate(it, "$where validity.until") }
                }
            }

            // Static overlap check: two same-priority rules whose conditions can co-match
            card.allRules().groupBy { it.priority }.filterValues { it.size > 1 }.forEach { (priority, rules) ->
                for (i in rules.indices) {
                    for (j in i + 1 until rules.size) {
                        if (canCoMatch(rules[i].condition, rules[j].condition, merchantsById)) {
                            errors += "card '${card.id}': rules '${rules[i].id}' and '${rules[j].id}' " +
                                "share priority $priority and can match the same transaction"
                        }
                    }
                }
            }
        }
    }

    private fun checkOwnedCards(dataset: DatasetDto, ownedCardIds: Set<String>, errors: MutableList<String>) {
        val cardIds = dataset.cards.map { it.id }.toSet()
        (ownedCardIds - cardIds).forEach {
            errors += "dataset drops card '$it' which the user owns — removal must be a deliberate, versioned decision"
        }
    }

    /**
     * Conservative static co-match check: can some transaction satisfy both
     * conditions? Over-reporting (forcing distinct priorities) is acceptable;
     * under-reporting is not — the tie-break backstop exists but should never
     * be reached (Spec §5 Step 4).
     */
    private fun canCoMatch(a: Condition, b: Condition, merchants: Map<String, MerchantDto>): Boolean = when {
        a == Condition.Always || b == Condition.Always -> true
        a is Condition.AllOf -> a.conditions.all { canCoMatch(it, b, merchants) }
        b is Condition.AllOf -> canCoMatch(b, a, merchants)
        a is Condition.MerchantIs && b is Condition.MerchantIs -> a.merchantId == b.merchantId
        a is Condition.MerchantFamilyIs && b is Condition.MerchantFamilyIs -> a.familyId == b.familyId
        a is Condition.CategoryIs && b is Condition.CategoryIs -> a.categoryId == b.categoryId
        a is Condition.MerchantIs && b is Condition.MerchantFamilyIs ->
            merchants[a.merchantId]?.familyId == b.familyId
        a is Condition.MerchantFamilyIs && b is Condition.MerchantIs -> canCoMatch(b, a, merchants)
        a is Condition.MerchantIs && b is Condition.CategoryIs ->
            merchants[a.merchantId]?.categoryId == b.categoryId
        a is Condition.CategoryIs && b is Condition.MerchantIs -> canCoMatch(b, a, merchants)
        a is Condition.MerchantFamilyIs && b is Condition.CategoryIs ->
            merchants.values.any { it.familyId == a.familyId && it.categoryId == b.categoryId }
        a is Condition.CategoryIs && b is Condition.MerchantFamilyIs -> canCoMatch(b, a, merchants)
        else -> true // unknown combination: be conservative
    }

    private fun checkDate(value: String, where: String): List<String> =
        try {
            LocalDate.parse(value)
            emptyList()
        } catch (_: IllegalArgumentException) {
            listOf("$where '$value' is not a valid ISO date")
        }

    private inline fun <reified E : Enum<E>> enumNames(): List<String> = enumValues<E>().map { it.name }
}

fun CardDto.allRules(): List<RewardRuleDto> = listOf(baseRule) + rules

/** Minimal semver, enough for the monotonic-version gate. */
data class SemVer(val major: Int, val minor: Int, val patch: Int) : Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int =
        compareValuesBy(this, other, SemVer::major, SemVer::minor, SemVer::patch)

    companion object {
        fun parseOrNull(value: String): SemVer? {
            val parts = value.split(".")
            if (parts.size != 3) return null
            val nums = parts.map { it.toIntOrNull() ?: return null }
            return SemVer(nums[0], nums[1], nums[2])
        }
    }
}

class DatasetValidationException(val errors: List<String>) :
    IllegalStateException("Dataset failed validation with ${errors.size} error(s):\n" + errors.joinToString("\n"))
