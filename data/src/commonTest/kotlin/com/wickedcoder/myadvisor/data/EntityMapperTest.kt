package com.wickedcoder.myadvisor.data

import com.wickedcoder.myadvisor.data.db.RewardRuleEntity
import com.wickedcoder.myadvisor.data.mapper.toDomain
import com.wickedcoder.myadvisor.domain.model.CapPeriod
import com.wickedcoder.myadvisor.domain.model.Condition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EntityMapperTest {

    @Test
    fun `reward rule entity maps to domain including polymorphic condition`() {
        val entity = RewardRuleEntity(
            id = "hdfc_swiggy_merchant",
            cardId = "hdfc_swiggy",
            priority = 10,
            conditionJson = """{"type":"merchant","merchantId":"swiggy"}""",
            effectiveRatePct = 10.0,
            earnDescription = "10% cashback on Swiggy",
            valuationNote = null,
            capAmountInr = 1500,
            capPeriod = "CALENDAR_MONTH",
            validFrom = null,
            validUntil = null,
            routeId = null,
            routeName = null,
            routeInstruction = null,
        )

        val rule = entity.toDomain()

        assertEquals(Condition.MerchantIs("swiggy"), rule.condition)
        assertEquals(10, rule.priority)
        assertEquals(10.0, rule.reward.effectiveRatePct)
        assertEquals(1500, rule.cap?.amountInr)
        assertEquals(CapPeriod.CALENDAR_MONTH, rule.cap?.period)
        assertNull(rule.validity)
        assertNull(rule.paymentRoute)
    }
}
