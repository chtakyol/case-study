---
name: Compose UI
description: Rules for Jetpack Compose UI in this single-module app — the stateful FeatureScreen / stateless FeatureContent split, state holders (ViewModel vs plain class), state-driven one-off events (no Channel/SharedFlow), StateFlow exposure and collectAsStateWithLifecycle, previews, and naming. Use when writing or changing @Composable functions, UI state, UI events, or Flow collection.
---

## Compose
- One `Screen` per route, split into a stateful `FeatureScreen` (collects UI state and reacts to it) and a stateless `FeatureContent` (renders data, emits lambdas). See Screen structure section.
- The `Screen` composable holds the scaffold and top-level layout; `Content` holds the actual UI tree.
- Extract reusable pieces into `components/` within the feature package.
- Pass lambdas down for events, never pass ViewModel references into child composables.
- This is a **single-module** app: `FeatureScreen` / `FeatureContent` / `FeatureViewModel` / `components/` live under `ui/[feature]` (see the `architecture` skill for package layout).

## State Ownership
Two kinds of UI state (see docs *State holders*):

- **Screen UI state** — *what* to display (items, loading flags, form values). Produced by the **business-logic state holder** (`ViewModel`) as a `StateFlow`.
- **UI element state** — properties intrinsic to a composable (expanded/collapsed, scroll position, drawer open). Lives in the composition and belongs to a **UI-logic state holder**.

Rules:
- Hold state **as low as possible** while maintaining proper ownership: screen UI state → ViewModel, UI element state → the composable that owns it.
- Do **not** use `remember` or `rememberSaveable` for screen UI state — that belongs in the ViewModel.
- Use `remember*` for framework-owned UI element state: `LazyListState`, `LazyGridState`, `ScrollState`, `PagerState`, `SnackbarHostState`, `TextFieldState` (foundation).
- When reusable UI logic (search bar, chip group, adaptive nav) gets complex, extract it into a **plain-class UI state holder** created by a `rememberXState()` function — not a ViewModel:
  ```kotlin
  @Stable
  class MyScreenState(
      private val someState: StateFlow<SomeState>,   // pass only what it needs
      private val doSomething: () -> Unit,
  ) {
      val shouldShowNavRail: Boolean get() = /* UI logic */
  }

  @Composable
  fun rememberMyScreenState(
      someState: StateFlow<SomeState>,
      doSomething: () -> Unit,
  ): MyScreenState = remember(someState, doSomething) { MyScreenState(someState, doSomething) }
  ```
  - Plain holders are composition-scoped: they do **not** survive Activity recreation.
  - Never pass a `ViewModel` into a plain holder — pass only the state/functions it needs.
  - A shorter-lived holder must never depend on a longer-lived one.
- Use `derivedStateOf` **only** in rare cases where Compose-internal state (LazyListState, ScrollState, etc.) drives a derived UI value — e.g. `val showFAB by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 } }`. If the derivation can happen in the ViewModel, it should.

## Flow
- Expose state as `StateFlow` from ViewModel (`_uiState` private, `uiState` public)
- Convert stream inputs with `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initialValue)`; use `SharingStarted.Lazily` when the pipeline must stay active while the UI is on the back stack or in an offscreen tab
- Prefer `combine` / `map` / `filter` over imperative collection logic
- Collect in UI with `collectAsStateWithLifecycle()`
- **Every ViewModel-produced event must become a UI state update** — never a one-off `Channel` / `SharedFlow`. See UI events.
- Don't launch async work in the ViewModel `init`/constructor. Seed the pipeline lazily with an idempotent `initialize()` called from the Screen:
  ```kotlin
  @MainThread
  fun initialize() {
      if (initialized) return
      initialized = true
      viewModelScope.launch { /* seed the state production pipeline */ }
  }
  ```

## Screen structure
Every `[Feature]Screen.kt` follows the stateful/stateless split:

> **UDF loop**: Events UI → VM via `onAction()` ; State VM → UI via `StateFlow`. Every ViewModel event results in a UI state update — there is no `SharedFlow` event channel.

- **`FeatureScreen` composable** (stateful):
  - Takes `viewModel: FeatureViewModel` plus navigation callbacks as parameters.
  - Collects state: `val uiState by viewModel.uiState.collectAsStateWithLifecycle()`.
  - Seeds the ViewModel: `LaunchedEffect(Unit) { viewModel.initialize() }`.
  - Reacts to **state** for one-shot UI behavior (navigation, messages) — see UI events.
  - Delegates rendering to `FeatureContent(state = uiState, onAction = viewModel::onAction)` (complex) or `FeatureContent(state = uiState, onBack = …)` (simple).
- **`FeatureContent` composable** (stateless):
  - Accepts **plain data + lambdas** derived from the UI state — never the ViewModel, never `LocalLifecycleOwner`.
  - Contains the actual UI tree and `components/` calls.
  - Bears `@Preview` annotations (see @Preview section).
- No `@Preview` on `FeatureScreen` (it needs a ViewModel + lifecycle — not renderable in Preview).

### Actions — individual lambdas vs. `onAction`

- When `FeatureContent` has **3+ distinct user actions**, collapse them into a single `onAction: (FeatureAction) -> Unit` lambda. The ViewModel declares one entry point:
  ```kotlin
  fun onAction(action: FeatureAction) {
      when (action) {
          is FeatureAction.ItemClicked -> …
          FeatureAction.BackPressed -> …
          FeatureAction.Refresh -> …
      }
  }
  ```
  And `FeatureContent` takes one callback:
  ```kotlin
  @Composable
  fun FeatureContent(state: FeatureUiState, onAction: (FeatureAction) -> Unit) { … }
  ```
- Below 3 actions, individual lambdas are fine — no ceremony for trivial screens:
  ```kotlin
  @Composable
  fun FeatureContent(state: FeatureUiState, onBack: () -> Unit) { … }
  ```

```kotlin
@Composable
fun FeatureScreen(
    viewModel: FeatureViewModel,
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.initialize() }

    // Business-logic-gated navigation: react to UI state, not to an event stream.
    val currentOnOpenDetail by rememberUpdatedState(onOpenDetail)
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        viewModel.uiState
            .map { it.selectedId }
            .filterNotNull()
            .flowWithLifecycle(lifecycle)
            .collect { id -> currentOnOpenDetail(id) }
    }

    FeatureContent(
        state = uiState,
        // UI-initiated navigation: guard with dropUnlessResumed.
        onBack = dropUnlessResumed { onBack() },
        onAction = viewModel::onAction,
    )
}

@Composable
fun FeatureContent(state: FeatureUiState, onAction: (FeatureAction) -> Unit) {
    val listState = rememberLazyListState()
    Scaffold { padding ->
        LazyColumn(state = listState, modifier = Modifier.padding(padding)) {
            // ... render state
        }
    }
}
```

## UI events
Where an event is handled depends on where it originates and what it needs (docs decision tree):

| Event origin | Needs | Handle |
|---|---|---|
| UI | UI behavior logic (expand, scroll, show/hide) | In the composable, by mutating UI element state |
| UI | Business logic | Delegate to the ViewModel through a lambda |
| ViewModel | — | Always via a UI state update |

**UI → UI logic** — mutate UI element state directly:

```kotlin
var expanded by remember { mutableStateOf(false) }
Button(onClick = { expanded = !expanded }) { Text(if (expanded) "Collapse" else "Expand") }
```

**UI → business logic** — hoist a lambda; never pass the ViewModel down (also inside lazy lists):

```kotlin
@Composable
fun MyList(items: List<String>, onItemClick: (MyItem) -> Unit) { /* render items, emit onItemClick */ }
```

**UI → navigation (UI behavior logic)** — pass a callback to the caller and guard it with `dropUnlessResumed` so it only runs when the lifecycle is at least `RESUMED`:

```kotlin
Button(onClick = dropUnlessResumed { onHelp() }) { Text("Get help") }
```

**ViewModel → UI** — model the outcome as UI state and let the UI react (e.g. navigation gated on business-logic validation):

```kotlin
data class LoginUiState(
    val isLoginInProgress: Boolean = false,
    val errorMessage: String? = null,
    val isUserLoggedIn: Boolean = false,
)

val lifecycle = LocalLifecycleOwner.current.lifecycle
val currentOnUserLogIn by rememberUpdatedState(onUserLogIn)
LaunchedEffect(lifecycle) {
    viewModel.uiState
        .filter { it.isUserLoggedIn }
        .flowWithLifecycle(lifecycle)
        .collect { currentOnUserLogIn() }
}
```

**Transient messages** — don't emit a toast/snackbar event; expose a nullable field and have the UI clear it when consumed:

```kotlin
data class LatestNewsUiState(
    val news: List<News> = emptyList(),
    val isLoading: Boolean = false,
    val userMessage: String? = null,   // set by the VM
)

// UI
uiState.userMessage?.let { message ->
    LaunchedEffect(message) {
        snackbarHostState.showSnackbar(message)
        viewModel.userMessageShown()   // VM clears userMessage
    }
}
```

**Don't use `Channel`/`SharedFlow` for one-off ViewModel events.** When the ViewModel outlives the UI, delivery isn't guaranteed and events can be dropped or leave the app in an inconsistent state. Reduce them to UI state (`UserMessage?`, `isUserLoggedIn`) so they survive config changes and are reproducible. If the destination stays on the back stack, hold the "already navigated" flag in the UI (`rememberSaveable`) so navigation doesn't repeat.

Event naming: Compose event lambda parameters use `on` + Verb + Target (`onExpandClicked`, `onItemClick`, `onValueChange`); ViewModel handlers are verb-named (`validateInput()`, `refreshNews()`).

## Navigation
- Library: **Jetpack Navigation Compose** (`androidx.navigation:navigation-compose`). Routes, `AppNavHost`, and `SavedStateHandle` wiring live in the `architecture` skill — this section covers the UI-side rules only.
- Declare a single `AppNavHost` composable hosting one `NavHost`, consumed from `MainActivity`'s `setContent { AppNavHost() }`.
- Each destination block is the **only** place a ViewModel is resolved and `NavController` is wired:
  ```kotlin
  composable(route = FEATURE_ROUTE) {
      FeatureScreen(
          viewModel = hiltViewModel(),
          onBack = { navController.popBackStack() },
          onOpenDetail = { id -> navController.navigate(itemDetailRoute(id)) },
      )
  }
  ```
- Navigation actions flow via **callbacks passed in** from the NavHost — never pass `NavController` into a Screen or Content composable.
- UI-initiated navigation (button tap) is handled in the UI by exposing the callback; guard it with `dropUnlessResumed { … }`.
- Business-logic-gated navigation (validate before advancing) is modeled as UI state — the Screen observes the state and navigates when it flips. See UI events.
- If the destination stays on the back stack, hold the navigation trigger flag in the UI (`rememberSaveable`) so it doesn't advance repeatedly.
- Group related destinations with nested `navigation(startDestination = …)` graphs when a feature has multiple screens.

## @Preview
- Every `FeatureContent` and every `components/` composable ships a single `@PreviewLightDark` preview — auto-renders both light and dark variants:
  ```kotlin
  @PreviewLightDark
  @Composable
  private fun FeatureContentPreview() {
      CasestudyTheme {
          FeatureContent(state = FeatureUiState.Preview, onAction = {})
      }
  }
  ```
- Previews live in the **same file** as the composable, as `private` functions — they must not leak into the public API.
- Supply representative sample state via a `companion object { val Preview = FeatureUiState(...) }` on the `UiState` class — never real data sources or ViewModels in previews.
- No `@Preview` on stateful `*Screen` composables; preview the stateless `*Content` instead. Use the `CasestudyTheme` wrapper in previews so light/dark styling renders correctly.

## Naming
- ViewModel state: `[Feature]UiState` (data class or sealed class), with a `companion object { val Preview = ... }` for previews.
- ViewModel actions (UI → VM): `[Feature]Action` (sealed class)
- Compose event lambdas: `on` + Verb + Target (`onExpandClicked`, `onItemClick`, `onValueChange`); ViewModel handlers are verb-named (`refreshNews`, `validateInput`).
- Plain UI-logic state holders: `[Feature]State` created by `remember[Feature]State()`.
- Screen route constants: `[FEATURE]_ROUTE` (top-level `const val`)
- Stateful composable: `FeatureScreen`; stateless composable: `FeatureContent`
