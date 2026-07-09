package com.wickedcoder.myadvisor.domain.engine

import com.wickedcoder.myadvisor.domain.model.Card

/**
 * The product (Guiding Principle 1). A pure function over immutable domain
 * objects: no I/O, no platform deps, no clock reads except validity checks
 * against an injected `today` (ADR-003). Fully deterministic (Spec D5).
 *
 * Evaluation algorithm: Rule Engine Spec §5. Implemented in Phase 2.
 */
interface RecommendationEngine {
    fun recommend(query: PurchaseQuery, userCards: List<Card>): List<Recommendation>
}
