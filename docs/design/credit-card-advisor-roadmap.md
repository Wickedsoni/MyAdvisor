# Credit Card Checkout Advisor — Project Roadmap (SSOT)

*Last updated: 2026-07-10 (rev 3)*

---

## 0. Guiding Principles

1. **Correctness > features.** A wrong recommendation in a money app is worse than no recommendation.
2. **Transparency > black-box.** Every recommendation shows its rate, rule, cap, and data-verification date.
3. **Honesty about limits.** Where the app can't know something (e.g. spend-to-date), it says so — it never fakes precision.
4. **Deterministic recommendations.** Same inputs, same output, always. Priorities and tie-breakers are explicit.
5. **Offline recommendation engine.** All ranking logic runs locally.
6. **Feature creep goes to the Parking Lot.** Scope is zero-sum (see Change Protocol).

---

## 1. North Star

**One-sentence goal:** Help Indian cardholders with multiple cards (4-6+) instantly know which card to use for a given purchase, to maximize rewards/cashback.

**Target user:** Someone juggling 4-6+ credit cards who defaults to habit at checkout because tracking which card wins for which category is mental overhead nobody wants to carry. Designed for the power-user case; works fine for someone with 2 cards too.

**Success definition:** Real Play Store launch, real users. Not a portfolio piece.

**Non-goals (v1):**
- New card recommender ("which card should I apply for") — parked, future version
- Debit card benefits — parked, post-launch
- Points/redemption value advisor — parked, post-launch
- Live curation of temporary bank offers — schema supports it, data curation parked
- Spend-to-date / remaining-cap tracking — the app does NOT know how much of a cap the user has consumed this month, and does not pretend to

**The Filter Question:**
> *Does this help a user with multiple cards pick the single best card for a specific purchase, faster than they could figure it out themselves?*

**Recommendation input model:** `Merchant OR Category` + `Amount (optional)` → ranked cards.
- Amount, when provided, is used for **within-purchase cap math**: a ₹45,000 purchase on a "5% capped ₹1,000/mo" card yields an effective ~2.2%, so an unlimited 2% card can legitimately win. This is deterministic and stays.
- **Prior spend this month is unknowable.** The engine assumes full cap headroom and every capped-card recommendation carries a visible caveat: *"Assumes you haven't used this month's cap."*

**[ASSUMPTION — needs validation]** Card reward-rule data will be manually researched from public bank T&Cs and manually maintained. Standing operational cost, not one-time. No public Indian API for this as far as we know. The data pipeline exists to make this sustainable.

---

## 2. Feature Inventory — MoSCoW

### Must (Core)
- Curated database covering **~80% of power users' wallets** (likely 10-25 cards; coverage outcome matters, not the count)
- **Rule engine** schema: condition → reward → cap → validity, plus:
  - **Rule priority / deterministic evaluation order** (two matching rules must resolve predictably)
  - **Exclusions** as first-class (excluded merchants, transaction types, MCC-class exclusions like rent/wallet-loads/gift-cards/EMI)
  - `Issuer` as an entity (no hardcoded bank strings); `TemporaryOffer` as a separate table (schema only)
- **Merchant resolution precedence:** merchant-specific rule → merchant family (Amazon Fresh vs Amazon Pay vs Amazon) → category → default
- Add/manage "my cards" from the curated catalog
- Category selection + searchable merchant list (~15-20 top Indian merchants) mapped via the precedence chain
- Optional amount input (within-purchase cap math only)
- Deterministic ranking with explicit tie-breakers
- Result screen with **rich explanation**: card, rate, which rule won and why (e.g. "Swiggy merchant rule overrides dining category"), cap + headroom caveat, data-verified date
- Data freshness: per-card `lastVerified` + **versioned reward DB** (e.g. "Reward DB v1.0.4, verified 10 Jul 2026")
- Data pipeline: JSON → parser → **domain objects → repository → Room** (importer decoupled from storage; a future remote source slots in behind the repository without touching the engine)
- Research audit trail: `research/<issuer>/` folder preserving source T&Cs behind every rule in `data/cards.json`
- Recommendation engine fully offline; reward data bundled with the app in v1

### Should
- Portal-routing guidance ("pay via HDFC Smartbuy for bonus rate")
- Merchant-*specific* reward rates beyond the mapping chain
- Edit/remove cards
- Basic onboarding — how ranking works, what the cap caveat means, why trust it

### Could
- Manual override — user corrects a rate before your data update lands
- Quick-access to most-used merchants/categories
- Multiple saved card sets
- Remote data updates without an app release (repository already shaped for it)

### Won't (this version)
- Debit cards; points/redemption advisor; new card recommender
- Spend logging / remaining-cap tracking
- Annual fee tracking; milestone alerts; family sharing; bill reminders
- Live temporary-offer curation
- Multi-country machinery (Issuer entity exists; nothing more)
- Full e-commerce-platform matrix

---

## 3. Dependency-Ordered Phases

**MVP line: end of Phase 2.**

| Phase | Name | Ends With |
|---|---|---|
| 0 | Project Setup + Test Harness | Empty app runs, CI green (build + lint + unit tests), architecture skeleton |
| 1 | Rule Engine Schema, Data Pipeline & My Cards | Versioned card data imported via pipeline; research trail in repo; user builds card list |
| 2 | **Recommendation Engine (MVP)** | Merchant/category (+ optional amount) → ranked cards, cap-honest, deterministic, tested |
| 3 | Merchant-Specific Rates & Portal-Routing | Merchant-level rates and portal bonuses in ranking |
| 4 | Polish & Launch Prep | Play Store-ready: onboarding, edit/delete, aggregate analytics + crash reporting, listing |

**Rev 3 is the final pre-code revision.** Remaining unknowns (schema expressiveness, real curation time per card) are only answerable by building Phase 1. Further roadmap changes now happen through the Change Protocol against real progress, not further review rounds.

---

## 4. Per-Phase Detail

### Phase 0 — Project Setup + Test Harness
**Goal:** Clean skeleton; tests wired from commit one — the engine is the product.
**Includes:** Repo + CI (build/lint/unit tests), Clean Architecture modules + Koin, Room wired but empty, Compose theming skeleton, JUnit5 infrastructure. Repo layout includes `research/`, `data/`, app modules.
**Definition of Done:**
- [ ] CI green including a trivial passing test
- [ ] App launches to blank Compose screen
- [ ] Module boundaries defined; `research/` + `data/` folders exist
**Effort:** S (~1 week)

---

### Phase 1 — Rule Engine Schema, Data Pipeline & My Cards
**Goal:** The project's central architecture, built to survive Phase 3 without migration and years of solo data maintenance.
**Includes:**
- Rule-engine schema: `Issuer` → `Card` → `RewardRule` where a rule = condition (category / merchant / payment-route) → reward → cap (amount + period) → validity, with:
  - **`priority` field + documented evaluation order** — overlapping rules (e.g. "dining 5%" + "weekend 10%") resolve deterministically
  - **Exclusion rules** — excluded merchants / transaction types / MCC-classes evaluated BEFORE positive rules (an excluded transaction earns nothing regardless of matching positive rules)
  - `TemporaryOffer` separate table (schema only); `lastVerified` per card
- **Merchant model with family precedence**: merchant-specific → merchant family → category → default, encoded in data, not code special-cases
- Pipeline: JSON → parser → domain objects → repository → Room. Validator rejects malformed rules pre-import. **Reward DB carries a version string.**
- `research/<issuer>/` folders: source links/screenshots of T&Cs for every curated rule — the audit trail for "why do you say Swiggy is 10%?"
- Seed data covering ~80% of power-user wallets
- Browse-catalog + My Cards screens
**Definition of Done:**
- [ ] Schema round-trips 3+ known-tricky real cards: one capped, one with exclusions (e.g. "5% except rent/wallet/gift cards"), one with overlapping rules needing priority
- [ ] Exclusion evaluated before positive rules — unit tested
- [ ] Merchant precedence chain unit tested (Amazon vs Amazon Fresh vs Amazon Pay resolve differently)
- [ ] Validator rejects malformed data with useful errors; import only via pipeline; DB version visible
- [ ] Every seeded rule has a research-trail entry
- [ ] Catalog browsing + My Cards persist locally
**Effort:** L (~3-4 weeks — schema design + pipeline + research)

---

### Phase 2 — Recommendation Engine (MVP LINE)
**Goal:** The killer feature — correct, cap-honest, deterministic, tested.
**Includes:**
- Input: searchable merchant list + category fallback + optional amount
- Ranking: evaluate the user's cards through the rule engine (exclusions first, then priority-ordered positive rules); when amount is given, compute **within-purchase effective value** (cap can flip the winner on large purchases)
- **Cap honesty:** full-headroom assumption + visible caveat on every capped recommendation: "Assumes you haven't used this month's ₹1,000 cap"
- Tie-breakers, in order: effective reward value → uncapped over capped → higher cap → stable card order. Documented in code and surfaced in explanations.
- Result screen per recommendation: card, rate, winning rule + why it won, cap + caveat, exclusion notes if relevant, data-verified date + DB version
- Engine test suite: cap-crossover flips winner with amount; exclusion kills an otherwise-winning card; priority resolution; merchant→family→category fallback; missing-amount behavior; tie-break determinism
**Definition of Done:**
- [ ] All engine behaviors above covered by unit tests
- [ ] Capped recommendations always show the headroom caveat
- [ ] Explanation includes rule provenance ("Swiggy merchant rule overrides dining category")
- [ ] Tested against 3+ real scenarios with your own cards
**Effort:** M-L (~3 weeks)

---

### Phase 3 — Merchant-Specific Rates & Portal-Routing
**Goal:** From category-accurate to actually optimal. Thanks to Phase 1, this is data + UI, not schema surgery.
**Includes:** merchant-specific rules for top merchants; portal-routing rules (Smartbuy-style) for top 5-6 cards with known quirks; result screen distinguishes "use this card" vs "use this card via this portal"; research-trail entries for all new rules.
**Definition of Done:**
- [ ] ≥5 cards with verified portal-routing rules where they exist
- [ ] Merchant rules override category rules in ranking — tested
- [ ] Routing guidance clearly surfaced
**Effort:** M (~2 weeks)

---

### Phase 4 — Polish & Launch Prep
**Goal:** From "works on my machine" to Play Store.
**Includes:** onboarding (how ranking works, what the cap caveat means, data-freshness explained); edit/remove cards; empty/error states; **aggregate-only analytics** (merchant search counts, card popularity, recommendation frequency, onboarding drop-off — no per-user search trails) + crash reporting; privacy policy (mandatory — card-ownership data); listing assets; manual QA pass.
**Definition of Done:**
- [ ] Install → add cards → first recommendation with no confusion
- [ ] Analytics aggregate-only, documented in privacy policy; crash reporting live
- [ ] Privacy policy published; listing submitted
**Effort:** M (~2-3 weeks)

---

## 5. Risks

- **Banks change rewards frequently and silently.** Mitigations: last-verified dates, DB versioning, pipeline for fast updates, research trail for auditability.
- **Data maintenance is a permanent solo tax.** Pipeline reduces cost, doesn't remove it. If curation lapses, the core promise breaks.
- **Cap headroom is unknowable in v1.** Engine assumes full headroom and says so visibly. Spend logging (parked) is the only real fix.
- **MCC ambiguity:** actual reward eligibility often depends on MCC codes that vary by network and are invisible to users. Recommendations are best-effort from published T&Cs; onboarding says so.
- **Rule expressiveness risk:** some bank T&Cs may not fit the schema. Phase 1's "3 tricky cards" DoD exists to surface this early; if it fails, the schema changes in week 3, not month 6.
- **Redemption value variance** intentionally out of scope (parked points-advisor problem).

---

## 6. Future & Parking Lot

| Idea | Why Parked | What Keeps the Door Open |
|---|---|---|
| Debit card benefits | Different data domain; triples curation pre-validation | Issuer/card schema generic; card type is a field |
| Points/redemption advisor | Separate valuation-data burden | Rules decoupled from redemption catalogs |
| New card recommender | Different product (comparison + credit inputs) | Phase 1 catalog is the foundation |
| Spend logging / remaining-cap tracking | Needs transaction input app doesn't collect | Cap model in schema; logging is a new input, not a remodel |
| Temporary-offer curation | Maintenance treadmill pre-launch | `TemporaryOffer` table exists |
| Remote data updates | Backend/infra pre-validation | Repository already abstracts data source; JSON is portable |
| Annual fee tracking | Not core to checkout decisions | Cheap card field |
| Milestone/spend alerts | Depends on spend logging | Same door |
| Family card sharing | Multi-user complexity | Bigger auth decision if promoted |
| Bill reminders | Off-thesis | N/A |
| Multi-country | Speculative | Issuer entity exists; country field trivial later |
| Full e-commerce matrix | Data volume, overlaps Phase 3 | Phase 3 merchant rules are the foundation |

---

## 7. Change Protocol

1. New idea mid-project → Parking Lot by default.
2. Promotion requires passing the Filter Question AND naming what gets bumped. Scope is zero-sum.
3. Every change gets a dated Changelog entry with a one-line reason.
4. Bypass attempts get challenged: *"This wasn't in the roadmap. Parking lot, or are we formally trading something out?"*
5. **Rev 3 closes the pre-code design period.** Further schema/design learnings get incorporated as dated amendments driven by Phase 1 build reality — not by additional abstract review rounds.

---

## 8. Changelog

| Date | Change | Reason |
|---|---|---|
| 2026-07-10 | Roadmap created | Initial planning session |
| 2026-07-10 | Rev 2: rule engine (condition/cap/validity), Issuer entity, TemporaryOffer table, optional amount + cap-aware ranking, merchant search in MVP, data pipeline, last-verified, tests in Phase 0/2, Risks section, coverage-based card target, analytics in Phase 4. Parked: temp-offer curation, day-1 analytics, multi-country, remote updates. | Cap-blind flat-% schema could give objectively wrong recommendations; engine avoids Phase 3 migration. |
| 2026-07-10 | Rev 3: cap honesty split (within-purchase cap math stays; prior-spend headroom assumed + visible caveat); rule priority + exclusions added to schema; merchant family precedence chain; DB versioning; research audit-trail folders; importer decoupled behind repository; richer explanations with rule provenance; aggregate-only analytics; Guiding Principles added; "fully offline" wording tightened. Declared final pre-code revision. | Second design review; overlapping-rule ambiguity and positive-only rules were latent schema defects; cap-tracking pretense would have been dishonest. |
| 2026-07-10 | Rule Engine Specification v1.0 created as Phase 1 design artifact (rule-engine-spec.md). Decisions: effective-value-% normalization for points cards; cap period types in schema, no user cycle-date input in v1; exclusion scopes (FULL / ACCELERATED_ONLY). | Phase 1 needs a concrete contract for the "3 tricky cards" validation gate. |
| 2026-07-10 | Phase 1 design set completed: Data Model (data-model.md), ADRs 001-006 (architecture-decision-records.md; ADR-006 newly decides Condition persisted as polymorphic JSON column), Data Curation Guide (data-curation-guide.md). Design period closed; implementation (Phase 0) begins. | Remaining pre-code artifacts per rev 3 protocol; all future changes now flow from build reality. |
| 2026-07-10 | Kotlin Multiplatform (Android + iOS, shared Compose UI) adopted as a project goal. ADR-002 amended per its own revisit clause: Room retained via Room KMP (bundled SQLite driver); ADR-001 realized as `:domain` / `:data` / `:shared` Gradle modules. Phase 0 implemented and pushed (github.com/Wickedsoni/MyAdvisor). | Owner decision; Room 2.7+ made KMP support official, so SQLDelight switch unnecessary. |
| 2026-07-10 | Phase 1 implemented: dataset DTOs + validator (all §3.1 rules, incl. AllOf-recursive dangling-ref and equal-priority co-match checks), atomic import pipeline behind repository, condition/exclusion matching in `:domain` (exclusions-first + precedence unit tested), 3-tricky-cards round-trip gate green, catalog + My Cards screens, DB-version footer. Verified end-to-end on emulator (import, add/remove, persistence across restart). ⚠️ Seed data (Axis ACE, HDFC Swiggy, HDFC Regalia Gold) is UNVERIFIED placeholder — research/ folders have TODO source tables that must be filled from real T&Cs before the Phase 1 DoD's "verified" claim holds. | Build reality per rev 3 protocol. |
| 2026-07-10 | Phase 2 (MVP line) implemented: `DefaultRecommendationEngine` — pure, deterministic Spec §5 algorithm (exclusions-first, priority + tie-breakers, within-purchase cap math, validity, route tagging, rich explanations with rule provenance); Spec §7 test suite items 1-9 green (12 engine tests; item 10 shipped with Phase 1); `GetRecommendationsUseCase` loads aggregates per ADR-003; Recommend tab (merchant search + category fallback + optional amount → ranked results with cap caveats, valuation notes, verified date + DB version). Verified live on emulator: Swiggy ₹20,000 ranks HDFC Swiggy 7.5% (₹1,500 cap-limited) > Axis ACE 2.5% > Regalia Gold 1.33%. | Build reality per rev 3 protocol. |
| 2026-07-10 | Design docs moved into the repo: `docs/design/` copies are now **canonical** (Desktop originals become working copies). Agent-executable roadmap created (`docs/agent-roadmap.md`) + `CLAUDE.md` conventions file. | Remaining work will be executed by AI agent sessions that need in-repo access to the SSOT. |
| 2026-07-10 | **UI/UX redesign track added** (agent-roadmap Track E): premium-fintech presentation-layer redesign (M3 Expressive where CMP supports it, dark mode, a11y, motion), replacing Phase 4's implicit "polish" line item; business logic and architecture untouched. | Owner decision via Change Protocol: user trust in a money app depends on production-grade presentation; traded against Phase 4 polish scope, net-zero. |
| 2026-07-10 | **Knowledge-extraction pipeline added as internal tooling** (agent-roadmap Track F): MITC PDF → LLM-drafted rules with citations + confidence → existing DatasetValidator → human approval via git diff; v1 is a Claude Code skill (no embeddings/vector store — no retrieval problem exists at single-document scale); programmatic extractor gated on curation volume; website change-monitoring parked post-launch. Invariant: Extraction → Validation → Human Approval; drafts never reach `main` directly. | Owner decision via Change Protocol: attacks the roadmap's #1 standing risk (permanent solo data-maintenance tax). Off the app's critical path — app scope unchanged. |

---

*This document is the single source of truth. Every future feature question gets checked against the Filter Question and MoSCoW before it goes near a sprint.*
