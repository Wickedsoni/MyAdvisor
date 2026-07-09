package com.wickedcoder.myadvisor.domain.engine

import com.wickedcoder.myadvisor.domain.model.Condition
import com.wickedcoder.myadvisor.domain.model.Exclusion
import com.wickedcoder.myadvisor.domain.model.ExclusionScope
import com.wickedcoder.myadvisor.domain.model.ExclusionTarget
import com.wickedcoder.myadvisor.domain.model.Merchant
import com.wickedcoder.myadvisor.domain.model.TransactionType

/**
 * Resolved transaction context (Spec §5 Step 1): a merchant query expands to
 * { merchant, its family, its default category }; a category-only query
 * carries just the category. Pure data — the engine and exclusion evaluation
 * both match against this.
 */
data class QueryContext(
    val merchantId: String? = null,
    val familyId: String? = null,
    val categoryId: String? = null,
    val transactionType: TransactionType? = null,
)

fun resolveQueryContext(query: PurchaseQuery, merchantsById: Map<String, Merchant>): QueryContext {
    val merchant = query.merchantId?.let { merchantsById[it] }
    return QueryContext(
        merchantId = merchant?.id,
        familyId = merchant?.familyId,
        categoryId = merchant?.categoryId ?: query.categoryId,
        transactionType = query.transactionType,
    )
}

/**
 * Condition matching (Spec §5 Step 3): merchant condition needs the exact
 * merchant; family condition needs the family; category condition needs the
 * resolved category. The merchant→family→category precedence chain itself is
 * enforced by rule `priority` in the data, never by code special-cases.
 */
fun Condition.matches(context: QueryContext): Boolean = when (this) {
    is Condition.MerchantIs -> merchantId == context.merchantId
    is Condition.MerchantFamilyIs -> familyId == context.familyId
    is Condition.CategoryIs -> categoryId == context.categoryId
    Condition.Always -> true
    is Condition.AllOf -> conditions.all { it.matches(context) }
}

fun ExclusionTarget.matches(context: QueryContext): Boolean = when (this) {
    is ExclusionTarget.MerchantIs -> merchantId == context.merchantId
    is ExclusionTarget.CategoryIs -> categoryId == context.categoryId
    is ExclusionTarget.TransactionTypeIs -> type == context.transactionType
}

/**
 * Exclusions evaluate BEFORE positive rules (Spec D3, §5 Step 2).
 * Returns the strictest matching scope: FULL (card earns nothing) beats
 * ACCELERATED_ONLY (base rule still applies); null = no exclusion matches.
 */
fun List<Exclusion>.effectiveExclusionScope(context: QueryContext): ExclusionScope? {
    val matching = filter { it.target.matches(context) }
    return when {
        matching.any { it.scope == ExclusionScope.FULL } -> ExclusionScope.FULL
        matching.isNotEmpty() -> ExclusionScope.ACCELERATED_ONLY
        else -> null
    }
}
