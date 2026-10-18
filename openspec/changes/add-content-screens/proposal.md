# Proposal

## Why

The app has a complete `data`/`domain` layer for the backend content document, but no UI: the app
renders a Compose scaffold (`MainActivity` + `Greeting`) and never shows the stored content. The
document — pages, each with a nested tree of `text`, `image`, and `choice` items — is invisible to
users, and there is no navigation host to move between screens. This change builds the first real
screens so the stored content becomes visible and navigable.

## What Changes

- Add **Navigation Compose** and **Coil** as dependencies (navigation-compose,
  hilt-navigation-compose, coil-compose) — none are present yet.
- Add navigation plumbing in `ui/navigation`: type-safe routes defined as `@Serializable` route types
  and a single `AppNavHost` hosted by `MainActivity` (replacing the scaffold greeting).
- Add the **page content screen**: a stateful `PageContentScreen` + `PageContentViewModel` and a
  stateless `PageContentContent` that renders the whole stored document — every page, each shown as a
  header followed by its items — in one scrollable lazy list. The nested item tree is flattened into
  rows (page headers, section sub-headers, and items) so a `LazyColumn` can scroll the whole document.
- Render `text`, `image`, and `choice` items with Material 3 components; load images with Coil, and
  present each choice item's responses as selectable chips (single or multiple, per the data) with a
  mode label and the current selection shown on its own line.
- Add the **image viewer screen**: tapping an image navigates to a full-screen viewer showing that
  image (Coil), with a top app bar and back navigation. The route carries the image source and title.
- Add a Material 3 **top app bar** to both screens; the content screen's shows the app title, the
  viewer's the image title.
- **Populate typography** in `ui/theme/Type.kt`: define the styles used for titles (page/section)
  and content (body/labels).
- Refresh content on first load using the existing `RefreshPagesUseCase`, then observe the Room SSOT
  reactively via `ObservePagesUseCase`; loading and failures are surfaced as UI state.
- The app will eventually have three screens; this change builds the two screens above plus the
  navigation host. The third screen and richer flows are out of scope.

## Capabilities

### New Capabilities

- `content-ui`: presenting the stored content document — rendering its pages and items in a scrollable
  list with Coil-backed images, choosing responses for choice items, viewing a single image
  full-screen, and navigating between these content screens using Material 3 theming and typography.

### Modified Capabilities

- (none) — the `content` domain capability (data/domain) is unchanged; this change only adds the
  presentation layer on top of it.

## Impact

- `gradle/libs.versions.toml` and `app/build.gradle.kts`: add `androidx.navigation:navigation-compose`,
  `androidx.hilt:hilt-navigation-compose`, and Coil (`io.coil-kt.coil3:coil-compose`).
- `app/src/main/java/com/cihatakyol/case_study/MainActivity.kt`: host `AppNavHost` inside
  `CasestudyTheme`; remove the scaffold `Greeting`.
- New `ui/navigation` package: `@Serializable` route types and `AppNavHost`.
- New `ui/content` package: `PageContentScreen`, `PageContentViewModel`, `PageContentUiState`, and
  `ui/content/components` (list row composables, including choice chips); a stateless image viewer
  screen.
- `ui/theme/Type.kt`: populate typography styles for titles and content.
- `app/src/main/res/values/strings.xml`: new user-facing strings (screen titles, choice mode/selection
  labels, error/empty text).
- No existing behavior or public API is broken; this is additive UI on top of the existing domain
  layer. No data-layer changes.
