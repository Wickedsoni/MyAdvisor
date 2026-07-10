# Axis ACE — research notes

**Status: ⚠️ UNVERIFIED PLACEHOLDER — must be verified against published T&Cs before launch (task R1-2).**

last_verified: —
sources:
  - NONE ARCHIVED YET — seeded from the rule-engine spec's worked examples to exercise the Phase 1 pipeline (exclusion-heavy card for the "3 tricky cards" gate). R1-2 must archive the real MITC/T&C PDF here before any rule can be cited.

## Rules extracted
| Rule id | Claim | Source location | Confidence |
|---|---|---|---|
| `axis_ace_base` | 1.5% unlimited cashback | TODO (unarchived) | — |
| `axis_ace_billpay` | 5% bill pay via Google Pay, ₹500/mo cap | TODO (unarchived) | — |
| `axis_ace_swiggy` | 4% Swiggy, ₹500/mo cap | TODO (unarchived) | — |
| `axis_ace_zomato` | 4% Zomato, ₹500/mo cap | TODO (unarchived) | — |

## Exclusions extracted
- Wallet loads, rent, fuel, EMI — FULL (unverified placeholder)
- Amazon Pay gift cards — ACCELERATED_ONLY (unverified placeholder; the lost C2 pass found NO gift-card clause in the real T&C — re-check and likely remove in R1-2)

## Judgment calls
- The real card shares one ₹500/mo cap across the accelerated categories; the v1 schema has per-rule caps, so it's modeled per-merchant. This overstates headroom when several are used heavily — acceptable under the full-headroom assumption (Spec D4); shared-cap decision tracked as launch-directive R1-4. (The lost C2 pass found the shared cap actually spans FOUR targets: bill pay + Swiggy + Zomato + Ola.)

## Open questions / couldn't verify
- Cap period: the lost C2 pass read the source as "per statement" (`STATEMENT_CYCLE`), not `CALENDAR_MONTH` as currently modeled — re-verify against a freshly archived T&C in R1-2.
- Exclusion set: lost C2 pass added JEWELLERY / INSURANCE / GOVERNMENT / EDUCATION from the MCC table — re-verify in R1-2.
