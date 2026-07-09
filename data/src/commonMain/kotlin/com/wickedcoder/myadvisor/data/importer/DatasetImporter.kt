package com.wickedcoder.myadvisor.data.importer

/**
 * Phase 1: JSON dataset → validator → domain objects → atomic Room replace
 * (ADR-004). The importer is the ONLY writer of the catalog zone, and it
 * writes inside a single transaction: delete-all catalog rows → insert new
 * dataset → update dataset_meta.
 *
 * Pipeline stages to build in Phase 1:
 *  1. Parse `data/cards.json` (bundled) into DTOs via [com.wickedcoder.myadvisor.domain.serialization.DomainJson]
 *  2. Validate per Rule Engine Spec §3.1 (duplicate ids, dangling refs incl.
 *     inside AllOf conditions, base-rule invariants, sanity bounds, priority
 *     overlaps, monotonic dataVersion, no dropping of user-owned cards)
 *  3. Map DTOs → entities and replace the catalog zone atomically
 */
interface DatasetImporter {
    suspend fun importBundledDatasetIfNewer()
}
