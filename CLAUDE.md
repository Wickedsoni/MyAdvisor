# MyAdvisor — Agent Instructions

## What this is

A Kotlin Multiplatform (Android + iOS, shared Compose UI) app that tells Indian cardholders with multiple credit cards which card to use for a specific purchase, maximizing rewards. The deterministic recommendation engine **is** the product. Guiding principles, in order: **correctness > features**, transparency > black-box, honesty about limits (no fake precision), deterministic output, offline engine.

## Source of truth

`docs/design/` is canonical (Desktop copies are the owner's working copies):

- `credit-card-advisor-roadmap.md` — the SSOT: scope (MoSCoW), phases, risks, **Change Protocol + Changelog**
- `rule-engine-spec.md` — domain model, JSON format, validator rules (§3.1), evaluation algorithm (§5), required tests (§7)
- `data-model.md` — Room persistence model, two-zone rule
- `architecture-decision-records.md` — ADRs 001–006
- `data-curation-guide.md` — dataset ops manual: source tiers, NOTES.md template (§4), priority bands (§6), red flags (§9)

**Change Protocol:** new feature ideas go to the roadmap's Parking Lot by default. Any scope change needs a dated Changelog row in the roadmap doc naming what was traded out. Every task you complete gets a dated Changelog row.

The task backlog for agents is `docs/agent-roadmap.md` — pick a task, follow its spec, tick its checkbox when done.

## Architecture rules (non-negotiable)

- **Module boundaries:** `:domain` is pure Kotlin — no Android, Room, or Compose imports, ever. `:data` owns Room, the importer, and repository impls. `:shared` owns Compose UI, ViewModels, and Koin assembly. `androidApp`/`iosApp` are thin entry points.
- **Engine purity (ADR-003):** the engine does no I/O and reads no clock (an injected `today: LocalDate` is the only time source). Repositories load full card aggregates into memory; the engine never queries.
- **Two-zone DB (ADR-002):** catalog tables are written ONLY by `ImportDao.replaceCatalog` (atomic transactional replace). The user zone (`user_cards`, `app_prefs`) survives imports. Never write catalog tables from anywhere else.
- **Validator discipline (ADR-006):** the validator (`DatasetValidator`) is the integrity gate for everything Room FKs can't see. Any new dataset field ⇒ a validator rule + a failing-fixture test (Spec §3.1 / §7.10).
- **Determinism (Spec D5):** any engine change must keep the Spec §7 suite in `DefaultRecommendationEngineTest` green; new behaviors need new tests there. Same inputs → same ordered output, always.
- **Extraction invariant:** `Extraction → Validation → Human Approval`. LLM-drafted card data lands on a `data/<card-id>` branch as a reviewable diff — never directly on `main`. Every drafted rule carries a citation (file, page, section) and a confidence level; LOW confidence must be flagged for human review. The validator cannot catch hallucinated business rules; the human review step exists for that.

## Naming conventions (match the existing code exactly)

| Thing | Convention | Example |
|---|---|---|
| Packages | `com.wickedcoder.myadvisor.{domain\|data}.<layer>`; UI `…myadvisor.ui.<feature>`; tools `…myadvisor.tools.<tool>` | `domain.engine`, `data.importer`, `ui.recommend` |
| Room entities | `XxxEntity`, tables snake_case plural | `RewardRuleEntity` → `reward_rules` |
| DAOs | `XxxDao` | `CatalogDao`, `ImportDao` |
| Dataset DTOs | `XxxDto` in `data.importer` | `RewardRuleDto` |
| Repositories | domain port `XxxRepository`; Room impl `RoomXxxRepository` | `CardCatalogRepository` / `RoomCardCatalogRepository` |
| Use cases | verb-first `XxxUseCase`, `operator fun invoke` | `GetRecommendationsUseCase` |
| ViewModels | `XxxViewModel` with nested `data class UiState` + private `MutableStateFlow` | `CardsViewModel.UiState` |
| Screens | `XxxScreen` composable in `ui/<feature>/`, gets its VM via `koinViewModel()` default param | `RecommendScreen` |
| Shared composables | `ui/components/` | `RateBadge`, `EmptyState` |
| Koin modules | `val xxxModule = module { }`; DI assembled in `shared/di/InitKoin.kt` | `dataModule`, `appModule` |
| Dataset ids | snake_case slugs; card `<issuerId>_<cardSlug>`; rule `<cardId>_<ruleSlug>` | `hdfc_swiggy`, `hdfc_swiggy_merchant` |
| Rule priorities | merchant 10–19, family 20–29, category 30–99, base always 1000 (curation guide §6) | |
| `dataVersion` | semver: patch = rate/cap fix; minor = new cards/merchants; major = schema change (with `schemaVersion` bump + Room migration) | |
| Research trail | `research/<issuerId>/<cardSlug>/NOTES.md` per curation-guide §4 template + archived source PDFs/screenshots | `research/hdfc/swiggy/NOTES.md` |
| Tests | backtick sentence names; `commonTest` for pure logic, `androidHostTest` for JVM-only (file access) | `` fun `cap crossover flips winner with amount`() `` |

**Prime data rule (curation guide):** a rule you can't source in Tier 1/2 (MITC PDF / official bank pages) is a rule you don't ship. Forums are discovery, never citation.

## Commands (Windows host, PowerShell)

```powershell
# Build Android APK
.\gradlew.bat :androidApp:assembleDebug

# All unit tests (JVM host)
.\gradlew.bat :domain:testAndroidHostTest :data:testAndroidHostTest :shared:testAndroidHostTest

# Dataset gate only (validates data/cards.json + research folders)
.\gradlew.bat :data:testAndroidHostTest --tests "*BundledDatasetValidationTest*"
```

**Emulator smoke test** (AVD `Pixel_8a` exists; SDK at `C:\Users\Avik\AppData\Local\Android\Sdk`):

```powershell
$sdk = "C:\Users\Avik\AppData\Local\Android\Sdk"
Start-Process "$sdk\emulator\emulator.exe" -ArgumentList "-avd","Pixel_8a","-no-snapshot-save" -WindowStyle Minimized
& "$sdk\platform-tools\adb.exe" wait-for-device   # then poll sys.boot_completed
& "$sdk\platform-tools\adb.exe" install -r "androidApp\build\outputs\apk\debug\androidApp-debug.apk"
& "$sdk\platform-tools\adb.exe" shell am start -n com.wickedcoder.myadvisor/.MainActivity
# Screenshots: screencap to device file, then adb pull.
# NEVER pipe binary output through PowerShell ">" — it corrupts it (UTF-16 re-encoding).
& "$sdk\platform-tools\adb.exe" shell screencap -p /data/local/tmp/s.png
& "$sdk\platform-tools\adb.exe" pull /data/local/tmp/s.png <scratchpad>\s.png
```

## Gotchas

- **No iOS compilation on this Windows host.** Apple targets are silently skipped; iOS tasks are flagged macOS-only in the roadmap. Don't attempt `iosSimulatorArm64Test` locally.
- **`data/cards.json` is the single canonical dataset.** It sits at the `:data` module root and is bundled into the app by the `prepareDatasetResources` Copy task in `shared/build.gradle.kts` (compose-resources custom directory). Never create a second copy.
- **Import is gated by a monotonic version check** (`BundledDatasetImporter`): a device that already imported v1.0.0 will NOT re-import unless `dataVersion` increases. Any dataset change without a version bump is invisible on-device.
- **`DomainJson` is strict** — unknown JSON keys fail parsing by design. New dataset fields require DTO + validator + test changes together.
- **Room KMP specifics:** `AppDatabaseConstructor` is an `expect object` (KSP generates actuals); expect/actual beta warnings are known noise (fix tracked as task D2). DB uses WAL mode.
- Git CRLF warnings on commit are noise; ignore them.

## Definition of done — every task

1. All tests green: `.\gradlew.bat :domain:testAndroidHostTest :data:testAndroidHostTest :shared:testAndroidHostTest` (and `:androidApp:assembleDebug` compiles).
2. Dataset gate green if `data/cards.json` or `research/` changed.
3. UI changes verified on the emulator with a screenshot.
4. Dated Changelog row added to `docs/design/credit-card-advisor-roadmap.md` (§8).
5. Task checkbox ticked in `docs/agent-roadmap.md`.
6. Commit message: imperative summary + body; end with `Co-Authored-By: Claude <model> <noreply@anthropic.com>`. Data-only drafts (extraction output) go to a `data/<card-id>` branch, not `main`.
