---
name: Architecture
description: Architecture rules for this single-module Android app — Clean layers (Data/Domain/UI), package layout, repositories and SSOT, Room + DataStore persistence, Retrofit/OkHttp + kotlinx.serialization, Hilt DI, use cases, Navigation Compose, unidirectional state and error handling. Use when adding or changing features, layers, data sources, dependency injection, navigation, state holders, or error handling.
---

# Architecture

Opinionated architecture for this repository. Follow it for every feature. When a rule and a
convenience conflict, follow the rule.

This is a **single-module** app (module `:app`, package `com.cihatakyol.case_study`).

## Ground rules

1. **Single module.** All code lives in `app`.
2. **Clean architecture with three layers:** `data`, `domain`, `ui`.
3. **Dependency direction:** `ui → domain ← data`. `domain` depends on nothing. `data` depends on
   `domain`. `ui` depends on `domain` only — **never** on `data` internals.
4. **SSOT (Single Source of Truth) lives in the repository.** ViewModels never hold their own copy
   of remote data. The repository decides where data comes from and exposes it.
5. **UDF (Unidirectional Data Flow).** The UI observes state and emits events. The UI never mutates
   state directly.
6. **Events become state.** ViewModel output that affects the UI must be expressed as UI state
   updates — never a `Channel`/`SharedFlow` one-off event. State updates survive configuration
   changes and guarantee delivery.
7. **No business logic in Composables.** Composables are dumb: they render state and emit events.
8. **Offline-first.** Local storage is the source the UI reads from; the network refreshes it.

## Layer responsibilities — "where does this class go?"

| Artifact | Layer / package | Rules |
| --- | --- | --- |
| DTOs (`@Serializable`) | `data/remote` | Mirror the wire format exactly. Never leave this layer. |
| API service (Retrofit interface) | `data/remote` | HTTP only. No mapping, no business rules. |
| Room `@Entity` | `data/local/database` | Table shape + relations. Never leave this layer. |
| Room `@Dao` | `data/local/database` | Query/insert. `Flow` for observable reads. |
| Room `@Database` | `data/local/database` | Version, entities, converters. |
| DataStore keys + accessors | `data/local/preferences` | Preferences and simple flags **only**. |
| Repository implementation | `data/repository` | Implements a `domain` interface. Owns SSOT, caching, mapping, error→`Result`. |
| Repository interface | `domain/repository` | Pure Kotlin. Depends on `domain/model` only. |
| Domain model (`data class`) | `domain/model` | Pure Kotlin, no Android/framework types. |
| `Result` | `domain/model` (or `domain`) | Shared success/failure type crossing the domain boundary. |
| Use case | `domain/usecase` | One public responsibility per class. Orchestrates repositories. |
| `[Feature]Screen.kt` | `ui/[feature]` | Renders state, emits actions. No business logic. |
| `[Feature]ViewModel.kt` | `ui/[feature]` | Holds UI state, calls use cases, maps `Result` → UI state. |
| Feature components | `ui/[feature]/components` | Stateless, reusable within the feature. |
| Routes / `AppNavHost` | `ui/navigation` | Route definitions and the single `NavHost` host. |
| Theme | `ui/theme` | Color, Type, Shape, Theme. |
| Hilt modules | `di` | Split by concern: database, network, datastore, repository, dispatcher. |
| Extensions / helpers | `util` | Prefer extension functions over utility classes. |

**Forbidden:**
- `domain` importing anything from `data` or `ui`, or any Android/framework type.
- `ui` importing `data`. Screens/ViewModels talk to use cases, not repositories or DAOs.
- DTOs or Room entities leaking into `domain` or `ui`.
- Composables calling use cases, repositories, or DAOs directly.

## Package structure

```
com.cihatakyol.case_study
├── data
│   ├── local
│   │   ├── database        # Room database, DAOs, entities, mappers
│   │   └── preferences     # DataStore (Preferences), settings keys
│   ├── remote              # API service, DTOs, mappers
│   └── repository          # Repository implementations
├── domain
│   ├── model               # Domain models (not DTOs)
│   ├── repository          # Repository interfaces
│   └── usecase             # Use cases (one per responsibility)
├── ui
│   ├── navigation          # AppNavHost, route definitions
│   ├── [feature]
│   │   ├── [Feature]Screen.kt
│   │   ├── [Feature]ViewModel.kt
│   │   └── components
│   └── theme               # Color, Type, Shape, Theme
├── di                      # Hilt modules
└── util                    # Extensions, helpers
```

## Persistence split

Choose storage by the nature of the data — do not mix.

- **Room** → domain/business data: structured entities, relationships, queryable, observable.
- **DataStore (Preferences)** → user preferences and simple flags: theme, locale, onboarding state.

Never store domain models in DataStore. Never store preferences/flags in Room. If DataStore ever
needs structured data, that is a signal it belongs in Room.

## Data layer

### Room (business data)

Observable reads return `Flow`; the ViewModel collects them so the UI stays reactive to the SSOT.

```kotlin
@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: String,
    val title: String,
    val isFavorite: Boolean,
)

@Dao
interface ItemDao {
    @Query("SELECT * FROM items ORDER BY title")
    fun observeAll(): Flow<List<ItemEntity>>

    @Upsert
    suspend fun upsertAll(items: List<ItemEntity>)

    @Query("DELETE FROM items")
    suspend fun clear()
}
```

Map entities to domain models in `data/local/database` (or a dedicated mapper), never leak the
`Entity` upward.

### DataStore (preferences)

```kotlin
private val Context.dataStore by preferencesDataStore(name = "user_prefs")

object PreferenceKeys {
    val DARK_THEME = booleanPreferencesKey("dark_theme")
}

class UserPreferencesDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val darkTheme: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[PreferenceKeys.DARK_THEME] ?: false }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[PreferenceKeys.DARK_THEME] = enabled }
    }
}
```

### Networking (Retrofit / OkHttp / kotlinx.serialization)

- **Retrofit** for typed endpoints, **OkHttp** for the client (interceptors, timeouts, logging).
- **kotlinx.serialization** for JSON via `Json` with `ignoreUnknownKeys = true`. DTOs are
  `@Serializable`.
- Only `data/remote` knows about HTTP. DTOs never leave `data`.

```kotlin
@Serializable
data class ItemDto(val id: String, val title: String)

interface ItemApi {
    @GET("items")
    suspend fun getItems(): List<ItemDto>
}
```

### Repository — the SSOT

The repository implements a `domain` interface, owns caching, maps DTO/entity ↔ domain, and converts
failures into `Result` at the boundary. Remote data is never returned directly to the UI.

```kotlin
class ItemRepositoryImpl @Inject constructor(
    private val dao: ItemDao,
    private val api: ItemApi,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ItemRepository {

    // UI reads from the local SSOT.
    override fun observeItems(): Flow<List<Item>> =
        dao.observeAll().map { entities -> entities.map(ItemEntity::toDomain) }

    // Network refreshes the SSOT; failures surface as Result.
    override suspend fun refreshItems(): Result<Unit> = withContext(ioDispatcher) {
        runCatching { api.getItems() }
            .mapCatching { dtos -> dao.upsertAll(dtos.map(ItemDto::toEntity)) }
            .fold(
                onSuccess = { Result.Success(Unit) },
                onFailure = { cause -> Result.Error(cause) },
            )
    }
}
```

## Domain layer

- **Models:** `data class` (or `sealed` when there is a fixed set of variants). Pure Kotlin, no
  Android types.
- **Repository interfaces:** pure Kotlin, expressed in domain terms. Observable state as `Flow`,
  one-shot work as `suspend` returning `Result`.
- **Use cases are the standard unit of work.** Every feature capability the UI triggers goes through
  a use case — do not let ViewModels call repositories directly. One responsibility per class, named
  `VerbNounUseCase`. Use `operator fun invoke` so call sites read naturally. Injected via Hilt.

```kotlin
// domain/model/Result.kt
sealed interface Result<out T> {
    data class Success<T>(val data: T) : Result<T>
    data class Error(val cause: Throwable, val message: String? = null) : Result<Nothing>
}
```

```kotlin
// domain/repository/ItemRepository.kt
interface ItemRepository {
    fun observeItems(): Flow<List<Item>>
    suspend fun refreshItems(): Result<Unit>
}
```

```kotlin
// domain/usecase
class ObserveItemsUseCase @Inject constructor(
    private val repository: ItemRepository,
) {
    operator fun invoke(): Flow<List<Item>> = repository.observeItems()
}

class RefreshItemsUseCase @Inject constructor(
    private val repository: ItemRepository,
) {
    suspend operator fun invoke(): Result<Unit> = repository.refreshItems()
}
```

Naming: `Observe…UseCase` for streams, `Get…UseCase` for one-shot reads, `…UseCase` for actions
(`RefreshItemsUseCase`, `ToggleFavoriteUseCase`).

## UI layer

Every feature has a `[Feature]Screen.kt` (rendering) and `[Feature]ViewModel.kt` (state holder).

- **UI state** is a single `data class` per screen (`…UiState`). Prefer one state object over many
  independent `StateFlow`s.
- **Actions** are a `sealed interface` (`…Action`) when a screen has 3+ distinct user actions: the
  screen emits actions and the ViewModel reduces them through a single `onAction(action)` entry point.
  Below 3 actions, individual lambdas (`onBack`, `onItemClick`) are fine — see the `compose-ui` skill.
- **The ViewModel holds `StateFlow<…UiState>`**, built from use cases and coroutines scoped to
  `viewModelScope`.
- **`Result` is mapped to UI state in the ViewModel** — Composables never see `Result`, DTOs, or
  exceptions.

```kotlin
data class ItemsUiState(
    val isLoading: Boolean = false,
    val items: List<Item> = emptyList(),
    val errorMessage: String? = null,
)

sealed interface ItemsAction {
    data object Refresh : ItemsAction
    data object ErrorShown : ItemsAction
}

@HiltViewModel
class ItemsViewModel @Inject constructor(
    observeItems: ObserveItemsUseCase,
    private val refreshItems: RefreshItemsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ItemsUiState())
    val uiState: StateFlow<ItemsUiState> = _uiState.asStateFlow()

    private var initialized = false

    // Seed the state pipeline lazily and idempotently — do not start work in init/constructor.
    @MainThread
    fun initialize() {
        if (initialized) return
        initialized = true
        observeItems()
            .onEach { items -> _uiState.update { it.copy(items = items) } }
            .launchIn(viewModelScope)
    }

    fun onAction(action: ItemsAction) {
        when (action) {
            ItemsAction.Refresh -> refresh()
            ItemsAction.ErrorShown -> _uiState.update { it.copy(errorMessage = null) }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = refreshItems()) {
                is Result.Success -> _uiState.update { it.copy(isLoading = false) }
                is Result.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message ?: "Something went wrong")
                }
            }
        }
    }
}
```

The screen is dumb — a stateful entry point that wires the ViewModel to a stateless renderer:

```kotlin
@Composable
fun ItemsScreen(viewModel: ItemsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.initialize() }
    ItemsContent(uiState = uiState, onAction = viewModel::onAction)
}

@Composable
private fun ItemsContent(
    uiState: ItemsUiState,
    onAction: (ItemsAction) -> Unit,
) {
    // Render uiState. Emit onAction(...). No business logic.
}
```

Keep the stateless `…Content` composable public-parameter-only so it can be previewed and tested.

## Navigation

This app uses **Jetpack Navigation Compose** (`androidx.navigation:navigation-compose`): a single
`AppNavHost` hosts one `NavHost`, and each `composable(route = …) { }` block wires one screen.

### Routes

Declare one top-level `const val` per destination in `ui/navigation`. Group a feature's multiple
screens with a nested `navigation(startDestination = …)` graph.

```kotlin
// ui/navigation/Routes.kt
const val HOME_ROUTE = "home"
const val ITEM_DETAIL_ROUTE = "item_detail/{itemId}"

fun itemDetailRoute(itemId: String) = "item_detail/$itemId"
```

### Host

```kotlin
// ui/navigation/AppNavHost.kt
@Composable
fun AppNavHost(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = HOME_ROUTE,
        modifier = modifier,
    ) {
        composable(route = HOME_ROUTE) {
            HomeScreen(
                viewModel = hiltViewModel(),
                onItemClick = { item -> navController.navigate(itemDetailRoute(item.id)) },
            )
        }

        composable(
            route = ITEM_DETAIL_ROUTE,
            arguments = listOf(navArgument("itemId") { type = NavType.StringType }),
        ) {
            ItemDetailScreen(
                viewModel = hiltViewModel(),
                onBack = { navController.popBackStack() },
            )
        }
    }
}
```

Wired up in `MainActivity`:

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CasestudyTheme {
                AppNavHost()
            }
        }
    }
}
```

### Passing an argument into a ViewModel

The `composable` block resolves the ViewModel with `hiltViewModel()`; nav arguments reach it through
`SavedStateHandle` — no need to thread them through by hand.

```kotlin
@HiltViewModel
class ItemDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    observeItem: ObserveItemUseCase,
) : ViewModel() {

    private val itemId: String = checkNotNull(savedStateHandle.get<String>("itemId"))

    // ...
}
```

Rules:
- The `composable(route = …) { }` block is the **only** place a ViewModel is resolved and the
  `NavController` is touched.
- Screens receive **callbacks** (`onItemClick`, `onBack`) — never the `NavController` or a raw route
  string; screens do not navigate themselves.
- UI-initiated navigation (button tap) is a plain callback invocation — guard it with
  `dropUnlessResumed { … }` so it only runs while the lifecycle is at least `RESUMED`.
- Business-logic-gated navigation (validate before advancing) is modeled as **UI state**; the Screen
  observes the state and navigates when it flips (see [State & events](#state--events)).
- If the destination stays on the back stack, hold the "already navigated" flag in the UI
  (`rememberSaveable`) so it doesn't advance repeatedly.

## State & events

One-time events (snackbars, toasts, navigation triggers) are modeled as **state**, consumed by the
UI — not emitted through a `Channel` or a fire-and-forget `SharedFlow`.

```kotlin
data class FormUiState(
    val isSaved: Boolean = false,
    val message: String? = null,
) {
    // Model the event as state; the UI acknowledges it so it is delivered exactly once.
}

fun onSaved() = _uiState.update { it.copy(isSaved = true) }
fun onSavedHandled() = _uiState.update { it.copy(isSaved = false) }
```

```kotlin
val uiState by viewModel.uiState.collectAsStateWithLifecycle()

LaunchedEffect(uiState.isSaved) {
    if (uiState.isSaved) {
        navigateBack()
        viewModel.onSavedHandled() // acknowledge, so it survives config change and isn't replayed
    }
}
```

Why not `Channel`/`SharedFlow`: one-off events are lost on configuration change and can be dropped
under backpressure. State-based delivery survives both and keeps the UDF contract intact.

## Dependency injection — Hilt

- `@HiltAndroidApp` on the `Application` class.
- `@AndroidEntryPoint` on `MainActivity` (and any other Android entry point).
- `@HiltViewModel` + `@Inject constructor` on ViewModels; resolve with `hiltViewModel()`.
- Modules live in `di`, one per concern, installed in `SingletonComponent` unless a narrower scope is
  genuinely required.

```kotlin
@HiltAndroidApp
class CaseStudyApplication : Application()
```

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "app.db").build()

    @Provides
    fun provideItemDao(db: AppDatabase): ItemDao = db.itemDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds @Singleton
    abstract fun bindItemRepository(impl: ItemRepositoryImpl): ItemRepository
}

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder().build()

    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.BASE_URL)
        .client(client)
        .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides @Singleton
    fun provideItemApi(retrofit: Retrofit): ItemApi = retrofit.create(ItemApi::class.java)
}
```

Qualifiers for dispatchers keep threads swappable in tests:

```kotlin
@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object DispatcherModule {
    @Provides @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO
}
```

## Kotlin conventions

- Prefer `data class` for models and UI state; `sealed` (`sealed interface`) for UI state variants and
  actions.
- Use `object` for singletons and stateless factories; `data object` for actions and similar markers
  with no fields.
- Scope coroutines to `viewModelScope` in ViewModels and `lifecycleScope` in Activities/Fragments.
  Never launch an unstructured `GlobalScope` coroutine.
- **Favour extension functions over utility classes.** Put them in `util`.
- Default to `val` and immutable data; expose `StateFlow`, keep `MutableStateFlow` private.
- Keep public Kotlin APIs explicit about nullability and suspend/Flow.

## Gradle setup

> **Versions:** the numbers below are the shapes/coordinates to add. Pin each to the latest stable
> release compatible with the Kotlin version in `gradle/libs.versions.toml` (KSP must match Kotlin;
> Room and Hilt need KSP).

`gradle/libs.versions.toml`:

```toml
[versions]
ksp = "2.2.10-2.0.2"                       # KSP version must match the Kotlin version
room = "2.8.0"
datastore = "1.2.0"
retrofit = "3.0.0"
okhttp = "5.2.0"
kotlinxSerialization = "1.9.0"
kotlinxCoroutines = "1.10.2"
hilt = "2.57"
hiltNavigationCompose = "1.3.0"
navigationCompose = "2.9.0"

[libraries]
androidx-room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
androidx-room-ktx = { module = "androidx.room:room-ktx", version.ref = "room" }
androidx-room-compiler = { module = "androidx.room:room-compiler", version.ref = "room" }
androidx-datastore-preferences = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
retrofit = { module = "com.squareup.retrofit2:retrofit", version.ref = "retrofit" }
retrofit-kotlinx-serialization = { module = "com.squareup.retrofit2:converter-kotlinx-serialization", version.ref = "retrofit" }
okhttp = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
okhttp-logging = { module = "com.squareup.okhttp3:logging-interceptor", version.ref = "okhttp" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinxSerialization" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "kotlinxCoroutines" }
hilt-android = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler = { module = "com.google.dagger:hilt-android-compiler", version.ref = "hilt" }
androidx-hilt-navigation-compose = { module = "androidx.hilt:hilt-navigation-compose", version.ref = "hiltNavigationCompose" }
androidx-navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigationCompose" }

[plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

Root `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
```

`app/build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    // ...
}

dependencies {
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.navigation.compose)
}
```

## Do / Don't

| Do | Don't |
| --- | --- |
| Keep one module, three layers. | Add feature/core Gradle modules without an explicit project decision. |
| Route UI work through use cases. | Call repositories or DAOs from ViewModels or Composables. |
| Keep the repository as SSOT. | Cache a second copy of remote data in the ViewModel. |
| Model one-time events as consumable state. | Fire one-off events through `Channel`/`SharedFlow`. |
| Store business data in Room, prefs/flags in DataStore. | Store domain models in DataStore or preferences in Room. |
| Keep Composables dumb (render + emit). | Put business logic or data access in Composables. |
| Map DTO/entity → domain at the repository boundary. | Leak DTOs or Room entities into `domain`/`ui`. |
| Use `data class`/`sealed`/`object` deliberately. | Use classes where a `data class` or `object` fits. |
| Scope coroutines to `viewModelScope`/`lifecycleScope`. | Launch unstructured `GlobalScope` coroutines. |
| Resolve ViewModels and wire the `NavController` only in `AppNavHost`. | Pass the `NavController` or raw route strings into screens. |
| Prefer extension functions in `util`. | Create utility classes for one-off helpers. |

## Boundaries with the other skills

- **architecture** (this skill): layers, package layout, repositories/SSOT, persistence choice, DI,
  use cases, state ownership, navigation structure and ViewModel wiring.
- **compose-ui**: rendering, theming, previews, accessibility, component design.
- **testing**: test strategy, fakes/in-memory sources, ViewModel/repository tests, Compose UI tests.

Load the relevant skill for the task at hand; they are complementary, not overlapping.
