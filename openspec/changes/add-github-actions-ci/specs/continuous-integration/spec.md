# Spec Delta

## Purpose

Automatically validates the Android project's JVM test suite and Kotlin style on changes headed to `master`, so regressions are caught by CI rather than discovered locally or after release.

## ADDED Requirements

### Requirement: Validation on pushes to master

The CI pipeline SHALL run automatically for every push to the `master` branch.

#### Scenario: Direct push to master starts validation
- **WHEN** a commit is pushed to `master`
- **THEN** a CI run is created for that commit and executes the project's validation checks

#### Scenario: Push to another branch does not start validation
- **WHEN** a commit is pushed to a branch other than `master` without a pull request targeting `master`
- **THEN** the CI pipeline does not create a run for that push

### Requirement: Validation on pull requests targeting master

The CI pipeline SHALL run automatically for pull requests whose target branch is `master`.

#### Scenario: Pull request opened against master
- **WHEN** a pull request targeting `master` is opened, reopened, or updated with a new commit
- **THEN** the CI pipeline creates a run for the pull request and reports its result on the pull request

#### Scenario: Pull request targeting another branch
- **WHEN** a pull request targets a branch other than `master`
- **THEN** the CI pipeline does not run for that pull request

### Requirement: JVM unit test execution

The CI pipeline SHALL execute the project's debug JVM unit test suite and SHALL fail when any test fails.

#### Scenario: All unit tests pass
- **WHEN** the unit test task completes with no test failures
- **THEN** the unit test check reports success

#### Scenario: A unit test fails
- **WHEN** at least one unit test fails or the test task errors
- **THEN** the unit test check reports failure and the CI run is marked failed

### Requirement: Kotlin style enforcement

The CI pipeline SHALL run the project's ktlint check task and SHALL fail when it reports style violations.

#### Scenario: Code is style compliant
- **WHEN** `ktlintCheck` completes without violations
- **THEN** the style check reports success

#### Scenario: Code violates ktlint rules
- **WHEN** `ktlintCheck` finds at least one violation
- **THEN** the style check reports failure and the CI run is marked failed

### Requirement: Failure diagnostics

The CI pipeline SHALL make test reports available for download when the validation job fails.

#### Scenario: Test failures are inspectable from the run
- **WHEN** a CI run fails because of unit test failures
- **THEN** the run publishes the generated test reports as a downloadable artifact

#### Scenario: Successful run does not require reports
- **WHEN** every validation check passes
- **THEN** publishing test reports is not required for the run to succeed

### Requirement: Superseded runs are cancelled

The CI pipeline SHALL cancel an in-progress run for the same branch or pull request when a newer run starts for that ref.

#### Scenario: New commit supersedes an in-progress run
- **WHEN** a new commit is pushed to a branch or pull request that already has a CI run in progress
- **THEN** the older run is cancelled and the newer run proceeds
