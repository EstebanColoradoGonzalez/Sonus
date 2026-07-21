## Dev Agent Record — Dev-Rápido

### Debug Log

| # | Tipo | Descripción | Resolución |
|---|------|-------------|------------|
| 1 | Compilación | `SourceFolderRepositoryImpl.remove` con cuerpo `=` devolvía el `Int` de `purgeOrphans` vía `withTransaction`, incompatible con el `Unit` del contrato del port. | Se cambió a cuerpo de bloque `{ }` para descartar el resultado y retornar `Unit`. |
| 2 | Entorno | Gradle tomaba JDK 1.7 (`JAVA_HOME` del sistema), incompatible con Gradle 8.11. | Se ejecutó con `JAVA_HOME=C:\apps\java\JDK_17.0.5`. |

### Completion Notes

- ⚡ **Dev-Rápido: US-006 — Remover una Carpeta Fuente (con purga en cascada).** Se implementó, desde la sección "Carpetas Fuente" de Configuración, la remoción destructiva de una carpeta con diálogo de confirmación (Invariante 5), comunicación del impacto (nº de tracks), borrado en cascada `SourceFolder → Track` (`onDelete = CASCADE`), purga de dimensiones huérfanas (`Artist/Album/Genre`, preservando centinelas `id=1`) en una única transacción Room, y liberación del permiso SAF (reutilizando el `RemoveSourceFolderUseCase` existente sin cambiar su firma). Se soporta la remoción de la última carpeta (biblioteca vacía, AC8), la cancelación sin efectos (AC6) y `ERR_ENTITY_NOT_FOUND` (AC9).
- **Discrepancias resueltas contra el contrato/esquema** (documentadas en `refinamiento.md`):
  1. La confirmación se modela como **diálogo de presentación** (no guardia de dominio): el contrato define `RemoveSourceFolder(folderId)` sin `confirmed`, y `ERR_CONFIRMATION_REQUIRED` pertenece a `TRG-FILE-01`. No se rompió el onboarding (US-002), que usa el mismo use case sin confirmación.
  2. La cascada se limita a `Track`: `PlaylistTrackCrossRef`, `QueueItem` y `PlaybackProgress` **no existen aún** en el esquema (v3). Los **Escenarios 3 (playlists) y 7 (cola activa)** quedan fuera de alcance técnico hasta que existan esas tablas; el diálogo comunica solo el nº de tracks.
- **Verificación:** suite unitaria JVM completa verde (todos los módulos), grafo Hilt de `:app` compila, lint verde en `feature:settings` y `core:data`, y el test instrumentado de cascada (`SourceFolderRemovalDaoTest`) pasó en emulador (Medium_Phone_API_35), confirmando borrado en cascada de tracks + purga de huérfanos + preservación de centinelas + aislamiento de otras carpetas.

### File List

| Acción | Archivo | Descripción |
|--------|---------|-------------|
| Modificado | `core/domain/.../port/SourceFolderRepository.kt` | `countTracksUnder(id)` + doc de `remove` (cascada + purga huérfanos) |
| Creado | `core/domain/.../usecase/GetSourceFolderRemovalImpactUseCase.kt` | Use case de impacto (nº tracks) para el diálogo |
| Modificado | `core/domain/.../fake/FakeSourceFolderRepository.kt` | Implementa `countTracksUnder` (mapa `tracksUnder`) |
| Modificado | `core/data/.../dao/TrackDao.kt` | `countBySourceFolder(id)` |
| Modificado | `core/data/.../repository/SourceFolderRepositoryImpl.kt` | `remove` transaccional (cascada + purga huérfanos), `countTracksUnder`, nuevas deps (DB + DAOs) |
| Modificado | `feature/settings/.../settings/SettingsSourceFoldersCommand.kt` | `RemoveFolderClicked/Confirmed/Dismissed` |
| Modificado | `feature/settings/.../settings/SettingsSourceFoldersEvent.kt` | `NotifyFolderRemoved`, `NotifyRemoveFailed` |
| Modificado | `feature/settings/.../settings/SettingsSourceFoldersUiState.kt` | `pendingRemoval: PendingRemovalUi?` |
| Modificado | `feature/settings/.../settings/SettingsSourceFoldersViewModel.kt` | Manejo de remoción (impacto → diálogo → confirmar/cancelar) |
| Modificado | `feature/settings/.../settings/SettingsSourceFoldersScreen.kt` | Botón "Quitar" por ítem + `AlertDialog` de confirmación + snackbars |
| Modificado | `feature/settings/src/main/res/values/strings.xml` | Claves `settings_source_folders_remove_*` |
| Creado | `core/domain/.../usecase/GetSourceFolderRemovalImpactUseCaseTest.kt` | Test del use case de impacto |
| Modificado | `feature/settings/.../settings/SettingsSourceFoldersViewModelTest.kt` | 6 casos nuevos de remoción (AC1/2/6/8/9) |
| Creado | `core/data/.../androidTest/.../repository/SourceFolderRemovalDaoTest.kt` | Test instrumentado de cascada + purga de huérfanos (AC2/AC4) |

### Métricas Dev-Rápido

- Tiempo sesión IA: 26 min
- Tareas manuales DoD: 0 min
- Tiempo total: 26 min
