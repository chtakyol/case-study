# Tasks

## 1. Gradle, plugins, and build configuration

- [x] 1.1 Add versions, libraries, and plugins to `gradle/libs.versions.toml` (KSP, Hilt,
      kotlinx.serialization, Retrofit, OkHttp + logging interceptor, Retrofit kotlinx-serialization
      converter, Room, coroutines). Verify with `./gradlew help`.
- [x] 1.2 Apply the plugins in root `build.gradle.kts` and `app/build.gradle.kts` and add the
      dependencies. Verify with `./gradlew :app:dependencies` resolving without errors.
- [x] 1.3 Enable `buildFeatures { buildConfig = true }` and add a `BASE_URL` `buildConfigField` per
      build type. Verify the generated `BuildConfig.BASE_URL` exists after `./gradlew :app:assembleDebug`.
- [x] 1.4 Add the `INTERNET` permission to `app/src/main/AndroidManifest.xml` and point
      `android:name` at the new `Application` class. Verify the merged manifest
      (`app/build/intermediates/merged_manifests`) contains both.

## 2. Application and DI foundation

- [x] 2.1 Add `CaseStudyApplication` with `@HiltAndroidApp`, register it in the manifest, and
      annotate `MainActivity` with `@AndroidEntryPoint`. Verify `./gradlew :app:assembleDebug`
      succeeds and Hilt code generation runs.
- [x] 2.2 Add `di/DispatcherModule` with an `@IoDispatcher` qualifier providing `Dispatchers.IO`.
      Verify with a Hilt compilation test or by injecting it in a unit test.

## 3. Data — remote (DTOs, API, network module)

- [x] 3.1 Add `@Serializable` DTOs in `data/remote/dto`: `PageDto`, sealed `ContentItemDto`
      (`SectionDto`, `TextDto`, `ImageDto`, `ChoiceDto`), `ResponseSetDto`, `ResponseDto`, with
      `@JsonClassDiscriminator("type")` and `@SerialName` per variant. Verify with a unit test that
      deserializes the sample payload and asserts every variant is populated.
- [x] 3.2 Add the unrecognized-`type` fallback so one bad item does not fail the document. Verify
      with a unit test that an unknown type is skipped while the remaining items still parse.
- [x] 3.3 Add `ContentApi` (`@GET("pages") suspend fun getPages(): List<PageDto>`) and
      `di/NetworkModule` (Json with `ignoreUnknownKeys`, OkHttp client, Retrofit, `ContentApi`).
      Verify with a `MockWebServer` test that the service fetches and deserializes a served payload.

## 4. Data — local (Room SSOT)

- [x] 4.1 Add entities (`PageEntity`, `ContentItemEntity`, `ResponseSetEntity`, `ResponseEntity`)
      with FKs, indices, `parentId`, and `position`, plus `ContentDao` (observe document,
      transactional replace/clear). Verify with an in-memory Room test that writes then reads back
      preserving nesting and sibling order.
- [x] 4.2 Add `AppDatabase` and `di/DatabaseModule`. Verify the database builds and the DAO is
      provided (Hilt test or database smoke test on an in-memory instance).
- [x] 4.3 Add mappers DTO→entity and entity→domain. Verify with unit tests covering nested sections
      and a `null` response `score`.

## 5. Domain

- [x] 5.1 Add domain models in `domain/model` (`Page`, sealed `ContentItem` with `Section`, `Text`,
      `Image`, `Choice`, plus `ResponseSet` and `Response`). Verify they compile with no Android or
      data-layer imports and are exercised by the 4.3 mapping tests.
- [x] 5.2 Add the `Result` sealed type (Success/Error) if not already present. Verify with a unit
      test asserting `Success` holds data and `Error` carries a cause.
- [x] 5.3 Add the `ContentRepository` interface in `domain/repository`. Verify it references only
      domain models/`Result` and exposes `observePages(): Flow<List<Page>>` and
      `suspend fun refreshPages(): Result<Unit>`.
- [x] 5.4 Add `ObservePagesUseCase` and `RefreshPagesUseCase` in `domain/usecase`. Verify with unit
      tests using a fake repository.

## 6. Data — repository implementation

- [x] 6.1 Add `ContentRepositoryImpl` (SSOT read from the DAO; refresh calls the API, replaces
      content in one transaction, folds failures into `Result.Error`, runs on `@IoDispatcher`).
      Verify with a repository test using an in-memory Room database and a fake API: success writes
      the document, and a failing API leaves prior content intact and returns `Result.Error`.
- [x] 6.2 Add `di/RepositoryModule` binding `ContentRepositoryImpl` to `ContentRepository`. Verify
      Hilt compilation and an injection test resolving the interface.

## 7. Integration verification

- [x] 7.1 Add an end-to-end test: a fake interceptor serves the sample payload, a refresh runs, and
      `observePages()` emits the expected domain tree (nesting, order, and `choice` responses).
- [x] 7.2 Run `./gradlew ktlintCheck test` and confirm it passes clean.

## 8. Debug content fixture (dev-only data)

- [x] 8.1 Add the sample document as a fixture under `app/src/debug/assets/`. Verify a unit test
      deserializes it through the existing DTOs into the expected domain tree.
- [x] 8.2 Add an interceptor that answers `GET pages` from the fixture, install it only for debug
      builds through a debug-only DI binding, and gate it behind a debug `buildConfigField` flag.
      Verify a debug build with the placeholder `BASE_URL` loads the fixture and a release build does
      not use it.
- [x] 8.3 Verify the fixture path end-to-end: with the fixture interceptor installed, a refresh writes
      the document and `observePages()` emits the expected tree.

## 9. Single-page read

- [x] 9.1 Add page-scoped DAO queries (`observePage(id)` plus that page's items and reachable response
      sets/responses). Verify with an in-memory Room test that writes two pages and reads one back with
      its full tree.
- [x] 9.2 Add `ContentRepository.observePage(id): Flow<Page?>` reusing the tree mapper and an
      `ObservePageUseCase`. Verify with a repository test: a known id emits the page, an unknown id
      emits null, and a change to that page re-emits.

## Workflow follow-up

- Archive the change after the project's review requirements are satisfied.
- Verify the archived result and that `openspec/specs/content/spec.md` reflects the added requirements.
