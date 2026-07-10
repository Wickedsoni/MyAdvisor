# MyAdvisor — Agent-Executable Roadmap

*Task backlog for AI agent sessions (Claude Opus / Sonnet class). Created 2026-07-10 after Phases 0–2 shipped. Companion to [CLAUDE.md](../CLAUDE.md) — read that first, always.*

## How to execute a task

1. Read `CLAUDE.md` (architecture rules, naming, commands, per-task DoD).
2. Read the task's **Context** pointers (spec sections + files) before writing anything.
3. Implement exactly the task's scope — new ideas go to the roadmap Parking Lot, not into your diff.
4. Run the task's **Verify** steps; UI tasks need emulator screenshots.
5. Commit (see CLAUDE.md DoD), tick the task's checkbox here, add the roadmap Changelog row.

**Model guidance:** tasks marked `opus` involve engine semantics, schema, or design-system judgment — use an Opus-class model. Tasks marked `sonnet` are well-bounded UI/data/doc work. Any task marked *agent-drafts / human-verifies* produces data that the owner must sign off before it ships (see the extraction invariant in CLAUDE.md).

**Dependency order (recommended):**

```
C0 ──► F1 ─────────────► A3, A4, C2, C3 (data tasks flow through F1)
 │
 └──► E1 ──► E2, E3, E4 ──► E5
A1 ──► A2 (A2 also wants E1)
B1 ──► B2;  B3, B4, B6 independent;  B5 LAST before launch
D2, D3 anytime;  D1 requires macOS;  F2 gated;  F3 parked
```

---

## Track E — UI/UX redesign (premium fintech)

**Design brief (applies to every E task):**
Redesign the UI to feel like a premium fintech app. Principles: Material 3 Expressive; minimal, modern, high-trust aesthetic; smooth motion and meaningful micro-interactions; information hierarchy optimized for one-handed use; rich empty and loading states; delightful but subtle animations; tactile cards with proper elevation and spacing; typography that emphasizes recommendation confidence and reward values; accessibility compliant (48dp touch targets, proper contrast); dark mode from day one. Every screen should feel production-ready, not developer-built. Inspiration: CRED, Google Wallet, Revolut, Monzo, Linear (spacing), Material 3 Expressive.

**Hard constraint (every E task):** do NOT change business logic, ViewModel contracts, `:domain`, or `:data`. Presentation layer and UX only.

**Reality check:** Material 3 *Expressive* APIs are only partially available in Compose **Multiplatform** (we're on `compose-material3` 1.11.0-alpha07). Use Expressive components where they exist in CMP; where they don't, achieve the expressive feel through the theme system (shape scale, motion tokens, typography) — do not fight missing APIs or add Android-only dependencies to `:shared`.

### [ ] E1 — Design-system foundation
`Model: opus · Prereqs: none · Touches: :shared`
**Context:** current theme `shared/src/commonMain/kotlin/com/wickedcoder/myadvisor/ui/theme/Theme.kt` (skeleton only); screens in `ui/home/`, `ui/recommend/`; brief above.
**Create/Modify:**
- `ui/theme/Color.kt` — full M3 color scheme, light + dark, from a deliberate brand seed (current seed green `0xFF1B5E4B` may be kept or replaced — document the choice)
- `ui/theme/Type.kt` — type scale; display-weight tabular numerals for reward rates/values
- `ui/theme/Shape.kt` — shape scale (expressive: larger radii on hero surfaces)
- `ui/theme/Motion.kt` — duration/easing tokens (e.g. `MotionTokens.emphasizedDecelerate`)
- `ui/theme/Theme.kt` — wire it together
- `ui/components/` — `RateBadge.kt`, `CardTile.kt`, `SectionHeader.kt`, `EmptyState.kt` (illustration slot + title + body + optional CTA), `LoadingSkeleton.kt` (shimmer), `CaveatBanner.kt` (warning surface for cap caveats), `RouteChip.kt` (portal-routing indicator), `PrimaryButton.kt`
**Steps:** build tokens first, then components; keep existing screens compiling (they migrate in E2/E3/E4).
**DoD:** both themes render; components used nowhere yet but previewable; all 47+ tests green; APK builds.
**Verify:** build + emulator launch in light AND dark (`adb shell "cmd uimode night yes|no"`), screenshot both.

### [ ] E2 — Recommend screen redesign
`Model: sonnet · Prereqs: E1 · Touches: :shared`
**Context:** `ui/recommend/RecommendScreen.kt`, `RecommendViewModel` (contract frozen); E1 components.
**Create/Modify:** `ui/recommend/RecommendScreen.kt` (rewrite presentation only).
**Steps:** hero treatment for the #1 result (elevated tile, large rate via `RateBadge`, "best pick" affordance); results 2..n compact; animated staggered reveal on result arrival; `CaveatBanner` for cap caveats; `RouteChip` for route instructions; polished amount input (₹ prefix, numeric keyboard via `KeyboardOptions`); merchant search with clear affordance; `EmptyState` for the no-cards case with a CTA that navigates to Catalog (callback prop — navigation stays in `HomeScreen`); `LoadingSkeleton` while recommending.
**DoD:** zero ViewModel changes; screen matches brief in both themes.
**Verify:** emulator flow Swiggy + ₹20,000 → screenshot light + dark; confirm #1 = HDFC Swiggy 7.5% with caveat rendered.

### [ ] E3 — Catalog + My Cards redesign
`Model: sonnet · Prereqs: E1 · Touches: :shared`
**Context:** `ui/home/HomeScreen.kt` (`CardList`, `CatalogCardRow`); `CardsViewModel` (contract frozen).
**Create/Modify:** extract `ui/cards/CatalogScreen.kt` + `ui/cards/MyCardsScreen.kt` from `HomeScreen`'s inline lists; use `CardTile`.
**Steps:** tactile card tiles (issuer monogram placeholder, name, base-rate line, verified date as subtle trust indicator); animated add/remove state transitions (`animateItem`, added-checkmark morph); `EmptyState` for empty My Cards; keep `HomeScreen` as the tab host only.
**DoD:** zero ViewModel changes; both themes.
**Verify:** emulator add/remove flow screenshots.

### [ ] E4 — App chrome + accessibility pass
`Model: sonnet · Prereqs: E2, E3 · Touches: :shared`
**Context:** `ui/home/HomeScreen.kt`; android-accessibility skill checklist.
**Create/Modify:** `HomeScreen.kt` (chrome), touched screens for a11y fixes.
**Steps:** replace top `TabRow` with bottom `NavigationBar` (one-handed reach) — icons + labels for Recommend / Catalog / My Cards; `TopAppBar` with app name and the Reward-DB version moved into it (or an info affordance) instead of the current footer text; dark-mode audit of every screen; a11y: ≥48dp touch targets, `contentDescription` on icon-only elements, contrast check on caveat/route colors, TalkBack sanity pass.
**DoD:** all screens navigable one-handed; a11y checklist items ticked in the PR description.
**Verify:** emulator screenshots of all 3 tabs × 2 themes.

### [ ] E5 — Visual QA screenshot suite
`Model: sonnet · Prereqs: E2–E4 · Touches: docs only`
**Create/Modify:** `docs/screenshots/` (`recommend_light.png`, `recommend_dark.png`, …), `docs/screenshots/README.md` with the capture script.
**Steps:** scripted adb capture of every screen (empty + populated states) × light/dark; store the exact PowerShell script used.
**DoD:** complete set committed; doubles as Play-listing asset source (B5).

---

## Track A — Phase 3: merchant rates & portal routing

### [ ] A1 — Engine route variants (Spec §5 Step 7, full)
`Model: opus · Prereqs: none · Touches: :domain`
**Context:** `docs/design/rule-engine-spec.md` §5 Step 7; `domain/.../engine/DefaultRecommendationEngine.kt`, `EngineIO.kt`, `DefaultRecommendationEngineTest.kt`.
**Create/Modify:** `EngineIO.kt` — `Recommendation` gains `val directAlternative: Recommendation? = null` (naming decision: nested single alternative, not a list — keeps ranking stable on the primary). `DefaultRecommendationEngine.kt` — when a card's winning rule has a `paymentRoute` AND a route-free rule is also eligible, rank on the route rule (it's the better rate) and attach the best route-free rule's recommendation as `directAlternative` ("1.33% direct / 6.65% via SmartBuy"). If the route-free rule would rank *better*, it stays primary and no alternative is attached (current behavior).
**DoD:** entire Spec §7 suite still green untouched; new tests: both-variant emission, route-only card (no alternative), determinism with variants.
**Verify:** `:domain:testAndroidHostTest`.

### [ ] A2 — Routing result UI
`Model: sonnet · Prereqs: A1, E1 (E2 ideally) · Touches: :shared`
**Context:** `ui/recommend/RecommendScreen.kt`; `RouteChip` from E1.
**Steps:** route-bearing results show `RouteChip` ("via HDFC SmartBuy") + instruction; when `directAlternative` present, render the split ("or 1.33% paying directly") as a secondary line/expandable.
**DoD:** roadmap Phase 3 DoD item "routing guidance clearly surfaced".
**Verify:** emulator: add HDFC Regalia Gold, query Travel category → screenshot showing portal vs direct.

### [ ] A3 — Merchant expansion dataset *(agent-drafts / human-verifies)*
`Model: sonnet · Prereqs: C0 (F1 makes this faster) · Touches: data/cards.json, research/`
**Context:** curation guide §2/§6/§9; `data/cards.json`; validator behavior via `DatasetValidatorTest`.
**Steps:** add ~10 top Indian merchants (Uber, Ola, Croma, Reliance Digital, Nykaa, Ajio, IRCTC, Cleartrip, Zepto, Dominos — adjust by coverage judgment) with correct default categories (+ new categories only if needed); add merchant-specific rules to seed cards ONLY where a Tier 1/2 source is plausibly known — every rule gets a NOTES.md row (confidence LOW + TODO-verify allowed pre-launch); respect priority bands; minor `dataVersion` bump.
**DoD:** dataset gate green; every new rule has a NOTES.md row; changelog row.
**Verify:** dataset gate test; emulator search finds new merchants.

### [ ] A4 — Portal-routing data, ≥5 cards *(agent-drafts / human-verifies)*
`Model: sonnet · Prereqs: C0, ideally F1 · Touches: data/cards.json, research/`
**Context:** roadmap Phase 3 DoD; curation guide.
**Steps:** add 2–3 cards with known portal mechanics (candidates: HDFC Millennia, HDFC Infinia, Axis Atlas — owner confirms which) so ≥5 catalog cards exist with `paymentRoute` rules where routes genuinely apply; research folders per convention; minor bump.
**DoD:** roadmap Phase 3 DoD: ≥5 cards with routing rules where they exist; merchant-over-category ranking already covered by engine tests.

---

## Track B — Phase 4: polish & launch prep

### [ ] B1 — App preferences plumbing
`Model: sonnet · Prereqs: none · Touches: :domain, :data`
**Context:** `data-model.md` user zone; `AppDatabase.kt` (version 1), `UserEntities.kt`.
**Create/Modify:** `data/.../db/UserEntities.kt` — `AppPrefEntity` (`app_prefs`: `key` PK, `value` String); `AppPrefsDao`; `AppDatabase` → version 2 **with Room auto-migration** (`@Database(version = 2, autoMigrations = [AutoMigration(1, 2)])` — schemas dir already exports); domain port `AppPreferencesRepository` (get/set string, observe) in `domain/.../repository/Repositories.kt`; `RoomAppPreferencesRepository`; Koin wiring.
**Decision recorded:** Room table over a settings library — no new dependency, reuses zone rules (user zone survives imports).
**DoD:** migration test or fresh-install + upgrade check on emulator; all tests green.

### [ ] B2 — Onboarding flow
`Model: sonnet · Prereqs: B1, E1 · Touches: :shared`
**Create/Modify:** `ui/onboarding/OnboardingScreen.kt`, `OnboardingViewModel.kt`; `App.kt` gate.
**Steps:** 3 pages using E1 components: (1) how ranking works (deterministic, explained recommendations); (2) the cap caveat — the app assumes full cap headroom because it can't know prior spend — plus the MCC best-effort disclaimer (roadmap risk item); (3) data freshness — lastVerified dates + Reward DB version, why trust it. Skippable; shown once via `app_prefs` key `onboarding_completed`.
**DoD:** roadmap Phase 4 DoD "install → add cards → first recommendation with no confusion"; fresh install shows it, second launch doesn't.
**Verify:** emulator: clear app data → relaunch → screenshots of all 3 pages.

### [ ] B3 — My Cards management polish
`Model: sonnet · Prereqs: E3 · Touches: :shared, :data (DAO update only)`
**Steps:** reorder via up/down buttons persisting `sort_order` (decision: buttons over drag — simpler cross-platform); remove confirmation dialog; import-failure state gets a Retry button (re-invokes importer through `CardsViewModel`).
**DoD:** order survives restart; remove is confirmable; retry works (test by temporarily breaking nothing — just verify the code path compiles and the button renders in the error state).

### [ ] B4 — Aggregate-only analytics + crash reporting
`Model: opus · Prereqs: none · Touches: :shared, androidApp`
**Context:** roadmap Phase 4 ("aggregate-only … no per-user search trails"); privacy is a launch blocker.
**Create/Modify:** `shared/.../analytics/AnalyticsLogger.kt` — `expect`/interface with no-op iOS + no-op default; Android actual backed by Firebase Analytics; Crashlytics in `androidApp`; google-services config (owner supplies `google-services.json` — task documents the manual step); event list doc `docs/analytics-events.md`: `merchant_search` (no query text — count only), `recommendation_shown` (merchant/category id, NOT amounts), `card_added` (card id), `onboarding_step`. `docs/privacy-policy.md` draft covering card-ownership data, aggregate analytics, no account, local storage.
**DoD:** events fire in debug (Firebase DebugView or logcat); NO free-text/amount params anywhere; privacy doc drafted.

### [ ] B5 — Release engineering (run LAST before launch)
`Model: sonnet · Prereqs: everything shipping · Touches: androidApp, docs`
**Steps:** `isMinifyEnabled = true` + `proguard-rules.pro` covering Room, kotlinx-serialization (keep `@Serializable` + serializers), Koin, Compose; verify release build runs on emulator (serialization is the usual R8 casualty — test the import flow specifically); real app icon + themed splash; versionCode/versionName policy note; signing via gitignored `keystore.properties` (documented manual step); `docs/play-listing.md` checklist (title, short/long description, screenshots from E5, content rating, data-safety form answers matching the privacy policy).
**DoD:** `assembleRelease` builds, installs, imports data, and recommends correctly on the emulator.

### [ ] B6 — Manual QA script
`Model: sonnet · Prereqs: B2 · Touches: docs only`
**Create/Modify:** `docs/qa-script.md`.
**Steps:** scripted pass with expected outputs computed from the current seed dataset: fresh install → onboarding → add 3 cards → Swiggy ₹20,000 (expect 7.5% / 2.5% / 1.33% ordering) → groceries no-amount → huge amount (₹5,00,000 cap crossover) → excluded case (wallet-load hint if UI exposes it, else amazon_pay_gc for ACE base-only) → remove card → restart persistence.
**DoD:** another agent can execute it verbatim.

---

## Track C — Data curation (human-in-the-loop)

### [x] C0 — Align Phase 1 output with the curation guide
`Model: sonnet · Prereqs: none — DO THIS FIRST · Touches: research/, data/cards.json`
**Context:** curation guide §4 (NOTES.md template) + §6 (priority bands); the Phase 1 files used `sources.md` and off-band priorities.
**Steps:** rename `research/*/*/sources.md` → `NOTES.md`, restructure to the §4 template (sources, rules-extracted table with confidence, exclusions, judgment calls, open questions — carry over existing content incl. the shared-cap modeling note); renumber `cards.json` priorities into bands: `axis_ace_billpay` (category) 10→30, `axis_ace_swiggy` 20→10, `axis_ace_zomato` 21→11, `hdfc_swiggy_merchant` stays 10, `hdfc_swiggy_online` (category) 20→30, `hdfc_regalia_gold_smartbuy_travel` (category) 10→30, `hdfc_regalia_gold_myntra` (merchant) 20→10; patch `dataVersion` → 1.0.1; check `BundledDatasetValidationTest` (it asserts folder existence, not filename — confirm).
**DoD:** dataset gate green; engine tests green (priorities changed but relative order per card preserved — verify winners unchanged via existing tests).

### [x] C1 — Research template
`Model: sonnet · Prereqs: C0 · Touches: research/`
**Create/Modify:** `research/_TEMPLATE/NOTES.md` — blank §4 template + §6 workflow checklist + §9 red-flags list inline.

### [x] C2 — Verify the 3 seed cards *(agent-drafts / HUMAN SIGNS OFF)*
`Model: sonnet + WebSearch · Prereqs: C0 · Touches: research/, data/cards.json`
**Steps:** for Axis ACE, HDFC Swiggy, HDFC Regalia Gold: locate current official MITC/product pages (Tier 1/2), archive into research folders, correct any wrong rates/caps/exclusions in `cards.json`, fill NOTES.md tables with real citations. **The owner dates the Verified column — an agent must not mark a rule verified.** Patch bump per correction.
**DoD:** all three cards' rules have Tier 1/2 citations; `lastVerified` updated by owner.
**Status (2026-07-10):** Done with one flagged exception. Axis ACE and HDFC Swiggy are fully Tier-1-cited (real dated MITC/T&C PDFs archived in `research/`, confirmed from the genuine `axisbank.com`/`hdfcbank.com` domains — `hdfc.bank.in` also treated as legitimate after `hdfcbank.com` 301-redirected to it). HDFC Regalia Gold's base and Myntra-family rules are Tier-1-cited; **`hdfc_regalia_gold_smartbuy_travel` remains unverified** — the SmartBuy portal pages are JS-rendered and returned no usable content via WebFetch or the browser tool (permission-denied on that banking domain); documented as an open question in NOTES.md rather than guess-corrected. `lastVerified` intentionally left untouched (owner-only per this task's own constraint) — dataset gate + full test suite green, `assembleDebug` compiles.

### [x] C3 — Add card X (repeatable template task) — HDFC Millennia (card 4/10-25)
`Model: sonnet (via /extract-card once F1 exists) · Prereqs: C0, F1 · Touches: research/, data/cards.json`
**Steps:** owner picks the card (coverage-driven, guide §7 — own cards first, then high-circulation power-user cards); run F1 skill on archived sources; review branch diff; merge; minor bump. Repeat toward 10–25 cards.
**DoD per card:** NOTES.md with citations; validator green; owner-approved diff.
**Status (2026-07-10):** HDFC Millennia added — owner picked it from the F1 skill's card-selection prompt. Archived the real T&C PDF, drafted `research/hdfc/millennia/NOTES.md` (HIGH confidence on all 10 rules) and `data/cards.json` entries (1 base + 9 merchant rules covering 9 of the sourced 10-merchant 5% list — Cult.fit skipped, no catalog category fits it). Discovered and documented the worst instance yet of the per-rule shared-cap schema limitation (9-way share on one ₹1,000/month cap, vs. 4-way on Axis ACE/Regalia Gold) — flagged as a real candidate for a future shared-cap-groups schema feature rather than silently narrowed. `dataVersion` bumped to 1.2.0 (new card + 3 new merchants: Uber, Sony LIV, Tata CLiQ). Validator + full test suite green, `assembleDebug` compiles. **Not yet on a `data/hdfc_millennia` branch or merged** — this and all of C0-C3's work sit uncommitted on `main`'s working tree; branch/commit handling deferred to explicit owner instruction per session git-safety rules. `lastVerified` untouched (owner-only).

---

## Track F — Knowledge-extraction pipeline (internal tool — NOT user-facing)

**Invariant (CLAUDE.md):** `Extraction → Validation → Human Approval`. Drafts never reach `main` directly. Every drafted rule carries citation + confidence.
**Decision:** no chunking/embeddings/vector store in v1 — a single card's MITC fits in one context window; there is no retrieval problem yet. Revisit trigger: F3 (many documents, change detection).

### [x] F1 — `/extract-card` Claude Code skill
`Model: opus (to build the skill; any model runs it) · Prereqs: C0, C1 · Touches: .claude/skills/`
**Create/Modify:** `.claude/skills/extract-card/SKILL.md`.
**Steps — the skill instructs the executing agent to:**
1. Input: `issuerId`, `cardSlug`, archived source files already placed in `research/<issuerId>/<cardSlug>/` (PDF/screenshots). Refuse to proceed from URLs alone — sources must be archived first (guide §1).
2. Read the sources; extract base rate, accelerated rules, caps (exact period type), exclusions (FULL vs ACCELERATED_ONLY — read the footnotes), portal rules; points cards get conservative valuation per guide §5.
3. Write `NOTES.md` per template: every rule row carries source location (file, page, section) + confidence HIGH/MED/LOW.
4. Draft `cards.json` entries: ids and priority bands per CLAUDE.md conventions; `earnDescription` human-readable; `valuationNote` for points.
5. Guardrails (guide §9 verbatim): "up to X%" is a ceiling, not a rate — find the table; 10%+ uncapped means you missed a cap; MITC beats product page on conflict (note the conflict); never infer a rule from "how it should work".
6. Bump `dataVersion` (minor), run the dataset gate test, run engine tests.
7. Commit to branch `data/<issuerId>_<cardSlug>` — NEVER `main` — and print a review summary: rules drafted, every LOW/MED-confidence row, open questions.
**DoD:** skill file exists; dry-run on one seed card's existing research reproduces sane output on a branch.
**Verify:** run `/extract-card` against `research/hdfc/swiggy/` materials; confirm branch + review summary.
**Status (2026-07-10):** `.claude/skills/extract-card/SKILL.md` built. Full content-extraction dry-run is **blocked until archived Tier 1/2 sources exist** — the seed research folders currently hold only placeholder `NOTES.md` (no MITC PDFs/screenshots), so `/extract-card` correctly *refuses* at its input gate rather than inventing rules. A genuine end-to-end run happens as part of **C2** (locate + archive real MITC/product pages for the 3 seed cards), which is the skill's first real exercise.

### [ ] F2 — Programmatic extractor `:tools:extraction` *(GATED — build only when C3 backlog >10 cards or curation >5 cards/month)*
`Model: opus · Prereqs: F1 in regular use · Touches: new module`
**Create/Modify:** `tools/extraction/` JVM Gradle module (`include(":tools:extraction")`), package `com.wickedcoder.myadvisor.tools.extraction`: `PdfTextExtractor` (Apache PDFBox), `RuleExtractor` (Claude API structured output — consult the claude-api skill for current model ids; temperature 0), `ExtractionDraft` schema: `{ rule: RewardRuleDto-shaped, source: { file, page, section }, confidence: Double }`, `DraftReviewReport` (markdown diff vs current dataset for approval), CLI `main`. **Reuses `DatasetValidator` + `DatasetDto` from `:data`** — that reuse is the point of living in this repo.
**DoD:** end-to-end on one archived MITC PDF → draft + report; validator wired; nothing writes `cards.json` directly (report + branch only).

### [ ] F3 — Website change monitoring *(PARKED — post-launch; promoting it requires a Change Protocol row)*
Sketch for the future task: crawler over Tier 1/2 URLs recorded in NOTES.md → content-hash diff → only changed pages → F2 extraction → diff report → notify owner → approve → dataset release. This is where chunking/embeddings may earn their keep.

---

## Track D — Platform & infra

### [ ] D1 — iOS verification *(macOS required — blocked on hardware)*
`Model: any, on a Mac · Touches: iosApp, possibly :shared iosMain`
**Steps:** open `iosApp` in Xcode; build; verify Koin init (`iOSApp.swift` calls `KoinIosKt.doInitKoinIos()`), Room DB path (Documents dir), dataset import, all tabs, dark mode. Known risk spots: `BundledSQLiteDriver` linking, compose-resources `files/cards.json` in the iOS bundle, `NSDocumentDirectory` path.
**DoD:** app runs on iOS simulator through the full QA happy path (B6 script).

### [ ] D2 — Build hygiene
`Model: sonnet · Prereqs: none · Touches: all module build files`
**Steps:** migrate deprecated `androidLibrary {}` → `android {}` DSL in `shared/`, `domain/`, `data/` build files (AGP 9.2 KMP plugin rename); add `-Xexpect-actual-classes` to suppress the known Room-constructor beta warning (per-module `compilerOptions.freeCompilerArgs`); add a short dependency-update policy note to CLAUDE.md (versions live in `gradle/libs.versions.toml`; bump deliberately, never mid-task).
**DoD:** clean build with neither the deprecation nor the expect/actual warning; all tests green.

### [ ] D3 — CI hardening
`Model: sonnet · Prereqs: none · Touches: .github/workflows/ci.yml`
**Steps:** split into jobs: `build` (assembleDebug + lint), `test` (the three testAndroidHostTest tasks), `dataset-gate` (the BundledDatasetValidationTest filter) so data PRs fail fast with a readable job name; enable Gradle caching via `gradle/actions/setup-gradle`; upload `**/build/test-results/**` as artifact on failure.
**DoD:** CI green on a test PR; dataset-gate job visibly separate.
