# MyAdvisor — Credit Card Checkout Advisor

Helps Indian cardholders with multiple cards instantly know which card to use
for a given purchase. Kotlin Multiplatform (Android + iOS) with shared
Compose Multiplatform UI.

Design docs (roadmap, rule-engine spec, data model, ADRs, curation guide) are
the single source of truth for scope and architecture decisions.

## Modules (Clean Architecture, ADR-001)

| Module | Contents | Rules |
|---|---|---|
| `:domain` | Pure Kotlin: domain model (Rule Engine Spec §4), `RecommendationEngine` contract, repository ports, shared JSON config | No Android/Room/UI dependencies — the engine is exhaustively unit-testable here |
| `:data` | Room KMP (two-zone schema per data-model.md), entity↔domain mappers, repository implementations, import pipeline (Phase 1) | Only the importer writes the catalog zone |
| `:shared` | Compose Multiplatform UI, theming, Koin DI assembly, iOS framework umbrella | Depends on `:domain` + `:data` |
| `:androidApp` | Android entry point (`MyAdvisorApplication` starts Koin) | |
| `/iosApp` | iOS entry point (SwiftUI host; `iOSApp.init` starts Koin) | Build from Xcode on macOS |

Repo folders per roadmap Phase 0: [data/cards.json](data/cards.json) is the
versioned reward dataset (lives at the `:data` module root); [research/](research/)
holds the per-issuer audit trail behind every curated rule.

## Building

- Android app: `./gradlew :androidApp:assembleDebug`
- iOS app: open [/iosApp](./iosApp) in Xcode on macOS and run.
- Unit tests (JVM host): `./gradlew :domain:testAndroidHostTest :data:testAndroidHostTest :shared:testAndroidHostTest`
- iOS-side tests (macOS only): `./gradlew :shared:iosSimulatorArm64Test`

CI (GitHub Actions) builds, lints, and runs the host tests on every push/PR.

## Key stack decisions

- **Room KMP** (bundled SQLite driver) — keeps ADR-002; Room supports
  Android + iOS since 2.7.
- **Koin** for DI, assembled in `:shared`, platform database builder injected
  per platform.
- **kotlinx.serialization** — one polymorphic `Condition` serializer shared by
  the dataset JSON and the `condition_json` Room column (ADR-006).
