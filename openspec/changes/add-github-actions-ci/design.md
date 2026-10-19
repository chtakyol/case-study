# Design

## Context

See `proposal.md` - Why. Constraints from the current repository:

- Single-module Android app (`:app`), Gradle 9.6.0 (wrapper), AGP 9.4.1, Kotlin 2.2.10, `compileSdk`/`targetSdk` 37, `minSdk` 30, Java source/target 11.
- `gradle/gradle-daemon-jvm.properties` pins the Gradle daemon toolchain to Java 25; local `java -version` is 21, so Gradle would otherwise provision a daemon JVM.
- `gradle.properties` enables `org.gradle.configuration-cache=true` and sets `android.disallowKotlinSourceSets=false` (KSP/AGP 9 workaround) - both must be picked up in CI.
- `:app:testDebugUnitTest` is the JVM suite (JUnit 4, Robolectric, MockWebServer, Compose UI test, Navigation testing) and needs Android resources (`isIncludeAndroidResources = true`); it does not need an emulator.
- `ktlintCheck` is wired via the `org.jlleitschuh.gradle.ktlint` plugin, pinned to ktlint 1.8.0, `android = true`, `ignoreFailures = false`, with PLAIN and CHECKSTYLE reporters.
- No `.github/` directory exists yet; `origin` is `github.com/chtakyol/case-study`, default branch `master`.
- JVM tests are run with `testImplementation` dependencies only; `androidTest` currently holds one generated instrumented test, which CI will not run.

## Goals / Non-Goals

**Goals:**

- One workflow file that a contributor can read end-to-end and that mirrors the local commands in `README.md`.
- Deterministic toolchain and SDK resolution so a green run locally implies a green run in CI.
- Fast feedback: cache Gradle state, cancel superseded runs, bound runtime with a timeout.

**Non-Goals:**

- Instrumented/emulator tests (`connectedDebugAndroidTest`), release builds, signing, or publishing.
- Making the checks required via branch protection (repository setting, owned by the maintainer).
- Changing Gradle build logic, test configuration, or ktlint rules.
- Multi-OS or multi-JDK matrices; a single Linux job is sufficient for the existing suite.

## Decisions

**1. Trigger on both `push` to `master` and `pull_request` targeting `master`.**
Rationale: post-merge validation is the explicit request; PR validation gives the same signal before merge and is what makes the workflow useful as a gate. Alternative considered: `push` only - rejected because it reports regressions after they are already on `master`.

**2. One job, one Gradle invocation: `./gradlew :app:testDebugUnitTest ktlintCheck`.**
Rationale: a single Gradle configuration/setup costs less than two independent jobs, and both tasks share the same build cache. Alternative considered: separate jobs per check for parallel wall-clock time - rejected as over-engineering for a single-module project where each check finishes in minutes. Failure attribution stays clear because Gradle reports the failing task.

**3. `ubuntu-latest` runner with `actions/setup-java` (Temurin, Java 25).**
Rationale: GitHub-hosted Ubuntu images ship the Android SDK, so no custom image or SDK install step is required, and Java 25 matches `gradle/gradle-daemon-jvm.properties` so Gradle does not need to download a daemon JVM from Foojay. Alternative considered: JDK 21 + Gradle toolchain auto-provisioning - rejected because it adds a network dependency and an extra multi-hundred-MB download per run.

**4. Depend on the runner's preinstalled Android SDK; do not add an explicit SDK install step initially.**
Rationale: `ANDROID_HOME`/`ANDROID_SDK_ROOT` and accepted licenses are already present on hosted runners, and AGP downloads any missing platform package automatically. Adding `android-actions/setup-android` would duplicate that and slow the job. If the first run cannot resolve `compileSdk 37`, add that action as the documented fallback (see Risks), not preemptively.

**5. `gradle/actions/setup-gradle@v4` for Gradle caching instead of a hand-rolled `actions/cache` block.**
Rationale: it caches the Gradle user home (dependencies, wrappers, caches) with correct keying and also restores the configuration cache, which this project enables. Hand-rolled caching of `~/.gradle/caches` and `~/.gradle/wrapper` is the fallback if the action's behavior changes.

**6. `concurrency` keyed on workflow + ref with `cancel-in-progress: true`.**
Rationale: matches the "superseded runs are cancelled" requirement and avoids wasting runner minutes on stale commits. Keying on `github.ref` keeps PR and master runs from cancelling unrelated work.

**7. Publish diagnostics only on failure.**
Rationale: keeps successful runs lean while satisfying the requirement. Upload `app/build/reports/**` with `if-no-files-found: ignore` rather than hard-coding the AGP/ktlint report subdirectories, whose paths differ between plugin versions.

**8. Pin official actions to their current major tags (`@v4`/`@v5`) rather than commit SHAs.**
Rationale: consistency and low maintenance for a case-study repo; note that SHA pinning is the stricter supply-chain practice if this becomes a production repo.

## Risks / Trade-offs

- [`compileSdk 37` platform may not be preinstalled on the runner image] → AGP auto-downloads accepted-license platforms; if that fails, add a setup step (`android-actions/setup-android@v3` or `sdkmanager "platforms;android-37"`) before the Gradle step.
- [Java 25 may not be offered by `actions/setup-java` on some runner images] → if unavailable, install JDK 25 explicitly or relax the daemon toolchain; the job fails fast and visibly rather than silently.
- [Robolectric tests download android-all jars on first run, which can be slow or flaky] → Gradle/Robolectric caches are included in the Gradle cache restored by `setup-gradle`; retry the run if the download fails.
- [Configuration cache + KSP/AGP 9 workaround in `gradle.properties`] → the workflow runs the same wrapper and properties file as local builds, so CI exercises the identical configuration; a local `configuration-cache` failure would reproduce in CI.
- [Instrumented tests are not covered] → accepted: the JVM suite is the project's test strategy; adding emulator coverage is a separate change.
- [The workflow reports status but does not block merges] → documented as a non-goal; the maintainer can mark the check required once it is proven stable.

## Migration Plan

1. Add `.github/workflows/ci.yml` with the triggers and steps above.
2. Push to a feature branch and open a PR targeting `master`; confirm the run appears, both checks execute, and the failure path (a deliberately broken test or style violation) produces an artifact.
3. Merge to `master` and confirm the push run succeeds.
4. Rollback: deleting the workflow file stops all runs; it does not affect the app build.

## Open Questions

- None. Trigger scope, checks, and toolchain were confirmed with the requester; no decision here would change the specs or task breakdown.
