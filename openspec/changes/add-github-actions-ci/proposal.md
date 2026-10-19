# Proposal

## Why

The project has a meaningful JVM test suite and ktlint configuration, but nothing runs them automatically. Regressions can reach `master` whenever a contributor forgets to run the checks locally, and pull requests have no machine-verified signal before merge.

## What Changes

- Add a GitHub Actions workflow at `.github/workflows/ci.yml` that validates the project on every push to `master` and every pull request targeting `master`.
- Run two required checks in a single job:
  - `./gradlew :app:testDebugUnitTest` for the JVM/Robolectric test suite.
  - `./gradlew ktlintCheck` for Kotlin style.
- Provision a JDK matching the project's Gradle daemon toolchain (Java 25) and rely on the GitHub-hosted runner's Android SDK.
- Enable Gradle dependency/build caching and cancel superseded runs on the same branch or pull request.
- Upload test reports as a workflow artifact when the job fails, so failures are diagnosable without rerunning locally.
- No application source, runtime behavior, product dependencies, or Gradle build logic changes.

## Capabilities

### New Capabilities

- `continuous-integration`: Automated CI validation of the Android project on pushes to `master` and pull requests targeting `master`, covering unit tests and Kotlin style.

### Modified Capabilities

None. No existing specs exist in this project.

## Impact

- New file: `.github/workflows/ci.yml`.
- New CI-only tooling: GitHub Actions (`actions/checkout`, `actions/setup-java`, `gradle/actions/setup-gradle`, `actions/upload-artifact`).
- No changes to `app/`, Gradle configuration, dependencies, or app behavior.
- Branch protection rules that would make the workflow check required are repository settings outside this change; the workflow itself only reports status.
