package com.wickedcoder.myadvisor.data

import com.wickedcoder.myadvisor.data.importer.DatasetDto
import com.wickedcoder.myadvisor.data.importer.DatasetValidator
import com.wickedcoder.myadvisor.data.importer.allRules
import com.wickedcoder.myadvisor.data.importer.toEntity
import com.wickedcoder.myadvisor.data.db.CardWithRules
import com.wickedcoder.myadvisor.data.mapper.toDomain
import com.wickedcoder.myadvisor.domain.model.CapPeriod
import com.wickedcoder.myadvisor.domain.model.Condition
import com.wickedcoder.myadvisor.domain.model.ExclusionScope
import com.wickedcoder.myadvisor.domain.model.ExclusionTarget
import com.wickedcoder.myadvisor.domain.model.TransactionType
import com.wickedcoder.myadvisor.domain.serialization.DomainJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Phase 1 "3 tricky cards" gate: one capped, one exclusion-heavy, one with
 * overlapping rules (+ points valuation + payment route) must round-trip
 * JSON → validator → entities → domain with everything intact. If a real
 * card can't be expressed here, the spec changes in week 3, not month 6.
 */
class TrickyCardsRoundTripTest {

    // Structurally identical to the shipped data/cards.json (which is
    // validated as a whole by BundledDatasetValidationTest on the JVM).
    private val json = """
    {
      "schemaVersion": 1,
      "dataVersion": "1.0.0",
      "generatedAt": "2026-07-10",
      "issuers": [
        { "id": "axis", "name": "Axis Bank" },
        { "id": "hdfc", "name": "HDFC Bank" }
      ],
      "categories": [
        { "id": "dining", "name": "Dining" },
        { "id": "online_shopping", "name": "Online Shopping" },
        { "id": "utilities", "name": "Utilities" },
        { "id": "travel", "name": "Travel" }
      ],
      "merchantFamilies": [ { "id": "amazon", "name": "Amazon" } ],
      "merchants": [
        { "id": "swiggy", "name": "Swiggy", "categoryId": "dining" },
        { "id": "amazon_pay_gc", "familyId": "amazon", "name": "Amazon Pay Gift Cards", "categoryId": "online_shopping" }
      ],
      "cards": [
        {
          "id": "capped_card",
          "issuerId": "hdfc",
          "name": "Capped Card",
          "lastVerified": "2026-07-10",
          "researchRef": "research/hdfc/capped/",
          "baseRule": {
            "id": "capped_base", "priority": 1000,
            "condition": { "type": "always" },
            "reward": { "effectiveRatePct": 1.0, "earnDescription": "1% cashback" }
          },
          "rules": [
            {
              "id": "capped_swiggy", "priority": 10,
              "condition": { "type": "merchant", "merchantId": "swiggy" },
              "reward": { "effectiveRatePct": 10.0, "earnDescription": "10% on Swiggy" },
              "cap": { "amountInr": 1500, "period": "CALENDAR_MONTH" }
            },
            {
              "id": "capped_online", "priority": 20,
              "condition": { "type": "category", "categoryId": "online_shopping" },
              "reward": { "effectiveRatePct": 5.0, "earnDescription": "5% online" },
              "cap": { "amountInr": 1500, "period": "CALENDAR_MONTH" }
            }
          ]
        },
        {
          "id": "exclusion_card",
          "issuerId": "axis",
          "name": "Exclusion Card",
          "lastVerified": "2026-07-10",
          "researchRef": "research/axis/exclusion/",
          "baseRule": {
            "id": "exclusion_base", "priority": 1000,
            "condition": { "type": "always" },
            "reward": { "effectiveRatePct": 1.5, "earnDescription": "1.5% cashback" }
          },
          "exclusions": [
            { "target": { "type": "transactionType", "value": "WALLET_LOAD" }, "scope": "FULL" },
            { "target": { "type": "merchant", "merchantId": "amazon_pay_gc" }, "scope": "ACCELERATED_ONLY" }
          ]
        },
        {
          "id": "points_route_card",
          "issuerId": "hdfc",
          "name": "Points + Portal Card",
          "lastVerified": "2026-07-10",
          "researchRef": "research/hdfc/points/",
          "baseRule": {
            "id": "points_base", "priority": 1000,
            "condition": { "type": "always" },
            "reward": {
              "effectiveRatePct": 1.33,
              "earnDescription": "4 RP per ₹150",
              "valuationNote": "1 RP valued at ₹0.50 (conservative)"
            }
          },
          "rules": [
            {
              "id": "points_smartbuy", "priority": 10,
              "condition": { "type": "allOf", "conditions": [ { "type": "category", "categoryId": "travel" } ] },
              "reward": {
                "effectiveRatePct": 6.65,
                "earnDescription": "5X RP via SmartBuy",
                "valuationNote": "1 RP valued at ₹0.50 (conservative)"
              },
              "cap": { "amountInr": 2000, "period": "CALENDAR_MONTH" },
              "paymentRoute": { "id": "smartbuy", "name": "HDFC SmartBuy", "instruction": "Book via SmartBuy" }
            }
          ]
        }
      ]
    }
    """.trimIndent()

    @Test
    fun `three tricky cards round-trip JSON to domain intact`() {
        // JSON → DTO
        val dataset = DomainJson.decodeFromString(DatasetDto.serializer(), json)

        // → validator
        assertEquals(emptyList(), DatasetValidator.validate(dataset))

        // → entities → domain (same path the importer + repository take)
        val domainCards = dataset.cards.map { dto ->
            CardWithRules(
                card = dto.toEntity(),
                rules = dto.allRules().map { it.toEntity(dto.id) },
                exclusions = dto.exclusions.mapIndexed { i, e -> e.toEntity(dto.id, i) },
            ).toDomain()
        }.associateBy { it.id }

        // Tricky card 1: capped + overlapping priorities
        val capped = assertNotNull(domainCards["capped_card"])
        assertEquals(1.0, capped.baseRule.reward.effectiveRatePct)
        val swiggyRule = capped.rules.first { it.id == "capped_swiggy" }
        assertEquals(Condition.MerchantIs("swiggy"), swiggyRule.condition)
        assertEquals(1500, swiggyRule.cap?.amountInr)
        assertEquals(CapPeriod.CALENDAR_MONTH, swiggyRule.cap?.period)
        assertTrue(swiggyRule.priority < capped.rules.first { it.id == "capped_online" }.priority)

        // Tricky card 2: exclusions with both scopes
        val exclusionCard = assertNotNull(domainCards["exclusion_card"])
        assertEquals(2, exclusionCard.exclusions.size)
        assertEquals(
            ExclusionScope.FULL,
            exclusionCard.exclusions.first {
                it.target == ExclusionTarget.TransactionTypeIs(TransactionType.WALLET_LOAD)
            }.scope,
        )
        assertEquals(
            ExclusionScope.ACCELERATED_ONLY,
            exclusionCard.exclusions.first {
                it.target == ExclusionTarget.MerchantIs("amazon_pay_gc")
            }.scope,
        )

        // Tricky card 3: points valuation + payment route + AllOf condition
        val pointsCard = assertNotNull(domainCards["points_route_card"])
        assertEquals("1 RP valued at ₹0.50 (conservative)", pointsCard.baseRule.reward.valuationNote)
        val routeRule = pointsCard.rules.single()
        assertEquals(Condition.AllOf(listOf(Condition.CategoryIs("travel"))), routeRule.condition)
        assertEquals("smartbuy", routeRule.paymentRoute?.id)
    }
}
