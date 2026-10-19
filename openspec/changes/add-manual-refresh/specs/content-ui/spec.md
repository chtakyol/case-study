# Spec Delta

## ADDED Requirements

### Requirement: Refresh content on demand

The page content screen SHALL provide a refresh action in its top app bar that requests fresh
content from the network, and SHALL keep that action available after a failed refresh so the user can
retry.

#### Scenario: Refresh action is available

- **WHEN** the page content screen is displayed with stored content
- **THEN** its top app bar shows a refresh action

#### Scenario: Manual refresh succeeds

- **WHEN** the user triggers the refresh action and the request succeeds
- **THEN** the displayed list reflects the newly stored content

#### Scenario: Manual refresh fails with content displayed

- **WHEN** the user triggers the refresh action and the request fails while content is displayed
- **THEN** the displayed content remains and a failure message is shown

#### Scenario: Manual refresh fails with nothing stored

- **WHEN** the user triggers the refresh action and the request fails while nothing is stored
- **THEN** the screen shows a failure message and the refresh action remains available

#### Scenario: Retry after a failed first load

- **WHEN** the screen shows a failure because nothing is stored and the user triggers the refresh
  action again
- **THEN** a new refresh is requested

### Requirement: Show refresh progress and prevent duplicate refreshes

While a refresh is in progress, the screen SHALL show progress in place of the refresh action and
SHALL NOT start another refresh.

#### Scenario: Progress while refreshing

- **WHEN** a refresh is in progress
- **THEN** the top app bar shows progress instead of the refresh action

#### Scenario: Repeated taps while refreshing

- **WHEN** a refresh is in progress and the user taps the refresh area again
- **THEN** no additional refresh is requested

#### Scenario: Progress clears when the refresh completes

- **WHEN** a refresh completes, whether it succeeded or failed
- **THEN** the refresh action is shown again instead of progress
