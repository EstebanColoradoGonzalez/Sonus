# Refinamiento Técnico (Developer)

**Autor**: Esteban Colorado González | **Fecha**: 2026-07-10

## Plan: Selección guiada de Carpetas Fuente iniciales (US-002)

**Arquitectura**: Arquitectura Limpia multi-módulo (Presentación → Dominio ← Datos),
MVVM + Single-Activity + Jetpack Compose + Hilt, 100% local (air-gapped). US-002 introduce
la **primera persistencia durable del proyecto** (Room/SQLite, ADR-001, contenedor C-03) y la
primera integración **SAF** (ADR-003, canal C5), materializando `TRG-LIB-01`/`TRG-LIB-02` y
el **Apalancamiento 2** (SDD §4.1).

> **Feature análoga leída completa**: **US-001** (commit `bef36c1`). Patrón *thin-slice*
> vertical a través de las capas: puerto en `:core:domain` → impl en `:core:data` →
> caso de uso en `:core:domain` → `ViewModel` + `Screen` en `:feature:settings`
> (paquete `presentation/onboarding`) → cableado Hilt en `:app` → tests JUnit5/MockK/Turbine +
> Konsist + autarquía. US-002 replica exactamente esa disposición y añade la capa de datos Room+SAF.

### Decisión de alcance clave (a validar con el usuario)

US-002 **debe persistir** las Carpetas Fuente porque `US-003` (escaneo) consume `SourceFolder.id`
como FK de cada `Track` (`domain_and_state_model §2/§3`, `onDelete = CASCADE`). El blueprint
mapea `[RF-01]` a `SourceFolderRepository → SafDataSource` sobre Room (§4.1, C-03). Por tanto se
introduce la **fundación Room mínima** (solo la entidad `SourceFolder`; nace vacía en el Big Bang
—§6.1— así que **no** requiere siembra). Se **difiere** a sus historias: el resto de `@Entity`,
los centinelas `id=1`, los singletons `PlaybackState`/`AppSettings`, `ErrorMapper` genérico y
`WorkManager`. Alternativa descartada: derivar la lista de `contentResolver.persistedUriPermissions`
(sin Room) — diverge del modelo de dominio y obligaría a `US-003` a rehacer la persistencia.

### Decisión de flujo (mapa criterios → diseño)

| AC | Escenario | Punto de enforcement |
|----|-----------|----------------------|
| 1 | Agregar carpeta (flujo principal) | `SourceFoldersScreen` lanza `OpenDocumentTree` → `FolderPicked(treeUri)` → `AddSourceFolderUseCase` → `SafPermissionGateway.takePersistablePermission` + `SourceFolderRepository.add` → la lista (Flow) re-emite y habilita "Continuar" |
| 2 | Múltiples carpetas | `AddSourceFolderUseCase` inserta cada `treeUri` distinto; `observeAll(): Flow<List<SourceFolder>>` acumula sin reemplazar |
| 3 | Obligatoriedad ≥1 | `SourceFoldersUiState.canContinue = folders.isNotEmpty()`; botón "Continuar" deshabilitado con lista vacía |
| 4 | Duplicado (`treeUri` repetido) | `AddSourceFolderUseCase`: `repo.exists(treeUri)` → `Failure(DuplicateSourceFolder)` (WARNING) **sin** re-tomar permiso ni duplicar → evento `NotifyDuplicate` |
| 5 | Cancelación / permiso denegado | `OpenDocumentTree` devuelve `null` → `SelectionCancelled` → `NotifySelectionCancelled`; si `takePersistablePermission` falla (SecurityException capturada en el borde → `false`) → `Failure(PermissionDenied)` (ERROR) → `NotifyPermissionDenied`, sin bucle |
| 6 | Quitar carpeta (remoción ligera) | `RemoveSourceFolderUseCase`: `SafPermissionGateway.releasePersistablePermission` + `repo.remove(id)`; lista re-emite; si queda vacía `canContinue=false` (sin purga en cascada — no hay tracks aún) |
| 7 | Confirmar y transitar al escaneo | `ContinueClicked` → `VerifySourceFoldersReadyUseCase` (todos los `treeUri` conservan permiso persistido) → evento `NavigateToScan` (US-003), **sin** iniciar escaneo |
| 8 | Autarquía verificable | Solo SAF por carpeta; **sin** `MediaStore`, `READ_MEDIA_AUDIO`, `READ_EXTERNAL_STORAGE`, `INTERNET`. `treeUri` como `String` en dominio (sin `android.net.Uri`). Verificado por `AutarkyManifestTest` (existente) + nuevo test Konsist `NoMediaStoreTest` |

### Fronteras de alcance (lo que NO entra en US-002)

- **Escaneo / construcción del Catálogo**: `US-003` (`TRG-LIB-03`). Aquí solo se navega a un destino
  `scan` **placeholder**.
- **Gestión post-onboarding**: agregar (`US-005`) y remover con **purga en cascada** (`US-006`).
- **Marcar `onboardingCompleted` / gating de arranque**: `US-004` (`AppSettings` + `SettingsRepository`).
- **Resto del esquema Room** (Track, dimensiones, playlists, sesión), centinelas y singletons del
  Big Bang, `TypeConverters`, `ErrorMapper` genérico, `WorkManager`: se difieren a sus historias.

---

## Tareas de Implementación

### Fase 0 — Tooling y dependencias (Room + SAF)

- [ ] **T1: Ampliar `gradle/libs.versions.toml`** — Room (runtime, ktx, compiler-ksp, testing), `androidx.documentfile`, `androidx-test-runner` — `Sonus/gradle/libs.versions.toml`
- [ ] **T2: `:core:data` build** — plugin `ksp` (ya presente); añadir Room (runtime+ktx, `ksp(room-compiler)`), `documentfile`; `androidTest` (room-testing, junit-ext, coroutines-test, truth, runner) + `defaultConfig.testInstrumentationRunner` (ya presente); `ksp { arg("room.schemaLocation", …) }` — `Sonus/core/data/build.gradle.kts`

### Fase 1 — Dominio · Contratos base (`:core:domain`)

- [ ] **T3: `OperationResult<T>`** — `sealed interface` (`Success<T>`/`Failure`) (Base: `interfaces_contract` §2.0) — `.../core/domain/result/OperationResult.kt`
- [ ] **T4: `DomainError` + `Severity` + `ErrorDetails` + `IoCauseCode`** — `sealed class` sin `message`, con `code`/`severity`/`recoverable`; subtipos necesarios ahora: `PermissionDenied` (ERR_PERMISSION_DENIED, ERROR), `DuplicateSourceFolder` (ERR_DUPLICATE_SOURCE_FOLDER, WARNING), `EntityNotFound` (ERR_ENTITY_NOT_FOUND, ERROR) (Base: `interfaces_contract` §3.1/§3.2) — `.../core/domain/error/DomainError.kt`

### Fase 2 — Dominio · Modelo, comando y puertos (`:core:domain`)

- [ ] **T5: Modelo `SourceFolder`** — `data class` pura (`id: Long`, `treeUri: String`, `displayPath: String`, `dateAddedMs: Long`); `typealias FolderId = Long` (Base: `domain_and_state_model` §2, blueprint "Modelos de Dominio") — `.../core/domain/model/SourceFolder.kt`
- [ ] **T6: `LibraryCommand`** — `sealed interface` con `AddSourceFolder(treeUri: String)` y `RemoveSourceFolder(folderId: FolderId)` (Base: `interfaces_contract` §2.1 `TRG-LIB-01/02`; nota: `displayPath` se deriva en datos vía SAF, no en presentación —contract §1.1—) — `.../core/domain/command/LibraryCommand.kt`
- [ ] **T7: Puerto `SourceFolderRepository`** — `observeAll(): Flow<List<SourceFolder>>`, `suspend exists(treeUri): Boolean`, `suspend add(folder): FolderId`, `suspend findById(id): SourceFolder?`, `suspend remove(id)` (Base: blueprint §3 "Puertos") — `.../core/domain/port/SourceFolderRepository.kt`
- [ ] **T8: Puerto `SafPermissionGateway`** — `suspend takePersistablePermission(treeUri): Boolean`, `suspend releasePersistablePermission(treeUri)`, `resolveDisplayPath(treeUri): String`, `suspend hasPersistedPermission(treeUri): Boolean` (Base: contract §1.2, ADR-003) — `.../core/domain/port/SafPermissionGateway.kt`
- [ ] **T9: Puerto `TimeProvider`** — `fun nowMs(): Long` (tiempo inyectable, coding-standards §5) — `.../core/domain/port/TimeProvider.kt`

### Fase 3 — Dominio · Casos de uso (`:core:domain`)

- [ ] **T10: `AddSourceFolderUseCase`** — `invoke(cmd: LibraryCommand.AddSourceFolder): OperationResult<FolderId>`: si `repo.exists` → `Failure(DuplicateSourceFolder)`; si `!saf.takePersistablePermission` → `Failure(PermissionDenied)`; si no `repo.add(SourceFolder(treeUri, saf.resolveDisplayPath, time.nowMs()))` → `Success(id)` (AC1/2/4/5) — `.../core/domain/usecase/AddSourceFolderUseCase.kt`
- [ ] **T11: `RemoveSourceFolderUseCase`** — `invoke(cmd: LibraryCommand.RemoveSourceFolder): OperationResult<Unit>`: `repo.findById` null → `Failure(EntityNotFound)`; si no `saf.releasePersistablePermission` + `repo.remove` → `Success` (AC6, remoción ligera) — `.../core/domain/usecase/RemoveSourceFolderUseCase.kt`
- [ ] **T12: `ObserveSourceFoldersUseCase`** — `invoke(): Flow<List<SourceFolder>>` delega en `repo.observeAll()` (AC1/2/3/6) — `.../core/domain/usecase/ObserveSourceFoldersUseCase.kt`
- [ ] **T13: `VerifySourceFoldersReadyUseCase`** — `invoke(): Boolean`: hay ≥1 carpeta y todas conservan `saf.hasPersistedPermission` (AC7) — `.../core/domain/usecase/VerifySourceFoldersReadyUseCase.kt`

### Fase 4 — Datos · Room + SAF (`:core:data`)

- [ ] **T14: `@Entity SourceFolder`** — Room, `tableName="source_folder"`, `Index(treeUri, unique=true)` (Base: `domain_and_state_model` §2) — `.../core/data/local/room/entity/SourceFolder.kt`
- [ ] **T15: `SourceFolderDao`** — `observeAll(): Flow<List<..>>`, `countByTreeUri(treeUri): Int`, `insert(..): Long`, `findById(id): ..?`, `deleteById(id)` (indexado) — `.../core/data/local/room/dao/SourceFolderDao.kt`
- [ ] **T16: `SonusDatabase`** — `@Database(entities=[SourceFolder], version=1, exportSchema=true)`, `abstract fun sourceFolderDao()` — `.../core/data/local/room/SonusDatabase.kt`
- [ ] **T17: `SourceFolderMappers`** — funciones puras entidad ↔ modelo de dominio — `.../core/data/mapper/SourceFolderMappers.kt`
- [ ] **T18: `SourceFolderRepositoryImpl`** — implementa el puerto sobre `SourceFolderDao` + mappers (captura de infra en el borde, P1) — `.../core/data/repository/SourceFolderRepositoryImpl.kt`
- [ ] **T19: `SafPermissionGatewayImpl`** — `@Inject @ApplicationContext`; `takePersistablePermission` con `contentResolver.takePersistableUriPermission(READ|WRITE)` (SecurityException → `false`); `release…`; `resolveDisplayPath` vía `DocumentFile.fromTreeUri(...).name`; `hasPersistedPermission` sobre `persistedUriPermissions` — `.../core/data/saf/SafPermissionGatewayImpl.kt`
- [ ] **T20: `SystemTimeProvider`** — implementa `TimeProvider` con `System.currentTimeMillis()` — `.../core/data/time/SystemTimeProvider.kt`

### Fase 5 — Presentación (`:feature:settings` · `presentation/onboarding`)

- [ ] **T21: `SourceFoldersUiState`** — `data class` inmutable (`folders: List<SourceFolderUi>`, `canContinue: Boolean`); `SourceFolderUi(id, displayPath)` (proyección de presentación) — `.../presentation/onboarding/SourceFoldersUiState.kt`
- [ ] **T22: `SourceFoldersCommand`** — `sealed interface`: `AddFolderClicked`, `FolderPicked(treeUri)`, `SelectionCancelled`, `RemoveFolder(id)`, `ContinueClicked` — `.../presentation/onboarding/SourceFoldersCommand.kt`
- [ ] **T23: `SourceFoldersEvent`** — `sealed interface` one-shot: `LaunchFolderPicker`, `NavigateToScan`, `NotifyDuplicate`, `NotifyPermissionDenied`, `NotifySelectionCancelled` — `.../presentation/onboarding/SourceFoldersEvent.kt`
- [ ] **T24: `SourceFoldersViewModel`** — `@HiltViewModel`; observa `ObserveSourceFoldersUseCase` en `init` (mapea a UiState); `onCommand(...)` `when` exhaustivo; delega en Add/Remove/Verify use cases; traduce `OperationResult`/`DomainError.code` a eventos (Base: coding-standards §3.2, `OnboardingViewModel`) — `.../presentation/onboarding/SourceFoldersViewModel.kt`
- [ ] **T25: `SourceFoldersScreen`** — `@Composable`; empty state orientador; `LazyColumn` de carpetas con acción quitar; botón "Agregar carpeta" (owns `rememberLauncherForActivityResult(OpenDocumentTree)`); botón "Continuar" (enabled = `canContinue`); `SnackbarHost` para avisos (Base: `NotificationPermissionScreen`) — `.../presentation/onboarding/SourceFoldersScreen.kt`
- [ ] **T26: Recursos de texto** — `strings.xml` (claves inglés `snake_case`, valores español: título, beneficio, empty state, agregar, continuar, avisos duplicado/cancelación/permiso) — `Sonus/feature/settings/src/main/res/values/strings.xml`

### Fase 6 — Ensamblaje (`:app`)

- [ ] **T27: `DatabaseModule`** — `@Provides` `SonusDatabase` (`Room.databaseBuilder`) + `SourceFolderDao` — `.../app/di/DatabaseModule.kt`
- [ ] **T28: `LibraryModule`** — `@Binds` `SourceFolderRepository→Impl`, `SafPermissionGateway→Impl`, `TimeProvider→SystemTimeProvider` — `.../app/di/LibraryModule.kt`
- [ ] **T29: `SonusNavHost`** — reemplazar `SourceFoldersPlaceholderScreen` por `SourceFoldersScreen`; ruta `onNavigateToScan` → nuevo destino `scan` placeholder (US-003) — `.../app/navigation/SonusNavHost.kt`

### Fase 7 — Pruebas (JUnit5 · MockK · Turbine · Room instrumentado)

- [ ] **T30: Fakes de dominio** — `FakeSourceFolderRepository` (lista en memoria + `MutableStateFlow`), `FakeSafPermissionGateway` (flags configurables: grantResult, displayPath, persisted), `FixedTimeProvider` — `Sonus/core/domain/src/test/.../fake/`
- [ ] **T31: Tests de casos de uso** — `AddSourceFolderUseCaseTest` (éxito, duplicado→WARNING sin duplicar, permiso denegado→ERROR), `RemoveSourceFolderUseCaseTest` (éxito libera permiso, not-found), `ObserveSourceFoldersUseCaseTest`, `VerifySourceFoldersReadyUseCaseTest` (3A) — `Sonus/core/domain/src/test/.../usecase/`
- [ ] **T32: `SourceFoldersViewModelTest`** — Turbine sobre `uiState`/`events`: AC1 (agregar→lista+canContinue), AC3 (vacía→deshabilitado), AC4 (duplicado→NotifyDuplicate), AC5 (cancela→NotifySelectionCancelled; denegado→NotifyPermissionDenied), AC6 (quitar→lista/canContinue), AC7 (continuar→NavigateToScan) — `Sonus/feature/settings/src/test/.../SourceFoldersViewModelTest.kt`
- [ ] **T33: `SourceFolderDaoTest` (instrumentado)** — Room in-memory (`androidTest`): insert+observe, unicidad `treeUri`, findById, deleteById (Base: coding-standards §5) — `Sonus/core/data/src/androidTest/.../SourceFolderDaoTest.kt`
- [ ] **T34: `NoMediaStoreTest` (Konsist, AC8)** — ningún archivo importa `android.provider.MediaStore` ni permisos de media runtime — `Sonus/app/src/test/.../architecture/NoMediaStoreTest.kt`

### Fase 8 — Quality gates

- [ ] **T35: Ejecutar el gate** — `ktlintCheck`, `detekt`, `konsistTest`, `testDebugUnitTest`, `koverXmlReport`, `verifyNoInternetPermission` en verde (100% de tests). DAO instrumentado (`connectedDebugAndroidTest`) documentado como verificación aparte si no hay emulador en CI.

---

## Checklist de refinamiento

- ☑ Feature análoga leída completa: **US-001** (`bef36c1`) → patrón *thin-slice* replicado + capa Room/SAF nueva
- ☑ TODOS los artefactos identificados: tooling/deps, contratos base, dominio (modelo/comando/puertos/casos de uso), datos (Room+SAF+mappers), presentación, ensamblaje Hilt, navegación, tests (unit + instrumentado + arquitectura)
- ☑ Respeta arquitectura: capas, sufijos por capa, dirección de dependencias, `treeUri:String` en dominio (sin Android), i18n desacoplada, air-gapped, errores como valor (P1)
- ☑ Incluye tests (el proyecto los exige) y quality gates
- ☑ Fronteras de alcance explícitas (US-003/US-004/US-005/US-006 diferidos)
