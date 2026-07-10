---
name: extract-card
description: Draft MyAdvisor reward-rule data for one credit card from its archived MITC/T&C sources. Use when the user says "extract card", "/extract-card", "draft rules for <card>", or points at a research/<issuer>/<card>/ folder with archived bank PDFs/screenshots and wants cards.json entries + NOTES.md. Produces a reviewable draft on a data/<issuerId>_<cardSlug> branch — NEVER on main.
---

# /extract-card — MITC → reviewable dataset draft

You are running MyAdvisor's knowledge-extraction pipeline for **one** card. The
product's entire value is being *right* about reward rules, so this skill is
deliberately conservative and gated. Read this whole file before acting.

**The invariant (CLAUDE.md, non-negotiable):**
`Extraction → Validation → Human Approval`. You do the first two. The human does
the third. **Your output is a draft on a branch, never a merge to `main`.** You
may not mark anything "verified" — only the owner dates the Verified/`lastVerified`
field (task C2).

**The prime rule (curation guide §1):** *A rule you can't source in a Tier 1/2
document is a rule you don't ship.* Forums, blogs, YouTube, and "how it should
work" are never citations. If it isn't in the archived MITC or official product
page in front of you, it does not go in the draft — flag it as an open question
instead.

---

## Input contract

Required from the user (ask if missing):

- `issuerId` — existing issuer slug (e.g. `hdfc`, `axis`) or a new one.
- `cardSlug` — the card's slug; the card id will be `<issuerId>_<cardSlug>`.
- **Archived source files already sitting in `research/<issuerId>/<cardSlug>/`** —
  MITC/T&C PDFs and/or product-page screenshots, with retrievable dates.

**Refuse to proceed from URLs alone (curation guide §1).** If the user gives you
only a link, stop and tell them: archive the source into the research folder
first (download the PDF / screenshot the page with a dated filename), *then* run
the skill. You extract from archived bytes, not live pages — banks silently
replace these documents and an un-archived source is unciteable.

If the folder is empty or missing, stop and say so.

---

## Step 1 — Read the sources, understand the card

Read every archived file in `research/<issuerId>/<cardSlug>/`. For PDFs use the
Read tool's `pages` parameter. Build a mental model of:

- **Base earn rate** — the unconditional rate every eligible spend earns.
- **Accelerated rules** — merchant-specific, merchant-family, and category rates.
- **Caps** — amount **and** exact period. Model the period *as written*, do not
  approximate ("500 points per statement cycle" → `STATEMENT_CYCLE`, not
  `CALENDAR_MONTH`).
- **Exclusions** — every "no rewards on…" clause. **These hide in footnotes** —
  read them. Decide scope per clause: does it kill *all* rewards (`FULL`) or only
  the accelerated rate while base still earns (`ACCELERATED_ONLY`)?
- **Portal-routing rules** — multipliers that only apply via an issuer portal
  (SmartBuy-style). Capture the route instruction a user actually needs.
- **Points cards** — earn rate as published **plus** a conservative valuation
  (see §5 below).

---

## Step 2 — Point valuation for points cards (curation guide §5 / ADR-005)

If the card earns points (not direct cashback):

- Use the **lowest common** redemption value a normal user actually gets
  (typically cashback / statement credit), **never** the best-case flight-portal
  value.
- Compute `effectiveRatePct` from that conservative value and **round *down*** to
  one decimal.
- Every points reward carries a `valuationNote` in the JSON *and* a source note in
  NOTES.md naming the redemption route ("statement credit at ₹0.30/RP per the
  archived redemption page").
- Never average across redemption routes. Conservative floor only.

---

## Step 3 — Guardrails: stop and re-check (curation guide §9, apply verbatim)

- A rate that looks too good (**10%+ uncapped**) — you've almost certainly missed
  a cap, a portal condition, or an expiry. Find it or flag it.
- T&C wording **"up to X%"** is a *ceiling* across tiers/conditions, **not a
  rate**. Find the actual rate table; do not ship "up to" as if it were the rate.
- A rule sourced from a page **without a retrievable date** — archive first,
  extract second (you should already have refused at the input gate).
- **Two sources disagree** (product page 5%, MITC 3%) — **MITC wins.** Record the
  conflict in NOTES.md; consider that the product page may be showing a temporary
  offer (which is out of scope — curation guide §3).
- You're **inferring** a rule from how "it should work" — stop. Source it or drop
  it to open questions.
- **Temporary/festive offers, welcome bonuses, milestone benefits, lounge/perks
  are OUT** (curation guide §3). Model permanent published per-purchase reward
  rules only.
- **Conditional-on-external-state rules** (e.g. "extra 2% for Prime members") —
  model the *guaranteed floor* every cardholder gets; note the conditional
  ceiling in NOTES.md open questions. The app can't verify membership.

Assign a **confidence** to every drafted rule:

- **HIGH** — explicit in a Tier 1 MITC rate/cap table.
- **MED** — in a Tier 2 product page, or requires light interpretation.
- **LOW** — ambiguous wording, footnote inference, or single weak source.
  **Every LOW row must be surfaced in the review summary for human attention.**

The validator catches structural problems (dangling refs, bad enums, overlap
ambiguity). It **cannot** catch a hallucinated business rule or a misread
footnote — that is exactly what the human review step is for. Draft accordingly:
when unsure, under-claim and flag.

---

## Step 4 — Write `research/<issuerId>/<cardSlug>/NOTES.md`

Start from `research/_TEMPLATE/NOTES.md`. Fill the curation-guide §4 template.
Delete the template's trailing workflow-checklist / red-flags sections from the
final file (they're a working aid, not archival content). Every rules-extracted
row **must** carry a source location — file name, page, section — and a
confidence. The **Judgment calls** and **Open questions** sections are the most
valuable part of the file; be specific there.

---

## Step 5 — Draft `data/cards.json` entries

`data/cards.json` at the `:data` module root is the single canonical dataset —
edit it in place, never create a second copy. The parser (`DomainJson`) is
**strict**: unknown keys fail. Match the existing entries exactly.

### Naming & id conventions (CLAUDE.md)

- Card id: `<issuerId>_<cardSlug>` (e.g. `hdfc_swiggy`).
- Rule id: `<cardId>_<ruleSlug>` (e.g. `hdfc_swiggy_merchant`).
- Ids are snake_case slugs.

### Priority bands (curation guide §6) — leave gaps for later inserts

| Rule kind | Band | Notes |
|---|---|---|
| merchant-specific | **10–19** | most specific wins |
| merchant-family | **20–29** | |
| category | **30–99** | |
| base rule | **1000** | always, exactly |

Two rules that can match the same transaction **must not share a priority** — the
validator rejects that. Lower number = higher precedence.

### Catalog references must exist

Rules and exclusions reference `merchants[].id`, `merchantFamilies[].id`,
`categories[].id`, and `issuers[].id`. If your card needs a merchant/category
that isn't in the catalog yet, **add it to the top-level catalog arrays** in the
same edit (merchant needs `id`, `name`, `categoryId`, optional `familyId`).
Missing refs fail the validator.

### JSON shapes (classDiscriminator key is `"type"`)

Card object:

```json
{
  "id": "<issuerId>_<cardSlug>",
  "issuerId": "<issuerId>",
  "name": "<Human Card Name>",
  "lastVerified": "<ISO date — leave the existing/generated date; the OWNER updates this on sign-off>",
  "researchRef": "research/<issuerId>/<cardSlug>/",
  "baseRule": {
    "id": "<cardId>_base",
    "priority": 1000,
    "condition": { "type": "always" },
    "reward": { "effectiveRatePct": 1.5, "earnDescription": "1.5% unlimited cashback" }
  },
  "rules": [ /* accelerated rules */ ],
  "exclusions": [ /* see below */ ]
}
```

Condition variants:

```json
{ "type": "always" }
{ "type": "merchant", "merchantId": "swiggy" }
{ "type": "merchantFamily", "familyId": "amazon" }
{ "type": "category", "categoryId": "dining" }
{ "type": "allOf", "conditions": [ { "type": "category", "categoryId": "travel" }, { "type": "merchant", "merchantId": "makemytrip" } ] }
```

Accelerated rule with cap + optional portal route + optional points valuation:

```json
{
  "id": "<cardId>_<ruleSlug>",
  "priority": 10,
  "condition": { "type": "merchant", "merchantId": "myntra" },
  "reward": {
    "effectiveRatePct": 6.65,
    "earnDescription": "5X Reward Points on Myntra",
    "valuationNote": "1 RP valued at ₹0.50 (conservative)"
  },
  "cap": { "amountInr": 2500, "period": "CALENDAR_MONTH" },
  "paymentRoute": { "id": "smartbuy", "name": "HDFC SmartBuy", "instruction": "Book through the SmartBuy portal to earn 5X points" }
}
```

`validity` is optional: `{ "from": "2026-01-01", "until": "2026-12-31" }` (ISO
dates; use only for genuinely time-boxed permanent rules, not temporary offers).

Exclusion (target discriminator key is also `"type"`):

```json
{ "target": { "type": "transactionType", "value": "WALLET_LOAD" }, "scope": "FULL" }
{ "target": { "type": "merchant", "merchantId": "amazon_pay_gc" }, "scope": "ACCELERATED_ONLY" }
{ "target": { "type": "category", "categoryId": "utilities" }, "scope": "FULL" }
```

### Allowed enum values (validator will reject anything else)

- `cap.period` — `PER_TRANSACTION`, `CALENDAR_MONTH`, `STATEMENT_CYCLE`,
  `QUARTER`, `YEAR`.
- exclusion `scope` — `FULL`, `ACCELERATED_ONLY`.
- exclusion `transactionType` `value` — `RENT`, `WALLET_LOAD`, `GIFT_CARD`,
  `FUEL`, `EMI`, `INSURANCE`, `GOVERNMENT`, `EDUCATION`, `UTILITY`, `JEWELLERY`.
- `effectiveRatePct` — sanity-bounded to `(0, 50]`. A value outside that is a
  red flag, not a valid rate.

### Self-review pass (curation guide §6)

Read the JSON back against NOTES.md **line by line**. Every rate, cap, and
exclusion in the JSON must trace to a NOTES.md row with a source location.

---

## Step 6 — Version bump + run the gates

- Bump `dataVersion` in `data/cards.json` per CLAUDE.md semver: **minor** for a
  new card (`1.0.1` → `1.1.0`), **patch** for a rate/cap fix on an existing card.
  A dataset change **without** a version bump is invisible on-device (the
  importer's monotonic check skips it).
- Run the gates from the repo root (Windows/PowerShell):

```powershell
# Dataset gate (validator + research-folder existence)
.\gradlew.bat :data:testAndroidHostTest --tests "*BundledDatasetValidationTest*"

# Engine + full unit suite (determinism must stay green)
.\gradlew.bat :domain:testAndroidHostTest :data:testAndroidHostTest :shared:testAndroidHostTest
```

If the validator reports errors, **fix the draft** — do not weaken the validator.
All tests must be green before you commit.

---

## Step 7 — Commit to a data branch (NEVER main) + review summary

1. Create/switch to branch **`data/<issuerId>_<cardSlug>`**. Never commit this
   draft to `main` — drafts reach `main` only through the owner's reviewed merge.
2. Commit `research/<issuerId>/<cardSlug>/` (NOTES.md + archived sources) **and**
   the `data/cards.json` change **together**. Commit message: imperative summary +
   body; end with `Co-Authored-By: Claude <model> <noreply@anthropic.com>`.
3. Also add a dated Changelog row to
   `docs/design/credit-card-advisor-roadmap.md` (§8) describing the drafted card,
   and (for a repeatable C3 run) tick nothing automatically — the owner decides
   when the card is done.
4. **Print a review summary** for the human approver containing:
   - Card id and the branch name.
   - Every rule drafted: id, condition, rate, cap, priority, confidence.
   - **Every LOW and MED confidence row, called out explicitly** — these need the
     closest human scrutiny.
   - All open questions / couldn't-verify items from NOTES.md.
   - Any source conflicts you resolved (and how).
   - An explicit reminder: *"`lastVerified` is unchanged — only the owner dates
     it after signing off (task C2)."*

Do **not** open a PR or merge. Stop at the branch + summary and hand back to the
human.

---

## Quick reference — the pipeline in one line

`archived Tier 1/2 sources → read → extract (conservative, cited, confidence-tagged) → NOTES.md + cards.json draft → version bump → validator + engine gates green → data/<id> branch + review summary → HUMAN approves & merges.`
