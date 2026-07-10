# HDFC Regalia Gold — research notes

**Status: ⚠️ UNVERIFIED PLACEHOLDER — must be verified against published T&Cs before launch (task R1-2).**

last_verified: —
sources:
  - NONE ARCHIVED YET — seeded as the points + portal-routing card for the Phase 1 "3 tricky cards" gate (exercises D1 effective-value normalization and D6 paymentRoute). R1-2 must archive the real MITC/T&C PDF here before any rule can be cited.

## Rules extracted
| Rule id | Claim | Source location | Confidence |
|---|---|---|---|
| `hdfc_regalia_gold_base` | 4 RP per ₹150 | TODO (unarchived) | — |
| `hdfc_regalia_gold_smartbuy_travel` | 5X RP on SmartBuy travel | TODO (unarchived) | — |
| `hdfc_regalia_gold_myntra` | 5X RP on Myntra | TODO (unarchived) | — |

## Exclusions extracted
- Wallet loads, rent, government — FULL (unverified placeholder)

## Judgment calls
- Point valuation (D1/ADR-005): 1 RP = ₹0.50 chosen conservatively; SmartBuy flight redemptions typically ~₹0.50/RP, cashback redemption is lower. Changing this valuation is a data change + version bump.
- `effectiveRatePct` values 1.33 / 6.65 violate ADR-005's round-DOWN-to-one-decimal rule (should be 1.3 / 6.6) — fix belongs to R1-2 (a rate change is a data correction, not restructuring).

## Open questions / couldn't verify
- `hdfc_regalia_gold_smartbuy_travel` (5X on SmartBuy travel): the lost C2 pass could NOT verify this — SmartBuy's pages are JS-rendered and returned no usable content to agent tooling. Needs a human-captured screenshot/PDF archived here.
- Myntra rule: lost C2 pass confirmed the ₹2,500/month figure but found it's actually a 5,000-RP/month cap SHARED across 4 merchants (per-rule modeling overstates headroom — see launch-directive R1-4); re-verify in R1-2.
- Exclusion set: lost C2 pass added FUEL / EMI — re-verify in R1-2.
