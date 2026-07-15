## Dev Agent Record — Dev-Rápido

### Debug Log

| # | Tipo | Descripción | Resolución |
|---|------|-------------|------------|
| 1 | Build | `JAVA_HOME` apuntaba a JDK 1.7; Gradle 8.11.1 exige Java 8+ | Ejecutar el build con `JAVA_HOME` = JDK 17 (Temurin) |
| 2 | Compilación | "Unclosed comment" en `SafDataSourceImpl` — el texto `audio/*` en un KDoc abría un comentario de bloque anidado (Kotlin los anida) | Reformular el KDoc para evitar la secuencia `/*` |
| 3 | Compilación | `when` no exhaustivo en `SourceFoldersViewModel` (US-002) al crecer el sellado `DomainError` con `PermissionRevoked`/`ScanAborted` | Añadir ramas defensivas explícitas (sin `else`) — guardrail de exhaustividad funcionando |
| 4 | Estilo | Violaciones de formato ktlint en tests nuevos | `ktlintFormat` (auto-corrección) |

### Completion Notes

- ⚡ Dev-Rápido: **Escaneo fundacional y transición a la biblioteca (US-003)**. Implementado el Motor de Biblioteca (C-04): recorrido recursivo SAF de las Carpetas Fuente, extracción ID3 con `MediaMetadataRetriever` (air-gapped, sin `MediaStore`), construcción determinista del Catálogo en Room (nuevo esquema v2 `Track`/`Artist`/`Album`/`Genre` con migración 1→2 y siembra de centinelas `id=1`), escaneo en background vía `LibraryScanWorker` (WorkManager, single-flight `KEEP`), progreso observable `ScanState` (C2) y pantalla de progreso con transición automática a la biblioteca.
- Cobertura de los 7 criterios de aceptación: progreso determinista (AC1), finish rápido (AC2), metadatos ausentes → centinela/NULL sin invención (AC3), `UNSUPPORTED` sin interrumpir (AC4), aborto por permiso revocado conservando el catálogo (AC5), cancelación limpia (AC6) y autarquía verificable (AC7).
- Se activaron los módulos `:service:indexer` y `:feature:library`; se introdujo la integración Hilt-Worker (`SonusApplication : Configuration.Provider`).
- **Fronteras respetadas** (diferido a sus historias): `onboardingCompleted`/gating de arranque (US-004), re-escaneo manual (US-007), sincronización incremental real (US-008), vista de biblioteca navegable (US-009/EPIC-02+ — aquí un *landing placeholder*), escritura de etiquetas ID3 (`TRG-META-01`).

### Verificación

- **Gates en verde (JDK 17)**: `testDebugUnitTest` (dominio, datos, `:feature:library`, `:feature:settings`, `:app`), `ktlintCheck`, `detekt`, Konsist (`ArchitectureTest`/`NoMediaStoreTest`/`AutarkyManifestTest` en `:app`), `verifyNoInternetPermission`, `koverXmlReport`.
- **Tests instrumentados** (`CatalogSynchronizerTest`, `MigrationTest` con `MigrationTestHelper`): creados; requieren emulador/dispositivo (`connectedDebugAndroidTest`) — verificación aparte del gate JVM.

### File List

| Acción | Archivo | Descripción |
|--------|---------|-------------|
| Modificado | `Sonus/gradle/libs.versions.toml` | WorkManager + androidx.hilt (hilt-work/compiler) |
| Modificado | `Sonus/service/indexer/build.gradle.kts` | Plugins ksp/hilt + deps WorkManager/Hilt-Worker + test |
| Modificado | `Sonus/feature/library/build.gradle.kts` | Plugins compose/ksp/hilt + deps Compose/Hilt + test |
| Modificado | `Sonus/app/build.gradle.kts` | Deps `:feature:library`, `:service:indexer`, WorkManager, hilt-work |
| Modificado | `Sonus/core/data/build.gradle.kts` | Test JVM (JUnit5/MockK) + assets de esquema para MigrationTestHelper |
| Modificado | `core/domain/.../error/DomainError.kt` | Subtipos `PermissionRevoked`, `ScanAborted(cause)` |
| Modificado | `core/domain/.../command/LibraryCommand.kt` | Subtipo `Scan(mode)` |
| Creado | `core/domain/.../model/ContentType.kt` | Enum MUSIC/PODCAST/UNKNOWN |
| Creado | `core/domain/.../model/TrackAvailability.kt` | Enum AVAILABLE/UNSUPPORTED/MISSING |
| Creado | `core/domain/.../model/TrackId.kt` | `typealias TrackId = Long` |
| Creado | `core/domain/.../model/ScannedTrack.kt` | Modelo puro de pista descubierta (nombres, no FKs) |
| Creado | `core/domain/.../model/ScanMode.kt` | Enum FULL/INCREMENTAL |
| Creado | `core/domain/.../model/ScanSummary.kt` | Resumen de escaneo |
| Creado | `core/domain/.../model/ScanState.kt` | Estado observable del ciclo (C2) |
| Creado | `core/domain/.../port/CatalogRepository.kt` | Puerto de sincronización del Catálogo |
| Creado | `core/domain/.../port/ScanStateEmitter.kt` | Puerto emisor/observador de `ScanState` |
| Creado | `core/domain/.../port/ScanScheduler.kt` | Puerto de agendado (WorkManager) |
| Creado | `core/domain/.../port/DispatcherProvider.kt` | Dispatchers inyectables |
| Creado | `core/domain/.../usecase/ScanLibraryUseCase.kt` | Orquesta escaneo + estado terminal |
| Creado | `core/domain/.../usecase/ObserveScanStateUseCase.kt` | Expone `StateFlow<ScanState>` |
| Creado | `core/domain/.../usecase/StartLibraryScanUseCase.kt` | Dispara el escaneo |
| Creado | `core/domain/.../usecase/CancelLibraryScanUseCase.kt` | Cancela el escaneo |
| Creado | `core/data/.../local/room/entity/{Artist,Genre,Album,Track}.kt` | Entidades Room del Catálogo |
| Creado | `core/data/.../local/room/converter/RoomTypeConverters.kt` | Converters de enums (por nombre) |
| Creado | `core/data/.../local/room/dao/{Artist,Genre,Album,Track}Dao.kt` | DAOs (get-or-create, purga, upsert) |
| Modificado | `core/data/.../local/room/SonusDatabase.kt` | v2: entidades + DAOs + converters |
| Creado | `core/data/.../local/room/migration/Migrations.kt` | `MIGRATION_1_2` + siembra de centinelas |
| Creado | `core/data/.../local/room/CatalogSynchronizer.kt` | Diff/sync determinista en transacción |
| Creado | `core/data/.../local/saf/SafDataSource.kt` (+`Impl`) | Descubrimiento recursivo SAF |
| Creado | `core/data/.../id3/Id3DataSource.kt` (+`Impl`) | Lectura ID3 con `MediaMetadataRetriever` |
| Creado | `core/data/.../repository/CatalogRepositoryImpl.kt` | Orquesta SAF+ID3+sync, progreso y aborto |
| Creado | `core/data/.../scan/ScanStateEmitterImpl.kt` | Emisor singleton de `ScanState` |
| Creado | `core/data/.../time/DefaultDispatcherProvider.kt` | Impl de dispatchers |
| Creado | `core/data/.../mapper/CatalogMappers.kt` | Mapeos raw→dominio→entidad |
| Creado | `service/indexer/.../LibraryScanWorker.kt` | `@HiltWorker` CoroutineWorker |
| Creado | `service/indexer/.../WorkManagerScanScheduler.kt` | Scheduler single-flight (`KEEP`) |
| Creado | `feature/library/.../presentation/scan/ScanUiState.kt` | Estado de UI del escaneo |
| Creado | `feature/library/.../presentation/scan/ScanCommand.kt` | Comandos (Cancel/Retry) |
| Creado | `feature/library/.../presentation/scan/ScanEvent.kt` | Eventos one-shot |
| Creado | `feature/library/.../presentation/scan/ScanViewModel.kt` | ViewModel del escaneo |
| Creado | `feature/library/.../presentation/scan/ScanScreen.kt` | Pantalla de progreso |
| Creado | `feature/library/.../presentation/LibraryLandingScreen.kt` | Placeholder de transición |
| Creado | `feature/library/src/main/res/values/strings.xml` | Textos (español) |
| Modificado | `app/.../di/DatabaseModule.kt` | DAOs nuevos + migración + callback de siembra |
| Creado | `app/.../di/CatalogModule.kt` | Binds de puertos de escaneo + `WorkManager` |
| Modificado | `app/.../SonusApplication.kt` | `Configuration.Provider` + `HiltWorkerFactory` |
| Modificado | `app/.../navigation/SonusNavHost.kt` | `ScanScreen` real + ruta `library` |
| Modificado | `Sonus/app/src/main/AndroidManifest.xml` | Inicialización on-demand de WorkManager |
| Modificado | `feature/settings/.../SourceFoldersViewModel.kt` | `when` exhaustivo ante nuevos `DomainError` |
| Creado | `core/domain/src/test/.../fake/{FakeCatalogRepository,FakeScanStateEmitter,FakeScanScheduler}.kt` | Fakes de dominio |
| Creado | `core/domain/src/test/.../usecase/{ScanLibrary,ObserveScanState,StartLibraryScan,CancelLibraryScan}UseCaseTest.kt` | Tests de casos de uso |
| Creado | `core/data/src/test/.../fake/ScanFakes.kt` | Fakes de data sources/emitter/dispatcher |
| Creado | `core/data/src/test/.../repository/CatalogRepositoryImplTest.kt` | Test JVM (progreso, UNSUPPORTED, aborto) |
| Creado | `core/data/src/androidTest/.../local/room/CatalogSynchronizerTest.kt` | Test instrumentado (centinela, purga §6.2) |
| Creado | `core/data/src/androidTest/.../local/room/MigrationTest.kt` | Test instrumentado de migración 1→2 |
| Creado | `feature/library/src/test/.../scan/ScanViewModelTest.kt` | Test del ViewModel (Turbine/MockK) |

### Métricas Dev-Rápido

- Tiempo sesión IA: ~90 min
- Tareas manuales DoD: 0 min
- Tiempo total: ~90 min
</content>
