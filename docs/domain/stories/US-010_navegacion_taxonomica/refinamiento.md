# Refinamiento Técnico (Developer)

**Autor**: Esteban Colorado González | **Fecha**: 2026-07-21

## Plan: Navegación Taxonómica del Catálogo (US-010)

**Arquitectura**: Clean Architecture en 3 capas (`Presentación → Dominio ← Datos`), MVVM, Single-Activity + Jetpack Compose, Room, Hilt. Se replica el patrón de `feature:scan` (ViewModel `StateFlow<UiState>` + `Channel<Event>` + casos de uso) y el patrón de lectura reactiva de Room (`Flow`), materializando `TRG-NAV-01` (`RF-12`).

### Decisiones de alcance (aprobadas)

1. **Dimensión Playlist diferida** — El subsistema de Playlists (entidad `Playlist`, `PlaylistTrackCrossRef`, DAO, repositorio, seeding) pertenece a EPIC-06 / Release 1.2 (US-032+) y no existe. El **Escenario 7** queda fuera de esta HU; se implementan las 4 dimensiones objetivas (Tipo, Género, Artista, Álbum).
2. **`Flow<List<TrackView>>`** según el contrato §2.5 (`TRG-NAV-01`). Sin Paging3. `LazyColumn` sobre lista reactiva completa apoyada en los índices de navegación existentes (`[F-1]`). Paginación real = mejora futura.
3. **Solo navegación/listado** — Sin acción de reproducir (US-013 no implementada). Se muestra la indicación visual de pista `UNSUPPORTED` no reproducible (Esc 9), sin afordancia de play.

### Decisiones técnicas derivadas

- **Sin cambio de esquema** → sin migración; la BD permanece en **versión 3**. Todas las consultas son de solo lectura sobre `track` + dimensiones existentes.
- **Puerto de lectura separado** `CatalogBrowseRepository` (SRP): `CatalogRepository`/`CatalogRepositoryImpl` actuales orquestan el escaneo (SAF/ID3/Synchronizer). Refina el sketch del blueprint §3 (que dibuja `browse()` sobre `CatalogRepository`) en un split lectura/escritura por SRP y testabilidad.
- **Room puebla los read-models de dominio directamente** (`TrackView`, `GenreView`, `ArtistView`, `AlbumView`) vía `SELECT` con alias que coinciden con los parámetros del constructor; las clases de dominio permanecen puras (sin anotaciones). Dependencia data→domain (permitida).
- **`[F-7]`**: "Artista → Álbumes" filtra por `Album.artistId`; "Género → Artistas" y "Artista → Pistas" por `Track.artistId`/`Track.genreId`. Se respeta en las consultas.
- **Invariante 2**: toda consulta excluye `availability = MISSING`; `UNSUPPORTED` es visible y marcado. Los conteos de dimensión cuentan solo pistas no-`MISSING`.
- **Centinelas `id=1`** (Invariante 4 / `CT-03`): las consultas agrupan por FK; el nombre vacío del centinela se resuelve a etiqueta localizada **solo en presentación** (`stringResource`), nunca persistida.
- **Carátulas (ADR-009 / `[F-5]`)**: `AlbumView.hasArtwork` se deriva por `EXISTS`; el **renderizado de imagen con Coil se difiere** (Coil no está en el proyecto y requiere un componente `ArtworkImage` + acceso a bytes SAF reutilizable). Se muestra placeholder. Documentado como límite de alcance.
- **`BrowseQuery`** incluye solo los filtros de US-010 (`contentType`, `genreId`, `artistId`, `albumId`, `availability`). `textFilter` (US-011) y `sort`/`TrackSort` (US-012) quedan fuera; se implementa el orden por defecto que exigen los ACs (Esc 6: `trackNumber` asc, nulls al final; resto: título).

### Archivos relevantes (estudiados)

- `feature/library/.../scan/ScanViewModel.kt`, `ScanScreen.kt`, `ScanUiState.kt` — patrón MVVM + eventos.
- `core/domain/.../usecase/ObserveScanStateUseCase.kt` — patrón caso de uso.
- `core/data/.../dao/TrackDao.kt`, `SonusDatabase.kt`, `di/DatabaseModule.kt`, `di/CatalogModule.kt` — patrón DAO/DI.
- `app/.../navigation/SonusNavHost.kt` — ruteo Single-Activity.
- `core/domain/.../fake/FakeCatalogRepository.kt`, `feature/library/.../ScanViewModelTest.kt` — patrón de fakes y tests.
- `docs/architecture/*` (blueprint §3/§4, interfaces_contract §2.5, domain_and_state_model §1/§2/[F-1]/[F-5]/[F-7], coding-standards).

### Tareas de Implementación

#### Fase 1 — Dominio (`:core:domain`)

- [ ] **T1: `TrackView`** — read-model de pista proyectada — `core/domain/model/TrackView.kt`
- [ ] **T2: `GenreView` / `ArtistView` / `AlbumView`** — read-models de dimensión con `trackCount` (y `hasArtwork`/`artistId`/`artistName` en Album) — `core/domain/model/{GenreView,ArtistView,AlbumView}.kt`
- [ ] **T3: `BrowseQuery`** — filtros soportados por US-010 — `core/domain/model/BrowseQuery.kt`
- [ ] **T4: Puerto `CatalogBrowseRepository`** — `browse`, `genres`, `artists`, `albums`, `observeCatalogEmpty` (`Flow`) — `core/domain/port/CatalogBrowseRepository.kt`
- [ ] **T5: Casos de uso** `BrowseCatalogUseCase`, `ListGenresUseCase`, `ListArtistsUseCase`, `ListAlbumsUseCase` (Base: `ObserveScanStateUseCase`) — `core/domain/usecase/*.kt`

#### Fase 2 — Datos (`:core:data`)

- [ ] **T6: `CatalogBrowseDao`** — consultas `Flow` de solo lectura sobre `track`+dimensiones (filtros nullable, exclusión `MISSING`, conteos, orden centinela-al-final, orden `trackNumber` en álbum, `EXISTS` de carátula) que pueblan los read-models de dominio (Base: `TrackDao.kt`) — `core/data/local/room/dao/CatalogBrowseDao.kt`
- [ ] **T7: `CatalogBrowseRepositoryImpl`** — implementa el puerto delegando en el DAO (Base: `CatalogRepositoryImpl.kt`) — `core/data/repository/CatalogBrowseRepositoryImpl.kt`
- [ ] **T8: Registrar DAO en `SonusDatabase`** — `abstract fun catalogBrowseDao()`; **sin** nuevas `@Entity` ni cambio de versión — `core/data/local/room/SonusDatabase.kt`

#### Fase 3 — Inyección de dependencias (`:app`)

- [ ] **T9: `DatabaseModule`** — `provideCatalogBrowseDao(...)` — `app/.../di/DatabaseModule.kt`
- [ ] **T10: `CatalogModule`** — `@Binds bindCatalogBrowseRepository(impl): CatalogBrowseRepository` — `app/.../di/CatalogModule.kt`

#### Fase 4 — Presentación (`:feature:library`)

- [ ] **T11: `BrowseDimension` + `LibraryNode`** — enum de pestañas (Música, Podcasts, Géneros, Artistas, Álbumes) y nodos de navegación jerárquica (Root / GenreArtists / ArtistAlbums / AlbumTracks) — `feature/library/.../presentation/browse/{BrowseDimension,LibraryNode}.kt`
- [ ] **T12: `LibraryUiState` + `LibraryContent` + `LibraryCommand`** — estado inmutable, contenido polimórfico (Genres/Artists/Albums/Tracks), estados vacíos, comandos (SelectDimension/OpenGenre/OpenArtist/OpenAlbum/NavigateUp) — `feature/library/.../presentation/browse/{LibraryUiState,LibraryCommand}.kt`
- [ ] **T13: `LibraryViewModel`** — back-stack de nodos, `flatMapLatest` sobre el nodo actual → contenido reactivo (Esc 13), distinción catálogo-vacío vs dimensión-vacía, breadcrumb (Base: `ScanViewModel.kt`) — `feature/library/.../presentation/browse/LibraryViewModel.kt`
- [ ] **T14: `LibraryScreen`** — pestañas + breadcrumb/back (`BackHandler`), `LazyColumn` por tipo de contenido, filas (género/artista/álbum/pista con marca UNSUPPORTED + placeholder de carátula), estados vacíos, acceso a Carpetas Fuente (Base: `ScanScreen.kt`) — `feature/library/.../presentation/browse/LibraryScreen.kt`
- [ ] **T15: `strings.xml`** — claves inglés / valores español: título, pestañas, etiquetas de centinela ("Sin artista/álbum/género/título"), estados vacíos, "No reproducible", conteos, acciones — `feature/library/src/main/res/values/strings.xml`
- [ ] **T16: Enrutar `LibraryScreen` en `SonusNavHost`** — ruta `LIBRARY` usa `LibraryScreen` (conserva `onNavigateToSourceFolders`) — `app/.../navigation/SonusNavHost.kt`

#### Fase 5 — Pruebas

- [ ] **T17: `FakeCatalogBrowseRepository`** (source set de prueba de dominio) — `core/domain/src/test/.../fake/FakeCatalogBrowseRepository.kt`
- [ ] **T18: Tests de casos de uso** — `BrowseCatalogUseCaseTest`, `ListGenresUseCaseTest`, `ListArtistsUseCaseTest`, `ListAlbumsUseCaseTest` (delegación + paso de query) — `core/domain/src/test/.../usecase/*.kt`
- [ ] **T19: `CatalogBrowseDaoTest`** (instrumentado, Room en memoria) — filtros por tipo (Esc 2/3), género→artistas (Esc 4), artista→álbumes por `Album.artistId` (Esc 5), álbum→pistas orden `trackNumber` (Esc 6), centinelas (Esc 8), `UNSUPPORTED` visible / `MISSING` excluido (Esc 9/Inv 2), listas vacías (Esc 10/11), `hasArtwork` — `core/data/src/androidTest/.../dao/CatalogBrowseDaoTest.kt`
- [ ] **T20: `LibraryViewModelTest`** — pestañas, drill-down push/pop, proyección de contenido, catálogo-vacío vs dimensión-vacía, reactividad (Base: `ScanViewModelTest.kt`) — `feature/library/src/test/.../browse/LibraryViewModelTest.kt`

#### Fase 6 — Calidad (quality gate)

- [ ] **T21: `./gradlew ktlintCheck detekt konsistTest testDebugUnitTest`** verdes (formato, estático, arquitectura, unitarias). Verificar ausencia de `android.permission.INTERNET` (RNF-06). Los tests instrumentados (T19) requieren emulador/dispositivo.

### Checklist

☑ Feature análoga leída completa (`feature:scan`) | ☑ Artefactos identificados (dominio, DAO/Room, DI, presentación, tests) | ☑ Sin cambio de esquema/migración | ☑ Respeta dirección de dependencias y estándares | ☑ Sin dependencias nuevas | ☑ Alcance de Playlist/Paging/Reproducción acordado con el usuario
