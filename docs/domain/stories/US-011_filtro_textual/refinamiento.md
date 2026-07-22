# Refinamiento Técnico (Developer)

**Autor**: Esteban Colorado González | **Fecha**: 2026-07-22

## Plan: Filtro Textual Local del Catálogo (US-011)

**Arquitectura**: Clean Architecture en 3 capas (`Presentación → Dominio ← Datos`), MVVM, Single-Activity + Jetpack Compose, Room, Hilt. Se **extiende** la feature análoga ya implementada de US-010 (`feature:library/presentation/browse` + `CatalogBrowseRepository`/`CatalogBrowseDao` + `BrowseQuery`), materializando el campo `textFilter` de `TRG-NAV-01` (interfaces_contract §2.5) y el debounce de coalescencia (§4.1). **No se crean capas nuevas**: cada artefacto de US-011 amplía uno existente.

### Feature análoga (leída completa)

- `core/domain/model/BrowseQuery.kt` — su KDoc ya anticipa: *"El filtro textual (US-011) … extenderá esta query"*.
- `core/domain/usecase/BrowseCatalogUseCase.kt`, `core/domain/port/CatalogBrowseRepository.kt` — puerto/caso de uso de lectura reactiva (`Flow<List<TrackView>>`).
- `core/data/repository/CatalogBrowseRepositoryImpl.kt`, `core/data/local/room/dao/CatalogBrowseDao.kt` — enrutado `browse()` → `browseTracks`/`browseAlbumTracks` con filtros nullable en SQL.
- `feature/library/presentation/browse/{LibraryViewModel,LibraryUiState,LibraryCommand,LibraryScreen,LibraryRows}.kt` — MVVM: back-stack + `flatMapLatest` + `combine`, `StateFlow<UiState>`, `LazyColumn`.
- Tests: `core/data/src/androidTest/.../CatalogBrowseDaoTest.kt`, `feature/library/src/test/.../LibraryViewModelTest.kt`, `core/domain/src/test/.../fake/FakeCatalogBrowseRepository.kt`.
- `docs/architecture/*` (interfaces_contract §2.5 `BrowseQuery.textFilter` / §4.1 debounce; coding-standards §2.2 ejemplo `SEARCH_DEBOUNCE_MS`; ADR-001 `LIKE` indexado).

### Patrón identificado

| Capa / archivo | Responsabilidad | Cómo la extiende US-011 |
| --- | --- | --- |
| `BrowseQuery` (dominio) | Intersección de filtros; `null` = sin restricción | Añadir `textFilter: String? = null` (contrato §2.5) |
| `CatalogBrowseDao` (datos) | Consultas `Flow` de solo lectura con filtros nullable en SQL | Añadir cláusula `LIKE` sobre `title`/`artist.name`/`album.name` en `browseTracks` y `browseAlbumTracks` |
| `CatalogBrowseRepositoryImpl` (datos) | Enruta `browse(query)` al DAO | Pasar `query.textFilter` a ambas consultas |
| `LibraryViewModel` (presentación) | Back-stack + contenido reactivo | `MutableStateFlow` de texto → **debounce ~280 ms** → `flatMapLatest` que deriva `BrowseQuery` del nodo actual + `textFilter` |
| `LibraryScreen`/`LibraryRows` | Pestañas, breadcrumb, listas, estados vacíos | Campo de búsqueda (lupa + limpiar) y estado vacío de búsqueda |

### Decisiones técnicas derivadas

- **Sin cambio de esquema / sin migración** — el `LIKE` opera sobre columnas existentes (`track.title`, `artist.name`, `album.name`); la BD conserva su versión actual. El `LIKE '%term%'` con comodín inicial **no usa índice** (limitación conocida y alineada con **ADR-001**: la búsqueda por relevancia con FTS4/5 es extensión futura); el debounce + tamaño de catálogo local mantienen [RNF-01] < 500 ms.
- **`textFilter` derivado del nodo de navegación** — el filtro textual reutiliza la intersección taxonómica ya existente: cada `LibraryNode` mapea a un `BrowseQuery` (contentType/genreId/artistId/albumId) al que se le suma `textFilter`. Con texto activo, cualquier nivel de navegación proyecta **una lista de pistas** filtrada por su contexto taxonómico + término (Escenario 4). Con texto vacío → comportamiento US-010 intacto.
- **Debounce en el ViewModel, no en Vista ni repositorio** (lección aprendida de la HU / contrato §4.1). Debounce **variable**: `0 ms` cuando el campo queda vacío (restauración inmediata, Esc 3/6) y `~280 ms` mientras se escribe. Requiere `@OptIn(FlowPreview::class)` para `debounce`.
- **Insensible a mayúsculas/minúsculas** — `LIKE` de SQLite es case-insensitive para ASCII por defecto (Esc 1). Acentos/Unicode fuera de alcance (coherente con ADR-001).
- **Centinela ausente excluido por dimensión** — un nombre de artista/álbum vacío (`""`, `id=1`) nunca casa contra un término no vacío (`"" LIKE '%Rock%'` = falso); la pista puede seguir casando por su título. Se cumple sin código especial (regla de negocio de la HU).
- **Blanco → `null`** — el ViewModel normaliza el texto (`trim().ifBlank { null }`) antes de construir la `BrowseQuery`; `:textFilter IS NULL` devuelve el catálogo completo (`textFilter = null`, Esc 3/6).
- **Sin persistencia del término** (CT-02 / RNF-07) — el texto vive **solo** en un `MutableStateFlow` transitorio del ViewModel; **no** se usa `SavedStateHandle` ni Room. Se pierde al recrear el proceso, por diseño.
- **Hilo principal libre** (RNF-03 / Esc 5) — las consultas `Flow` de Room ya corren fuera del *Main Thread*; el debounce reduce la frecuencia de consultas. Sin cambios de dispatcher.
- **Sin dependencias nuevas, sin DI nueva** — no cambian constructores ni *bindings* Hilt; `FakeCatalogBrowseRepository` ya captura la `BrowseQuery` completa (incluye `textFilter` por ser `data class`).
- **Paginación mantenida** (CT-07 / Restricción 4) — se conserva el `LazyColumn` virtualizado de US-010 sobre la lista reactiva; no se carga nada adicional en memoria.

### Tareas de Implementación

#### Fase 1 — Dominio (`:core:domain`)

- [ ] **T1: `BrowseQuery` + `textFilter`** — añadir `val textFilter: String? = null` con KDoc (contrato §2.5) — `core/domain/model/BrowseQuery.kt` (Base: mismo archivo, US-010)

#### Fase 2 — Datos (`:core:data`)

- [ ] **T2: `CatalogBrowseDao`** — parámetro `textFilter: String?` y cláusula `AND (:textFilter IS NULL OR t.title LIKE '%'||:textFilter||'%' OR ar.name LIKE … OR al.name LIKE …)` en `browseTracks` y `browseAlbumTracks` — `core/data/local/room/dao/CatalogBrowseDao.kt`
- [ ] **T3: `CatalogBrowseRepositoryImpl`** — reenviar `query.textFilter` a ambas consultas del DAO — `core/data/repository/CatalogBrowseRepositoryImpl.kt`

#### Fase 3 — Presentación (`:feature:library`)

- [ ] **T4: `LibraryCommand`** — añadir `SetTextFilter(query: String)` y `ClearTextFilter` — `feature/library/.../browse/LibraryCommand.kt`
- [ ] **T5: `LibraryUiState`** — añadir `textFilter: String = ""` e `isSearchActive` derivado; `LibraryNode.toBrowseQuery(textFilter)` (mapeo nodo → contexto taxonómico) — `feature/library/.../browse/LibraryUiState.kt` (+ `LibraryNode.kt` si aplica)
- [ ] **T6: `LibraryViewModel`** — `MutableStateFlow` de texto, `debounce` variable, `flatMapLatest` sobre (nodo + filtro), `combine` con el texto crudo para el campo; con filtro activo → `browseCatalog(node.toBrowseQuery(filter))` como `Tracks` — `feature/library/.../browse/LibraryViewModel.kt`
- [ ] **T7: `LibraryScreen`** — campo de búsqueda (icono lupa + botón limpiar «X» cuando hay texto) visible sin scroll; estado vacío de búsqueda — `feature/library/.../browse/LibraryScreen.kt`
- [ ] **T8: `strings.xml`** — claves inglés / valor español: `library_search_hint` («Buscar…»), `library_search_clear`, `library_search_empty` («No se encontraron pistas») — `feature/library/src/main/res/values/strings.xml`

#### Fase 4 — Pruebas

- [ ] **T9: `CatalogBrowseDaoTest`** (instrumentado) — casos `textFilter`: coincidencia por título / artista / álbum, insensible a mayúsculas (Esc 1), sin coincidencias → lista vacía (Esc 2), intersección con `contentType`/`genreId` (Esc 4), centinela excluido por dimensión — `core/data/src/androidTest/.../CatalogBrowseDaoTest.kt`
- [ ] **T10: `LibraryViewModelTest`** — escribir texto proyecta `Tracks` con `textFilter` reenviado (Esc 1/4), limpiar restaura la vista (Esc 3), campo vacío al inicio (Esc 6), debounce (coalescencia) — `feature/library/src/test/.../LibraryViewModelTest.kt`

#### Fase 5 — Calidad (quality gate)

- [ ] **T11: `./gradlew ktlintCheck detekt konsistTest testDebugUnitTest`** verdes (formato, estático, arquitectura, unitarias). Verificar ausencia de `android.permission.INTERNET` (CT-01). Los tests instrumentados (T9) requieren emulador/dispositivo.

### Checklist

☑ Feature análoga leída completa (US-010 `feature:library/browse`) | ☑ TODOS los artefactos identificados (dominio, DAO/SQL, repositorio, presentación, strings, tests) | ☑ Sin cambio de esquema / migración | ☑ Sin dependencias ni DI nuevas | ☑ Respeta dirección de dependencias y estándares | ☑ Debounce en ViewModel (contrato §4.1) | ☑ Cero persistencia del término (CT-02/RNF-07)
