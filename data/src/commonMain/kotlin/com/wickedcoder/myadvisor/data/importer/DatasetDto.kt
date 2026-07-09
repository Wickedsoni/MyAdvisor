package com.wickedcoder.myadvisor.data.importer

import com.wickedcoder.myadvisor.domain.model.Condition
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire format of `data/cards.json` (Rule Engine Spec §3). Parsed with
 * [com.wickedcoder.myadvisor.domain.serialization.DomainJson] — strict, so
 * unknown keys fail at the pipeline gate. `Condition` is the domain type
 * itself: one serializer for dataset and DB column (ADR-006).
 *
 * Enum-like fields (cap period, exclusion scope, transaction type) are kept
 * as strings here so the validator can reject unknown values with a useful
 * error instead of a serialization stack trace.
 */
@Serializable
data class DatasetDto(
    val schemaVersion: Int,
    val dataVersion: String,
    val generatedAt: String,
    val issuers: List<IssuerDto>,
    val categories: List<CategoryDto>,
    val merchantFamilies: List<MerchantFamilyDto>,
    val merchants: List<MerchantDto>,
    val cards: List<CardDto>,
)

@Serializable
data class IssuerDto(val id: String, val name: String)

@Serializable
data class CategoryDto(val id: String, val name: String)

@Serializable
data class MerchantFamilyDto(val id: String, val name: String)

@Serializable
data class MerchantDto(
    val id: String,
    val familyId: String? = null,
    val name: String,
    val categoryId: String,
)

@Serializable
data class CardDto(
    val id: String,
    val issuerId: String,
    val name: String,
    val lastVerified: String,
    val researchRef: String,
    val baseRule: RewardRuleDto,
    val rules: List<RewardRuleDto> = emptyList(),
    val exclusions: List<ExclusionDto> = emptyList(),
)

@Serializable
data class RewardRuleDto(
    val id: String,
    val priority: Int,
    val condition: Condition,
    val reward: RewardDto,
    val cap: CapDto? = null,
    val validity: ValidityDto? = null,
    val paymentRoute: PaymentRouteDto? = null,
)

@Serializable
data class RewardDto(
    val effectiveRatePct: Double,
    val earnDescription: String,
    val valuationNote: String? = null,
)

@Serializable
data class CapDto(val amountInr: Int, val period: String)

@Serializable
data class ValidityDto(val from: String? = null, val until: String? = null)

@Serializable
data class PaymentRouteDto(val id: String, val name: String, val instruction: String)

@Serializable
data class ExclusionDto(val target: ExclusionTargetDto, val scope: String)

@Serializable
sealed interface ExclusionTargetDto {
    @Serializable
    @SerialName("merchant")
    data class MerchantTarget(val merchantId: String) : ExclusionTargetDto

    @Serializable
    @SerialName("category")
    data class CategoryTarget(val categoryId: String) : ExclusionTargetDto

    @Serializable
    @SerialName("transactionType")
    data class TransactionTypeTarget(val value: String) : ExclusionTargetDto
}
