# Proposal

## Why

The page content screen only refreshes when it is first shown. After that, a user who changes
something on the backend (or whose first load failed) has no way to pull fresh content without
leaving and re-entering the screen. The app bar has room for the action, and the domain layer already
exposes `RefreshPagesUseCase`, so adding a manual refresh is a small, high-value addition.

## What Changes

- Add a **refresh action to the page content screen's top app bar** (an icon button with a
  "Refresh" content description) that triggers the existing refresh flow.
- Track an **in-flight refresh** in screen state so the action can't be triggered twice concurrently:
  while refreshing, the app bar shows progress instead of the refresh icon and the action is
  disabled.
- **Preserve existing failure behavior**: a failed manual refresh keeps the displayed content and
  surfaces the transient message; a failed refresh with nothing stored leaves the screen in its
  error state, and the refresh action remains available there so the user can retry.
- Refresh is still the only write path: content continues to be read from the Room SSOT, and a
  successful refresh updates the list reactively.

## Capabilities

### New Capabilities

- (none)

### Modified Capabilities

- `content-ui`: the page content screen gains a user-triggered refresh action with visible in-flight
  feedback, in addition to the existing refresh-on-first-load behavior. (This capability's main spec
  is still pending archive from the `add-content-screens` change; this delta adds one requirement to
  the same capability path.)

## Impact

- `app/src/main/java/com/cihatakyol/case_study/ui/content/PageContentViewModel.kt`: add a
  user-triggered refresh entry point and in-flight tracking; reuse `RefreshPagesUseCase`.
- `app/src/main/java/com/cihatakyol/case_study/ui/content/PageContentUiState.kt`: carry the
  in-flight flag on the success state.
- `app/src/main/java/com/cihatakyol/case_study/ui/content/PageContentScreen.kt`: add the app bar
  action and pass a refresh callback into the stateless content composable.
- `app/src/main/res/values/strings.xml`: a string for the refresh action's content description.
- Tests: ViewModel unit tests for manual refresh (success, failure, retry from error, no duplicate
  concurrent calls) and Compose tests for the app bar action; existing `PageContentContent` test call
  sites gain the new callback.
- No data-layer, domain, navigation, or dependency changes.
