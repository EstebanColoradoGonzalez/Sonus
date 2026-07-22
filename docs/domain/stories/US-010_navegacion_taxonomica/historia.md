# Historia de Usuario

**Como** El Oyente,
**Quiero** navegar y filtrar mi biblioteca de audio por dimensiones taxonómicas —Género, Artista, Álbum, Tipo de contenido y Playlists—,
**Para** encontrar rápidamente el contenido que quiero escuchar en cualquier momento y contexto, cruzando las dimensiones objetivas de mis metadatos con mis agrupaciones personalizadas, sin importar el tamaño de mi colección.

## Descripción

US-010 materializa la exploración del Catálogo de Biblioteca a través de sus ejes de organización. Cuando el Motor de Biblioteca ha completado al menos un escaneo y el Catálogo tiene contenido, el Oyente puede navegar su colección de audio cruzando las cinco dimensiones disponibles: Género, Artista, Álbum, Tipo de contenido (MUSIC / PODCAST) y Playlists.

La navegación sigue una jerarquía natural derivada de los metadatos ID3:

- **Por Tipo de contenido:** separa la música de los podcasts, respetando la frontera semántica del sistema. El Oyente puede restringir la vista a solo música o solo podcasts.
- **Por Género → Artista → Álbum → Pistas:** recorre la taxonomía objetiva de la biblioteca desde la dimensión más amplia hasta la más específica.
- **Por Artista → Álbum → Pistas:** entrada directa por intérprete sin filtro previo de género.
- **Por Álbum → Pistas:** entrada directa por álbum.
- **Por Playlist:** navega las agrupaciones personalizadas del Oyente, mostrando las pistas en el orden definido por él.

El sistema aplica el contrato `TRG-NAV-01` (`BrowseQuery`), que devuelve un `Flow<List<TrackView>>` reactivo: cualquier modificación del Catálogo (nuevo escaneo, edición de metadatos, eliminación de pista) se propaga automáticamente a la vista sin intervención del Oyente.

Los registros centinela (`id = 1` en Artist, Album, Genre) aparecen agrupados bajo etiquetas localizadas ("Sin artista", "Sin álbum", "Sin género") para preservar la no invención de datos (Invariante 4). Los tracks con `availability = UNSUPPORTED` son visibles en la lista pero no reproducibles; los de `availability = MISSING` no aparecen (serán purgados en el próximo escaneo).

La navegación taxonómica es el punto de entrada primario para iniciar la reproducción de un contexto (US-013). Cada nivel de la jerarquía puede convertirse en una `PlaybackSource` (Genre, Artist, Album, PlaylistRef o AdHoc).

---

## Criterios de Aceptación

### Escenario 1: Vista raíz — todas las dimensiones disponibles

- **Dado** que el Catálogo tiene al menos una pista indexada
- **Cuando** el Oyente abre la pantalla principal de biblioteca
- **Entonces** el sistema presenta las dimensiones de navegación disponibles: Tipo de contenido, Género, Artista, Álbum y Playlists; todas accesibles sin pasos previos

### Escenario 2: Filtrar por Tipo de contenido — solo música

- **Dado** que el Catálogo contiene pistas con `contentType = MUSIC` y con `contentType = PODCAST`
- **Cuando** el Oyente selecciona la dimensión "Música"
- **Entonces** la vista muestra únicamente los tracks con `contentType = MUSIC`; los podcasts no aparecen

### Escenario 3: Filtrar por Tipo de contenido — solo podcasts

- **Dado** que el Catálogo contiene pistas con `contentType = PODCAST`
- **Cuando** el Oyente selecciona la dimensión "Podcasts"
- **Entonces** la vista muestra únicamente los tracks con `contentType = PODCAST`; la música no aparece

### Escenario 4: Navegación por Género → listado de artistas del género

- **Dado** que el Catálogo contiene géneros con pistas asociadas
- **Cuando** el Oyente selecciona un género específico
- **Entonces** la vista muestra los artistas que tienen al menos una pista clasificada con ese género, sin pistas de otros géneros

### Escenario 5: Navegación por Artista → álbumes del artista

- **Dado** que el Oyente está en la vista de artistas (con o sin filtro de género previo)
- **Cuando** el Oyente selecciona un artista específico
- **Entonces** la vista muestra los álbumes cuyo `Album.artistId` corresponde al artista seleccionado, con la carátula representativa de cada álbum si existe artwork embebido

### Escenario 6: Navegación por Álbum → pistas del álbum

- **Dado** que el Oyente está en la vista de álbumes de un artista
- **Cuando** el Oyente selecciona un álbum específico
- **Entonces** la vista muestra las pistas del álbum ordenadas por `trackNumber` (ascendente); las pistas sin número de pista aparecen al final

### Escenario 7: Navegación por Playlist → pistas en orden definido por el Oyente

- **Dado** que el Oyente tiene al menos una Playlist con pistas
- **Cuando** el Oyente selecciona una Playlist desde la dimensión de Playlists
- **Entonces** la vista muestra las pistas de la playlist en el orden definido por `PlaylistTrackCrossRef.position` (ascendente), preservando el orden creado por el Oyente

### Escenario 8: Centinelas visibles con etiqueta localizada

- **Dado** que el Catálogo contiene pistas con metadatos ausentes (artista `id=1`, género `id=1`, álbum `id=1`)
- **Cuando** el Oyente navega la dimensión correspondiente
- **Entonces** las pistas sin artista aparecen agrupadas bajo "Sin artista"; las sin género bajo "Sin género"; las sin álbum bajo "Sin álbum"; el texto localizado solo se muestra en la UI y nunca se persiste como dato

### Escenario 9: Tracks UNSUPPORTED visibles pero no accionables como reproducción directa

- **Dado** que el Catálogo contiene tracks con `availability = UNSUPPORTED`
- **Cuando** el Oyente navega cualquier dimensión que incluya esos tracks
- **Entonces** los tracks aparecen en la lista con una indicación visual de que no son reproducibles; el Oyente puede verlos pero no iniciar su reproducción directa

### Escenario 10: Catálogo vacío — lista vacía sin error

- **Dado** que el Catálogo no tiene pistas (biblioteca vacía o ninguna carpeta fuente configurada)
- **Cuando** el Oyente abre cualquier dimensión de navegación
- **Entonces** la vista muestra un estado vacío informativo (no un error); se sugiere al Oyente agregar carpetas fuente o ejecutar un escaneo

### Escenario 11: Dimensión sin coincidencias — lista vacía sin error

- **Dado** que el Oyente aplica un filtro por dimensión que no tiene pistas asociadas (ej. un género sin tracks tras una purga)
- **Cuando** el flujo `BrowseQuery` devuelve una lista vacía
- **Entonces** la vista muestra un estado vacío informativo; nunca lanza un error ni colapsa la navegación

### Escenario 12: Latencia sub-500ms en catálogos grandes

- **Dado** que el Catálogo tiene decenas de miles de pistas
- **Cuando** el Oyente cambia de dimensión o aplica un filtro
- **Entonces** la actualización visual de la lista ocurre en menos de 500 ms ([RNF-01]); la UI no se congela ni muestra estados de carga prolongados

### Escenario 13: Reactividad ante cambios del Catálogo

- **Dado** que el Oyente tiene la pantalla de biblioteca abierta mostrando una dimensión específica
- **Cuando** un escaneo termina y modifica el Catálogo (nuevas pistas, pistas purgadas, metadatos editados)
- **Entonces** la vista se actualiza automáticamente reflejando el nuevo estado del Catálogo, sin que el Oyente deba navegar fuera y volver

### Escenario 14: Hilo principal nunca bloqueado

- **Dado** que el Oyente navega la biblioteca con un Catálogo grande
- **Cuando** la UI construye o actualiza la lista de resultados
- **Entonces** el hilo principal permanece libre; toda consulta a la base de datos ocurre en background threads y los resultados se emiten reactivamente ([CT-08])

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Oyente — único agente humano del sistema, autoridad soberana absoluta
- **Permisos requeridos:** Ninguno adicional. Los permisos SAF y el Catálogo ya están establecidos por US-001, US-002 y US-003. Esta historia es puramente de consulta sobre el Catálogo existente
- **Valor de negocio:** US-010 materializa el Equilibrio de Organización (SDD §1.2): el Catálogo construido por el Motor de Biblioteca solo tiene valor para el Oyente si puede navegarlo de forma fluida y contextual. Sin esta historia, el Oyente tiene una biblioteca indexada pero inaccesible. Es el punto de entrada a toda experiencia de escucha (US-013 depende de esta historia para construir contextos de reproducción). También es el primer contacto del Oyente con la organización de su colección tras el onboarding, por lo que la experiencia debe ser inmediata, fluida y sin fricción

### Reglas de Negocio

- **[RF-12]:** El sistema debe proveer una interfaz visual que permita filtrar y explorar el Catálogo intersectando las dimensiones extraídas (Género, Artista, Álbum, Tipo de contenido) y las Agrupaciones Personalizadas (Playlists)
- **[RNF-01]:** Toda navegación y filtrado debe responder en menos de 500 ms, incluso sobre catálogos de decenas de miles de pistas. Se logra mediante índices de navegación en Room (`genreId`, `artistId`, `albumId`, `sourceFolderId`) y paginación (`PagingSource`)
- **[CT-03]:** El sistema nunca infiere ni autocompleta metadatos ausentes. Los centinelas `id=1` se representan en la UI con etiquetas localizadas; el texto "Sin artista" / "Sin género" nunca se persiste como dato
- **[CT-07]:** Toda transición de pantalla y filtrado responde en menos de 500 ms
- **[CT-08]:** La interfaz nunca se bloquea. Las consultas al Catálogo ocurren en background threads; la UI solo renderiza el estado recibido del `Flow`
- **[Invariante 4]:** La ausencia de un metadato se presenta como "Sin información"; nunca se infiere el valor
- **[Invariante 2]:** Solo aparecen en la vista pistas con `availability = AVAILABLE` o `UNSUPPORTED`; las de `availability = MISSING` están excluidas hasta ser purgadas en el próximo escaneo
- **[F-7] Doble artista (pista vs. álbum):** La navegación "Artista → Álbumes" usa `Album.artistId`; la navegación "Artista → Pistas directas" usa `Track.artistId`. El Catálogo puede mostrar inconsistencias aparentes en compilaciones o colaboraciones; esto es el comportamiento correcto derivado de los metadatos
- **Paginación obligatoria:** Para catálogos de decenas de miles de pistas, la vista debe virtualizar los resultados usando `LazyColumn` con paginación Room (`PagingSource`) para cumplir [RNF-01] y [Restricción 4]

### Interfaz

La pantalla de biblioteca es el destino central de la navegación de Sonus. Actúa como hub desde el que el Oyente puede acceder a todas las dimensiones del Catálogo. Coexiste con el mini-reproductor persistente (siempre visible en la parte inferior cuando hay reproducción activa), que permite al Oyente controlar la reproducción sin abandonar la pantalla de biblioteca.

#### Detalle de Interfaz de Usuario

- **Diseño general:** Pantalla principal de biblioteca con navegación por pestañas o secciones para las dimensiones disponibles (Tipo, Género, Artista, Álbum, Playlist). Cada dimensión muestra una lista virtualizada con scroll. El mini-reproductor persistente ocupa la parte inferior de la pantalla cuando hay reproducción activa (SDD §2.1)
- **Campos y controles:**
  - Selector de dimensión (pestañas o chips): Tipo de contenido, Géneros, Artistas, Álbumes, Playlists
  - Lista de ítems de la dimensión seleccionada (virtualizada con `LazyColumn`)
  - Carátulas en listas de Álbumes (cargadas on-demand desde bytes embebidos, caché solo en memoria — [F-5] / ADR-009)
  - Estado vacío informativo cuando la dimensión no tiene contenido
  - Indicación visual de tracks UNSUPPORTED (no reproducibles) dentro de las listas de pistas
  - Mini-reproductor persistente en la parte inferior (componente compartido, no parte del scope de esta historia)
- **Flujo de navegación visual:**
  - `Biblioteca (raíz)` → `Géneros` → `Artistas del Género` → `Álbumes del Artista` → `Pistas del Álbum`
  - `Biblioteca (raíz)` → `Artistas` → `Álbumes del Artista` → `Pistas del Álbum`
  - `Biblioteca (raíz)` → `Álbumes` → `Pistas del Álbum`
  - `Biblioteca (raíz)` → `Tipo (Música / Podcast)` → `Pistas filtradas`
  - `Biblioteca (raíz)` → `Playlists` → `Pistas de la Playlist`
  - Desde cualquier lista de pistas: el Oyente puede iniciar reproducción → transición al contexto US-013
- **Mensajes y feedback:**
  - Estado vacío de biblioteca: "Tu biblioteca está vacía. Agrega carpetas fuente para comenzar."
  - Estado vacío de dimensión: "No hay [género / artistas / álbumes / playlists] en tu biblioteca."
  - Track UNSUPPORTED: indicador visual (ej. icono de advertencia) junto al ítem; sin texto de error expandido

### Sistemas Externos

- **`Flow<List<TrackView>>` / `Flow<PagingData<TrackView>>` (Canal C2, `TRG-NAV-01`):** fuente de datos reactiva de la pantalla. El `LibraryViewModel` consume `BrowseCatalogUseCase` que traduce `BrowseQuery` a consultas Room indexadas. Cualquier mutación del Catálogo re-emite automáticamente a la UI (Bucle de Coherencia del Catálogo, SDD §4.1)
- **`Flow<NowPlayingState>` (Canal C2, `TRG-OBS-01`):** alimenta el mini-reproductor persistente visible durante la navegación. No es parte del scope de esta historia pero convive en la misma pantalla
- **Sin integraciones externas:** sistema autárquico ([Invariante 1 / RNF-06]). Ninguna llamada de red

### Preview de Interfaz

```
┌─────────────────────────────────────────┐
│  🎵 Mi Biblioteca                        │
│  ┌──────┬──────┬──────┬──────┬───────┐  │
│  │Música│Géneros│Artistas│Álbumes│Listas│  │
│  └──────┴──────┴──────┴──────┴───────┘  │
│                                          │
│  [Géneros — 12]                          │
│  ┌──────────────────────────────────┐    │
│  │ 🎸 Rock                   > 48   │    │
│  │ 🎹 Clásica                > 120  │    │
│  │ 🎷 Jazz                   > 31   │    │
│  │ ❓ Sin género              > 14  │    │
│  └──────────────────────────────────┘    │
│                                          │
│  ▼ Mini-reproductor (siempre visible)    │
│  ┌──────────────────────────────────┐    │
│  │ ▶ Bohemian Rhapsody — Queen  ⏭   │    │
│  └──────────────────────────────────┘    │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  ← Rock  /  Artistas (7)                │
│                                          │
│  ┌──────────────────────────────────┐    │
│  │ 🎤 Queen                   > 12  │    │
│  │ 🎤 Led Zeppelin            > 24  │    │
│  │ 🎤 The Beatles             > 31  │    │
│  │ 🎤 Sin artista              > 3  │    │
│  └──────────────────────────────────┘    │
│                                          │
│  ▼ Mini-reproductor                      │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│  ← Queen  /  Álbumes (3)                │
│                                          │
│  ┌──────────────────────────────────┐    │
│  │ [🖼] A Night at the Opera   > 12 │    │
│  │ [🖼] News of the World      > 11 │    │
│  │ [🖼] Innuendo               > 10 │    │
│  └──────────────────────────────────┘    │
│                                          │
│  ▼ Mini-reproductor                      │
└─────────────────────────────────────────┘
```

---

## Contexto y Referencias

**Arquitectura:**
- `docs/architecture/domain_and_state_model.md` — Entidades Track, Artist, Album, Genre, Playlist, ContentType, TrackAvailability (§2); índices de navegación taxonómica ([F-1]); registros centinela `id=1` (§6.1); Convención de ausencia de datos (§1); nota topológica [F-7] (doble artista pista vs. álbum)
- `docs/architecture/interfaces_contract.md` — `TRG-NAV-01` (Consultar / Filtrar Catálogo), `BrowseQuery`, `TrackSort`, `Flow<List<TrackView>>`; §4.1 Debounce del filtro textual; §4.3 Paginación obligatoria ([Restricción 4])
- `docs/architecture/architecture_blueprint.md` — `LibraryViewModel` + `BrowseCatalogUseCase` + `CatalogRepositoryImpl` + `TrackDao` (C-01 / Datos / C-03); `CatalogRepository.browse()` paginado; ADR-005 (Jetpack Compose + LazyColumn); ADR-009 (carátulas Coil, caché en memoria)
- `docs/domain/definition/requirements_specification.md` — [RF-12] (Navegación Taxonómica Multicapa); [RNF-01] (Latencia < 500ms); [Restricción 4] (decenas de miles de filas)
- `docs/domain/definition/system_definition_document.md` — Equilibrio de Organización (SDD §1.2); Vía principal de organización por metadatos (SDD §1.2); Equifinalidad del doble eje (SDD §3); Bucle de Coherencia del Catálogo (SDD §4.1)

**Historias relacionadas:**
- **US-003** (Escaneo Fundacional) — precondición: el Catálogo debe tener al menos un escaneo completado para que US-010 tenga contenido
- **US-008** (Sincronización Determinista del Catálogo) — produce y actualiza el Catálogo que US-010 navega; los cambios de US-008 se propagan reactivamente a esta historia
- **US-011** (Filtro Textual) — complementa US-010 con búsqueda por texto libre sobre título/artista/álbum; comparte el mismo `BrowseQuery` con el campo `textFilter`
- **US-012** (Ordenamiento del Catálogo) — complementa US-010 permitiendo al Oyente cambiar el criterio de ordenamiento (`TrackSort`) dentro de la misma vista
- **US-013** (Reproducir Contexto) — US-010 es el punto de entrada: desde cualquier nivel de la jerarquía taxonómica el Oyente puede iniciar la reproducción de un contexto (`PlaybackSource`: Genre, Artist, Album, PlaylistRef, AdHoc)

**Lecciones aprendidas:** El diseño de la navegación taxonómica ya está completamente modelado en el dominio (`BrowseQuery`, `TrackView`, `PlaybackSource`). US-010 es la materialización visual de ese contrato; no debe extender ni modificar los contratos de datos existentes. La nota [F-7] del modelo de dominio es crítica para no generar resultados aparentemente inconsistentes al navegar "Artista → Álbumes" vs. "Artista → Pistas directas".

---

## Definición de Terminado (Inicial)

- [ ] Funcionalidad implementada según los 14 criterios de aceptación
- [ ] Pantalla raíz muestra las 5 dimensiones de navegación disponibles
- [ ] Filtro por Tipo de contenido (MUSIC / PODCAST) funcional
- [ ] Jerarquía Género → Artistas → Álbumes → Pistas navegable
- [ ] Jerarquía Artistas → Álbumes → Pistas navegable sin filtro de género
- [ ] Jerarquía Álbumes → Pistas navegable sin filtro previo
- [ ] Navegación por Playlist con pistas en orden definido por el Oyente
- [ ] Centinelas `id=1` mostrados con etiquetas localizadas ("Sin artista", "Sin género", "Sin álbum")
- [ ] Tracks UNSUPPORTED visibles con indicación visual de no reproducibles
- [ ] Estado vacío informativo (catálogo vacío y dimensión sin coincidencias), sin errores
- [ ] Actualización reactiva ante cambios del Catálogo (re-escaneo, edición de metadatos, purga)
- [ ] Latencia de navegación y filtrado < 500 ms en catálogos de decenas de miles de pistas ([RNF-01])
- [ ] Paginación/virtualización implementada con `LazyColumn` + `PagingSource` ([Restricción 4])
- [ ] Hilo principal nunca bloqueado; consultas en background threads ([CT-08])
- [ ] Mini-reproductor persistente visible durante la navegación sin interferir con las listas
- [ ] Sin permiso `android.permission.INTERNET` compilado en el binario ([RNF-06] / [CT-01])
