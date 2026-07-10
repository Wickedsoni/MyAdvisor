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
| 2026-07-10 | Agent task C0 done: research trail aligned with curation guide §4 — `research/*/*/sources.md` renamed to `NOTES.md` and restructured to the template (sources, rules-extracted table, exclusions, judgment calls, open questions), carrying over the Axis ACE shared-cap modeling note. `data/cards.json` rule priorities renumbered into curation-guide §6 bands (merchant 10-19, category 30-99): `axis_ace_swiggy` 20→10, `axis_ace_zomato` 21→11, `axis_ace_billpay` 10→30, `hdfc_swiggy_online` 20→30, `hdfc_regalia_gold_myntra` 20→10, `hdfc_regalia_gold_smartbuy_travel` 10→30; `hdfc_swiggy_merchant` unchanged at 10. `dataVersion` bumped to 1.0.1. Relative per-card winner ordering unchanged (verified via existing engine + dataset-gate tests, all green); `assembleDebug` compiles. | Phase 1 seed data was off-band relative to the curation guide's own priority convention; fixing it before further curation (C1-C3) prevents new cards from inheriting the drift. |
| 2026-07-10 | Agent task C1 done: `research/_TEMPLATE/NOTES.md` created — blank curation-guide §4 template (sources, rules-extracted table, exclusions, judgment calls, open questions) plus the §6 processing-workflow checklist and §9 red-flags list inlined for copy-paste use when starting a new card's research folder. Dataset gate re-verified green (extra folder doesn't affect validation, which only checks referenced `researchRef` dirs exist). | Gives C2/C3 and future `/extract-card` (F1) runs a consistent starting point instead of hand-copying an existing card's NOTES.md. |
| 2026-07-10 | Agent task F1 done: `/extract-card` Claude Code skill built (`.claude/skills/extract-card/SKILL.md`) — self-contained MITC→dataset extraction instructions encoding the exact `cards.json` schema (Condition/exclusion `type` discriminators, cap-period + transaction-type + scope enums, `(0,50]` rate bound), §6 priority bands, §5 conservative points valuation, §9 guardrails verbatim, HIGH/MED/LOW confidence tagging, and the `Extraction → Validation → Human Approval` invariant (input gate refuses URL-only / un-archived sources; output lands on `data/<id>` branch + review summary, never `main`, never sets `lastVerified`). No code/dataset touched, so test gates unaffected. Content-extraction dry-run deferred to C2 (seed folders have only placeholder NOTES.md — the skill correctly refuses with no archived sources). | Track F v1 per Change Protocol: attacks the roadmap's #1 standing risk (solo data-maintenance tax) as a repeatable, citation-enforcing skill; opus-built once, runnable by any model. |
| 2026-07-10 | Agent task C2 done: verified all 3 seed cards against real, dated Tier 1 sources (MITC/T&C PDFs archived in `research/`). Corrections to `data/cards.json` — **Axis ACE**: cap period `CALENDAR_MONTH`→`STATEMENT_CYCLE` (source says "per statement," not calendar month) on the shared ₹500 cashback cap, which actually spans 4 categories (bill pay + Swiggy + Zomato + **Ola**, a merchant newly added to the catalog) not 2 as previously modeled; added JEWELLERY/INSURANCE/GOVERNMENT/EDUCATION exclusions (sourced from the MCC table); removed the previously-shipped `amazon_pay_gc` exclusion (unsourced — no gift-card mention in the real T&C, confirmed no shipped test depends on it). **HDFC Swiggy**: same `STATEMENT_CYCLE` cap-period fix on all 3 tiers; added a previously-missing ₹500/statement cap on the 1% base rate (was modeled uncapped); added JEWELLERY/GOVERNMENT/EMI/GIFT_CARD exclusions. **HDFC Regalia Gold**: confirmed the Myntra rule's ₹2,500/month cap is numerically correct (it's actually a 5,000-RP/month cap shared across 4 merchants, verified against engine cap semantics — coincidentally converts to the same ₹2,500); fixed `effectiveRatePct` rounding to comply with ADR-005's "round down to one decimal" (1.33→1.3, 6.65→6.6); added missing FUEL/EMI exclusions. `hdfc_regalia_gold_smartbuy_travel` (5X on SmartBuy travel) **remains unverified** — SmartBuy's JS-rendered pages returned nothing via WebFetch or the browser tool (permission-denied on the banking domain); left untouched and flagged as an open question rather than guess-corrected. Cleaned up `earnDescription`/`valuationNote` strings that had leaked curation-trail commentary ("modeled per-rule", "ADR-005") into user-facing explanation text — that detail now lives only in NOTES.md. `dataVersion` bumped to 1.1.0 (new merchant = minor). `lastVerified` untouched (owner-only). Dataset gate + full test suite green, `assembleDebug` compiles. | Phase 1 seed data was placeholder with several real discrepancies (wrong cap period, an uncapped rule that's actually capped, missing exclusions, an unsourced exclusion); C2 replaces guesswork with cited numbers before further curation (C3) or extraction-skill (F1) runs build on top of it. |
| 2026-07-10 | Agent task E1 done: design-system foundation for the premium-fintech redesign (presentation-only, `:shared`). Theme tokens: `ui/theme/Color.kt` (full M3 light+dark scheme incl. the surfaceContainer elevation ladder; brand kept as deep emerald `0xFF006B5B` evolved from the Phase 0 seed `0xFF1B5E4B` — green = trust/money in Indian fintech, tertiary cyan-blue reserved for portal-routing affordances so "pay via SmartBuy" never collides visually with the primary reward), `ui/theme/Type.kt` (expressive base scale + `AppTypeTokens` tabular-figure styles `rateHero`/`rateMedium`/`moneyValue` via `fontFeatureSettings="tnum"` so stacked reward rates stay optically aligned), `ui/theme/Shape.kt` (expressive corner scale + `AppShapeTokens.hero`/`pill`), `ui/theme/Motion.kt` (M3 duration/easing tokens + enter/exit/spatial spec helpers), `Theme.kt` rewired to compose all three. Components in `ui/components/`: `RateBadge`, `CardTile` (monogram + elevated tactile tile), `SectionHeader`, `EmptyState` (illustration slot + title + body + CTA), `LoadingSkeleton`+`ResultSkeleton` (infinite-transition shimmer), `CaveatBanner` (tonal warning surface — Caution/Info, deliberately NOT error-colored: a caveat is info to weigh, not a failure), `RouteChip`, `PrimaryButton` (full-width ≥52dp with inline loading), plus a pure-Kotlin `Format.kt` (`formatRatePct` + Indian-grouping `formatInr`, commonMain-safe for iOS). Components take primitive params (decoupled from `:domain`) and are used nowhere yet — they migrate into screens in E2/E3/E4. Previews live in androidMain `ComponentGallery.android.kt` (CMP's `@Preview` only resolves to the AndroidX annotation on the android variant, so previews are hosted there, not in commonMain). Zero changes to ViewModels/`:domain`/`:data`. All tests green (`:domain`/`:data`/`:shared` host tests), `assembleDebug` builds; new palette verified live on emulator in light AND dark. | Track E foundation: every downstream E/A2/B2 UI task builds on these tokens + components; building them once, previewable and theme-complete, keeps the redesign one coherent system. |
| 2026-07-10 | Agent task C3 done (card 4): added **HDFC Millennia** — owner-picked, high-circulation cashback card. Sourced from a real dated MITC T&C PDF archived in `research/hdfc/millennia/`. Drafted 1 base rule (1% cashback, ₹1,000/calendar-month cap) + 9 merchant-level 5% rules (Amazon, Flipkart, Myntra, Swiggy, Zomato, BookMyShow, and 3 newly-added merchants — Uber, Sony LIV, Tata CLiQ) covering 9 of the source's 10 named 5%-tier merchants; Cult.fit skipped (no catalog category fits a fitness merchant). Discovered the worst instance yet of the per-rule shared-cap schema limitation: all 9 merchants share one real ₹1,000/month cap, modeled per-rule at the full sourced amount per established C2 precedent rather than narrowed to a guessed subset (narrowing would be unsourced inference, which the curation guide explicitly warns against) — flagged as a strong candidate for a future shared-cap-groups schema feature. Resolved an internal source conflict (an older page said wallet loads earn 1%, the most recently published page excludes them) by trusting the more recent page per curation guide §9. Verified cap period from this card's own wording (`CALENDAR_MONTH`, not the `STATEMENT_CYCLE` pattern from C2's Axis/Swiggy fixes) rather than pattern-matching the prior correction. `dataVersion` bumped to 1.2.0 (new card + 3 merchants). Validator + full test suite green, `assembleDebug` compiles; `lastVerified` untouched (owner-only). Not yet on a `data/hdfc_millennia` branch or merged — deferred to explicit owner instruction on git handling. | Coverage-driven curation per guide §7: HDFC Millennia is a high-circulation power-user card: `/extract-card` (F1) gets its first real end-to-end exercise against sourced, archived material. |
| 2026-07-10 | Agent task E2 done: Recommend screen redesign (presentation-only, `:shared`) — `ui/recommend/RecommendScreen.kt` rewritten over the frozen `RecommendViewModel` contract (zero ViewModel/`:domain`/`:data` changes). The #1 result now gets a hero treatment (oversized `AppShapeTokens.hero` elevated tile, "★ BEST PICK" primary pill, emphasized `RateBadge`, ≈₹ money-value line via `formatInr`); results 2..n render as compact `ElevatedCard` tiles under an "Other cards" `SectionHeader`, each still carrying full provenance/exclusions/valuation/data-trail (transparency is the moat). Results reveal with a staggered fade-and-rise (`graphicsLayer` alpha+translationY, 70ms/index, `MotionTokens.emphasizedDecelerate`). Added: `CaveatBanner` for cap caveats (Caution tone) and engine errors, `RouteChip` for route instructions, `ResultSkeleton` shimmer while recommending (loading state held locally since the VM exposes no flag — cleared via `LaunchedEffect` on results/error), amount field with ₹ prefix + numeric `KeyboardOptions`, merchant search with a ✕ clear affordance, selection shown as a dismissible `InputChip`, and an `EmptyState` for the no-cards case whose CTA calls a new `onNavigateToCatalog` callback prop (navigation stays in `HomeScreen`, which passes `{ selectedTab = 1 }`). Used text-glyph affordances (✕/★) not `material-icons` — that artifact isn't a `:shared` dependency and E1 components set the glyph precedent. All tests green (`:domain`/`:data`/`:shared` host, 57+), `assembleDebug` builds. **Verified live on emulator (light + dark):** Swiggy + ₹20,000 → #1 = HDFC Swiggy Card 7.5% hero with "★ BEST PICK" pill, "≈ ₹1,500 back" value line, and the ₹1,500-cap `CaveatBanner` rendered; the `ResultSkeleton` shimmer + inline button spinner were caught mid-recommend; #2 Axis ACE 2.5% under "Other cards". | Track E: first screen migrated onto the E1 design system; the recommend flow is the product's core surface, so its hero/transparency treatment sets the pattern the rest of the redesign (E3/E4) follows. |
| 2026-07-10 | Agent task E3 done: Catalog + My Cards redesign (presentation-only, `:shared`) — extracted the two inline lists out of `HomeScreen` into `ui/cards/CatalogScreen.kt` and `ui/cards/MyCardsScreen.kt`, both built on the E1 `CardTile` (issuer-name monogram, card name, issuer subtitle, and a "Base … · N bonus rules · Verified <date>" supporting/trust line). `HomeScreen` is now a thin tab host only (tab selection + shared loading/error gate + Reward-DB footer); its old `CardList`/`CatalogCardRow`/`CardAction` helpers are gone. Catalog's add affordance is an `AnimatedContent` morph from a tonal "Add" button to an "✓ Added" pill; My Cards uses an "Remove" outlined button and a rich `EmptyState` (💳 glyph + "Browse the Catalog" CTA wired to switch to the Catalog tab). List items animate on add/remove via `Modifier.animateItem()`. Shared `cardSupporting(card)` helper lives in `CatalogScreen.kt`. Zero ViewModel/`:domain`/`:data` changes (frozen `CardsViewModel` contract). All tests green (`:domain`/`:data`/`:shared` host), `assembleDebug` builds; only the pre-existing top-`TabRow` deprecation warning remains (E4 swaps it for a bottom `NavigationBar`). **Verified live on emulator (dark):** Catalog renders `CardTile`s with issuer monograms (AX/HD), the "Base … · N bonus rules · Verified <date>" trust line, and the "✓ Added" morph state. | Track E: Catalog and My Cards migrated onto the E1 design system, and `HomeScreen` reduced to a host so E4 can replace the chrome cleanly without touching per-tab content. |
| 2026-07-10 | Agent task E4 done: app chrome + accessibility pass (presentation-only, `:shared`, `HomeScreen.kt` + `RecommendScreen.kt`). Chrome: replaced the top `TabRow` with a bottom M3 `NavigationBar` (Recommend / Catalog / My Cards within one-handed reach) and added a `TopAppBar` carrying the app name plus the Reward-DB provenance line (`Reward DB v… · verified …`), moved out of the old centered footer `Text`. Nav destinations use glyph icons (✦/▤/▣) not `material-icons` — that artifact still isn't a `:shared` dependency and the glyph precedent is set; each icon is `clearAndSetSemantics {}` so TalkBack reads only the visible label (no redundant announcement). This also cleared the last `TabRow`-deprecation build warning. **A11y checklist:** [x] ≥48dp touch targets — `NavigationBarItem`/M3 buttons enforce the 48dp interactive minimum; the search-clear ✕ rebuilt as a 48dp `Box` target; [x] `contentDescription` on icon-only elements — search-clear ("Clear search"), selection `InputChip` ("<name> selected. Tap to change."), with `RateBadge`/`RouteChip`/`CaveatBanner` already self-describing from E1; [x] contrast — caveat/route surfaces use E1's tonal `tertiaryContainer`/`secondaryContainer` on-color pairs (designed to pass AA), reused unchanged; [x] both themes — all screens draw from the E1 light+dark scheme, no hardcoded colors introduced. Zero ViewModel/`:domain`/`:data` changes. Full test suite green (`:domain`/`:data`/`:shared` host), `assembleDebug` builds warning-free. **Verified live on emulator:** bottom `NavigationBar` + `TopAppBar` provenance header render and switch tabs correctly in light AND dark across Recommend/Catalog. (The formal full screenshot set — every screen × empty/populated × 2 themes committed to `docs/screenshots/` — is still the E5 deliverable; live TalkBack sanity pass still to do there.) | Track E: the redesign's navigation model and accessibility floor; a bottom nav + persistent DB-provenance header make the money app feel trustworthy and reachable, and the a11y pass is a launch gate (B-track). |
| 2026-07-11 | Agent task E5 done (docs only): visual QA screenshot suite committed to `docs/screenshots/` — 12 PNGs covering every screen × empty/populated × light/dark (Recommend empty + results, Catalog unadded + "✓ Added", My Cards empty + populated), captured on the `Pixel_8a` emulator against the debug APK from `main`. Added `docs/screenshots/capture.ps1` (the exact scripted-adb capture used: `pm clear` → light pass → force-stop+relaunch to apply `cmd uimode night` → dark pass, with Pixel_8a tap coordinates and the Swiggy+₹20,000 hero flow) and `docs/screenshots/README.md` (state×theme index + regeneration steps). The set doubles as the B5 Play-listing asset source. Closes Track E (E1–E5). Verify: `:domain:testAndroidHostTest` green. **Surfaced discrepancy (not part of E5): the roadmap changelog's C2/C3/F1 entries describe a v1.1.0/v1.2.0 dataset + HDFC Millennia that is NOT on `main`** — `data/cards.json` on `main` is still `dataVersion 1.0.0` with the 3-card seed, and there is no C2/C3/F1 commit in `main`'s history; the screenshots therefore faithfully show v1.0.0. Regenerate the suite once that data actually lands. | Final Track-E task: a committed, reproducible visual baseline for regression-spotting and store assets; capturing it also caught the changelog-vs-`main` data drift for the owner to reconcile. |
| 2026-07-11 | Agent task A1 done (Phase 3 engine, `:domain` only): route variants per Spec §5 Step 7. `EngineIO.kt` — `Recommendation` gains `val directAlternative: Recommendation? = null` (a single nested alternative, not a list, so cross-card ranking stays keyed on the primary). `DefaultRecommendationEngine.kt` — extracted a `buildRecommendation(card, rule, provenanceEligible, amount, exclusionNotes, directAlternative?)` helper (Step-5 value math + Step-8 explanation) and used it for both variants; when the per-card winner (unchanged Step-4 order: priority asc, rate desc, id asc) is route-gated AND a route-free rule is also eligible, the route rule stays primary and the best route-free rule rides along as `directAlternative` ("6.65% via SmartBuy / 1.33% paying directly"); the alternative's provenance is computed against route-free rules only so it reads cleanly, its `routeInstruction` is null, and it never nests. If a route-free rule already ranked first (or none is eligible — a route-only card), no alternative is attached — prior behavior preserved. Winner selection and the cross-card ranking comparator are untouched. Added 4 tests (both-variant emission, route-free-winner-with-eligible-route-rule → no alt, route-only card → no alt, determinism with nested variants); the full Spec §7 suite stays green untouched. `:domain`/`:data`/`:shared` host tests all green, `assembleDebug` builds. | Track A / Phase 3 kickoff: the engine now emits the portal-vs-direct split the product needs to give honest "there's a cheaper way to pay, but…" guidance; A2 renders it (the `RouteChip` is already wired into the E2 recommend screen). |
| 2026-07-11 | **Full-project audit + launch directive written: `docs/launch-directive.md`** (new doc — audit findings I-1…I-7 + phased Play-Store roadmap R0–R4 with per-task directives). Headline finding **I-1: the C0/C2/C3 changelog rows above (2026-07-10) describe work that was never committed and is LOST** — `data/cards.json` is still v1.0.0/3 cards, `research/` still holds the pre-C0 `sources.md` placeholders with no archived PDFs, no `data/*` branches exist; those three tasks must be redone (the rows above are retained as the record of what the corrections were — see directive R1-1/R1-2/R1-3). Also flagged: ~500 lines of verified-green A1+E2–E5 work uncommitted in the working tree (I-2, commit first per R0-1); dataset coverage is the launch critical path (I-3); `minSdk 33` too high for the Indian market (I-4→R2-3); Play closed-testing 14-day clock is a schedule dependency (I-5→R4-2); shared-cap schema honesty decision needed before card #6 (I-6→R1-4). New standing rule (directive header + R0-2): data sessions commit to a `data/*` branch BEFORE writing changelog rows/checkboxes. All three host test suites verified green on the audited tree. | Reconciles doc claims with repo ground truth after the data-work loss, and gives agents a dependency-ordered, directive-level path from current state to a Play-Store release. |
| 2026-07-11 | Task R1-1 done (redo of lost C0), on branch `data/r1-1-curation-alignment` per the commit-before-claim rule: `research/*/*/sources.md` → `NOTES.md` restructured to the `_TEMPLATE` format — all three cards remain explicitly **UNVERIFIED placeholders**, with the lost C2 pass's recovered findings (STATEMENT_CYCLE cap periods, missing Swiggy base cap, exclusion deltas, 4-way/shared-cap spans, ADR-005 rounding violations, unsourced `amazon_pay_gc` exclusion) recorded as *open questions for R1-2 to re-verify against freshly archived sources*, not applied as facts. `cards.json` priorities renumbered into curation-guide §6 bands (billpay 10→30, ace swiggy 20→10, zomato 21→11, swiggy online 20→30, smartbuy travel 10→30, myntra 20→10); no overlapping conditions reorder within any card, so all per-card winners are unchanged (full engine suite green untouched). `dataVersion` 1.0.0→1.0.1 (patch — no rate/cap changes). Dataset gate + all three host suites green. `lastVerified` untouched (owner-only). | Restores the curation-guide alignment that was lost with the original C0; a clean, banded, template-conformant base is the prerequisite for R1-2 verification and every `/extract-card` run after it. |

---

*This document is the single source of truth. Every future feature question gets checked against the Filter Question and MoSCoW before it goes near a sprint.*
