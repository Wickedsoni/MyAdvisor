# Rule Engine Specification — v1.0

*Phase 1 design artifact. Companion to the project roadmap (SSOT). Last updated: 2026-07-10.*

**Purpose:** Define the data model, JSON format, Kotlin domain model, and evaluation algorithm for the recommendation engine. This is the contract that the "3 tricky cards" test in Phase 1's Definition of Done validates. If a real card's T&Cs can't be expressed in this spec, the spec changes — in week 3, not month 6.

---

## 1. Design Decisions (locked for v1)

| # | Decision | Rationale |
|---|---|---|
| D1 | All rewards normalize to an **effective value %**, curated at research time | Points cards (e.g. "4 RP per ₹150") and cashback cards must rank on one axis. Point valuations are baked in conservatively during curation, with the valuation shown transparently. Runtime point-valuation is the parked points-advisor problem. |
| D2 | Cap schema supports period **types** (per-transaction / calendar month / statement cycle / quarter / year); **no user cycle-date input in v1** | Reset dates only matter with spend tracking, which is parked. Schema keeps the door open; UI collects nothing it can't use. |
| D3 | **Exclusions evaluate before positive rules** and have a scope: `FULL` (card earns nothing) or `ACCELERATED_ONLY` (falls back to base rule) | Matches real T&Cs: "no rewards on fuel" vs. "5% doesn't apply to gift cards, base 1% does." |
| D4 | Engine assumes **full cap headroom**; every capped recommendation carries the caveat | Prior spend is unknowable in v1. Honesty over fake precision. |
| D5 | Evaluation is **fully deterministic**: explicit priority, explicit tie-breakers, stable ordering | Same inputs → same output, always. Testable, explainable, trustworthy. |
| D6 | Portal-routing (`paymentRoute`) is **in the schema from day one**, surfaced in UI from Phase 3 | Phase 3 must be a data + UI phase, not a migration. |

---

## 2. Entity Model

```
Issuer 1──* Card
Card   1──* RewardRule        (positive earning rules, incl. base rule)
Card   1──* Exclusion
Card   1──* TemporaryOffer    (separate table; schema only in v1, no curation)

MerchantFamily 1──* Merchant
Merchant       *──1 Category   (default category mapping)
```

### 2.1 Issuer
No hardcoded bank strings anywhere. `{ id, name }` — e.g. `{ "hdfc", "HDFC Bank" }`.

### 2.2 Card
Belongs to an issuer. Carries `lastVerified` (date the curator last confirmed its rules against published T&Cs) and exactly one **base rule** (the catch-all earn rate when nothing else matches).

### 2.3 RewardRule
The atom of the engine:

```
condition  →  reward  →  cap?  →  validity?  →  paymentRoute?
```

- **condition** — what transaction this rule applies to (merchant / merchant family / category / always / all-of combination)
- **reward** — effective value % + human-readable earn description + valuation note
- **cap** — optional; max reward value (₹) per period type
- **validity** — optional date window (permanent rules omit it; this is NOT the temporary-offer mechanism — offers are a separate table)
- **paymentRoute** — optional; rule applies only when paid via a route (e.g. SmartBuy). Dormant until Phase 3 UI.
- **priority** — integer, lower wins. Resolves overlapping matches deterministically.

### 2.4 Exclusion
Negative rules, evaluated first:

- **target**: a merchant, a category, or a transaction type (`RENT`, `WALLET_LOAD`, `GIFT_CARD`, `FUEL`, `EMI`, `INSURANCE`, `GOVERNMENT`, `EDUCATION`, `UTILITY`, `JEWELLERY`)
- **scope**: `FULL` (this card earns nothing on the transaction) or `ACCELERATED_ONLY` (only the base rule applies)

### 2.5 Merchant, MerchantFamily, Category
Merchants belong to families (`amazon_fresh` → family `amazon`) and map to a default category (`amazon_fresh` → `groceries`). Precedence at resolution time:

```
merchant-specific rule  >  merchant-family rule  >  category rule  >  base rule
```

Encoded in data + the algorithm — never as code special-cases.

### 2.6 TemporaryOffer
Separate table mirroring RewardRule plus mandatory start/end dates and a source link. **Exists in schema only in v1; no offer data is curated** (parked — see roadmap).

---

## 3. JSON Data Format

One dataset file, versioned, produced by the research process, consumed by the validator + importer. The importer never writes Room by hand.

```json
{
  "schemaVersion": 1,
  "dataVersion": "1.0.0",
  "generatedAt": "2026-07-10",

  "issuers": [
    { "id": "hdfc", "name": "HDFC Bank" },
    { "id": "axis", "name": "Axis Bank" }
  ],

  "categories": [
    { "id": "dining", "name": "Dining" },
    { "id": "groceries", "name": "Groceries" },
    { "id": "online_shopping", "name": "Online Shopping" }
  ],

  "merchantFamilies": [
    { "id": "amazon", "name": "Amazon" }
  ],

  "merchants": [
    { "id": "amazon_in",    "familyId": "amazon", "name": "Amazon.in",        "categoryId": "online_shopping" },
    { "id": "amazon_fresh", "familyId": "amazon", "name": "Amazon Fresh",     "categoryId": "groceries" },
    { "id": "amazon_pay_gc","familyId": "amazon", "name": "Amazon Pay Gift Cards", "categoryId": "online_shopping" },
    { "id": "swiggy",       "familyId": null,     "name": "Swiggy",           "categoryId": "dining" }
  ],

  "cards": [
    {
      "id": "axis_ace",
      "issuerId": "axis",
      "name": "Axis ACE",
      "lastVerified": "2026-07-08",
      "researchRef": "research/axis/ace/",
      "baseRule": {
        "id": "axis_ace_base",
        "priority": 1000,
        "condition": { "type": "always" },
        "reward": { "effectiveRatePct": 1.5, "earnDescription": "1.5% cashback", "valuationNote": null }
      },
      "rules": [
        {
          "id": "axis_ace_billpay",
          "priority": 10,
          "condition": { "type": "category", "categoryId": "utilities" },
          "reward": { "effectiveRatePct": 5.0, "earnDescription": "5% cashback via Google Pay", "valuationNote": null },
          "cap": { "amountInr": 500, "period": "CALENDAR_MONTH" },
          "paymentRoute": { "id": "google_pay", "name": "Google Pay", "instruction": "Pay the bill through Google Pay" }
        }
      ],
      "exclusions": [
        { "target": { "type": "transactionType", "value": "WALLET_LOAD" }, "scope": "FULL" },
        { "target": { "type": "transactionType", "value": "RENT" },        "scope": "FULL" },
        { "target": { "type": "merchant", "merchantId": "amazon_pay_gc" }, "scope": "ACCELERATED_ONLY" }
      ]
    },
    {
      "id": "hdfc_swiggy",
      "issuerId": "hdfc",
      "name": "HDFC Swiggy Card",
      "lastVerified": "2026-07-09",
      "researchRef": "research/hdfc/swiggy/",
      "baseRule": {
        "id": "hdfc_swiggy_base",
        "priority": 1000,
        "condition": { "type": "always" },
        "reward": { "effectiveRatePct": 1.0, "earnDescription": "1% cashback", "valuationNote": null }
      },
      "rules": [
        {
          "id": "hdfc_swiggy_merchant",
          "priority": 10,
          "condition": { "type": "merchant", "merchantId": "swiggy" },
          "reward": { "effectiveRatePct": 10.0, "earnDescription": "10% cashback on Swiggy", "valuationNote": null },
          "cap": { "amountInr": 1500, "period": "CALENDAR_MONTH" }
        },
        {
          "id": "hdfc_swiggy_online",
          "priority": 20,
          "condition": { "type": "category", "categoryId": "online_shopping" },
          "reward": { "effectiveRatePct": 5.0, "earnDescription": "5% on online spends", "valuationNote": null },
          "cap": { "amountInr": 1500, "period": "CALENDAR_MONTH" }
        }
      ],
      "exclusions": []
    }
  ]
}
```

Points-card reward example (D1 in action):

```json
"reward": {
  "effectiveRatePct": 3.3,
  "earnDescription": "10 RP per ₹150 on SmartBuy",
  "valuationNote": "1 RP valued at ₹0.50 (conservative; flight redemptions can reach ₹1.0)"
}
```

### 3.1 Validator rules (pipeline gate)
Reject the dataset with a useful error if any of:
- Duplicate ids anywhere; dangling references (issuerId, merchantId, familyId, categoryId)
- Card without exactly one `baseRule` with condition `always`
- `effectiveRatePct` ≤ 0 or > 50 (sanity bound); cap `amountInr` ≤ 0
- Two rules on the same card with equal `priority` whose conditions can co-match (static overlap check on merchant/family/category)
- Missing `lastVerified` or `researchRef`
- `dataVersion` not greater than the currently shipped version

---

## 4. Kotlin Domain Model

```kotlin
// ── Identity ────────────────────────────────────────────────
data class Issuer(val id: String, val name: String)

data class Category(val id: String, val name: String)

data class MerchantFamily(val id: String, val name: String)

data class Merchant(
    val id: String,
    val familyId: String?,
    val name: String,
    val categoryId: String,
)

// ── Card & rules ────────────────────────────────────────────
data class Card(
    val id: String,
    val issuerId: String,
    val name: String,
    val lastVerified: LocalDate,
    val baseRule: RewardRule,
    val rules: List<RewardRule>,
    val exclusions: List<Exclusion>,
)

data class RewardRule(
    val id: String,
    val priority: Int,                 // lower = higher precedence
    val condition: Condition,
    val reward: Reward,
    val cap: Cap? = null,
    val validity: Validity? = null,
    val paymentRoute: PaymentRoute? = null,
)

sealed interface Condition {
    data class MerchantIs(val merchantId: String) : Condition
    data class MerchantFamilyIs(val familyId: String) : Condition
    data class CategoryIs(val categoryId: String) : Condition
    data object Always : Condition
    data class AllOf(val conditions: List<Condition>) : Condition
}

data class Reward(
    val effectiveRatePct: Double,      // the ONLY number the engine ranks on
    val earnDescription: String,       // "10 RP per ₹150 on SmartBuy"
    val valuationNote: String? = null, // "1 RP valued at ₹0.50 (conservative)"
)

data class Cap(val amountInr: Int, val period: CapPeriod)

enum class CapPeriod { PER_TRANSACTION, CALENDAR_MONTH, STATEMENT_CYCLE, QUARTER, YEAR }

data class Validity(val from: LocalDate?, val until: LocalDate?)

data class PaymentRoute(val id: String, val name: String, val instruction: String)

// ── Exclusions ──────────────────────────────────────────────
data class Exclusion(val target: ExclusionTarget, val scope: ExclusionScope)

sealed interface ExclusionTarget {
    data class MerchantIs(val merchantId: String) : ExclusionTarget
    data class CategoryIs(val categoryId: String) : ExclusionTarget
    data class TransactionTypeIs(val type: TransactionType) : ExclusionTarget
}

enum class ExclusionScope { FULL, ACCELERATED_ONLY }

enum class TransactionType {
    RENT, WALLET_LOAD, GIFT_CARD, FUEL, EMI, INSURANCE,
    GOVERNMENT, EDUCATION, UTILITY, JEWELLERY,
}

// ── Engine I/O ──────────────────────────────────────────────
data class PurchaseQuery(
    val merchantId: String? = null,    // exactly one of merchantId / categoryId set
    val categoryId: String? = null,
    val amountInr: Int? = null,
    val transactionType: TransactionType? = null, // optional user hint, e.g. "this is a gift card"
)

data class Recommendation(
    val card: Card,
    val winningRule: RewardRule,       // may be the base rule
    val nominalRatePct: Double,
    val effectiveValueInr: Int?,       // null when amount absent
    val effectiveRatePct: Double,      // == nominal when uncapped or amount absent
    val capCaveat: String?,            // "Assumes you haven't used this month's ₹1,500 cap"
    val routeInstruction: String?,     // Phase 3 UI; populated when winningRule has a paymentRoute
    val explanation: Explanation,
)

data class Explanation(
    val ruleProvenance: String,        // "Swiggy merchant rule overrides online-shopping category rule"
    val earnDescription: String,
    val valuationNote: String?,
    val exclusionNotes: List<String>,  // "Accelerated rate excluded for gift cards; base 1% applies"
    val dataVerified: LocalDate,
    val dataVersion: String,
)

interface RecommendationEngine {
    fun recommend(query: PurchaseQuery, userCards: List<Card>): List<Recommendation>
}
```

The engine is a **pure function** — no clock reads except `validity` checks against an injected `today: LocalDate`, no I/O, no randomness. That's what makes it exhaustively unit-testable.

---

## 5. Evaluation Algorithm

Deterministic, in order. For a `PurchaseQuery` and the user's cards:

**Step 1 — Resolve the query context.**
- If `merchantId` given: context = { merchant, its family (if any), its default category }.
- If only `categoryId` given: context = { category }.

**Step 2 — Per card, apply exclusions FIRST.**
- An exclusion matches if its target is the query's merchant, the query's resolved category, or the query's `transactionType` hint.
- `FULL` match → the card's result is 0% with an explanation ("Axis ACE earns nothing on wallet loads"). Skip to Step 6 for this card.
- `ACCELERATED_ONLY` match → only the base rule is eligible in Step 3.

**Step 3 — Collect eligible positive rules.**
A rule is eligible if: its condition matches the context (merchant condition needs the exact merchant; family condition needs the family; category condition needs the resolved category; `Always` always matches), AND `validity` contains today (absent = always valid), AND — in v1, pre-Phase-3 UI — rules with a `paymentRoute` are still eligible but produce a route-tagged recommendation (see Step 7). The base rule is always eligible unless FULL-excluded.

**Step 4 — Pick the winning rule per card.**
1. Lowest `priority` number wins.
2. Tie → higher `effectiveRatePct`.
3. Tie → lexicographically smallest rule `id`. (Never reached if the validator's overlap check holds; exists as a guaranteed backstop.)

**Step 5 — Compute effective value (within-purchase cap math, D4).**
- No amount: `effectiveRatePct = reward.effectiveRatePct`; `effectiveValueInr = null`; if capped, set `capCaveat`.
- Amount given: `rawReward = rate × amount`; if the rule has a cap, `reward = min(rawReward, cap.amountInr)` (full headroom assumed — say so in `capCaveat`); `effectiveRatePct = reward / amount × 100`. This is how a 5%-capped card loses to unlimited 2% on a big purchase.

**Step 6 — Rank cards.** In order:
1. Higher `effectiveRatePct` (post-cap when amount given).
2. Uncapped rule beats capped rule.
3. Higher cap amount.
4. Stable order: card `id` ascending.

**Step 7 — Route variants (schema-live now, UI in Phase 3).**
If a card's best rule requires a `paymentRoute`, emit it with `routeInstruction` populated. If the same card also has an eligible route-free rule, emit only the better of the two in v1 UI; Phase 3 UI shows both ("2% direct / 6.6% via SmartBuy").

**Step 8 — Build explanations.** Every recommendation states: winning rule + why it beat others (provenance), earn description, valuation note for points cards, cap + caveat, exclusion notes, `lastVerified`, `dataVersion`. Transparency is the product's moat.

---

## 6. Worked Examples

**A. Cap crossover (the rev-2 bug, now a feature).**
Query: groceries, ₹45,000. Card A: 5% capped ₹1,000/mo → min(2250, 1000)=₹1,000 → 2.2%. Card B: unlimited 2% → ₹900 → 2.0%. Card A still wins here — but at ₹60,000, B (₹1,200) beats A (₹1,000). Same cards, ranking flips on amount. Both carry appropriate caveats.

**B. Exclusion with fallback.**
Query: merchant `amazon_pay_gc`. Axis ACE has `ACCELERATED_ONLY` exclusion on it → only base 1.5% eligible. Explanation: "Accelerated rates excluded for Amazon Pay gift cards; base 1.5% applies."

**C. Precedence + priority.**
Query: merchant `swiggy`. HDFC Swiggy card: merchant rule (prio 10, 10%) and category rule via dining→... only if conditions match resolved context; merchant rule wins by priority. Provenance: "Swiggy merchant rule overrides category rule."

---

## 7. Required Test Suite (Phase 2 DoD)

1. Cap crossover flips the winner between two amounts (example A, both directions).
2. `FULL` exclusion zeroes an otherwise-winning card.
3. `ACCELERATED_ONLY` exclusion falls back to base rule.
4. Priority resolves overlapping rules; equal-priority backstop is deterministic.
5. Merchant → family → category → base fallback chain, each level.
6. Missing amount: nominal rate + cap caveat present.
7. Expired `validity` rule never wins.
8. Route-bearing rule emits `routeInstruction`.
9. Full determinism: identical query + cards → identical ordered output, repeated.
10. Validator: each rejection rule in §3.1 has a failing-fixture test.

**Phase 1 "3 tricky cards" gate:** one capped card, one exclusion-heavy card, one overlapping-rules card must round-trip JSON → validator → domain → correct engine output before Phase 1 is done. If any can't be expressed, this spec gets amended (with changelog) before Phase 2 starts.

---

## 8. Explicit v1 Simplifications (not oversights)

- No spend-to-date / remaining-cap tracking (full-headroom assumption + caveat, per roadmap).
- No user statement-cycle dates (D2 — useless without spend tracking).
- Point valuations are curator-fixed constants, disclosed via `valuationNote` (D1).
- Temporary offers: table exists, no data.
- MCC codes are not modeled; `TransactionType` is a curated approximation. Real MCC behavior varies by network — onboarding says so (roadmap risk).
- No milestone/annual-spend benefits (e.g. "₹2L annual spend → fee waiver") — that's the parked milestone-alerts feature; the schema does not model it in v1.
