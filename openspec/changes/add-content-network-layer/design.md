# Design

## Context

Single-module Android app (`:app`, package `com.cihatakyol.case_study`), greenfield apart from a
Compose scaffold. See `proposal.md` for motivation. The project architecture skill fixes the shape:
Clean layers `ui → domain ← data`, Room as the Single Source of Truth, Retrofit/OkHttp +
kotlinx.serialization, Hilt DI, and `Result` at the domain boundary. This change adds only the
`data` and `domain` layers.

The wire payload is a JSON array of `page` objects. Each page has `id`, `title`, and `items`. An
item has an `id` and a `type`:

- `text` → `content: String`
- `image` → `src: String`, `title: String`
- `choice` → `content: String`, `response_set: { id, multiple_selection, responses[] }`, where each
  response has `id`, `label`, and an optional (`null`-able) `score`
- `section` → `title: String`, and nested `items` (unbounded depth)

## Goals / Non-Goals

**Goals:**

- Model the payload faithfully in the data layer and expose it as clean domain models.
- Make the document queryable and observable through Room rather than a raw JSON blob.
- Keep every DTO and Room entity inside `data`.
- Make serialization, mapping, and repository behavior unit-testable with a fake server.

**Non-Goals:**

- Rendering, ViewModels, or navigation.
- Submitting choice responses back to the server (read-only for now).
- Pagination, auth, or a background sync strategy.

## Decisions

### Polymorphic items via a sealed DTO

Model items as a sealed `ContentItemDto` with `@JsonClassDiscriminator("type")` and one `@SerialName`
per variant (`section`, `text`, `image`, `choice`). This maps 1:1 to the wire discriminator and lets
each variant declare only its own fields (no nullable soup). Configure
`Json { ignoreUnknownKeys = true }`. A fallback handles an unrecognized `type` so one bad item does
not fail the whole document.

- *Alternative:* a single DTO with every field nullable — rejected: loses type safety and pushes
  null-checks into mapping.
- *Alternative:* a custom `JsonContentPolymorphicSerializer` keyed on `type` — rejected as more
  machinery than the discriminator needs, though it is the fallback mechanism if `@SerialName`
  registration proves brittle.

### Room schema: one item table with a self-reference

Use `pages`, `content_items`, `response_sets`, and `responses` tables. `content_items` carries a
`type`, nullable type-specific columns (`content`, `src`, `title`), a `pageId` FK, a nullable
`parentId` self-FK for nesting, and a `position` column for sibling order. Reads reassemble the tree
in the mapper.

- *Alternative:* `@Relation`-based page/section trees — rejected: nesting depth is unbounded and
  recursive relations are awkward in Room.
- *Alternative:* store the raw JSON body in Room — rejected: defeats the "queryable SSOT" goal.

### Repository is the SSOT

`ContentRepositoryImpl` exposes `observePages(): Flow<List<Page>>` (from the DAO) and
`refreshPages(): Result<Unit>` (calls the API, replaces local content in one transaction, folds
failures into `Result.Error`). Runs on a `@IoDispatcher`-qualified dispatcher. This keeps the UI that
follows free of network/DTO concerns and matches the skill's SSOT and UDF rules.

### Base URL from BuildConfig

Enable `buildFeatures { buildConfig = true }` and add a `BASE_URL` `buildConfigField` per build type;
`NetworkModule` reads `BuildConfig.BASE_URL`. Tests point at a mock server instead of editing code.

### Testing with a fake interceptor

Unit-test the Retrofit service with an OkHttp interceptor returning the sample payload (and a
`MockWebServer` variant) to assert deserialization and mapping. Repository tests use an in-memory
Room database plus a fake API for both success and failure paths.

### Debug-only fixture interceptor

The debug `BASE_URL` is a placeholder host, so a running debug build can't fetch anything. Rather than
commit a real URL, bundle a sample document under `app/src/debug/assets/` and add an OkHttp
`Interceptor` that answers `GET pages` from it. Install the interceptor only through the `debug` source
set (a debug-only DI binding that adds it to the client), so `main` stays backend-agnostic and release
builds are untouched. Gate it behind a debug `buildConfigField` flag so a developer can still exercise
the real HTTP path. `MockWebServer` stays a test-only tool; it cannot back a running app.

- *Alternative:* point `BASE_URL` at a real dev server — rejected: couples the repo to an external host
  and network availability.
- *Alternative:* ship a prebuilt Room database as the debug SSOT — rejected: bypasses the
  fetch-then-persist path this change exists to exercise.
- *Trade-off:* debug no longer exercises the real network by default → the flag restores it on demand.

### Single-page read

A page-detail screen observes one page by id, so the repository gains
`observePage(id): Flow<Page?>` backed by page-scoped DAO queries (the page row plus its items and the
response sets/responses reachable from that page). Reassembly reuses the existing tree mapper, scoped to
that page, and an `ObservePageUseCase` wraps it for the UI.

- *Alternative:* have the screen call `observePages()` and filter by id — rejected: couples every detail
  screen to the whole document and re-emits on unrelated page changes.
- *Trade-off:* a second read path to keep in sync → both whole-document and single-page reads reassemble
  through the same mapper function, and tests pin both.

## Risks / Trade-offs

- [Integer IDs from the wire are assumed unique across the document] → If the backend can reuse an
  ID across pages/sections, switch to composite keys (`pageId` + `id`); the self-referencing schema
  already carries `pageId` to make this cheap.
- [Single `content_items` table trades SQL type-safety for flexible nesting] → The Kotlin mapper
  restores type safety; a CHECK constraint on `type` bounds invalid rows.
- [Polymorphic deserialization can be brittle to `type` casing/values] → The unknown-type fallback
  keeps the document loadable, and tests pin the exact discriminators.
- [kotlinx.serialization/KSP/Gradle versions must stay compatible with Kotlin 2.2.10] → Pin versions
  in `libs.versions.toml` and let the build fail fast on mismatch.
- [The debug fixture masks the real HTTP path during development] → It is installed only in `debug` and
  gated behind a debug flag; release builds always use the configured base URL.
- [Single-page and whole-document reads can drift as the tree mapper evolves] → Both share one
  reassembly function, and tests cover each path.
