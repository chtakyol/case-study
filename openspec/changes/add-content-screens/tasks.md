# Tasks

## 1. Dependencies and navigation foundation

- [x] 1.1 Add navigation-compose, hilt-navigation-compose, and Coil (`coil-compose`) to
      `gradle/libs.versions.toml` and `app/build.gradle.kts`; verify with
      `./gradlew :app:dependencies --configuration debugRuntimeClasspath`.
- [x] 1.2 Add `ui/navigation/Routes.kt` with `@Serializable` route types (`PageContentRoute`,
      `ImageViewerRoute`) and `ui/navigation/AppNavHost.kt` hosting both destinations with
      `composable<T>`; verify `./gradlew :app:assembleDebug` compiles both `composable` entries.
- [x] 1.3 Point `MainActivity` at `AppNavHost()` inside `CasestudyTheme` and remove the scaffold
      `Greeting`; verify a debug build launches to the page-content destination.

## 2. Theme typography

- [x] 2.1 Populate `ui/theme/Type.kt` with the title and content styles the screens use (page header,
      app bar/viewer title, section sub-header, body, choice/chip label); verify with a preview
      rendering every style.

## 3. Page content screen (state + rendering)

- [x] 3.1 Add a sealed `PageContentUiState` (`Loading`/`Success`/`Error`) plus the flattened row model
      (`PageHeaderRow`/`SectionHeaderRow`/`TextRow`/`ImageRow`/`ChoiceRow`) and a pure
      `List<Page> → List<PageContentRow>` mapper with unique row keys; verify with a unit test covering
      multiple pages, nested sections, depth-first order, and depth.
- [x] 3.2 Add `PageContentViewModel` (emit `Loading`, collect `ObservePagesUseCase` into `Success`, one
      `RefreshPagesUseCase` call in an idempotent `initialize()`, `Error` when a refresh fails with
      nothing stored and a transient `userMessage` on `Success` otherwise); verify with unit tests over
      fake use cases for load, SSOT update, refresh failure with and without stored content, and no
      stored pages.
- [x] 3.3 Add `PageContentScreen` (collect with `collectAsStateWithLifecycle`, `LaunchedEffect(Unit) {
      initialize() }`, snackbar for the `Success.userMessage` then clear, `onImageClick` guarded by
      `dropUnlessResumed`) and stateless `PageContentContent` rendering `Loading`, `Success` (the
      `LazyColumn` rows or the empty state, with an app-title top bar), and `Error`; verify with
      previews for loaded, empty, and error states.
- [x] 3.4 Wire the page-content `composable<PageContentRoute>` destination (no arguments) to resolve the
      ViewModel and pass the image-tap navigation callback; verify a debug build shows every page's
      items in one scrollable list.

## 4. Image viewer screen

- [x] 4.1 Add `ImageViewerScreen` (receives the `ImageViewerRoute` values `src`/`title`, top app bar with
      back, full-screen `AsyncImage` with placeholder/error); verify with previews for loaded and
      missing-source states.
- [x] 4.2 Wire the `composable<ImageViewerRoute>` destination via `entry.toRoute<ImageViewerRoute>()`
      and the navigation `navigate(ImageViewerRoute(...))` from an image tap; verify a debug build opens
      the viewer for a tapped image and back returns to the page, including a source URL containing a
      query string.

## 5. Integration

- [x] 5.1 Add a Robolectric Compose UI test (add Compose test deps to `testImplementation` if needed)
      covering: the list renders page headers and items in order, tapping an image reports its
      source/title, and the viewer shows the image and returns on back; plus a route test that a
      type-safe image route round-trips a source URL containing a query string. Verify with
      `./gradlew test`.

## 6. Choice selection

- [x] 6.1 Add `multipleSelection` to `ChoiceRow` and carry `selections: Map<Int, Set<Int>>` on the
      `Success` state; verify with unit tests that the mapper carries `multipleSelection` and the state
      defaults to no selection.
- [x] 6.2 Add `onChoiceClicked(choiceId, responseId)` to `PageContentViewModel` updating the `Success`
      selection and honoring `multipleSelection` (single replaces, multiple toggles, tapping a selected
      chip deselects); verify with unit tests for single, multiple, and deselect.
- [x] 6.3 Render each choice item's responses as Material 3 `FilterChip`s with selected/unselected
      state, show a "Single select"/"Multi select" mode label, and show the selected labels on their
      own line beneath the chips (hidden when nothing is selected), reading the selection from the
      `Success` state; verify with previews and a Compose UI test that a chip tap reports the
      choice/response and that selection updates.

## Workflow follow-up

- Archive the change after the project's review requirements are satisfied.
- Verify `openspec/specs/content-ui/spec.md` reflects the added requirements after archive.
