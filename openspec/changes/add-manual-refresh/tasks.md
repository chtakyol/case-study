# Tasks

## 1. Refresh state and ViewModel entry point

- [x] 1.1 Add `isRefreshing` to `PageContentUiState` (default `false` on the sealed interface, carried
      by `Success` and `Error`) and verify existing ViewModel and Compose tests still pass with
      `./gradlew :app:testDebugUnitTest --tests '*PageContent*'`.
- [x] 1.2 Add `refresh()`/`onAction(PageContentAction.Refresh)` to `PageContentViewModel` guarded by a
      single `refreshJob` (no concurrent refresh), setting `isRefreshing = true` before the request and
      clearing it on success and failure; on success reset an `Error` state to an empty `Success` so a
      retry can recover; verify with unit tests covering success, failure with content (message
      preserved), failure with nothing stored (`Error`), retry from `Error`, and a second tap while a
      refresh is in flight being ignored.
- [x] 1.3 Add the sealed `PageContentAction` (`Refresh`, `ImageClicked`, `ChoiceClicked`) and route the
      screen's actions through `onAction`, keeping `onChoiceClicked` behavior unchanged; verify the
      existing choice-selection tests still pass.

## 2. App bar action

- [x] 2.1 Add `action_refresh` to `res/values/strings.xml` and an `actions` slot on the page content
      screen's `TopAppBar`: a refresh `IconButton` (`Icons.Filled.Refresh`, content description from
      the new string) when idle, and a small `CircularProgressIndicator` with the same content
      description while `isRefreshing`; hide the slot only in `Loading`; verify with previews of idle
      and refreshing states.
- [x] 2.2 Pass the refresh action from `PageContentScreen` into `PageContentContent` through the new
      `onAction` callback; verify the app bar action appears with stored content and in the `Error`
      state.
- [x] 2.3 Update `PageContentScreenTest` call sites for the collapsed `onAction` signature and add
      Compose tests that the refresh action is displayed, tapping it reports `Refresh`, the progress
      indicator replaces it while `isRefreshing`, and the action is available in the `Error` state.

## 3. Verification

- [x] 3.1 Run `./gradlew :app:testDebugUnitTest` and `./gradlew ktlintCheck`; verify the full suite
      and style check pass.
- [x] 3.2 Manually verify on a debug build (`./gradlew :app:installDebug`) that tapping refresh
      reloads content from the fake fixture, shows the app bar progress meanwhile, and that a failed
      refresh keeps the list with a snackbar message.

## Workflow follow-up

- Archive the change after the project's review requirements are satisfied.
- Verify `openspec/specs/content-ui/spec.md` reflects the refresh requirements after archive.
