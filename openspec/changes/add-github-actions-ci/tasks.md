# Tasks

## 1. Workflow triggers and run control

- [x] 1.1 Create `.github/workflows/ci.yml` with a workflow name, `on.push.branches: [master]`, and `on.pull_request.branches: [master]`.
- [x] 1.2 Add the `build` job running on `ubuntu-latest` with `timeout-minutes` and a `concurrency` block keyed on `${{ github.workflow }}-${{ github.ref }}` with `cancel-in-progress: true`.

## 2. Toolchain and validation steps

- [x] 2.1 Add `actions/checkout` and `actions/setup-java` (Temurin, Java 25) steps.
- [x] 2.2 Add `gradle/actions/setup-gradle` and a step running `./gradlew :app:testDebugUnitTest ktlintCheck`.

## 3. Failure diagnostics

- [x] 3.1 Add an `actions/upload-artifact` step with `if: failure()`, `path: app/build/reports/**`, and `if-no-files-found: ignore`.

## 4. Documentation

- [x] 4.1 Document the CI workflow in `README.md` (what runs, when it runs, and the local equivalents).

## Workflow follow-up

- Archive the change after review requirements are satisfied.
