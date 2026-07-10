# MyAdvisor — Launch Directive (audit + Play-Store roadmap)

*Written 2026-07-11 after a full repo audit. This is a **directive document**: each task states what is wrong, what to change, exactly where, and how to verify. Read [CLAUDE.md](../CLAUDE.md) first, always. This document sequences and extends [agent-roadmap.md](agent-roadmap.md) — task IDs (A1, B2, C0…) refer to that backlog; tasks marked **NEW** exist only here and should be added to the backlog when picked up.*

**Execution rule for every task below:** follow the CLAUDE.md Definition of Done (tests green, dataset gate if data touched, emulator screenshot if UI touched, dated changelog row in `docs/design/credit-card-advisor-roadmap.md` §8, checkbox ticked). **Additional rule created by incident I-1 below: any session touching `data/cards.json` or `research/` must commit to a `data/*` branch BEFORE writing its changelog row or ticking its checkbox.** Docs claiming work that was never committed is how this project lost three tasks of work.

---

## Part 1 — Ground truth: what the docs claim vs. what the repo contains

Audited 2026-07-11 against `main` @ `90d2327` + working tree. All three host test suites pass (`:domain`, `:data`, `:shared`) on the current working tree.

### I-1 🔴 C0, C2, C3 are claimed done but the work is LOST — they must be REDONE

**What's wrong:** `docs/design/credit-card-advisor-roadmap.md` §8 has dated 2026-07-10 changelog rows for C0 (research restructure), C2 (seed-card verification, dataVersion→1.1.0), and C3 (HDFC Millennia, →1.2.0), and `docs/agent-roadmap.md` shows their checkboxes ticked. **None of the actual file changes exist anywhere:**

| Claimed | Actual state on disk |
|---|---|
| `research/*/*/NOTES.md` per template, archived MITC PDFs | `research/axis/ace/sources.md`, `research/hdfc/swiggy/sources.md`, `research/hdfc/regalia-gold/sources.md` — old filenames, placeholder content, **zero archived PDFs** |
| `data/cards.json` at v1.2.0, 4 cards incl. HDFC Millennia, corrected caps/exclusions | `data/cards.json` is **v1.0.0, 3 cards** (axis_ace, hdfc_swiggy, hdfc_regalia_gold), 7 accelerated rules, the uncorrected Phase-1 guesswork |
| Work parked on `data/*` branches | `git branch -a` shows no data branches; nothing in Desktop copy either |

The session that did that work ran in a worktree that was discarded; only its changelog rows/checkboxes survived (committed later by other sessions). F1 (`.claude/skills/extract-card/SKILL.md`) and C1 (`research/_TEMPLATE/NOTES.md`) DO exist and are fine.

**What to change:** see tasks R0-2 (correct the records) and R1-1/R1-2/R1-3 (redo the work — the lost changelog rows are a usable record of what the corrections were; they are reproduced in R1-2 below so nothing is re-discovered from scratch).

### I-2 🟠 ~500 lines of verified-green work sit UNCOMMITTED in the working tree

**What's wrong:** the following are modified/untracked on `main`'s working tree, verified green (all host tests pass, `assembleDebug` builds), but not committed — one `git checkout` away from repeating I-1:

- `domain/.../engine/DefaultRecommendationEngine.kt`, `EngineIO.kt`, `DefaultRecommendationEngineTest.kt` — **A1 route variants** (`directAlternative`, +4 tests)
- `shared/.../ui/recommend/RecommendScreen.kt`, `ui/home/HomeScreen.kt` — **E2 + E4** (hero redesign, bottom nav, a11y)
- `shared/.../ui/cards/CatalogScreen.kt`, `MyCardsScreen.kt` (untracked) — **E3**
- `docs/screenshots/` (untracked, 12 PNGs + capture.ps1 + README) — **E5**
- `docs/agent-roadmap.md`, `docs/design/credit-card-advisor-roadmap.md` — checkbox ticks + changelog rows for the above

**What to change:** commit it (task R0-1).

### I-3 🟠 The dataset is the product, and it is placeholder

3 cards, unverified rules, no citations. A card advisor covering 3 cards has no install-worthy value proposition. The roadmap's own target is 10–25 verified cards. **This is the schedule-critical path to launch** — it is human-gated (owner signs off every rule and dates `lastVerified`) and nothing in the B/D tracks shortens it. Start R1 immediately and run everything else in parallel.

### I-4 🟡 `minSdk = 33` excludes a huge share of Indian devices

`gradle/libs.versions.toml`: `android-minSdk = "33"` (Android 13+). India's installed base skews old; nothing in the stack (Compose MP, Room KMP, kotlinx-serialization, Koin) needs 33. Not in any backlog. Fix in R2-4.

### I-5 🟡 Release engineering not started

`androidApp/build.gradle.kts`: `isMinifyEnabled = false`, `versionCode 1`, no signing config, default launcher mipmaps, no release smoke test. Covered by B5 — but note the **Play Console closed-testing clock** (R4-2): if the Play account is a personal account created after Nov 2023, Google requires 12 testers opted-in for 14 continuous days before production access can even be requested. That is a ~3-week calendar dependency; start it the moment an installable signed build exists, not at the end.

### I-6 🟡 Shared-cap schema limitation is now a data-honesty problem

The lost C2/C3 analysis found real cards where ONE cap spans 4 rules (Axis ACE: ₹500 across billpay+Swiggy+Zomato+Ola) or 9 rules (Millennia: ₹1,000 across all 5% merchants). The schema models caps per-rule, so the app over-promises available headroom. For an app whose stated moat is honesty, decide this **before** the card count passes ~6 (task R1-4) — every extraction after that inherits the choice.

### I-7 ⚪ Minor / hygiene
- D2 (AGP DSL deprecations, expect/actual warning), D3 (CI job split) — real, not launch-blocking, do opportunistically.
- No in-app About/Disclaimer surface ("not financial advice", data freshness, privacy link) — task R2-5.
- D1 (iOS) requires macOS — irrelevant to the Play Store; stays blocked.

---

## Part 2 — The roadmap: phases R0–R4

```
R0 (today) ──► R1 data truth (LONG POLE, loops until ~10-12 cards) ──► launch dataset
        └────► R2 product completeness ──► R3 launch infra ──► R4 release
                                   (R1 and R2/R3 run in parallel; R4-2 closed-testing
                                    clock starts as soon as R2 + minimal R4-1 exist)
```

---

### Phase R0 — Stabilize the repo (one session, do FIRST)

#### R0-1 — Commit the working-tree work
`Touches: everything in I-2 · Model: sonnet`
**What's wrong:** verified work is uncommitted (I-2).
**What to do:** run the full test suite + `assembleDebug` once more; commit the I-2 file list to `main` as one commit per logical task if practical (A1 engine; E2–E4 UI; E5 screenshots+docs) or one combined commit if the diffs interleave — with proper messages per CLAUDE.md DoD.
**Verify:** `git status` clean; CI green on push.

#### R0-2 — Correct the false records
`Touches: docs/agent-roadmap.md, docs/design/credit-card-advisor-roadmap.md · Model: sonnet`
**What's wrong:** I-1 — C0/C2/C3 marked done but the work is lost.
**What to do:**
1. In `docs/agent-roadmap.md`: change C0, C2, C3 checkboxes back to `[ ]` and append to each Status line: *"2026-07-11 audit: this session's file changes were never committed and are lost — task must be redone; see docs/launch-directive.md I-1/R1."* Do NOT delete the old status text (it documents what was learned).
2. In the roadmap changelog §8: do NOT delete the C0/C2/C3 rows (they are the only record of the corrections). Add ONE new dated row stating the audit finding: those three rows describe work that never landed and is being redone per this directive.
3. Add the **data-branch-first rule** (top of this doc) to CLAUDE.md's Extraction invariant paragraph.
**Verify:** docs render; no checkbox for undone work remains ticked.

---

### Phase R1 — Data truth (the long pole; loops in parallel with R2/R3)

**Standing rules for all R1 tasks:** curation guide is law (Tier 1/2 sources only; forums never cited); owner-only `lastVerified`; every rule gets a NOTES.md row with citation + confidence; commit to `data/*` branch BEFORE docs; minor `dataVersion` bump per new card/merchant, patch per correction.

#### R1-1 — Redo C0: research restructure + priority bands
**What's wrong:** `research/*/*/sources.md` are off-template; `cards.json` priorities are off-band (curation guide §6: merchant 10–19, family 20–29, category 30–99, base 1000).
**Where to change:** rename the three `sources.md` → `NOTES.md`, restructure to `research/_TEMPLATE/NOTES.md`; in `data/cards.json` renumber: `axis_ace_billpay` 10→30, `axis_ace_swiggy` 20→10, `axis_ace_zomato` 21→11, `hdfc_swiggy_online` 20→30, `hdfc_regalia_gold_smartbuy_travel` 10→30, `hdfc_regalia_gold_myntra` 20→10 (`hdfc_swiggy_merchant` stays 10). `dataVersion` → 1.0.1.
**Verify:** dataset gate + full engine suite green (relative per-card order preserved ⇒ winners unchanged).

#### R1-2 — Redo C2: verify the 3 seed cards *(agent-drafts / HUMAN signs off)*
**What's wrong:** every shipped rule is uncited guesswork, and the lost C2 session found real errors. **Known corrections recovered from the lost session's changelog row — re-verify each against a freshly archived source, do not copy blindly:**
- **Axis ACE:** cap period `CALENDAR_MONTH`→`STATEMENT_CYCLE`; the ₹500 cap actually spans 4 categories (billpay + Swiggy + Zomato + **Ola** — a merchant to add); add JEWELLERY/INSURANCE/GOVERNMENT/EDUCATION exclusions; **remove** the unsourced `amazon_pay_gc` exclusion.
- **HDFC Swiggy:** `STATEMENT_CYCLE` on all 3 tiers; add a missing **₹500/statement cap on the 1% base** (currently modeled uncapped); add JEWELLERY/GOVERNMENT/EMI/GIFT_CARD exclusions.
- **HDFC Regalia Gold:** Myntra ₹2,500/month cap numerically confirmed (5,000 RP shared across 4 merchants); fix `effectiveRatePct` rounding per ADR-005 round-down (1.33→1.3, 6.65→6.6); add FUEL/EMI exclusions. `hdfc_regalia_gold_smartbuy_travel` was **unverifiable** (SmartBuy pages are JS-rendered) — leave flagged as an open question, do not guess.
**Where:** archive real MITC/T&C PDFs into `research/<issuer>/<card>/`; corrections into `data/cards.json`; citations into NOTES.md. Bump → 1.1.0 (Ola is a new merchant).
**Verify:** dataset gate + full suite green; owner reviews the branch diff and dates `lastVerified`.

#### R1-3 — Card expansion loop: C3 × N + A3 + A4, target 10–12 cards
**What's wrong:** I-3 — no install-worthy coverage.
**What to do:** one card per session via `/extract-card` (sources archived first — the skill refuses URLs). Priority order (owner may reorder; own-cards first per guide §7): HDFC Millennia (redo — the lost session's findings: 1 base rule 1%/₹1,000 CALENDAR_MONTH cap + 9 merchant 5% rules incl. new merchants Uber/Sony LIV/Tata CLiQ; Cult.fit skipped), then SBI Cashback, ICICI Amazon Pay, Axis Flipkart, HDFC Infinia, Axis Atlas, ICICI Coral/Rubyx, SBI SimplyCLICK, HDFC Diners Black. Fold in **A3** (≈10 merchants: Uber, Ola, Croma, Reliance Digital, Nykaa, Ajio, IRCTC, Cleartrip, Zepto, Dominos) and **A4** (≥5 cards with genuine `paymentRoute` rules) as the cards that carry them arrive.
**Verify per card:** dataset gate green; owner-approved `data/<card-id>` branch; minor bump.

#### R1-4 — NEW: shared-cap decision (decide before card #6)
**What's wrong:** I-6 — per-rule caps over-promise when one real cap spans many rules.
**What to do — owner decides between:**
- **(a) Schema v2:** optional `capGroupId` on rules; validator rule (all members of a group share identical cap amount/period) + failing-fixture test per ADR-006; engine change in `DefaultRecommendationEngine` cap math + Spec §7 tests; `schemaVersion` 2 + DTO changes (`DomainJson` is strict — DTO/validator/test move together); major `dataVersion` bump. Correct but touches engine semantics — Opus-class task.
- **(b) UI caveat only:** a NOTES-driven `sharedCapNote` string per rule surfaced through the existing `CaveatBanner`; no engine change; honest labelling instead of correct math.
Recommendation: (b) for launch, (a) post-launch — but the choice needs a Change-Protocol changelog row either way.

---

### Phase R2 — Product completeness (parallel with R1)

#### R2-1 — A2: routing result UI
**What's wrong:** the engine (A1, committed in R0-1) now emits `Recommendation.directAlternative`, but `RecommendScreen.kt` never renders it — users don't see the portal-vs-direct split.
**Where:** `shared/.../ui/recommend/RecommendScreen.kt` only (VM contract frozen). Route-bearing results already show `RouteChip`; add the alternative line ("or 1.3% paying directly") as a secondary/expandable line on the result tile.
**Verify:** emulator with an added Regalia Gold, Travel category → screenshot showing both variants.

#### R2-2 — B1 → B2 → B3 (in that order, as specced in agent-roadmap.md)
B1 app-prefs (Room v2 + auto-migration — migration test mandatory), B2 onboarding (3 pages: how ranking works / the cap-headroom assumption / data freshness; gated by `onboarding_completed` pref), B3 My Cards polish (reorder persistence, remove confirm, import-retry). No changes to their existing specs.

#### R2-3 — NEW: lower `minSdk` to 26
**What's wrong:** I-4.
**Where:** `gradle/libs.versions.toml` `android-minSdk = "26"`. Then: full test suite, `assembleDebug`, and a boot + full QA happy path on an **API 26 emulator image** (watch for: java.time usage is fine on 26+; check any `NumberFormat`/locale behavior in `Format.kt` renders ₹ grouping identically).
**Verify:** screenshot of the recommend flow on API 26.

#### R2-4 — NEW: About/Disclaimer screen
**What's wrong:** I-7 — no persistent legal/trust surface.
**Where:** `shared/.../ui/about/AboutScreen.kt` reachable from the `TopAppBar` info affordance (E4 put the Reward-DB version there — extend it). Content: what the app does/doesn't know (cap headroom, MCC best-effort), "not financial advice — verify with your bank", Reward DB version + last-verified dates, privacy-policy link (URL from R3-1), OSS licenses.
**Verify:** emulator screenshots both themes.

---

### Phase R3 — Launch infrastructure

#### R3-1 — B4: analytics + crash reporting + privacy policy (as specced), plus:
**NEW addition to B4's spec:** the privacy policy must end up **hosted at a public URL** (GitHub Pages off this repo is fine) — the Play data-safety form requires a live link, not a markdown file. The data-safety answers in R4-2 must match the actual event list in `docs/analytics-events.md` exactly (aggregate-only, no free text, no amounts).

#### R3-2 — D2 + D3 (build hygiene, CI job split) — as specced, any time.

#### R3-3 — B6: manual QA script — as specced, but **recompute all expected outputs from the dataset version current at the time** (the spec's 7.5%/2.5%/1.33% expectations predate R1's corrections — e.g. base-cap and rounding fixes change them).

---

### Phase R4 — Release engineering + Play process

#### R4-1 — B5: release build (as specced, emphases:)
- `isMinifyEnabled = true`: R8's usual casualty here is kotlinx-serialization — after minify, **test the dataset import flow specifically** on the emulator (fresh install → import → recommend).
- Real adaptive icon + themed splash (replace the default mipmaps in `androidApp/src/main/res/`).
- Signing via gitignored `keystore.properties` (documented owner-manual step); versionCode policy: monotonic integer, versionName tracks `dataVersion` major.minor at first.

#### R4-2 — NEW: Play Console process (owner-driven; start EARLY)
1. Create the Play Console app listing as soon as R2 lands — do not wait for R4-1 polish.
2. **If the account is personal (post-Nov-2023): upload the first signed AAB to closed testing immediately and recruit 12 testers — the 14-day continuous clock is the launch schedule's second-longest pole after R1.**
3. Store listing from `docs/screenshots/` (regenerate the suite after R1 lands — current PNGs faithfully show the v1.0.0 dataset); content rating questionnaire; data-safety form per R3-1.
4. Production: staged rollout 20% → 100% watching Crashlytics.

---

### Explicitly post-launch (do NOT pull forward without a Change-Protocol row)
F2 programmatic extractor (gated on curation volume), F3 change monitoring, D1 iOS (needs macOS), R1-4 option (a) if (b) was chosen, in-app dataset updates without app releases (v1 policy: a wrong rate ships as a patch release — acceptable, document it in About).

---

## Part 3 — Owner-only actions (agents must not do these)
1. Date `lastVerified` on every rule (R1-2, R1-3 — every card).
2. Approve every `data/*` branch diff before merge.
3. Decide R1-4 (shared caps) and the R1-3 card priority order.
4. Create/own the Play Console account, signing keystore, `google-services.json`, and recruit the 12 closed testers.

**Critical path:** R0 (a day) → R1 card loop (weeks, owner-gated) ∥ R2+R3 (agent-parallel) → R4-2 closed-testing clock (≥14 days, overlaps R1's tail) → production.
