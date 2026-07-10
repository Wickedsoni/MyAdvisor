# Data Curation Guide — v1.0

*The operations manual for the project's single biggest risk: the reward dataset. Last updated: 2026-07-10.*

**Who this is for:** you, during Phase 1 seeding — and future-you, every month after launch, forever. The roadmap is blunt about this: data maintenance is a permanent solo tax. This guide exists to make that tax small, repeatable, and auditable.

**The prime rule:** *A rule you can't source is a rule you don't ship.* Every number in `data/cards.json` must trace to an official bank document in the research trail. No exceptions — the app's entire competitive advantage is being right.

---

## 1. Source Hierarchy

Use sources in this order. A rule needs at least one **Tier 1 or Tier 2** source to ship.

| Tier | Source | Use for |
|---|---|---|
| 1 | Bank's official **MITC** (Most Important Terms & Conditions) and card T&C PDFs | The authoritative rates, caps, exclusions. Download and archive the PDF — banks silently replace these. |
| 2 | Bank's official card **product page** + official rewards-program pages (e.g. issuer's SmartBuy-style portal T&Cs) | Portal multipliers, category definitions, redemption context for valuations |
| 3 | Bank's official app/statements (your own cards) | Confirming real-world behavior of rules you personally can test |
| 4 | Reputable card-review sites, forums (TechnoFino etc.), Reddit | **Discovery only** — "this card apparently has a Swiggy rule" → then verify in Tier 1/2. NEVER a shipping source. |

**Hard rule:** Tier 4 can point you at a rule; it can never *be* the citation. If you can't find it in Tier 1/2, it doesn't go in the dataset — even if five forum threads swear it's true. Forums are full of expired offers and regional variants.

---

## 2. What Goes IN the Dataset

- **Permanent published reward rules**: base rate, category rates, merchant-specific rates — as written in current T&Cs.
- **Caps**: amount + period, exactly as published. If T&Cs say "capped at 500 points per statement cycle," model `STATEMENT_CYCLE`, don't approximate to calendar month.
- **Exclusions**: every "no rewards on / excluded categories" clause. These are usually a table or a footnote in the MITC — read the footnotes; that's where banks hide rent, wallet loads, gift cards, fuel, EMI, insurance, government, education, utilities, jewellery. Choose scope carefully: does the clause kill *all* rewards (`FULL`) or only the accelerated rate (`ACCELERATED_ONLY`)?
- **Portal-routing rules** (Phase 3 curation, schema-ready now): multipliers that apply only via an issuer portal, with the route instruction a user actually needs ("buy the voucher on SmartBuy, not Amazon directly").
- **Points cards**: earn rate as published + your conservative valuation (see §5).

## 3. What Stays OUT (decisions, not laziness)

| Excluded | Why | Where it's parked |
|---|---|---|
| Temporary/festive offers (Prime Day 10%, "extra 5% till July 20") | Maintenance treadmill; stale offer data is worse than none | `temporary_offers` table exists; curation parked (roadmap) |
| Welcome/joining bonuses, milestone benefits, fee waivers | Not a per-purchase decision input; milestone alerts are a parked feature | Parking lot |
| Lounge access, insurance, concierge perks | Not reward-rate data; belongs to the parked new-card-recommender | Parking lot |
| Rates sourced only from forums/videos | Prime rule | — |
| Devalued/legacy variants of a card no longer issued | Unless it's plausibly in a power user's wallet — judgment call, document it | Case-by-case, noted in research trail |
| Co-brand rules requiring a paired subscription state the app can't know (e.g. "extra 2% for Prime members") | App can't verify membership | Model the unconditional rate; note the conditional one in `research/` for a future "I have Prime" toggle |
| Exact MCC lists | Networks vary; users can't see MCCs anyway | `TransactionType` approximation (Spec §8) |

**Grey zone — "5% on Amazon via issuer's co-brand":** if the rule is unconditional for every cardholder, it's IN. If it depends on external state (membership tiers, salary accounts, spend milestones), model the guaranteed floor and document the conditional ceiling in research notes.

---

## 4. The Research Trail (`research/` folder)

One folder per card. Nothing ships without one.

```
research/
  hdfc/
    swiggy_card/
      NOTES.md          ← required (template below)
      mitc_2026-06.pdf  ← archived source PDFs (banks replace these silently)
      product_page_2026-07-09.png
  axis/
    ace/
      ...
data/
  cards.json
```

**NOTES.md template:**

```markdown
# HDFC Swiggy Card — research notes
last_verified: 2026-07-09
sources:
  - MITC PDF (archived: mitc_2026-06.pdf), retrieved 2026-07-09 from <url>
  - Product page screenshot, 2026-07-09

## Rules extracted
| Rule id | Claim | Source location | Confidence |
|---|---|---|---|
| hdfc_swiggy_merchant | 10% on Swiggy, cap ₹1500/cal-month | MITC p.3 §2.1 | High |
| hdfc_swiggy_online | 5% online, same cap pool? → NO, separate cap (MITC p.3 §2.2) | MITC p.3 | High |

## Exclusions extracted
Rent (FULL), wallet loads (FULL), gift cards (ACCELERATED_ONLY, base 1% still applies — MITC footnote 4)

## Judgment calls
- Cap period stated as "calendar month" explicitly → CALENDAR_MONTH
- ...

## Open questions / couldn't verify
- Whether Instamart counts as "Swiggy" — MITC ambiguous. Modeled conservatively as NOT included. Revisit.
```

The **Judgment calls** and **Open questions** sections are the most valuable part. When a user disputes a rate in month 6, this file is the difference between a 5-minute answer and re-doing the research.

---

## 5. Point Valuation Policy (ADR-005)

- **Always conservative:** use the *lowest common* redemption value a normal user actually gets (typically cashback/statement-credit conversion), not the best-case flight-portal value.
- Round *down* to one decimal in `effectiveRatePct`.
- Every valuation gets a `valuationNote` in the dataset AND a source note in NOTES.md ("statement credit at ₹0.30/RP per redemption page, archived").
- If a bank devalues points: that's a data update with a version bump and a fresh `lastVerified`, same as any rate change.
- Never average across redemption routes. Conservative floor only — the app under-promising beats over-promising, always.

---

## 6. Processing Workflow (per card)

```
Pick card (coverage-driven, §7)
 → Gather Tier 1/2 sources, archive them into research/<issuer>/<card>/
 → Extract rules, caps, exclusions into NOTES.md (with source locations)
 → Translate NOTES.md → entries in data/cards.json (rule ids, priorities, scopes)
 → Run validator locally (catches dangling refs, overlap ambiguity, missing fields)
 → Self-review pass: read the JSON back against NOTES.md line by line
 → Commit research/ + data change together, bump dataVersion, set lastVerified
 → CI runs validator + engine tests
```

**Priority assignment convention:** merchant-specific rules 10-19, family rules 20-29, category rules 30-99, base rule always 1000. Leaves gaps for later insertions without renumbering.

**Budget honestly:** expect **2-4 hours per card** for first-time curation (MITC PDFs are hostile documents), less for simple cashback cards, more for points cards with portal rules. The Phase 1 estimate (~3-4 weeks) already assumes this — if a card takes 8 hours, that's a signal its rules may be too conditional to model honestly; consider dropping it and noting why.

---

## 7. Which Cards, In What Order

Coverage-driven, per the roadmap ("~80% of power users' wallets"), not a fixed list:

1. **Start with your own cards** — you can verify behavior against real statements (Tier 3), and they make Phase 2's "3 real scenarios" test honest.
2. Then the highest-circulation power-user cards per major issuer (HDFC, SBI, ICICI, Axis, Amex, ...), prioritizing cards that create *interesting decisions* — a card nobody would ever choose adds curation cost and zero recommendation value.
3. **The Phase 1 gate cards come first regardless:** one capped card, one exclusion-heavy card, one overlapping-rules card (Spec §7) — these validate the schema before you sink weeks into volume curation.

**[ASSUMPTION — needs validation]:** which specific cards constitute "80% of power-user wallets" is a research question. Answer it during Phase 1 with Tier 4 sources (forum polls, card-community consensus) — this is the one place Tier 4 is decision-grade, because it's a popularity question, not a rates question.

---

## 8. Maintenance Cadence (post-launch)

- **Monthly sweep:** re-check Tier 1/2 sources for the top ~10 owned cards (analytics will tell you which, Phase 4). Update `lastVerified` even when nothing changed — freshness display depends on it.
- **Event-driven:** bank announces a devaluation/revamp (these hit Tier 4 forums loudly within hours — this is Tier 4's real job: an early-warning system) → verify in Tier 1/2 → data release.
- **Every data release:** version bump + changelog line in the dataset commit. Never edit data without touching `dataVersion`.
- **Honesty valve:** if a card hasn't been re-verified in >90 days, that's visible to users via `lastVerified` by design. Don't fight the transparency — it's the trust mechanism doing its job.

---

## 9. Red Flags — Stop and Re-check

- A rate that looks too good (10%+ uncapped) — you've almost certainly missed a cap, a portal condition, or an expiry.
- T&C wording "up to X%" — that's a ceiling across tiers/conditions, not a rate. Find the actual table.
- A rule sourced from a page without a retrievable date — archive first, extract second.
- Two sources disagree (product page says 5%, MITC says 3%) — MITC wins; note the conflict in NOTES.md; consider that the product page may be showing a temporary offer (§3).
- You're inferring a rule from how "it should work" — stop. Source it or drop it.
