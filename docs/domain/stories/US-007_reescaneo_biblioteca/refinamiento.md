# Refinamiento Técnico (Developer)

**Autor**: Esteban Colorado González | **Fecha**: 2026-07-21

## Plan: Ejecutar Escaneo / Re-escaneo manual de la Biblioteca (US-007)

**Arquitectura**: Arquitectura Limpia multi-módulo (Presentación → Dominio ← Datos), MVVM +
Single-Activity + Jetpack Compose + Hilt, 100% local (air-gapped). US-007 **reutiliza el Motor de
Biblioteca ya construido en US-003** (contenedor **C-04**) y añade únicamente el **disparador manual**
del ciclo de escaneo por parte del Oyente y la **selección de modo** (`ScanMode`) extremo-a-extremo.
No reconstruye el pipeline de escaneo: el recorrido SAF, la extracción ID3 sin invención (Invariante 4),
la sincronización determinista (`CatalogSynchronizer`), el emisor de estado (`ScanStateEmitter`), el
`Worker` (`LibraryScanWorker`) y el *single-flight* (`ExistingWorkPolicy.KEEP`) **ya existen** y
satisfacen los ACs 2/3/4/5/7/8/9/10 tanto en primer escaneo como en re-escaneo.

> **Feature análoga leída completa**: **US-003 — Escaneo Fundacional** (commit `f90054c`). US-003
> materializó todo el Motor de Biblioteca para un **catálogo vacío** (`ScanMode.FULL`, `purged=0`) y
> dejó explícitamente diferido a US-007 el *"re-escaneo manual posterior"* (ver
> `US-003/refinamiento.md`, §"Fronteras de alcance"). US-007 activa ese disparador manual. Artefactos
> análogos estudiados: `ScanScheduler`/`WorkManagerScanScheduler` (encolado single-flight),
> `LibraryScanWorker` (ejecución en background), `StartLibraryScanUseCase` (disparo onboarding),
> `ScanLibraryUseCase`/`CatalogRepositoryImpl`/`CatalogSynchronizer` (ciclo `Scanning→Syncing→
> Finished`, purga y huérfanos), y `SettingsSourceFoldersViewModel` (US-005/US-006, punto de entrada
> de gestión de carpetas que **ya expone `hasPendingScanContent` documentado como "delegado a US-007"**).

### Diagnóstico: qué YA existe (no se reimplementa)

| AC | Escenario | Ya cubierto por (US-003) |
|----|-----------|--------------------------|
| 2 | Alta de archivos nuevos | `CatalogSynchronizer.sync` da de alta por `uri` (upsert), `added` en `ScanSummary` |
| 3 (tracks) | Purga de `MISSING` | `TrackDao.deleteWhereUriNotIn(uris)` dentro de la transacción; `purged` contado |
| 4 | Purga de dimensiones huérfanas | `Artist/Album/GenreDao.purgeOrphans()` (`id != 1`), centinelas preservados |
| 5 | `OperationResult<ScanSummary>` al cierre | `ScanLibraryUseCase` retorna `Success(summary)` y publica `Finished` |
| 6 | Single-flight | `WorkManagerScanScheduler` con `ExistingWorkPolicy.KEEP` |
| 7 | Asíncrono / no bloquea UI | `LibraryScanWorker : CoroutineWorker` + `dispatcherProvider.io`; progreso `Scanning(processed,total)` |
| 8 | Degradación grácil (permiso revocado) | `CatalogRepositoryImpl` captura `SecurityException` → `Failure(ScanAborted(PermissionRevoked))` |
| 9 | Formato no soportado | `Id3DataSource` → `TrackAvailability.UNSUPPORTED`; `unsupported` contado |
| 10 | No invención de datos | Nombres ausentes → `null`/centinela `id=1` en `CatalogSynchronizer` (Invariante 4) |

### Delta real de US-007 (lo que SÍ se implementa)

1. **Selección de modo extremo-a-extremo**: el `ScanScheduler` hoy solo sabe `enqueueFullScan()`; se
   generaliza a `enqueueScan(mode: ScanMode)` y el `LibraryScanWorker` lee el modo desde `inputData`
   (hoy `ScanMode.FULL` está *hardcodeado*). Así un re-escaneo manual puede pedir `INCREMENTAL` o `FULL`.
2. **Disparador manual del Oyente (AC1)**: nuevo `RescanLibraryUseCase` y su cableado en la pantalla de
   **Carpetas Fuente** de Configuración (`SettingsSourceFoldersViewModel`), que ya declara
   `hasPendingScanContent` como *"offering to start the re-scan explicitly (delegated to US-007)"*.
3. **Tests del re-escaneo sobre catálogo NO vacío**: US-003 solo probó catálogo vacío; se añade
   cobertura de re-escaneo (altas + purga de `MISSING` + huérfanos + summary) y del disparo manual.

### Decisiones técnicas clave (a validar con el usuario)

1. **`ScanMode` por `inputData` de WorkManager** (no por un segundo `Worker`): un único `LibraryScanWorker`
   parametrizado por `Data("mode" -> mode.name)`; *default* defensivo `FULL` si falta. Mantiene el
   *single-flight* con un único nombre de trabajo (`"library_scan"`), de modo que onboarding y re-escaneo
   manual **nunca corren en paralelo** (AC6).
2. **Modo por defecto del re-escaneo manual = `INCREMENTAL`** (historia: *"más eficiente para re-escaneos
   habituales"*). Se **difiere a US-008** la optimización real del *diff* por `fileLastModifiedMs`; hasta
   entonces `INCREMENTAL` ejecuta una re-sincronización determinista completa (idempotente por `uri`),
   funcionalmente equivalente y correcta. No se construye un selector de modo en la UI (detalle no
   especificado en el intake; `FULL` queda disponible por la ruta de onboarding).
3. **Disparo no bloqueante y sin pantalla intermedia** (AC7): el re-escaneo se encola en WorkManager y la
   pantalla de Configuración solo muestra un aviso efímero *"re-escaneo iniciado"* y limpia
   `hasPendingScanContent`. **La visualización del progreso es responsabilidad de US-009** — US-007 no
   navega a una pantalla de progreso ni observa `ScanState` aquí.
4. **Guardia de precondición**: si no hay carpetas fuente configuradas, el re-escaneo no se encola y se
   emite un aviso (`NotifyNoFoldersToScan`); la historia exige ≥1 carpeta (US-005).

### Fronteras de alcance (lo que NO entra en US-007)

- **`ScanMode.INCREMENTAL` real (diff por `fileLastModifiedMs`)** → **US-008** (sincronización
  determinista detallada). US-007 solo cablea el modo.
- **Purga de referencias en playlists / cola / marcadores (AC3, parte no-track)** → diferido hasta que
  existan las tablas `PlaylistTrackCrossRef`, `QueueItem`, `PlaybackProgress` (aún **no** en el esquema
  Room v3; misma frontera declarada en `US-006/dev-record.md`). La purga del `Track` sí opera; el borrado
  en cascada de sus vínculos se resolverá vía `onDelete = CASCADE` cuando esas tablas se creen.
- **Visualización de progreso / resumen en pantalla (AC7 feedback visual)** → **US-009**.
- **Selector de modo en la UI** → fuera de alcance (no especificado); *default* `INCREMENTAL`.

---

## Tareas de Implementación

### Fase 1 — Dominio · Generalizar el disparo por modo (`:core:domain`)

- [ ] **T1: Generalizar puerto `ScanScheduler`** — reemplazar `fun enqueueFullScan()` por
  `fun enqueueScan(mode: ScanMode)` (mantener `fun cancel()`); actualizar KDoc (single-flight sigue en
  la impl) — `core/domain/.../port/ScanScheduler.kt`
- [ ] **T2: Ajustar `StartLibraryScanUseCase` (US-003)** — `invoke() = scanScheduler.enqueueScan(ScanMode.FULL)`
  (conserva la semántica fundacional del onboarding sin cambiar su firma pública) —
  `core/domain/.../usecase/StartLibraryScanUseCase.kt`
- [ ] **T3: Nuevo `RescanLibraryUseCase` (US-007, `TRG-LIB-03`)** — `operator fun invoke(mode: ScanMode =
  ScanMode.INCREMENTAL) = scanScheduler.enqueueScan(mode)`; KDoc: disparo manual del Oyente, single-flight
  garantizado por el scheduler (Base: `StartLibraryScanUseCase`) —
  `core/domain/.../usecase/RescanLibraryUseCase.kt`

### Fase 2 — Servicio · Propagar el modo por WorkManager (`:service:indexer`)

- [ ] **T4: `WorkManagerScanScheduler.enqueueScan(mode)`** — construir `OneTimeWorkRequestBuilder<
  LibraryScanWorker>().setInputData(workDataOf(KEY_MODE to mode.name)).build()` y encolar con
  `enqueueUniqueWork("library_scan", ExistingWorkPolicy.KEEP, request)`; `const KEY_MODE = "scan_mode"`
  (Base: impl existente) — `service/indexer/.../WorkManagerScanScheduler.kt`
- [ ] **T5: `LibraryScanWorker` lee el modo** — `val mode = inputData.getString(KEY_MODE)?.let {
  ScanMode.valueOf(it) } ?: ScanMode.FULL`; `doWork()` invoca `scanLibraryUseCase(LibraryCommand.Scan(mode))`;
  exponer `KEY_MODE` compartido con el scheduler (companion) — `service/indexer/.../LibraryScanWorker.kt`

### Fase 3 — Presentación · Disparador manual en Carpetas Fuente (`:feature:settings`)

- [ ] **T6: `SettingsSourceFoldersCommand.RescanClicked`** — `data object` (intención C1 del Oyente de
  lanzar el re-escaneo) — `feature/settings/.../settings/SettingsSourceFoldersCommand.kt`
- [ ] **T7: Eventos `NotifyRescanStarted` / `NotifyNoFoldersToScan`** — avisos efímeros one-shot —
  `feature/settings/.../settings/SettingsSourceFoldersEvent.kt`
- [ ] **T8: `SettingsSourceFoldersViewModel`** — inyectar `RescanLibraryUseCase`; manejar `RescanClicked`:
  si `uiState.folders` vacío → `emit(NotifyNoFoldersToScan)`; si no → `rescanLibrary(ScanMode.INCREMENTAL)`,
  `update { it.copy(hasPendingScanContent = false) }`, `emit(NotifyRescanStarted)` (`when` exhaustivo) —
  `feature/settings/.../settings/SettingsSourceFoldersViewModel.kt`
- [ ] **T9: `SettingsSourceFoldersScreen`** — acción "Re-escanear biblioteca" (botón); si
  `hasPendingScanContent`, resaltar aviso "contenido pendiente de escanear"; `SnackbarHost` para
  `NotifyRescanStarted`/`NotifyNoFoldersToScan` — `feature/settings/.../settings/SettingsSourceFoldersScreen.kt`
- [ ] **T10: Recursos de texto** — claves inglés `snake_case`, valores en español:
  `settings_source_folders_rescan_action`, `..._rescan_started`, `..._rescan_no_folders`,
  `..._pending_scan_content` — `feature/settings/src/main/res/values/strings.xml`

### Fase 4 — Pruebas (JUnit5 · MockK · Turbine · Room instrumentado)

- [ ] **T11: Actualizar `FakeScanScheduler`** — capturar el/los `mode` encolados
  (`enqueuedModes: List<ScanMode>`) además del conteo; ajustar a la nueva firma `enqueueScan(mode)` —
  `core/domain/src/test/.../fake/FakeScanScheduler.kt`
- [ ] **T12: `RescanLibraryUseCaseTest`** — AC1: `invoke(FULL)`/`invoke(INCREMENTAL)` encolan con el modo
  dado; *default* = `INCREMENTAL`; verifica interacción con `ScanScheduler` (Fake) —
  `core/domain/src/test/.../usecase/RescanLibraryUseCaseTest.kt`
- [ ] **T13: Ajustar `StartLibraryScanUseCaseTest`** — verificar `enqueueScan(ScanMode.FULL)` tras el
  refactor del puerto — `core/domain/src/test/.../usecase/StartLibraryScanUseCaseTest.kt`
- [ ] **T14: `SettingsSourceFoldersViewModelTest` (+casos US-007)** — con Turbine sobre `events`:
  (a) `RescanClicked` con carpetas → `NotifyRescanStarted` + `hasPendingScanContent=false` + invoca
  `RescanLibraryUseCase(INCREMENTAL)`; (b) `RescanClicked` sin carpetas → `NotifyNoFoldersToScan` y **no**
  invoca el use case — `feature/settings/src/test/.../settings/SettingsSourceFoldersViewModelTest.kt`
- [ ] **T15: Test instrumentado de re-escaneo sobre catálogo NO vacío** — `CatalogSynchronizer` con Room
  in-memory: pre-poblar tracks + dimensiones; ejecutar `sync(scanned)` donde (i) hay 1 URI nuevo, (ii)
  falta 1 URI previo (→ purgado), (iii) queda una dimensión huérfana; assert `ScanSummary(added=1,
  purged=1, unsupported=…, orphanDimsPurged≥1)`, centinelas `id=1` preservados (AC2/3/4/5/10 en
  re-escaneo) — `core/data/src/androidTest/.../local/room/CatalogSynchronizerTest.kt` (extiende el existente)

### Fase 5 — Quality gates

- [ ] **T16: Ejecutar el gate** — `ktlintCheck`, `detekt`, `konsistTest`, `testDebugUnitTest`,
  `koverXmlReport`, `verifyNoInternetPermission` en verde (100% de tests JVM). El test instrumentado
  (`CatalogSynchronizerTest`, re-escaneo) se documenta como verificación aparte si no hay emulador
  disponible.

---

## Checklist de refinamiento

- ☑ **Feature análoga leída completa**: US-003 (`f90054c`) — Motor de Biblioteca completo; US-005/US-006
  (patrón de gestión de Carpetas Fuente y su ViewModel, ya con el *hook* `hasPendingScanContent`).
- ☑ **TODOS los artefactos identificados**: dominio (puerto `ScanScheduler`, `StartLibraryScanUseCase`,
  nuevo `RescanLibraryUseCase`), servicio (`WorkManagerScanScheduler` + `LibraryScanWorker` con `inputData`),
  presentación (command/event/viewmodel/screen/strings de `:feature:settings`), tests (fake, use case,
  viewmodel, sync instrumentado). Sin cambios de esquema Room ni de DI (Hilt auto para el nuevo use case).
- ☑ **Respeta arquitectura**: sufijos por capa (`UseCase`/`Scheduler`/`Worker`/`ViewModel`/`Screen`),
  dirección de dependencias (dominio sin Android; `ScanMode.name` como `String` cruza a WorkManager en la
  capa de servicio), errores como valor, dispatchers inyectados, i18n desacoplada, air-gapped intacto.
- ☑ **Incluye tests** (el proyecto los exige) y quality gates.
- ☑ **Fronteras de alcance explícitas**: `INCREMENTAL` real → US-008; purga de vínculos playlist/cola →
  hasta que existan las tablas; progreso visual → US-009; selector de modo UI → fuera de alcance.
</content>
</invoke>
