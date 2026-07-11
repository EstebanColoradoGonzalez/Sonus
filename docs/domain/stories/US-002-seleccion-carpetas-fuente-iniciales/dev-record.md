## Dev Agent Record — Dev-Rápido

### Debug Log

| # | Tipo | Descripción | Resolución |
|---|------|-------------|------------|
| 1 | Tooling | Gradle tomaba `JAVA_HOME` = JDK 1.7 | Fijar `JAVA_HOME` a Temurin JDK 17 en la sesión de ejecución del gate |
| 2 | detekt | `ReturnCount` en `AddSourceFolderUseCase.invoke` (3 returns > 2) | Reescrito como expresión `when` sin returns múltiples, preservando el orden dedup→permiso |
| 3 | detekt | `SwallowedException` sobre la traducción de `SecurityException` a valor (P1) | Añadido `SecurityException` a `ignoredExceptionTypes` en `detekt.yml` (justificado por P1) |
| 4 | Warning | Opt-in `ExperimentalCoroutinesApi` en `SourceFoldersViewModelTest` (setMain/UnconfinedTestDispatcher) | Anotada la clase con `@OptIn(ExperimentalCoroutinesApi::class)` |

### Completion Notes

- ⚡ Dev-Rápido: selección guiada de Carpetas Fuente iniciales (US-002). Se introdujo la **fundación Room mínima** (`SonusDatabase` + entidad/DAO `SourceFolder`) y la **primera integración SAF** (`takePersistableUriPermission`/`release`, `DocumentFile`), materializando `TRG-LIB-01`/`TRG-LIB-02` y el Apalancamiento 2.
- Vertical thin-slice replicando el patrón de US-001: contratos base (`OperationResult`/`DomainError`) → modelo/comando/puertos/casos de uso en `:core:domain` → Room+SAF+mappers en `:core:data` → `SourceFoldersScreen`/`ViewModel` en `:feature:settings` → Hilt+NavHost en `:app`.
- 8 ACs cubiertos: agregar vía SAF (AC1), múltiples (AC2), obligatoriedad ≥1 (AC3), duplicado sin re-tomar permiso (AC4), cancelación/permiso denegado sin bucle (AC5), remoción ligera que libera permiso (AC6), verificación de permisos + navegación al escaneo (AC7), autarquía SAF-only verificada por `NoMediaStoreTest` + `AutarkyManifestTest` (AC8).
- **Quality gate en verde** (`check` + `koverXmlReport`): ktlint · detekt · Konsist · JUnit5 · verifyNoInternetPermission. 20 tests JVM nuevos (9 casos de uso + 10 ViewModel + 1 Konsist) + regresión US-001 verde.
- Diferido por alcance: escaneo/Catálogo (US-003, destino `scan` placeholder), gestión post-onboarding (US-005/US-006), `onboardingCompleted`/gating (US-004), resto del esquema Room/Big Bang.
- Verificación manual pendiente: `SourceFolderDaoTest` (androidTest, Room in-memory) requiere emulador/dispositivo — no ejecutado en el gate JVM.

### File List

| Acción | Archivo | Descripción |
|--------|---------|-------------|
| Mod | `gradle/libs.versions.toml` | Room, documentfile, test-runner, room-testing |
| Mod | `core/data/build.gradle.kts` | Room+ksp, documentfile, coroutines, androidTest, schemaLocation |
| Mod | `app/build.gradle.kts` | `room-runtime` para el módulo Hilt de base de datos |
| Mod | `config/detekt/detekt.yml` | `SwallowedException.ignoredExceptionTypes` += `SecurityException` (P1) |
| Nuevo | `core/domain/.../result/OperationResult.kt` | Envelope de desenlace (Success/Failure) |
| Nuevo | `core/domain/.../error/DomainError.kt` | Taxonomía de error tipada (+Severity/ErrorDetails/IoCauseCode) |
| Nuevo | `core/domain/.../model/FolderId.kt` | `typealias FolderId = Long` |
| Nuevo | `core/domain/.../model/SourceFolder.kt` | Modelo de dominio puro |
| Nuevo | `core/domain/.../command/LibraryCommand.kt` | `AddSourceFolder`/`RemoveSourceFolder` |
| Nuevo | `core/domain/.../port/SourceFolderRepository.kt` | Puerto de persistencia |
| Nuevo | `core/domain/.../port/SafPermissionGateway.kt` | Puerto SAF (permiso/displayPath) |
| Nuevo | `core/domain/.../port/TimeProvider.kt` | Tiempo inyectable |
| Nuevo | `core/domain/.../usecase/AddSourceFolderUseCase.kt` | AC1/2/4/5 |
| Nuevo | `core/domain/.../usecase/RemoveSourceFolderUseCase.kt` | AC6 |
| Nuevo | `core/domain/.../usecase/ObserveSourceFoldersUseCase.kt` | AC1/2/3/6 |
| Nuevo | `core/domain/.../usecase/VerifySourceFoldersReadyUseCase.kt` | AC7 |
| Nuevo | `core/domain/test/.../fake/FakeSourceFolderRepository.kt` | Fake en memoria |
| Nuevo | `core/domain/test/.../fake/FakeSafPermissionGateway.kt` | Fake configurable |
| Nuevo | `core/domain/test/.../fake/FixedTimeProvider.kt` | Tiempo fijo |
| Nuevo | `core/domain/test/.../usecase/AddSourceFolderUseCaseTest.kt` | 3 tests |
| Nuevo | `core/domain/test/.../usecase/RemoveSourceFolderUseCaseTest.kt` | 2 tests |
| Nuevo | `core/domain/test/.../usecase/ObserveSourceFoldersUseCaseTest.kt` | 1 test |
| Nuevo | `core/domain/test/.../usecase/VerifySourceFoldersReadyUseCaseTest.kt` | 3 tests |
| Nuevo | `core/data/.../local/room/entity/SourceFolder.kt` | `@Entity source_folder` |
| Nuevo | `core/data/.../local/room/dao/SourceFolderDao.kt` | DAO indexado |
| Nuevo | `core/data/.../local/room/SonusDatabase.kt` | `@Database` v1 |
| Nuevo | `core/data/.../mapper/SourceFolderMappers.kt` | Entidad ↔ dominio |
| Nuevo | `core/data/.../repository/SourceFolderRepositoryImpl.kt` | Impl del puerto |
| Nuevo | `core/data/.../saf/SafPermissionGatewayImpl.kt` | SAF/DocumentFile |
| Nuevo | `core/data/.../time/SystemTimeProvider.kt` | `System.currentTimeMillis()` |
| Nuevo | `core/data/androidTest/.../local/room/SourceFolderDaoTest.kt` | 3 tests instrumentados |
| Nuevo | `feature/settings/.../presentation/onboarding/SourceFoldersUiState.kt` | UiState + SourceFolderUi |
| Nuevo | `feature/settings/.../presentation/onboarding/SourceFoldersCommand.kt` | Comandos C1 |
| Nuevo | `feature/settings/.../presentation/onboarding/SourceFoldersEvent.kt` | Eventos one-shot |
| Nuevo | `feature/settings/.../presentation/onboarding/SourceFoldersViewModel.kt` | `@HiltViewModel` |
| Nuevo | `feature/settings/.../presentation/onboarding/SourceFoldersScreen.kt` | Pantalla + picker SAF |
| Mod | `feature/settings/src/main/res/values/strings.xml` | Textos ES (título, empty, avisos) |
| Nuevo | `feature/settings/test/.../presentation/onboarding/SourceFoldersViewModelTest.kt` | 10 tests |
| Nuevo | `app/.../di/DatabaseModule.kt` | Provee `SonusDatabase`+DAO |
| Nuevo | `app/.../di/LibraryModule.kt` | `@Binds` repo/saf/time |
| Mod | `app/.../navigation/SonusNavHost.kt` | Ruta `source_folders` real + `scan` placeholder |
| Mod | `app/src/main/res/values/strings.xml` | `scan_placeholder` (US-003) |
| Nuevo | `app/test/.../architecture/NoMediaStoreTest.kt` | AC8 Konsist |

### Métricas Dev-Rápido

- Tiempo sesión IA: 25 min
- Tareas manuales DoD: 0 min
- Tiempo total: 25 min
