# Design

## Context

Single-module Android app (`:app`, package `com.cihatakyol.case_study`). The `content` capability
(`data`/`domain`) already exists: `ObservePagesUseCase(): Flow<List<Page>>`,
`ObservePageUseCase(pageId)`, and `RefreshPagesUseCase(): Result<Unit>`, over a Room SSOT, with domain
models `Page`/`Section`/`Text`/`Image`/`Choice`/`ResponseSet`/`Response`. A `ResponseSet` carries a
`multipleSelection` flag and the response `score`s. `MainActivity` still renders the scaffold
`Greeting`; there is no navigation host and no image-loading library. The debug fixture serves two
pages ("Main Page", with nested sections, images, and a choice, plus "Second Page"), so a running app
has content to render. See `proposal.md` for motivation. The architecture and compose-ui skills fix the
shape: `ui → domain`, stateful `Screen` + stateless `Content`, `StateFlow` UI state, events-as-state, a
single `AppNavHost`, ViewModels resolved only inside `composable` blocks, and callbacks (never
`NavController`) passed to screens.

## Goals / Non-Goals

**Goals:**

- Render the whole content document — every page and its items — in a single scrollable list with
  Coil-backed images, and view one image full-screen.
- Let the user choose responses for a choice item with chips that reflect the single/multiple mode.
- Establish the navigation host and a type-safe route pattern the remaining screen(s) will follow.
- Make the Material theme's typography usable for titles and body content.

**Non-Goals:**

- Submitting choice responses to the server (selection is held in memory for the session).
- The third screen and any separate page-picker screen.
- Image zoom/pan gestures, custom image caching, or sharing.
- Pagination, pull-to-refresh, or changes to the offline-first data flow (refresh-on-load only).

## Decisions

### Type-safe navigation with Navigation Compose

Add `androidx.navigation:navigation-compose` and `androidx.hilt:hilt-navigation-compose` and host one
`AppNavHost` in `MainActivity`. Routes are **type-safe**: `@Serializable` types in
`ui/navigation/Routes.kt` — `data object PageContentRoute` and
`data class ImageViewerRoute(val src: String, val title: String)`. Destinations use
`composable<PageContentRoute> { }` / `composable<ImageViewerRoute> { }`, navigation calls
`navController.navigate(ImageViewerRoute(src, title))`, and the viewer reads its arguments with
`entry.toRoute<ImageViewerRoute>()`. The start destination is `PageContentRoute`.

- *Alternative:* string routes with `{arg}` placeholders and manual `Uri.encode` (this change's
  original plan) — rejected: type-safe routes make routing compile-checked and drop the stringly-typed
  arguments and manual encoding.
- *Alternative:* state-based screen switching (`when (screen)`) — rejected: the architecture skill
  mandates `NavHost`, and back handling plus typed arguments come for free.

### Flatten the document into a UI row model

The ViewModel maps the stored `List<Page>` into a flat `List<PageContentRow>` (a sealed interface with
`PageHeaderRow`, `SectionHeaderRow`, `TextRow`, `ImageRow`, `ChoiceRow`) in depth-first document order:
each page emits a header (depth 0) followed by its items (depth 1+). `ChoiceRow` carries the item's
content, its `multipleSelection` flag, and its responses. `LazyColumn` renders rows by type with stable
keys — `page_$id` for page headers and `item_$id` for items — because page and item identifiers come
from different spaces and can collide. Depth drives indentation.

- *Alternative:* nested `LazyColumn`/`Column` per section — rejected: nested vertically scrolling lazy
  containers measure and scroll poorly.
- *Alternative:* a separate pages list screen with one screen per page — rejected: the agreed single
  screen shows the whole document.

### Choice selection is screen UI state

Which responses are selected is screen UI state, so it lives in the ViewModel on the `Success`
variant: `Success.selections: Map<Int, Set<Int>>` (choice item id → selected response ids) and a
single `onChoiceClicked(choiceId, responseId)` entry point. The rule honors the item's
`multipleSelection`: single-select replaces the current selection; multi-select toggles the tapped id.
The selection is *not* baked into `rows` (they stay a pure projection of the stored document), so the
list does not re-flatten on every tap. Each response renders as a Material 3 `FilterChip` with a
`selected` state; the item shows a mode label ("Single select" / "Multi select") and, beneath the
chips, the current selection on its own line. Rows cross the screen boundary as data; the content
composable receives the selection map and an `onChoiceClick` lambda.

- *Alternative:* `remember { mutableStateOf(...) }` per choice item — rejected: this is screen UI state
  and belongs to the ViewModel (compose-ui skill), and per-item `remember` loses selection across
  configuration changes.
- *Alternative:* a `rememberXState()` UI-logic holder — rejected for the same reason; selection is
  screen state, not intrinsic composable state.
- *Alternative:* persist selection (Room/DataStore) or submit it — out of scope for this change.

### Screen state is a sealed Loading/Success/Error

`PageContentUiState` is a sealed interface with three variants:

- `Loading` — before the stored content is first observed.
- `Success(rows, selections, userMessage = null)` — the stored document as flattened rows (empty rows →
  the empty state), the current choice selections, and an optional transient refresh-failure message.
- `Error(message)` — a refresh failed and nothing is stored.

`PageContentContent` renders `when (state)`: a spinner for `Loading`, the `LazyColumn` (or the empty
state) for `Success`, and the failure message for `Error`. `PageContentViewModel.initialize()`
(idempotent, called from the Screen's `LaunchedEffect`) collects `ObservePagesUseCase()` into
`Success`, then calls `RefreshPagesUseCase` once. The list always reflects the SSOT; refresh only
updates it. A refresh failure keeps `Success` and sets `userMessage` (shown as a snackbar and cleared
by the UI) when content is present, and yields `Error` only when nothing is stored — so a failed
refresh never discards displayed content, and `Error` (which carries no content) cannot lose any.
This follows the skills' "seed lazily, no work in `init`" and "events become state" rules.

- *Alternative:* one data class with `isLoading`/`isEmpty`/`userMessage` flags (this change's original
  model) — rejected: the sealed variants make the screen's states exhaustive and mutually exclusive.
- *Alternative:* several independent `StateFlow`s — rejected: the skill prefers one state object.
- *Alternative:* call `RefreshPagesUseCase` in `init {}` — rejected by the skill (no async work in the
  constructor, and it can't be re-seeded after config changes).

### Image viewer as a stateless screen

`ImageViewerScreen` receives `src` and `title` (resolved from `ImageViewerRoute` via
`entry.toRoute<ImageViewerRoute>()` in the NavHost) and renders the image full-screen with a top app
bar and a back callback. It has no ViewModel because it holds no screen UI state and no business logic
— a deliberate exception to the Screen+ViewModel pairing that keeps the viewer trivially previewable.

- *Alternative:* an `ImageViewerViewModel` reading `SavedStateHandle` — rejected as ceremony for two
  pass-through arguments.

### Coil for image loading

Add Coil 3 (`io.coil-kt.coil3:coil-compose`) and use `AsyncImage` with placeholder/error painters so
image rows and the viewer share one loader. Pin the version in `libs.versions.toml`.

- *Alternative:* Coil 2 (`io.coil-kt:coil-compose`) — acceptable; Coil 3 is the current line with an
  equivalent Compose API.
- *Alternative:* hand-rolled download+decode — rejected: reinvents caching and cancellation.

### Route arguments are serialized, not hand-encoded

Because routes are `@Serializable` types, Navigation serializes their arguments itself; an image source
containing `?`/`&` survives without manual percent-encoding. This replaces the original plan's string
route builder and `Uri.encode` call.

- *Alternative:* keep string routes and encode arguments by hand — rejected: easy to get wrong and
  stringly-typed.

### Typography in the theme

Populate `ui/theme/Type.kt` with the styles the screens consume: `headlineSmall` for page headers,
`titleLarge` for the app bar, `titleMedium` for section sub-headers, `bodyLarge` for text content, and
`labelLarge` for chip labels, the choice mode label, and the selected-responses line. Colors keep using
the existing `MaterialTheme` scheme.

- *Alternative:* per-composable `TextStyle(...)` literals — rejected: typography belongs in the single
  theme file.

## Risks / Trade-offs

- [Flattening loses the tree for future interactions] → The row model keeps `id` and `depth`; the mapper
  is the single place to extend if nesting matters later.
- [Page and item ids can collide in the flattened list] → Row keys are prefixed (`page_$id` /
  `item_$id`) so the `LazyColumn` never sees a duplicate key.
- [Choice selection is in-memory and lost on process death or when leaving the screen] → Session-scoped
  by design; persisting or submitting responses is out of scope (a `SavedStateHandle`/Room follow-up).
- [Single vs multiple semantics come from `ResponseSet.multipleSelection`] → The ViewModel owns the
  rule and tests pin both single and multiple selection, plus deselect.
- [A failed first refresh with no stored content shows an error screen] → The `Error` variant carries
  the failure message; the debug fixture makes debug builds succeed regardless.
- [A failed refresh must not discard displayed content] → Failures with content present keep `Success`
  and surface a transient `userMessage`; `Error` carries no content and is used only when nothing is
  stored.
- [Type-safe routes require `@Serializable` on every route type] → The serialization plugin is already
  applied to the module; a missing annotation fails at compile time.
- [Coil 3.6+ is built with Kotlin 2.4 and forces `kotlin-stdlib` past the project's Kotlin 2.2.10] →
  Pin Coil to 3.2.0 (whose stdlib requirement is 2.1.20); revisit when the project's Kotlin is upgraded.
- [Long documents flatten into many rows] → `LazyColumn` with stable keys handles it and preserves
  scroll position across SSOT updates.

## Open Questions

None blocking. Choice *submission* is intentionally out of scope; selection stays in memory for the
session, which the specs and tasks reflect.
