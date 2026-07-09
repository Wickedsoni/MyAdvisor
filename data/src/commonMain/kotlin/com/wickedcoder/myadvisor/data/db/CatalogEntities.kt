package com.wickedcoder.myadvisor.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Catalog zone (data-model.md §2): read-only at runtime, replaced wholesale
 * by the import pipeline in one transaction. Never mutated by user actions.
 *
 * Dates are stored as ISO strings; enums as their names. Values are
 * validated by the pipeline validator before import — unknown values never
 * reach runtime (ADR-004).
 */

@Entity(tableName = "issuers")
data class IssuerEntity(
    @PrimaryKey val id: String,
    val name: String,
)

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
)

@Entity(tableName = "merchant_families")
data class MerchantFamilyEntity(
    @PrimaryKey val id: String,
    val name: String,
)

@Entity(tableName = "merchants")
data class MerchantEntity(
    @PrimaryKey val id: String,
    val familyId: String?,
    val categoryId: String,
    val name: String,
)

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey val id: String,
    val issuerId: String,
    val name: String,
    val lastVerified: String,   // ISO date
    val researchRef: String,
    val baseRuleId: String,     // points at a reward_rules row with condition Always (validator-enforced)
)

@Entity(tableName = "reward_rules")
data class RewardRuleEntity(
    @PrimaryKey val id: String,
    val cardId: String,
    val priority: Int,
    val conditionJson: String,  // serialized Condition (ADR-006)
    val effectiveRatePct: Double,
    val earnDescription: String,
    val valuationNote: String?,
    val capAmountInr: Int?,     // both-or-neither with capPeriod (validator-enforced)
    val capPeriod: String?,
    val validFrom: String?,     // ISO date
    val validUntil: String?,    // ISO date
    val routeId: String?,       // all-or-none route triple (validator-enforced)
    val routeName: String?,
    val routeInstruction: String?,
)

@Entity(tableName = "exclusions")
data class ExclusionEntity(
    @PrimaryKey val id: String,
    val cardId: String,
    val targetType: String,     // MERCHANT | CATEGORY | TXN_TYPE
    val targetValue: String,    // merchant/category id or TransactionType name
    val scope: String,          // FULL | ACCELERATED_ONLY
)

/** Schema only in v1; the importer accepts an empty array and the app never queries it. */
@Entity(tableName = "temporary_offers")
data class TemporaryOfferEntity(
    @PrimaryKey val id: String,
    val cardId: String,
    val conditionJson: String,
    val effectiveRatePct: Double,
    val earnDescription: String,
    val capAmountInr: Int?,
    val capPeriod: String?,
    val startsOn: String,       // ISO date, mandatory
    val endsOn: String,         // ISO date, mandatory
    val sourceUrl: String,
)

@Entity(tableName = "dataset_meta")
data class DatasetMetaEntity(
    @PrimaryKey val id: Int = 1, // singleton row
    val schemaVersion: Int,
    val dataVersion: String,    // semver
    val generatedAt: String,    // ISO date
    val importedAt: String,     // ISO datetime
)
