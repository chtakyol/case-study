# Spec Delta

## Purpose

Lets the app load, locally store, and observe the backend content document — pages with recursive
sections and text, image, and choice items — as typed domain models.

## ADDED Requirements

### Requirement: Fetch content document from the API

The system SHALL fetch the content document from a `GET pages` endpoint and deserialize it into the
data layer, including pages, recursive sections, and text, image, and choice items, preserving each
item's nesting and sibling order.

#### Scenario: Successful fetch

- **WHEN** the API returns a valid document of pages and nested items
- **THEN** every page, section, and item is deserialized without loss of nesting or order

#### Scenario: Unknown fields present

- **WHEN** the API response contains fields the data layer does not model
- **THEN** deserialization succeeds and the unknown fields are ignored

#### Scenario: Unrecognized item type

- **WHEN** an item declares a `type` the data layer does not recognize
- **THEN** that item is skipped and the rest of the document is still deserialized

### Requirement: Persist content locally

The system SHALL store the fetched document in local storage as the Single Source of Truth,
preserving parent-child nesting and sibling order, so later reads do not depend on the network.

#### Scenario: Content is written on refresh

- **WHEN** a refresh completes successfully
- **THEN** local storage contains exactly the fetched document

#### Scenario: Failed refresh preserves local content

- **WHEN** a refresh fails because the network is unavailable
- **THEN** previously stored content remains intact and readable

### Requirement: Expose content as domain models

The system SHALL expose stored content to callers as domain models through the repository, emitting
updates as the stored content changes. No wire or storage types SHALL cross the domain boundary.

#### Scenario: Reactive read

- **WHEN** the stored content changes
- **THEN** a caller observing the content receives the updated domain models

#### Scenario: No transport types leak

- **WHEN** a caller consumes content from the repository
- **THEN** only domain models are visible, never serialization or storage types

### Requirement: Report failures as a result

The system SHALL report network-backed operations through a result type carrying either success or
the failure cause, rather than letting exceptions escape the repository.

#### Scenario: Network error

- **WHEN** the network call throws (timeout, HTTP error, or I/O failure)
- **THEN** the refresh returns a failure result carrying the cause

### Requirement: Configurable content base URL

The system SHALL read the content API base URL from build configuration so each build type,
including tests, can target a different server.

#### Scenario: Test server redirection

- **WHEN** a test configures the base URL to a local mock server
- **THEN** the content API is called against that mock server

### Requirement: Debug builds serve local content

The system SHALL serve the content document in debug builds from a bundled local fixture without
requiring a reachable backend, and SHALL NOT use that fixture in release builds.

#### Scenario: Debug build without a backend

- **WHEN** a debug build triggers a content refresh and no backend is reachable
- **THEN** the fetch resolves from the bundled fixture and the document is stored locally

#### Scenario: Release build uses the configured server

- **WHEN** a release build triggers a content refresh
- **THEN** the fetch goes to the configured base URL and the bundled fixture is not used

### Requirement: Observe a single page

The system SHALL expose a single stored page by its identifier as a domain model, emitting updates as
that page's stored content changes, so callers need not observe or filter the whole document.

#### Scenario: Observe one page by id

- **WHEN** a caller observes a page by its id
- **THEN** it receives that page with its full nested item tree

#### Scenario: Unknown page id

- **WHEN** a caller observes an id that is not stored
- **THEN** the observed value is absent rather than an error

#### Scenario: Updates for that page only

- **WHEN** the stored content of the observed page changes
- **THEN** the caller receives the updated page
