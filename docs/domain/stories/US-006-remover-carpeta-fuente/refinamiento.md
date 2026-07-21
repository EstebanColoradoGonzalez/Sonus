# Refinamiento Técnico (Developer)

**Autor**: Esteban Colorado González | **Fecha**: 2026-07-21

## Plan: Remover una Carpeta Fuente (con purga en cascada)

**Arquitectura**: Clean Architecture 3 capas (Presentación → Dominio ← Datos), MVVM, Single-Activity, Hilt (blueprint §3; ADR-005/008). Comandos `sealed interface`, estado `StateFlow<UiState>` inmutable, eventos one-shot por `Channel`, errores como valor (`OperationResult`/`DomainError`, coding-standards §4.2/§4.3).

**Feature análoga leída completa**: US-005 (agregar Carpeta Fuente post-onboarding). Comparte la pantalla "Carpetas Fuente" (`SettingsSourceFoldersViewModel`/`Screen`), la capa de datos (`SourceFolderRepositoryImpl`/`SourceFolderDao`, `SafPermissionGatewayImpl`) y el use case ya existente `RemoveSourceFolderUseCase`. También referencia US-003 (`CatalogSynchronizer`), que ya orquesta la **purga de dimensiones huérfanas** en transacción Room — patrón directo a replicar.

---

## ⚠️ Discrepancias detectadas (validación contra código y contrato)

El discovery reveló tres desajustes entre la narrativa de `historia.md` y la realidad del contrato/esquema. El plan sigue el **contrato de interfaces** (fuente de verdad de firmas) y el **esquema Room real**, no la narrativa literal. Requieren tu confirmación:

1. **Confirmación: diálogo de UI, NO guardia de dominio.**
   `historia.md` afirma que invocar sin `confirmed=true` produce `ERR_CONFIRMATION_REQUIRED`. Pero el contrato (`interfaces_contract.md` §2.1) define `LibraryCommand.RemoveSourceFolder(folderId)` **sin** campo `confirmed`, y `ERR_CONFIRMATION_REQUIRED` está atado a `TRG-FILE-01` (borrado físico de archivo), **no** a `TRG-LIB-02`. Además, el `RemoveSourceFolderUseCase` **ya existe** y lo usa el onboarding (US-002) sin confirmación.
   → **Decisión adoptada**: la confirmación es el **diálogo en presentación** (Invariante 5 satisfecha en la capa correcta). El use case NO recibe `confirmed` ni emite `ERR_CONFIRMATION_REQUIRED`. Se preserva el contrato y no se rompe el onboarding.

2. **Cascada limitada a `Track`: Playlist/Cola/Marcadores aún no existen.**
   El esquema real (`SonusDatabase` v3) solo tiene `SourceFolder, Artist, Album, Genre, Track, AppSettings`. **No existen** `PlaylistTrackCrossRef`, `QueueItem` ni `PlaybackProgress` (son de historias futuras). El `onDelete = CASCADE` de `Track.sourceFolderId → SourceFolder.id` ya está declarado y elimina los tracks; la cascada a playlists/cola/marcadores es automática **cuando esas tablas existan**.
   → **Decisión adoptada**: esta historia implementa cascada `SourceFolder → Track` + purga de dimensiones huérfanas + liberación SAF. Los **Escenarios 3 (playlists) y 7 (cola activa)** quedan **fuera de alcance técnico** hasta que existan sus tablas; el diálogo de impacto comunica **solo el nº de tracks** (no refs en playlists, que hoy son siempre 0). Se documenta como frontera.

3. **`RemoveSourceFolderUseCase` se reutiliza, no se duplica.**
   Ya existe (onboarding). Se le añade la purga de dimensiones huérfanas **en la capa de datos** (dentro de `SourceFolderRepositoryImpl.remove`, transaccional), de modo que ambos flujos (onboarding y settings) la ejecutan. En onboarding es un no-op inofensivo (0 tracks/dimensiones antes del escaneo). El use case de dominio no cambia su firma.

---

### Reutilización directa (NO se modifica)

- Dominio: `RemoveSourceFolderUseCase` (firma intacta), `ObserveSourceFoldersUseCase`, `OperationResult`, `DomainError.EntityNotFound`, `SafPermissionGateway` (ya tiene `releasePersistablePermission`), modelo `SourceFolder`, `LibraryCommand.RemoveSourceFolder`.
- Datos: `SafPermissionGatewayImpl`, mappers, DAOs de dimensiones (`Artist/Album/Genre` ya tienen `purgeOrphans()`), `CatalogSynchronizer` (referencia de purga transaccional).
- Presentación: la pantalla `SettingsSourceFoldersScreen`/`ViewModel` de US-005 (se extienden, no se reescriben).
- El onboarding `SourceFoldersViewModel` y su remoción ligera: **sin cambios de comportamiento**.

### Tareas de Implementación

#### Fase 1 — Dominio

- [ ] **T1: `SourceFolderRepository.countTracksUnder(id)`** — agregar `suspend fun countTracksUnder(id: FolderId): Int` a la interfaz. Actualizar doc de `remove` (ahora cascada a tracks + purga huérfanos). — `core/domain/.../port/SourceFolderRepository.kt`
- [ ] **T2: `GetSourceFolderRemovalImpactUseCase`** — `@Inject constructor(repository)`; `suspend operator fun invoke(folderId): Int` → nº de tracks bajo la carpeta (para el diálogo). (Base: `ObserveSourceFoldersUseCase.kt`) — depende de T1

#### Fase 2 — Datos

- [ ] **T3: `TrackDao.countBySourceFolder(id)`** — `@Query("SELECT COUNT(*) FROM track WHERE sourceFolderId = :id")`. (Base: `TrackDao.countByAvailability`)
- [ ] **T4: `SourceFolderRepositoryImpl` — cascada + purga transaccional** — inyectar `SonusDatabase`, `ArtistDao`, `AlbumDao`, `GenreDao`, `TrackDao`; `remove(id)` → `database.withTransaction { dao.deleteById(id); albumDao.purgeOrphans(); genreDao.purgeOrphans(); artistDao.purgeOrphans() }` (orden idéntico a `CatalogSynchronizer`); implementar `countTracksUnder`. (Base: `CatalogSynchronizer.sync`) — depende de T1/T3

#### Fase 3 — Presentación (feature settings)

- [ ] **T5: Extender `SettingsSourceFoldersCommand`** — `RemoveFolderClicked(id, displayPath)`, `RemoveFolderConfirmed`, `RemoveFolderDismissed`. (Base: `SettingsSourceFoldersCommand.kt`)
- [ ] **T6: Extender `SettingsSourceFoldersEvent`** — `NotifyFolderRemoved`, `NotifyRemoveFailed`. (Base: `SettingsSourceFoldersEvent.kt`)
- [ ] **T7: Extender `SettingsSourceFoldersUiState`** — `pendingRemoval: PendingRemovalUi?` (`id`, `displayPath`, `trackCount`, `isLastFolder`) para dirigir el diálogo (estado durable, sobrevive recomposición). (Base: `SettingsSourceFoldersUiState.kt`)
- [ ] **T8: Extender `SettingsSourceFoldersViewModel`** — inyectar `RemoveSourceFolderUseCase` + `GetSourceFolderRemovalImpactUseCase`; `RemoveFolderClicked` → consulta impacto + calcula `isLastFolder` (de `uiState.folders`) → set `pendingRemoval`; `RemoveFolderConfirmed` → invoca `RemoveSourceFolderUseCase`, limpia `pendingRemoval`, emite `NotifyFolderRemoved` o mapea `EntityNotFound → NotifyRemoveFailed`; `RemoveFolderDismissed` → limpia `pendingRemoval` sin efecto. (Base: `SettingsSourceFoldersViewModel.kt`) — depende de T2/T5/T6/T7
- [ ] **T9: Extender `SettingsSourceFoldersScreen`** — botón "Quitar" por ítem; `AlertDialog` cuando `pendingRemoval != null` con mensaje de impacto (nº tracks + aviso "biblioteca quedará vacía" si `isLastFolder`), botones Confirmar (destructivo)/Cancelar; snackbar de éxito y de error. (Base: `SettingsSourceFoldersScreen.kt`) — depende de T8
- [ ] **T10: Recursos de texto** — claves `settings_source_folders_remove_*`: acción quitar, título diálogo, cuerpo con nº tracks, aviso última carpeta, confirmar, cancelar, snackbar éxito, snackbar error no-encontrada. — `feature/settings/src/main/res/values/strings.xml`

#### Fase 4 — Pruebas

- [ ] **T11: `GetSourceFolderRemovalImpactUseCaseTest`** — JUnit5 + Truth + `FakeSourceFolderRepository`; retorna el conteo esperado. (Base: `RemoveSourceFolderUseCaseTest.kt`)
- [ ] **T12: `SettingsSourceFoldersViewModelTest` (extensión)** — `RemoveFolderClicked → pendingRemoval` con `trackCount`+`isLastFolder`; `RemoveFolderConfirmed → NotifyFolderRemoved` y `pendingRemoval=null`; `EntityNotFound → NotifyRemoveFailed`; `RemoveFolderDismissed → pendingRemoval=null` sin efecto. (Base: `SettingsSourceFoldersViewModelTest.kt`)
- [ ] **T13: `SourceFolderRemovalDaoTest` (instrumentado)** — Room in-memory + `SourceFolderRepositoryImpl`: insertar carpeta + tracks + dimensiones; `remove` → tracks en cascada eliminados, dimensiones huérfanas purgadas, **centinelas `id=1` preservados**, tracks de otra carpeta intactos (Escenarios 2/4). (Base: `SourceFolderDaoTest.kt`) — depende de T4

### Checklist

☑ Feature análoga (US-005) leída completa | ☑ Artefactos identificados (dominio, datos, DI, presentación, tests, strings) | ☑ Respeta arquitectura y contrato (firmas, errores como valor) | ☑ Incluye tests (dominio + ViewModel + Room instrumentado) | ☑ Preserva US-005 y el onboarding sin cambios de comportamiento | ⚠️ Escenarios 3/7 (playlists/cola) fuera de alcance: tablas inexistentes en el esquema actual
