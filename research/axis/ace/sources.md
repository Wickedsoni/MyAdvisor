# Axis ACE — Research Trail

**Status: ⚠️ UNVERIFIED PLACEHOLDER — must be verified against published T&Cs before launch.**

Seeded from the rule-engine spec's worked examples to exercise the pipeline
(exclusion-heavy card for the Phase 1 "3 tricky cards" gate). Every rule
below needs a source link + snapshot before `lastVerified` can be trusted.

| Rule id | Claim | Source (T&C link) | Snapshot | Verified on |
|---|---|---|---|---|
| axis_ace_base | 1.5% unlimited cashback | TODO | TODO | — |
| axis_ace_billpay | 5% bill pay via Google Pay, ₹500/mo cap | TODO | TODO | — |
| axis_ace_swiggy | 4% Swiggy, ₹500/mo cap | TODO | TODO | — |
| axis_ace_zomato | 4% Zomato, ₹500/mo cap | TODO | TODO | — |
| exclusions | No rewards: wallet loads, rent, fuel, EMI; gift cards base-only | TODO | TODO | — |

**Modeling notes:**
- The real card shares one ₹500/mo cap across Swiggy+Zomato; the v1 schema
  has per-rule caps, so it's modeled per-merchant. This overstates headroom
  when both are used heavily — acceptable under the full-headroom assumption
  (Spec D4), revisit if shared caps become common.
