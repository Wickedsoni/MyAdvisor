package com.wickedcoder.myadvisor.domain

import com.wickedcoder.myadvisor.domain.model.Condition
import com.wickedcoder.myadvisor.domain.serialization.DomainJson
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * ADR-006 action item 1: the polymorphic Condition serializer is the shared
 * contract between the dataset JSON and the condition_json Room column.
 */
class ConditionSerializationTest {

    @Test
    fun `every condition type round-trips through JSON`() {
        val conditions: List<Condition> = listOf(
            Condition.MerchantIs("swiggy"),
            Condition.MerchantFamilyIs("amazon"),
            Condition.CategoryIs("dining"),
            Condition.Always,
            Condition.AllOf(
                listOf(
                    Condition.CategoryIs("online_shopping"),
                    Condition.MerchantFamilyIs("amazon"),
                ),
            ),
        )

        for (condition in conditions) {
            val json = DomainJson.encodeToString(Condition.serializer(), condition)
            val decoded = DomainJson.decodeFromString(Condition.serializer(), json)
            assertEquals(condition, decoded)
        }
    }

    @Test
    fun `discriminators are stable wire-format names`() {
        val json = DomainJson.encodeToString(
            Condition.serializer(),
            Condition.MerchantIs("swiggy"),
        )
        assertEquals("""{"type":"merchant","merchantId":"swiggy"}""", json)
    }
}
