## Dev Agent Record — Dev-Rápido

### Debug Log

| # | Tipo | Descripción | Resolución |
|---|------|-------------|------------|
| 1 | Estilo (ktlint) | `LibraryScanWorker.kt` superó los 120 caracteres al leer el `ScanMode` desde `inputData`. | Se partió la expresión con encadenamiento `?.let { … } ?: ScanMode.FULL` en varias líneas. |
| 2 | Estilo (ktlint) | Al modificar `CatalogSynchronizerTest` se re-verificó el *source set* `androidTest` de `:core:data`, aflorando violaciones **preexistentes** (US-006) en `SourceFolderRemovalDaoTest.kt`. | Se auto-corrigieron con `./gradlew ktlintFormat` (formato, sin cambio de comportamiento). |
| 3 | Análisis estático (detekt) | `LongMethod`: el `@Composable SettingsSourceFoldersScreen` llegó a 80 líneas (máx 60) tras añadir el botón de re-escaneo y sus eventos. | Se extrajo el mapeo evento→recurso de texto a la función `SettingsSourceFoldersEvent.noticeRes()` fuera del composable (mantiene el `when` exhaustivo) y se resolvió el `String` vía `LocalContext`. |

### Completion Notes

- ⚡ **Dev-Rápido: US-007 — Ejecutar Escaneo / Re-escaneo de la Biblioteca.** Se añadió el **disparador
  manual de re-escaneo** por parte del Oyente (`TRG-LIB-03`) reutilizando íntegramente el Motor de
  Biblioteca ya construido en **US-003**. El delta implementado fue quirúrgico: (1) **propagación de
  `ScanMode` extremo-a-extremo** — se generalizó el puerto `ScanScheduler.enqueueFullScan()` a
  `enqueueScan(mode: ScanMode)`, el `WorkManagerScanScheduler` transporta el modo por `inputData` y el
  `LibraryScanWorker` lo lee (default defensivo `FULL`); (2) nuevo caso de uso **`RescanLibraryUseCase`**
  (*default* `INCREMENTAL`) cableado en la pantalla de **Carpetas Fuente** de Configuración, que ya
  exponía `hasPendingScanContent` documentado como "delegado a US-007"; el disparo es *fire-and-forget*
  y no bloquea la UI (AC7), con guardia de precondición si no hay carpetas; (3) el *single-flight*
  (`ExistingWorkPolicy.KEEP`) garantiza que onboarding y re-escaneo manual **nunca corran en paralelo**
  (AC6).
- **ACs ya satisfechos por US-003 (verificados, no reimplementados):** alta de nuevos (AC2), purga de
  `MISSING` (AC3, parte-track), purga de dimensiones huérfanas con centinelas preservados (AC4),
  `OperationResult<ScanSummary>` (AC5), asíncrono/progreso (AC7), aborto grácil por permiso revocado
  (AC8), `UNSUPPORTED` (AC9), no invención de datos (AC10). Se añadió cobertura explícita de **re-escaneo
  sobre catálogo NO vacío** (US-003 solo probó catálogo vacío).
- **Fronteras de alcance (documentadas en `refinamiento.md`):**
  1. `ScanMode.INCREMENTAL` real (diff por `fileLastModifiedMs`) → **US-008**. Hasta entonces `INCREMENTAL`
     ejecuta una re-sincronización determinista completa (idempotente por `uri`), correcta pero no
     optimizada.
  2. Purga de referencias en **playlists / cola / marcadores** (parte no-track del AC3) → diferida hasta
     que existan las tablas `PlaylistTrackCrossRef`/`QueueItem`/`PlaybackProgress` (aún no en el esquema
     Room v3; misma frontera declarada en `US-006/dev-record.md`).
  3. Visualización de progreso/resumen en pantalla → **US-009**. Selector de modo en la UI → fuera de
     alcance (el re-escaneo manual usa `INCREMENTAL`).
- **Verificación:** `ktlintCheck`, `detekt`, `testDebugUnitTest` (incluye tests de arquitectura Konsist en
  `:app`) y `verifyNoInternetPermission` (autarquía) en **verde**. El test instrumentado nuevo
  (`CatalogSynchronizerTest.reScanOnNonEmptyCatalogAddsPurgesAndCountsSummary`) requiere emulador; como no
  se tocó código de producción de `CatalogSynchronizer` y ejercita la misma ruta que el test de huérfanos
  ya validado en emulador (US-006), se documenta como verificación aparte (precedente US-003/US-006).

### File List

| Acción | Archivo | Descripción |
|--------|---------|-------------|
| Modificado | `core/domain/.../port/ScanScheduler.kt` | `enqueueFullScan()` → `enqueueScan(mode: ScanMode)` |
| Modificado | `core/domain/.../usecase/StartLibraryScanUseCase.kt` | Invoca `enqueueScan(ScanMode.FULL)` (semántica fundacional intacta) |
| Creado | `core/domain/.../usecase/RescanLibraryUseCase.kt` | Caso de uso del re-escaneo manual (default `INCREMENTAL`) |
| Modificado | `service/indexer/.../WorkManagerScanScheduler.kt` | `enqueueScan(mode)` con `setInputData(KEY_MODE)` + `KEEP` |
| Modificado | `service/indexer/.../LibraryScanWorker.kt` | Lee `ScanMode` desde `inputData` (default `FULL`); `KEY_MODE` |
| Modificado | `feature/settings/.../settings/SettingsSourceFoldersCommand.kt` | `RescanClicked` |
| Modificado | `feature/settings/.../settings/SettingsSourceFoldersEvent.kt` | `NotifyRescanStarted`, `NotifyNoFoldersToScan` |
| Modificado | `feature/settings/.../settings/SettingsSourceFoldersViewModel.kt` | Inyecta `RescanLibraryUseCase`; maneja `RescanClicked` (guardia + limpia pending) |
| Modificado | `feature/settings/.../settings/SettingsSourceFoldersScreen.kt` | Botón "Re-escanear biblioteca" + refactor `noticeRes()` |
| Modificado | `feature/settings/src/main/res/values/strings.xml` | Claves `settings_source_folders_rescan_*` |
| Modificado | `core/domain/src/test/.../fake/FakeScanScheduler.kt` | Captura `enqueuedModes` para la nueva firma |
| Creado | `core/domain/src/test/.../usecase/RescanLibraryUseCaseTest.kt` | Test del re-escaneo manual (default + FULL) |
| Modificado | `core/domain/src/test/.../usecase/StartLibraryScanUseCaseTest.kt` | Verifica `enqueueScan(FULL)` |
| Modificado | `feature/settings/src/test/.../settings/SettingsSourceFoldersViewModelTest.kt` | +2 casos US-007 (con/sin carpetas) |
| Modificado | `core/data/src/androidTest/.../local/room/CatalogSynchronizerTest.kt` | Test de re-escaneo sobre catálogo NO vacío (AC2/3/4/5/9/10) |
| Modificado | `core/data/src/androidTest/.../repository/SourceFolderRemovalDaoTest.kt` | Formato ktlint (auto-corrección preexistente US-006) |

### Métricas Dev-Rápido

- Tiempo sesión IA: 35 min
- Tareas manuales DoD: 0 min
- Tiempo total: 35 min
</content>
