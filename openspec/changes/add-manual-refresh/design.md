# Design

## Context

See `proposal.md` — Why. The content screen already refreshes once from an idempotent
`PageContentViewModel.initialize()` and keeps displayed content on failure via
`Success.userMessage`; `RefreshPagesUseCase` is the only refresh entry point and the repository
owns the Room SSOT. `PageContentUiState` is a sealed `Loading` / `Success` / `Error`, and
`PageContentContent` is stateless with callbacks for image taps and choice taps. Adding a refresh
button means the content composable gains a third user action, which the compose-ui skill says
should be collapsed into a single `onAction` entry point.

## Goals / Non-Goals

**Goals:**

- Let the user re-run the existing refresh flow from the app bar at any time after the screen has
  loaded, including from the error state.
- Make the in-flight state visible and idempotent (no concurrent refreshes).
- Keep the failure contract unchanged: content is never discarded by a failed refresh.

**Non-Goals:**

- Pull-to-refresh, periodic/background refresh, or caching changes.
- A retry action in the error body (the app bar action covers it).
- Refreshing individual pages or observing network state.

## Decisions

### In-flight flag on the UI state, guarded by a refresh job

Add `isRefreshing` to the state that can be shown while a refresh runs — `Success` and `Error` —
declared on the sealed interface with a `false` default so `Loading` needs no change:

```kotlin
sealed interface PageContentUiState {
    val isRefreshing: Boolean get() = false
    data object Loading : PageContentUiState
    data class Success(..., override val isRefreshing: Boolean = false) : PageContentUiState
    data class Error(val message: String, override val isRefreshing: Boolean = false) : PageContentUiState
}
```

`PageContentViewModel` holds a single `refreshJob: Job?`; `refresh()` returns immediately when that
job is active, otherwise launches the refresh, sets `isRefreshing = true` before the request, and
sets it back to `false` in both outcomes. The flag drives the app bar; the job is the authoritative
concurrency guard, so a fast double tap cannot enqueue two requests even before recomposition.

- *Alternative:* only a `Job` guard, no state flag — rejected: the user would get no feedback that a
  refresh is running.
- *Alternative:* a separate `StateFlow<Boolean>` — rejected: the skill prefers one state object.
- *Alternative:* wrap the sealed state in a `data class (content, isRefreshing)` — rejected: it
  would restructure every existing state read and test for one flag.

On a successful refresh, if the state is still `Error` (the initial observation was empty and the
failure was shown), reset it to `Success(emptyList())`; the SSOT flow overwrites that with the stored
rows whenever the refreshed document is non-empty. This is what makes retry recover.

### App bar action: icon normally, progress while refreshing

The screen's `TopAppBar` gains an `actions` slot. When `state.isRefreshing`, the slot shows a small
`CircularProgressIndicator` (fixed size, with a content description) instead of the button; otherwise
it shows an `IconButton` with `Icons.Filled.Refresh` and a "Refresh" content description. The action
is hidden only in `Loading`, where the body already shows a spinner and there is no stored content to
refresh into. It stays visible in `Error`, so the user can retry.

- *Alternative:* disable the icon button without a spinner — rejected: disabled state is easy to
  miss and reads as broken.
- *Alternative:* keep the action visible during `Loading` — rejected: it duplicates the body spinner
  and cannot be represented by the current state.

### Collapse screen callbacks into `PageContentAction`

With refresh, `PageContentContent` has three user actions (refresh, image tap, choice tap), so add a
sealed `PageContentAction` with `Refresh`, `ImageClicked(src, title)`, and
`ChoiceClicked(choiceId, responseId)`. `PageContentContent` takes `onAction: (PageContentAction) ->
Unit`, the stateful `PageContentScreen` passes `viewModel::onAction`, and the ViewModel's `onAction`
delegates to the existing `onChoiceClicked` logic and the new `refresh()`. The row-level
`PageContentRowItem` keeps its narrow lambdas; only the screen boundary changes.

- *Alternative:* add a third individual lambda (`onRefresh`) — rejected by the compose-ui skill's
  3+ actions rule; the single entry point also matches the architecture skill's `…Action` pattern.
- *Alternative:* pass the ViewModel into the content — forbidden.

### String resource for the action

Add `action_refresh` to `strings.xml` ("Refresh") and use it for the icon's content description so
the action is accessible and testable by description.

## Risks / Trade-offs

- [A user taps refresh repeatedly and queues requests] → `refreshJob?.isActive` guards entry; the
  state flag removes the button from the tree while running.
- [A failed manual refresh loses displayed content] → Reuses the existing failure path: keep
  `Success` and set `userMessage`; `Error` is only used when nothing is stored.
- [The in-flight spinner in the app bar is not announced to accessibility] → Give the progress
  indicator a content description from the same string resource.
- [Existing `PageContentContent` tests break when the callbacks collapse into `onAction`] → Update
  the call sites in `PageContentScreenTest` and add action assertions; the behavior under test is
  unchanged.
- [Refresh on an error state with an unchanged empty document leaves a stale error] → On success,
  reset `Error` to an empty `Success`; the SSOT emission replaces it when content exists.
