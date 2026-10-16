# Proposal

## Why

The app currently has no data layer — only a Compose scaffold (`MainActivity` + theme). The
backend serves a structured content document: a list of pages, each containing a recursive tree of
sections and items (`text`, `image`, `choice`). We need to load that document and expose it through
clean `data`/`domain` layers with a local Single Source of Truth, so the UI added later reads
reactive domain data instead of touching HTTP or JSON. Two gaps block that: the debug build's base
URL is a placeholder host, so a running app has no content to show, and a page-detail screen will need
to read a single page rather than the whole document.

## What Changes

- Add the networking stack: Retrofit + OkHttp + kotlinx.serialization, with the API base URL read
  from a `BuildConfig` field.
- Add `@Serializable` DTOs mirroring the wire payload exactly, including the recursive item tree,
  the `type` discriminator, and `response_set`.
- Add Room as the local Single Source of Truth: entities, DAO, and database for pages, sections,
  and items.
- Map DTO ↔ entity ↔ domain at the repository boundary.
- Add `ContentRepository` (domain interface) and `ContentRepositoryImpl` (data) with reactive reads
  (`Flow`) and a `Result`-returning refresh that pulls from the network into the SSOT.
- Add use cases: `ObservePagesUseCase` (stream) and `RefreshPagesUseCase` (action).
- Add Hilt wiring: `Application` class, `@AndroidEntryPoint` on `MainActivity`, and
  network / database / repository / dispatcher modules.
- Add tests covering JSON deserialization and DTO→entity→domain mapping using a fake interceptor /
  `MockWebServer`.
- Add a debug-only content fixture so debug builds render data without a reachable backend: a bundled
  payload served by an interceptor that is installed only in the `debug` build.
- Add a single-page read path (`ContentDao` query, `ContentRepository.observePage(id): Flow<Page?>`,
  and an `ObservePageUseCase`) so a page-detail screen observes one page instead of filtering the
  whole document.

No UI work is included: screens, ViewModels, and navigation are out of scope for this change.

## Capabilities

### New Capabilities

- `content`: loading, locally persisting, and exposing the structured content document (pages with
  recursive sections and text/image/choice items) as domain models.

### Modified Capabilities

- (none)

## Impact

- `gradle/libs.versions.toml` and `app/build.gradle.kts`: new plugins (KSP, Hilt,
  kotlinx.serialization) and dependencies (Room, Retrofit, OkHttp, coroutines, serialization).
- `app/src/main/AndroidManifest.xml`: `INTERNET` permission and the `Application` class name.
- New packages under `com.cihatakyol.case_study`: `data/remote`, `data/local/database`,
  `data/repository`, `domain/model`, `domain/repository`, `domain/usecase`, `di`.
- New `app/src/debug/` source set: the fixture payload and the debug-only interceptor/DI that installs
  it; `app/src/main` network wiring gains the seam to add an interceptor.
- `data/local/database` (page-scoped queries), `domain/repository` (`observePage`),
  `domain/usecase` (`ObservePageUseCase`), and `data/repository` (single-page reassembly).
- No public API or existing behavior is broken; this is additive.
