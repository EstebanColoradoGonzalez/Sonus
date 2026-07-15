# Refinamiento Técnico (Developer)

**Autor**: Esteban Colorado González | **Fecha**: 2026-07-11

## Plan: Escaneo fundacional y transición a la biblioteca (US-003)

**Arquitectura**: Arquitectura Limpia multi-módulo (Presentación → Dominio ← Datos),
MVVM + Single-Activity + Jetpack Compose + Hilt, 100% local (air-gapped). US-003 materializa el
**Motor de Biblioteca** (contenedor **C-04**): construye el Catálogo (`Track` + dimensiones) a
partir del recorrido recursivo SAF de las Carpetas Fuente, extrae etiquetas ID3 sin inventar datos
(Invariante 4) y reporta progreso determinista. Introduce las **primeras dependencias de fondo**
del proyecto: **WorkManager** (`CoroutineWorker`, ADR-006), la **extracción de metadatos** y el
resto del **esquema Room** (Track/Artist/Album/Genre + centinelas `id=1`, migración 1→2).

> **Feature análoga leída completa**: **US-002** (commit `a3da2ca`). Patrón *thin-slice*
> vertical a través de las capas: puertos en `:core:domain` → impls en `:core:data` →
> casos de uso en `:core:domain` → `ViewModel` + `Screen` en un `:feature:*` → cableado Hilt
> en `:app` → tests JUnit5/MockK/Turbine + Room instrumentado + Konsist + autarquía. US-003
> replica esa disposición y añade: (1) el módulo de servicio `:service:indexer` (Worker +
> scheduler WorkManager), (2) la presentación en `:feature:library`, (3) el resto del esquema
> Room con migración y siembra de centinelas.

### Decisiones técnicas clave (a validar con el usuario)

1. **Extracción ID3 → `android.media.MediaMetadataRetriever`** (plataforma, cero dependencias de
   terceros, air-gapped por construcción — ADR-010, no requiere auditar librería externa). Cubre el
   alcance de **solo lectura** de US-003 (título, artista, álbum, género, nº pista, año, duración,
   presencia de carátula embebida). **No** es `MediaStore` (prohibido por `NoMediaStoreTest`); es API
   de decodificación local. La **escritura** de etiquetas (`TRG-META-01`) se difiere y necesitará una
   librería con capacidad de *write* en su historia. Alternativa descartada por ahora: empaquetar
   JAudioTagger — mayor superficie a auditar sin beneficio para US-003.
2. **Escaneo vía WorkManager `CoroutineWorker` + single-flight `ExistingWorkPolicy.KEEP`**
   (mandato ADR-006 / contract §4.1). Se introduce integración **Hilt-Worker**
   (`@HiltWorker` + `HiltWorkerFactory`, `SonusApplication : Configuration.Provider`).
3. **Progreso observable** mediante un **`ScanStateEmitter` singleton** (contract C2): la capa de
   datos publica `Scanning`/`Syncing`; el caso de uso publica el estado terminal `Finished`/`Aborted`;
   la presentación observa el mismo `StateFlow<ScanState>` a través de `ObserveScanStateUseCase`.
4. **Destino "biblioteca principal"**: la vista real de biblioteca pertenece a EPICs posteriores. El
   alcance de US-003 termina en la **transición**; se implementa un **landing placeholder** en
   `:feature:library` como destino de la transición, claramente marcado como provisional.
5. **Dispatchers inyectados** (`DispatcherProvider`, coding-standards §5): la I/O de escaneo corre en
   `io` inyectable para determinismo en pruebas; nunca `Dispatchers.IO` fijo.

### Decisión de flujo (mapa criterios → diseño)

| AC | Escenario | Punto de enforcement |
|----|-----------|----------------------|
| 1 | Escaneo exitoso con progreso | `ScanScreen` (init) → `StartLibraryScanUseCase` → `WorkManagerScanScheduler.enqueueFullScan()` (KEEP) → `LibraryScanWorker` ejecuta `ScanLibraryUseCase(Scan(FULL))`; `ScanStateEmitter` publica `Scanning(processed,total)`→`Syncing`→`Finished(summary)`; `ScanViewModel` observa y emite `NavigateToLibrary` |
| 2 | Escaneo rápido (< 1 s) | El `ScanState` transita a `Finished` sin permanencia perceptible en `Scanning`; `ScanViewModel` navega directo a la biblioteca (la UI no fuerza una pantalla intermedia mínima) |
| 3 | Metadatos ausentes/incompletos | `Id3DataSource` devuelve `null` en nombres ausentes; `CatalogSynchronizer` resuelve dimensión ausente al **centinela `id=1`** (Artist/Album/Genre) y `title` a `NULL`; **nunca** infiere (Invariante 4). Etiqueta localizada solo en presentación |
| 4 | Formato no soportado | `Id3DataSource` marca el archivo indecodificable como `TrackAvailability.UNSUPPORTED`; se indexa igual, cuenta en `ScanSummary.unsupported`; el escaneo continúa (P1 / [Restricción 2]) |
| 5 | Carpeta inaccesible / permiso revocado | `SafDataSource`/`Id3DataSource` lanzan `SecurityException`/IO → capturado en el borde (`CatalogRepositoryImpl`) → `Failure(DomainError.ScanAborted(PermissionRevoked))`; `ScanState.Aborted`; **no** se purga lo ya indexado; `ScanViewModel` ofrece volver a Carpetas Fuente / reintentar |
| 6 | Cancelación por el Oyente | `ScanCommand.CancelScan` → `CancelLibraryScanUseCase` → `WorkManagerScanScheduler.cancel()`; el `CoroutineWorker` se detiene limpio (cooperativo con `isActive`); el Catálogo parcial se conserva (transacción por lotes); permite reintentar |
| 7 | Autarquía (sin red ni media runtime) | Único acceso: SAF sobre `treeUri` persistido; `MediaMetadataRetriever` (no `MediaStore`, no permisos media runtime, no `INTERNET`); carátulas **no** persistidas (`hasEmbeddedArtwork` boolean). Verificado por `AutarkyManifestTest` + `NoMediaStoreTest` (existentes) |

### Fronteras de alcance (lo que NO entra en US-003)

- **Marcar `onboardingCompleted` / gating de arranque**: `US-004`.
- **Re-escaneo manual posterior**: `US-007`. **Sincronización incremental** (`ScanMode.INCREMENTAL`
  real con diff por `fileLastModifiedMs`): `US-008` — aquí solo `ScanMode.FULL` fundacional (Catálogo
  vacío, sin estado previo que comparar; `purged`=0).
- **Vista de biblioteca navegable/reproducible y progreso desde biblioteca**: `US-009` y EPIC-02+.
  US-003 entrega un **landing placeholder** como destino de transición.
- **Escritura de etiquetas ID3, playlists, cola, reproducción, singletons `PlaybackState`/
  `AppSettings`**: sus historias. Solo se crean las tablas `track`/`artist`/`album`/`genre` y sus
  centinelas.

---

## Tareas de Implementación

### Fase 0 — Tooling y dependencias (WorkManager + Hilt-Worker)

- [ ] **T1: Ampliar `gradle/libs.versions.toml`** — WorkManager (`androidx.work:work-runtime-ktx`),
  Hilt-Worker (`androidx.hilt:hilt-work` + `androidx.hilt:hilt-compiler` como `ksp`); versiones
  `work` y `androidxHilt` — `Sonus/gradle/libs.versions.toml`
- [ ] **T2: `:service:indexer` build** — añadir plugins `ksp` + `hilt`; deps `project(:core:domain)`,
  `project(:core:data)` (presentes), `hilt.android` + `ksp(hilt.compiler)`, `hilt-work` +
  `ksp(androidx-hilt-compiler)`, `work-runtime-ktx`, `kotlinx.coroutines.core`; test (junit5,
  mockk, coroutines-test, truth) + `testOptions { unitTests.all { useJUnitPlatform() } }` —
  `Sonus/service/indexer/build.gradle.kts`
- [ ] **T3: `:feature:library` build** — añadir plugins `kotlin.compose` + `ksp` + `hilt`;
  `buildFeatures { compose = true }`; deps compose (bom, ui, ui-graphics, ui-tooling-preview,
  material3, lifecycle-viewmodel-compose, hilt-navigation-compose), `hilt.android` +
  `ksp(hilt.compiler)`, `debugImplementation(ui-tooling)`; test (junit5, mockk, turbine, truth) +
  `useJUnitPlatform()` — `Sonus/feature/library/build.gradle.kts`
- [ ] **T4: `:app` build** — añadir `project(:feature:library)` y `project(:service:indexer)`;
  `work-runtime-ktx` + `androidx.hilt:hilt-work` para `HiltWorkerFactory` — `Sonus/app/build.gradle.kts`

### Fase 1 — Dominio · Contratos base (extender `:core:domain`)

- [ ] **T5: Extender `DomainError`** — añadir `PermissionRevoked` (`ERR_PERMISSION_REVOKED`, WARNING,
  recoverable=true, `details=null`) y `ScanAborted(cause: DomainError)` (`ERR_SCAN_ABORTED`, WARNING,
  recoverable=true, `details=ErrorDetails.Cause(cause)`) (Base: `interfaces_contract` §3.2; `ErrorDetails.Cause`
  ya existe) — `.../core/domain/error/DomainError.kt`

### Fase 2 — Dominio · Modelo, estado y comando de escaneo (`:core:domain`)

- [ ] **T6: Enums de dominio** — `ContentType { MUSIC, PODCAST, UNKNOWN }`,
  `TrackAvailability { AVAILABLE, UNSUPPORTED, MISSING }` (Base: `domain_and_state_model` §4;
  serialización por nombre) — `.../core/domain/model/ContentType.kt`, `TrackAvailability.kt`
- [ ] **T7: `TrackId` + modelo `ScannedTrack`** — `typealias TrackId = Long`; `data class ScannedTrack`
  puro con nombres de dimensión (no FKs): `uri: String`, `title: String?`, `artistName: String?`,
  `albumName: String?`, `genreName: String?`, `contentType: ContentType`, `trackNumber: Int?`,
  `releaseYear: Int?`, `durationMs: Long`, `hasEmbeddedArtwork: Boolean`, `availability:
  TrackAvailability`, `sourceFolderId: FolderId`, `fileLastModifiedMs: Long` (la resolución
  nombre→`id` centinela/FK es responsabilidad de Datos, Invariante 4) (Base: `domain_and_state_model`
  §2) — `.../core/domain/model/TrackId.kt`, `ScannedTrack.kt`
- [ ] **T8: `ScanMode` + `ScanSummary`** — `enum ScanMode { FULL, INCREMENTAL }`;
  `data class ScanSummary(added, purged, unsupported, orphanDimsPurged: Int)` (Base: `interfaces_contract`
  §2.1 `TRG-LIB-03`) — `.../core/domain/model/ScanMode.kt`, `ScanSummary.kt`
- [ ] **T9: `ScanState`** — `sealed interface`: `Idle`, `Scanning(processed: Int, total: Int?)`,
  `Syncing`, `Finished(summary: ScanSummary)`, `Aborted(reason: DomainError)` (Base:
  `interfaces_contract` §2.1 `TRG-LIB-04`; sufijo `State` en dominio, coding-standards §2.3) —
  `.../core/domain/model/ScanState.kt`
- [ ] **T10: Extender `LibraryCommand`** — añadir `data class Scan(val mode: ScanMode =
  ScanMode.INCREMENTAL)` (Base: `interfaces_contract` §2.1 `TRG-LIB-03`) —
  `.../core/domain/command/LibraryCommand.kt`

### Fase 3 — Dominio · Puertos (`:core:domain`)

- [ ] **T11: Puerto `CatalogRepository`** — `suspend fun synchronize(sourceFolders:
  List<SourceFolder>, mode: ScanMode): OperationResult<ScanSummary>` (descubre+extrae+sincroniza en
  Datos; publica `Scanning`/`Syncing` vía emitter; `Failure(ScanAborted)` ante acceso perdido)
  (Base: coding-standards §2.1 `Catalog`; blueprint C-04) — `.../core/domain/port/CatalogRepository.kt`
- [ ] **T12: Puerto `ScanStateEmitter`** — `val state: StateFlow<ScanState>`; `suspend fun
  update(state: ScanState)` (canal C2; `kotlinx` permitido en dominio) —
  `.../core/domain/port/ScanStateEmitter.kt`
- [ ] **T13: Puerto `ScanScheduler`** — `fun enqueueFullScan()`; `fun cancel()` (abstrae WorkManager;
  single-flight vive en la impl) — `.../core/domain/port/ScanScheduler.kt`
- [ ] **T14: Puerto `DispatcherProvider`** — `val io: CoroutineDispatcher`; `val default:
  CoroutineDispatcher` (dispatchers inyectables, coding-standards §5) —
  `.../core/domain/port/DispatcherProvider.kt`

### Fase 4 — Dominio · Casos de uso (`:core:domain`)

- [ ] **T15: `ScanLibraryUseCase`** — deps `SourceFolderRepository`, `CatalogRepository`,
  `ScanStateEmitter`. `suspend operator fun invoke(command: LibraryCommand.Scan):
  OperationResult<ScanSummary>`: emite `Scanning(0,null)`; `folders = sourceFolderRepository.observeAll().first()`;
  `res = catalog.synchronize(folders, command.mode)`; `Success` → emite `Finished(summary)`; `Failure`
  → emite `Aborted(error)`; retorna `res` (AC1/3/4/5) — `.../core/domain/usecase/ScanLibraryUseCase.kt`
- [ ] **T16: `ObserveScanStateUseCase`** — `operator fun invoke(): StateFlow<ScanState> =
  emitter.state` (AC1/2/5/6) — `.../core/domain/usecase/ObserveScanStateUseCase.kt`
- [ ] **T17: `StartLibraryScanUseCase`** — `operator fun invoke() = scanScheduler.enqueueFullScan()`
  (AC1) — `.../core/domain/usecase/StartLibraryScanUseCase.kt`
- [ ] **T18: `CancelLibraryScanUseCase`** — `operator fun invoke() = scanScheduler.cancel()` (AC6) —
  `.../core/domain/usecase/CancelLibraryScanUseCase.kt`

### Fase 5 — Datos · Esquema Room, SAF, ID3 (`:core:data`)

- [ ] **T19: `@Entity` del Catálogo** — `Artist` (`artist`, `Index(name, unique)`), `Genre` (`genre`,
  `Index(name, unique)`), `Album` (`album`, `Index([name,artistId], unique)` + `Index(artistId)`,
  FK Artist RESTRICT), `Track` (`track`, `Index(uri, unique)` + índices FK, FKs Artist/Album/Genre
  RESTRICT y SourceFolder CASCADE) con TODOS los campos y tipos exactos (Base: `domain_and_state_model`
  §2/§3) — `.../core/data/local/room/entity/{Artist,Genre,Album,Track}.kt`
- [ ] **T20: DAOs del Catálogo** — `ArtistDao`/`GenreDao`/`AlbumDao` (get-or-create: `insert(OnConflict
  IGNORE)` + `selectIdBy…`), `TrackDao` (`upsertByUri`, `selectAllUrisWithMtime`, `deleteByUriNotIn`,
  `countByAvailability`) (Base: `SourceFolderDao`) — `.../core/data/local/room/dao/{Artist,Genre,Album,Track}Dao.kt`
- [ ] **T21: `SonusDatabase` v2** — `entities=[SourceFolder, Artist, Genre, Album, Track]`, `version=2`;
  `abstract fun` de los 4 DAOs nuevos (Base: existente v1) — `.../core/data/local/room/SonusDatabase.kt`
- [ ] **T22: `Migrations` (1→2) + siembra** — `MIGRATION_1_2`: `CREATE TABLE` de artist/genre/album/track
  (índices+FKs idénticos al esquema generado) + `INSERT` centinelas `artist(1,'')`, `genre(1,'')`,
  `album(1,'',1)`; `RoomDatabase.Callback.onCreate` siembra los mismos centinelas en instalaciones
  nuevas (Base: `domain_and_state_model` §6.1) — `.../core/data/local/room/migration/Migrations.kt`
- [ ] **T23: Esquema exportado `2.json`** — generado por KSP al compilar; la SQL de `MIGRATION_1_2`
  debe coincidir con el esquema — `Sonus/core/data/schemas/…SonusDatabase/2.json`
- [ ] **T24: Contrato `SafDataSource` (interface) + `SafDataSourceImpl`** — `suspend fun
  listAudioFiles(treeUri: String): List<DiscoveredFile>` (recorrido recursivo con
  `DocumentsContract.buildChildDocumentsUriUsingTree`, sin `MediaStore`); `DiscoveredFile(uri, mimeType,
  lastModifiedMs)`; `SecurityException` propagada al borde (Base: `SafPermissionGatewayImpl`, ADR-003) —
  `.../core/data/local/saf/SafDataSource.kt`, `SafDataSourceImpl.kt`
- [ ] **T25: Contrato `Id3DataSource` (interface) + `Id3DataSourceImpl`** — `fun readMetadata(uri:
  String, mimeType: String): RawTrackMetadata` con `android.media.MediaMetadataRetriever`; archivo
  indecodificable → `availability=UNSUPPORTED`; nombres ausentes → `null` (sin inventar) (Base: ADR-003,
  Invariante 4) — `.../core/data/id3/Id3DataSource.kt`, `Id3DataSourceImpl.kt`
- [ ] **T26: `CatalogSynchronizer`** — colaborador (sufijo `Synchronizer`) que, dentro de una
  **transacción Room** (`SonusDatabase.withTransaction`): resuelve/crea dimensiones (get-or-create,
  ausente→`id=1`), da de alta/actualiza `Track` por `uri`, marca/purga `MISSING` (FULL: 0), y ejecuta
  la **purga de dimensiones huérfanas** (`id != 1`, §6.2); devuelve conteos de `ScanSummary` (Base:
  `domain_and_state_model` §6.2) — `.../core/data/local/room/CatalogSynchronizer.kt`
- [ ] **T27: `CatalogRepositoryImpl`** — implementa `CatalogRepository` sobre `dispatcherProvider.io`:
  por carpeta `saf.listAudioFiles` → por archivo `id3.readMetadata` → `ScannedTrack`, publicando
  `ScanState.Scanning(processed,total)` vía `ScanStateEmitter`; emite `Syncing`; delega en
  `CatalogSynchronizer`; captura `SecurityException`/IO en el borde → `Failure(ScanAborted(PermissionRevoked))`
  (P1) — `.../core/data/repository/CatalogRepositoryImpl.kt`
- [ ] **T28: `ScanStateEmitterImpl`** — `@Singleton`; `MutableStateFlow(ScanState.Idle)` privado
  expuesto como `StateFlow`; `update` con asignación atómica — `.../core/data/scan/ScanStateEmitterImpl.kt`
- [ ] **T29: `DefaultDispatcherProvider`** — implementa `DispatcherProvider` con `Dispatchers.IO/Default`
  (impl role-named, como `SystemTimeProvider`) — `.../core/data/time/DefaultDispatcherProvider.kt`
- [ ] **T30: `CatalogMappers`** — funciones puras: `RawTrackMetadata`→`ScannedTrack`, `ScannedTrack`→
  `Track` entity (con FKs resueltos), entidades↔dominio — `.../core/data/mapper/CatalogMappers.kt`

### Fase 6 — Servicio · WorkManager (`:service:indexer`)

- [ ] **T31: `LibraryScanWorker`** — `@HiltWorker class … @AssistedInject (@Assisted context,
  @Assisted params, scanLibraryUseCase)` : `CoroutineWorker`; `doWork()` = `invoke(LibraryCommand.Scan(
  ScanMode.FULL))` → `Result.success()`/`Result.failure()` según `OperationResult` (Base: ADR-006) —
  `.../service/indexer/LibraryScanWorker.kt`
- [ ] **T32: `WorkManagerScanScheduler`** — implementa `ScanScheduler`; `enqueueFullScan()` =
  `enqueueUniqueWork("library_scan", ExistingWorkPolicy.KEEP, OneTimeWorkRequestBuilder<LibraryScanWorker>())`
  (single-flight); `cancel()` = `cancelUniqueWork("library_scan")` (impl role-named, no sufijo `Impl`
  — vive en servicio, referencia al Worker) (Base: contract §4.1) —
  `.../service/indexer/WorkManagerScanScheduler.kt`

### Fase 7 — Presentación (`:feature:library` · `presentation/scan`)

- [ ] **T33: `ScanUiState`** — `data class` inmutable derivado de `ScanState`: `status`
  (`ScanStatus { STARTING, SCANNING, SYNCING, FINISHED, ABORTED }`), `processed: Int`, `total: Int?`,
  `summary: ScanSummaryUi?`; `ScanSummaryUi(added, unsupported, orphanDimsPurged)` — `.../feature/library/presentation/scan/ScanUiState.kt`
- [ ] **T34: `ScanCommand` + `ScanEvent`** — `ScanCommand { StartScan, CancelScan }`;
  `ScanEvent` one-shot `{ NavigateToLibrary, NavigateBackToSourceFolders, NotifyAborted(code:
  String), NotifyCancelled }` — `.../feature/library/presentation/scan/ScanCommand.kt`, `ScanEvent.kt`
- [ ] **T35: `ScanViewModel`** — `@HiltViewModel`; deps `ObserveScanStateUseCase`,
  `StartLibraryScanUseCase`, `CancelLibraryScanUseCase`; en `init` dispara `StartScan` y colecta
  `ObserveScanStateUseCase()` mapeando a `ScanUiState`; `Finished`→`NavigateToLibrary`;
  `Aborted`→`NotifyAborted(reason.code)`; `onCommand` `when` exhaustivo (Base: `SourceFoldersViewModel`)
  — `.../feature/library/presentation/scan/ScanViewModel.kt`
- [ ] **T36: `ScanScreen`** — `@Composable`; título; indicador determinista (`processed/total`,
  indeterminado si `total==null`); subtítulo de fase; botón "Cancelar"; en `FINISHED` resumen breve;
  `SnackbarHost` para `Aborted`/`Cancelled` con acción volver a Carpetas Fuente (Base: `SourceFoldersScreen`,
  `US-003.preview.md`) — `.../feature/library/presentation/scan/ScanScreen.kt`
- [ ] **T37: `LibraryLandingScreen` (placeholder de transición)** — `@Composable` mínimo, destino de
  `NavigateToLibrary`; marcado como provisional (vista real en EPIC-02+) —
  `.../feature/library/presentation/LibraryLandingScreen.kt`
- [ ] **T38: Recursos de texto** — `strings.xml` de `:feature:library` (claves inglés `snake_case`,
  valores español: título de progreso, fase, cancelar, resumen con conteos, aviso de aborto/cancelación,
  volver a carpetas, landing) — `Sonus/feature/library/src/main/res/values/strings.xml`

### Fase 8 — Ensamblaje (`:app`)

- [ ] **T39: `DatabaseModule`** — `@Provides` de `ArtistDao`/`GenreDao`/`AlbumDao`/`TrackDao`; añadir
  `.addMigrations(MIGRATION_1_2)` y `.addCallback(seedCallback)` al `databaseBuilder` —
  `.../app/di/DatabaseModule.kt`
- [ ] **T40: `CatalogModule`** (nuevo) — `@Binds` `CatalogRepository→CatalogRepositoryImpl`,
  `ScanStateEmitter→ScanStateEmitterImpl` (`@Singleton`), `DispatcherProvider→DefaultDispatcherProvider`,
  `ScanScheduler→WorkManagerScanScheduler`; `@Provides WorkManager = WorkManager.getInstance(context)`
  — `.../app/di/CatalogModule.kt`
- [ ] **T41: `SonusApplication : Configuration.Provider`** — inyectar `HiltWorkerFactory`; exponer
  `workManagerConfiguration`; quitar el inicializador por defecto de WorkManager en el manifiesto
  (`provider androidx.startup … tools:node="remove"`) si aplica (Base: Hilt-Worker) —
  `.../app/SonusApplication.kt`, `Sonus/app/src/main/AndroidManifest.xml`
- [ ] **T42: `SonusNavHost`** — reemplazar `ScanPlaceholderScreen` por `ScanScreen`; añadir ruta
  `library`; `ScanScreen(onNavigateToLibrary → navigate(library) popUpTo(scan) inclusive,
  onNavigateBackToSourceFolders → navigate(source_folders))` — `.../app/navigation/SonusNavHost.kt`

### Fase 9 — Pruebas (JUnit5 · MockK · Turbine · Room instrumentado)

- [ ] **T43: Fakes de dominio** — `FakeCatalogRepository` (resultado configurable + folders
  capturados), `FakeScanStateEmitter` (`MutableStateFlow` inspeccionable), `FakeScanScheduler`
  (flags `enqueued`/`cancelled`), `TestDispatcherProvider` (`StandardTestDispatcher`) —
  `Sonus/core/domain/src/test/.../fake/`
- [ ] **T44: Tests de casos de uso** — `ScanLibraryUseCaseTest` (AC1 éxito→`Finished`+summary,
  publica `Scanning` inicial; AC5 `Failure`→`Aborted(ScanAborted)`; usa folders del repo),
  `ObserveScanStateUseCaseTest`, `StartLibraryScanUseCaseTest`/`CancelLibraryScanUseCaseTest`
  (verify interacción con `ScanScheduler`, MockK) — `Sonus/core/domain/src/test/.../usecase/`
- [ ] **T45: `ScanViewModelTest`** — Turbine sobre `uiState`/`events`: AC1 (progreso `Scanning`→
  UiState; `Finished`→`NavigateToLibrary`), AC2 (finish rápido→navega), AC5 (`Aborted`→`NotifyAborted`),
  AC6 (`CancelScan`→invoca `CancelLibraryScanUseCase`) — `Sonus/feature/library/src/test/.../scan/ScanViewModelTest.kt`
- [ ] **T46: Tests de datos JVM (fakes de data sources)** — `CatalogRepositoryImplTest`
  (`FakeSafDataSource`/`FakeId3DataSource`, `TestDispatcherProvider`, `FakeScanStateEmitter`): progreso
  emitido, `UNSUPPORTED` contado (AC4), `SecurityException`→`ScanAborted(PermissionRevoked)` (AC5);
  `CatalogSynchronizer` unit (get-or-create, centinela `id=1` AC3, purga huérfanos §6.2) — requiere
  extraer la lógica de sync testeable — `Sonus/core/data/src/test/.../`
- [ ] **T47: Tests instrumentados Room** — `CatalogDaoTest` (in-memory: FKs, unicidad `uri`,
  get-or-create dimensiones, purga huérfanos) y `MigrationTest` (`MigrationTestHelper` 1→2 crea tablas
  y siembra centinelas) — `Sonus/core/data/src/androidTest/.../`
- [ ] **T48: `verifyNoInternetPermission` / autarquía** — `AutarkyManifestTest` y `NoMediaStoreTest`
  (existentes) deben seguir en verde tras añadir `MediaMetadataRetriever` (no `MediaStore`) y
  WorkManager (sin `INTERNET`) — verificación, sin nuevo archivo

### Fase 10 — Quality gates

- [ ] **T49: Ejecutar el gate** — `ktlintCheck`, `detekt`, `konsistTest`, `testDebugUnitTest`,
  `koverXmlReport`, `verifyNoInternetPermission` en verde (100% de tests). Los tests instrumentados
  (`connectedDebugAndroidTest`: DAO + migración) se documentan como verificación aparte si no hay
  emulador disponible.

---

## Checklist de refinamiento

- ☑ Feature análoga leída completa: **US-002** (`a3da2ca`) → patrón *thin-slice* replicado + módulos
  `:service:indexer` y `:feature:library` activados + esquema Room ampliado con migración
- ☑ TODOS los artefactos identificados: tooling/deps (WorkManager, Hilt-Worker), dominio
  (enums/modelo/estado/comando/puertos/casos de uso), datos (esquema Room v2 + migración + siembra,
  SAF, ID3, synchronizer, emitter, dispatchers, mappers), servicio (Worker + scheduler), presentación
  (`:feature:library`), ensamblaje Hilt + navegación, tests (unit + datos + instrumentado + migración
  + arquitectura)
- ☑ Respeta arquitectura: capas, sufijos por capa (`UseCase`/`Impl`/`Dao`/`DataSource`/`Worker`/
  `Synchronizer`/`Emitter`/`ViewModel`/`Screen`/`State`), dirección de dependencias, dominio sin
  Android (`treeUri`/`uri` como `String`), i18n desacoplada, air-gapped (SAF + MMR, sin `MediaStore`/
  `INTERNET`), errores como valor (P1), dispatchers inyectados, centinela `id=1` / `NULL` (Invariante 4)
- ☑ Incluye tests (el proyecto los exige) y quality gates
- ☑ Fronteras de alcance explícitas (US-004/US-007/US-008/US-009 y EPIC-02+ diferidos)
</content>
</invoke>
