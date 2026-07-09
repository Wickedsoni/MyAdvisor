package com.wickedcoder.myadvisor.data.importer

import com.wickedcoder.myadvisor.data.db.CardEntity
import com.wickedcoder.myadvisor.data.db.CategoryEntity
import com.wickedcoder.myadvisor.data.db.ExclusionEntity
import com.wickedcoder.myadvisor.data.db.IssuerEntity
import com.wickedcoder.myadvisor.data.db.MerchantEntity
import com.wickedcoder.myadvisor.data.db.MerchantFamilyEntity
import com.wickedcoder.myadvisor.data.db.RewardRuleEntity
import com.wickedcoder.myadvisor.domain.model.Condition
import com.wickedcoder.myadvisor.domain.serialization.DomainJson

/** DTO → entity mapping. Runs only on datasets that passed the validator. */

fun IssuerDto.toEntity() = IssuerEntity(id = id, name = name)

fun CategoryDto.toEntity() = CategoryEntity(id = id, name = name)

fun MerchantFamilyDto.toEntity() = MerchantFamilyEntity(id = id, name = name)

fun MerchantDto.toEntity() = MerchantEntity(
    id = id,
    familyId = familyId,
    categoryId = categoryId,
    name = name,
)

fun CardDto.toEntity() = CardEntity(
    id = id,
    issuerId = issuerId,
    name = name,
    lastVerified = lastVerified,
    researchRef = researchRef,
    baseRuleId = baseRule.id,
)

fun RewardRuleDto.toEntity(cardId: String) = RewardRuleEntity(
    id = id,
    cardId = cardId,
    priority = priority,
    conditionJson = DomainJson.encodeToString(Condition.serializer(), condition),
    effectiveRatePct = reward.effectiveRatePct,
    earnDescription = reward.earnDescription,
    valuationNote = reward.valuationNote,
    capAmountInr = cap?.amountInr,
    capPeriod = cap?.period,
    validFrom = validity?.from,
    validUntil = validity?.until,
    routeId = paymentRoute?.id,
    routeName = paymentRoute?.name,
    routeInstruction = paymentRoute?.instruction,
)

fun ExclusionDto.toEntity(cardId: String, index: Int) = ExclusionEntity(
    id = "${cardId}_excl_$index",
    cardId = cardId,
    targetType = when (target) {
        is ExclusionTargetDto.MerchantTarget -> "MERCHANT"
        is ExclusionTargetDto.CategoryTarget -> "CATEGORY"
        is ExclusionTargetDto.TransactionTypeTarget -> "TXN_TYPE"
    },
    targetValue = when (val t = target) {
        is ExclusionTargetDto.MerchantTarget -> t.merchantId
        is ExclusionTargetDto.CategoryTarget -> t.categoryId
        is ExclusionTargetDto.TransactionTypeTarget -> t.value
    },
    scope = scope,
)
