# HDFC Swiggy Card — research notes

**Status: ⚠️ UNVERIFIED PLACEHOLDER — must be verified against published T&Cs before launch (task R1-2).**

last_verified: —
sources:
  - NONE ARCHIVED YET — seeded from the rule-engine spec's worked examples (capped + overlapping-rules card for the Phase 1 "3 tricky cards" gate). R1-2 must archive the real MITC/T&C PDF here before any rule can be cited.

## Rules extracted
| Rule id | Claim | Source location | Confidence |
|---|---|---|---|
| `hdfc_swiggy_base` | 1% cashback (Swiggy Money) | TODO (unarchived) | — |
| `hdfc_swiggy_merchant` | 10% on Swiggy, ₹1,500/mo cap | TODO (unarchived) | — |
| `hdfc_swiggy_online` | 5% online spends, ₹1,500/mo cap | TODO (unarchived) | — |

## Exclusions extracted
- Fuel, rent, wallet loads — FULL (unverified placeholder)

## Judgment calls
- Cashback credits as Swiggy Money, not statement credit — noted in `earnDescription`; a redemption-value discount is a points-advisor concern (parked).

## Open questions / couldn't verify
- Cap period: the lost C2 pass read all 3 tiers as `STATEMENT_CYCLE`, not `CALENDAR_MONTH` as currently modeled — re-verify in R1-2.
- The lost C2 pass found a ₹500/statement cap on the 1% BASE rate (currently modeled uncapped) — re-verify in R1-2; this changes recommendation values materially at high amounts.
- Exclusion set: lost C2 pass added JEWELLERY / GOVERNMENT / EMI / GIFT_CARD — re-verify in R1-2.
