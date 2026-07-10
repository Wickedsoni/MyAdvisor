# <Card Name> — research notes

**Status: ⚠️ UNVERIFIED PLACEHOLDER — must be verified against published T&Cs before launch.**

last_verified: —
sources:
  - <Tier 1: MITC PDF, archived as `<filename>.pdf` in this folder>, retrieved <date> from <url>
  - <Tier 2: product page screenshot/archive, retrieved <date> from <url>>

## Rules extracted
| Rule id | Claim | Source location | Confidence |
|---|---|---|---|
| `<card>_base` | | | |
| `<card>_<merchant>` | | | |

## Exclusions extracted
<Every "no rewards on / excluded categories" clause — read the footnotes. Scope each as FULL or ACCELERATED_ONLY.>

## Judgment calls
- <Any modeling approximation you made and why — cap-sharing, TransactionType mapping, conditional-rule floor, etc.>

## Open questions / couldn't verify
- <Anything ambiguous in the source that a future re-read should revisit.>

---

## Workflow checklist (curation guide §6 — delete before committing NOTES.md)

- [ ] Pick card (coverage-driven per guide §7)
- [ ] Gather Tier 1/2 sources, archive them into this folder (PDFs/screenshots, dated filenames)
- [ ] Extract rules, caps, exclusions into the tables above (with source locations)
- [ ] Translate this file → entries in `data/cards.json` (rule ids, priorities per §6 bands: merchant 10-19, family 20-29, category 30-99, base 1000)
- [ ] Run the validator locally (catches dangling refs, overlap ambiguity, missing fields)
- [ ] Self-review pass: read the JSON back against this file line by line
- [ ] Commit `research/` + `data/cards.json` together, bump `dataVersion`, set `lastVerified`
- [ ] Confirm CI (validator + engine tests) is green

## Red flags — stop and re-check (curation guide §9)

- A rate that looks too good (10%+ uncapped) — you've almost certainly missed a cap, a portal condition, or an expiry.
- T&C wording "up to X%" — that's a ceiling across tiers/conditions, not a rate. Find the actual table.
- A rule sourced from a page without a retrievable date — archive first, extract second.
- Two sources disagree (product page says 5%, MITC says 3%) — MITC wins; note the conflict above; consider that the product page may be showing a temporary offer.
- You're inferring a rule from how "it should work" — stop. Source it or drop it.
