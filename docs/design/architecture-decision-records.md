# Architecture Decision Records — v1.0

*Last updated: 2026-07-10. Deciders: solo developer (you). Constraint context for all ADRs: solo dev, full-time-equivalent, Kotlin + Compose, offline recommendation engine, Play Store launch goal, permanent solo data-maintenance tax.*

ADRs 001–005 record decisions already made in the roadmap/spec, with the alternatives that were weighed. ADR-006 is decided here.

---

## ADR-001: Clean Architecture, multi-module, Koin DI

**Status:** Accepted · **Date:** 2026-07-10

**Context:** Project structure must survive Phases 3-5+ (portal routing, debit cards, points advisor) without rewrites, and match the developer's existing conventions.

**Decision:** Layered modules — `:domain` (pure Kotlin), `:data` (Room, importer, repositories), `:app`/presentation (Compose, MVI). Koin for DI.

**Options considered:**
- *Single module:* lowest ceremony, but nothing stops Android/Room types leaking into the engine — and ADR-003 depends on that not happening. Rejected.
- *Hilt:* fine, but Koin is the developer's established convention; switching buys nothing here. Rejected.
- *Full feature-modularization:* overkill for one feature surface in v1. Deferred — layer split is enough.

**Consequences:** Engine testable on the JVM without an emulator (fast CI). Slightly more Gradle setup in Phase 0. Feature modules can be introduced later without undoing this.

---

## ADR-002: Room for local persistence; two-zone data model

**Status:** Accepted · **Date:** 2026-07-10

**Context:** Card catalog is read-only, wholesale-replaced on import; user's owned cards are mutable and must survive imports.

**Decision:** Room, with a catalog zone (atomic transactional replace on import) and a user zone (`user_cards`). Importer rejects datasets that would drop a user-owned card.

**Options considered:**
- *Bundled JSON parsed at startup, no DB:* viable at this size, but loses migration tooling, `dataset_meta` bookkeeping, and structured user-zone persistence; startup parsing grows with the catalog. Rejected.
- *SQLDelight:* good, and better if KMP ever matters — but KMP is not a goal, and Room is the established convention. Rejected (revisit only if a KMP goal ever appears via change protocol).
- *DataStore:* wrong shape for relational catalog data. Rejected.

**Consequences:** Migration support for free; two-zone rule must be respected by all future code (no runtime writes to catalog tables). Import-rejection-on-owned-card-removal makes catalog removals a deliberate, versioned act.

---

## ADR-003: Rule engine as a pure Kotlin function in `:domain`

**Status:** Accepted · **Date:** 2026-07-10

**Context:** The engine *is* the product (roadmap Guiding Principle 1). Its required test suite (Spec §7) must be fast, exhaustive, deterministic.

**Decision:** `RecommendationEngine` is a pure function over immutable domain objects: no I/O, no Android deps, no clock reads except an injected `today`. Repository loads full card aggregates; engine never queries.

**Options considered:**
- *Engine queries Room directly:* fewer layers, but tests need a DB, determinism gets murky, and Phase-3+ data sources would couple to the engine. Rejected.
- *SQL-based ranking (do it in queries):* clever, unreadable, untestable at the rule-provenance level the Explanation type requires. Rejected.

**Consequences:** Millisecond JVM tests; explanation building is a natural by-product of evaluation. Memory-loading full aggregates is trivially fine at tens-of-cards scale — revisit only if the catalog somehow reaches thousands.

---

## ADR-004: Bundled, versioned JSON dataset + build-time validator + import pipeline

**Status:** Accepted · **Date:** 2026-07-10

**Context:** No public Indian API for card reward rules exists [ASSUMPTION — roadmap]. Data is hand-curated; the maintenance workflow must be sustainable solo, and bad data must never reach users.

**Decision:** `data/cards.json` in-repo → validator (runs in CI and before import; enforces Spec §3.1) → parser → domain objects → repository → Room. Dataset ships bundled in the APK; updates ship as app updates in v1. Repository abstracts the source so a remote source can slot in later (parked).

**Options considered:**
- *Backend service serving rules:* enables instant data updates, but adds infra cost, an availability dependency, and ops burden — all before validating anyone wants the app. Parked, door open via repository abstraction.
- *Hardcoded Kotlin card definitions:* no pipeline to build, but every data fix becomes a code change with no validation layer and no research-trail discipline. Rejected.
- *Firebase Remote Config for data:* payload limits and no schema validation; wrong tool. Rejected.

**Consequences:** Data fixes require an app release in v1 (accepted trade-off; `lastVerified` transparency mitigates). CI catches malformed data before any human sees it. The JSON format is the portable contract a future backend would serve unchanged.

---

## ADR-005: Points normalized to curated effective-value % (no runtime valuation)

**Status:** Accepted · **Date:** 2026-07-10 (Spec D1)

**Context:** Ranking needs one comparison axis; points ↔ money conversion is genuinely hard and is the parked points-advisor problem.

**Decision:** Curator bakes a conservative point valuation into `effectiveRatePct` at research time; `earnDescription` + `valuationNote` keep it transparent ("10 RP per ₹150 · 1 RP valued at ₹0.50, conservative").

**Options considered:**
- *Per-card point-value field, engine multiplies:* marginally more flexible, but implies a precision that doesn't exist (point value varies by redemption route) and creates a second number to maintain per card. Rejected.
- *Cashback-only v1:* dodges the problem but excludes exactly the premium points cards power users hold. Rejected — kills the target user.

**Consequences:** Point valuations are editorial judgments — they must be conservative, sourced, and documented in the research trail (see Data Curation Guide §5). Changing a valuation is a data change with a version bump, not a code change. If a future points advisor is promoted, it adds a valuation model *alongside* this field without breaking it.

---

## ADR-006: Persist sealed `Condition` as a polymorphic JSON column

**Status:** Accepted · **Date:** 2026-07-10 · *(new decision, made here)*

**Context:** `Condition` is a sealed hierarchy including the composable `AllOf` (Spec §4). Room has no native sealed-class story. Exclusion targets, by contrast, are always a single flat reference.

**Decision:** Store `Condition` as one `condition_json` TEXT column via kotlinx.serialization polymorphic serialization. Store exclusion targets as flat `target_type`/`target_value` discriminator columns. Referential integrity for ids *inside* condition JSON is enforced by the pipeline validator (which runs before anything reaches Room), not by DB FKs.

**Options considered:**

*Option A — flat discriminator columns for conditions (`condition_type` + nullable `merchant_id`/`family_id`/`category_id`):*
| Dimension | Assessment |
|---|---|
| Complexity | Low until `AllOf`; then broken |
| Integrity | DB-level FKs — strongest |
| Queryability | Full SQL |

**Pros:** FKs; inspectable rows. **Cons:** cannot represent `AllOf` without a self-referencing conditions table (Option C); schema change for every new condition type.

*Option B — polymorphic JSON column (chosen):*
| Dimension | Assessment |
|---|---|
| Complexity | Low — mirrors the domain type exactly |
| Integrity | Validator-enforced, pre-import |
| Queryability | None in SQL — irrelevant (ADR-003: engine never queries; full aggregates loaded) |

**Pros:** handles `AllOf` and future condition types with zero migrations; one serializer shared with the JSON dataset format. **Cons:** ids inside JSON invisible to DB FKs — mitigated because the validator is already the mandatory integrity gate for the whole catalog zone.

*Option C — normalized self-referencing `conditions` table:* fully relational and fully general, but the most complex option in the project for data the app never queries relationally. Rejected as ceremony.

**Trade-off analysis:** The deciding fact is ADR-003's load pattern — the engine consumes whole in-memory aggregates, so SQL-queryability of conditions has zero value, which removes Option A/C's main advantage. Option B is then strictly simpler and migration-proof for the exact axis (new condition types) most likely to evolve in Phases 3-5.

**Consequences:** New condition types = serializer registration + validator update, no DB migration. Validator discipline is now load-bearing for integrity — its test fixtures (Spec §7.10) are non-optional. Debugging conditions means reading JSON in a column; acceptable.

**Action items:**
1. [ ] Register polymorphic serializers module for `Condition` in `:domain`
2. [ ] Validator: dangling-reference checks must recurse into `AllOf`
3. [ ] Room `TypeConverter` for `condition_json` delegating to the shared serializer
