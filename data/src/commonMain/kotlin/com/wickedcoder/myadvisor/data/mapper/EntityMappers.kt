package com.wickedcoder.myadvisor.data.mapper

import com.wickedcoder.myadvisor.data.db.CardWithRules
import com.wickedcoder.myadvisor.data.db.CategoryEntity
import com.wickedcoder.myadvisor.data.db.ExclusionEntity
import com.wickedcoder.myadvisor.data.db.IssuerEntity
import com.wickedcoder.myadvisor.data.db.MerchantEntity
import com.wickedcoder.myadvisor.data.db.MerchantFamilyEntity
import com.wickedcoder.myadvisor.data.db.RewardRuleEntity
import com.wickedcoder.myadvisor.domain.model.Cap
import com.wickedcoder.myadvisor.domain.model.CapPeriod
import com.wickedcoder.myadvisor.domain.model.Card
import com.wickedcoder.myadvisor.domain.model.Category
import com.wickedcoder.myadvisor.domain.model.Condition
import com.wickedcoder.myadvisor.domain.model.Exclusion
import com.wickedcoder.myadvisor.domain.model.ExclusionScope
import com.wickedcoder.myadvisor.domain.model.ExclusionTarget
import com.wickedcoder.myadvisor.domain.model.Issuer
import com.wickedcoder.myadvisor.domain.model.Merchant
import com.wickedcoder.myadvisor.domain.model.MerchantFamily
import com.wickedcoder.myadvisor.domain.model.PaymentRoute
import com.wickedcoder.myadvisor.domain.model.Reward
import com.wickedcoder.myadvisor.domain.model.RewardRule
import com.wickedcoder.myadvisor.domain.model.TransactionType
import com.wickedcoder.myadvisor.domain.model.Validity
import com.wickedcoder.myadvisor.domain.serialization.DomainJson
import kotlinx.datetime.LocalDate

/**
 * Persistence → domain mapping (data-model.md §3). Values were validated by
 * the pipeline before import, so mapping failures here are programming
 * errors, not data errors — hence the non-defensive parsing.
 */

fun IssuerEntity.toDomain() = Issuer(id = id, name = name)

fun CategoryEntity.toDomain() = Category(id = id, name = name)

fun MerchantFamilyEntity.toDomain() = MerchantFamily(id = id, name = name)

fun MerchantEntity.toDomain() = Merchant(
    id = id,
    familyId = familyId,
    name = name,
    categoryId = categoryId,
)

fun CardWithRules.toDomain(): Card {
    val allRules = rules.map { it.toDomain() }
    val baseRule = allRules.first { it.id == card.baseRuleId }
    return Card(
        id = card.id,
        issuerId = card.issuerId,
        name = card.name,
        lastVerified = LocalDate.parse(card.lastVerified),
        baseRule = baseRule,
        rules = allRules.filterNot { it.id == baseRule.id },
        exclusions = exclusions.map { it.toDomain() },
    )
}

fun RewardRuleEntity.toDomain() = RewardRule(
    id = id,
    priority = priority,
    condition = DomainJson.decodeFromString(Condition.serializer(), conditionJson),
    reward = Reward(
        effectiveRatePct = effectiveRatePct,
        earnDescription = earnDescription,
        valuationNote = valuationNote,
    ),
    cap = capAmountInr?.let { Cap(amountInr = it, period = CapPeriod.valueOf(capPeriod!!)) },
    validity = if (validFrom != null || validUntil != null) {
        Validity(from = validFrom?.let(LocalDate::parse), until = validUntil?.let(LocalDate::parse))
    } else {
        null
    },
    paymentRoute = routeId?.let { PaymentRoute(id = it, name = routeName!!, instruction = routeInstruction!!) },
)

fun ExclusionEntity.toDomain() = Exclusion(
    target = when (targetType) {
        "MERCHANT" -> ExclusionTarget.MerchantIs(targetValue)
        "CATEGORY" -> ExclusionTarget.CategoryIs(targetValue)
        "TXN_TYPE" -> ExclusionTarget.TransactionTypeIs(TransactionType.valueOf(targetValue))
        else -> error("Unknown exclusion target type: $targetType")
    },
    scope = ExclusionScope.valueOf(scope),
)
