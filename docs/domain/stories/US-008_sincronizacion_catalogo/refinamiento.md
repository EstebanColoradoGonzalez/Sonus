# Refinamiento Técnico (Developer)

**Autor**: Esteban Colorado González | **Fecha**: 2026-07-21

## Plan: Sincronización Determinista del Catálogo — diff `INCREMENTAL` real (US-008)

**Arquitectura**: Arquitectura Limpia multi-módulo (Presentación → Dominio ← Datos), MVVM +
Single-Activity + Hilt, 100% local (air-gapped). US-008 **completa el Motor de Biblioteca ya construido
en US-003** (contenedor **C-04**): implementa el **diff real por `fileLastModifiedMs`** que US-007 dejó
cableado pero no efectivo. No reconstruye el pipeline: recorrido SAF, extracción ID3 sin invención
(Invariante 4), transacción atómica, purga por fidelidad (Invariante 2) y purga de huérfanos (§6.2)
**ya existen** y satisfacen los ACs 3/4/6/7/9/10. El delta es la **eficiencia del modo `INCREMENTAL`**
(ACs 1/2/8): omitir los archivos sin cambio *antes* de extraer sus metadatos.

> **Feature análoga leída completa**: **US-003 — Escaneo Fundacional** (`CatalogSynchronizer`,
> `CatalogRepositoryImpl`, `TrackDao`, mappers) y **US-007 — Re-escaneo** (cableado de `ScanMode`
> extremo-a-extremo, `refinamiento.md` §"Fronteras de alcance"). Artefactos estudiados completos:
> `CatalogSynchronizer.kt`, `CatalogRepositoryImpl.kt`, `TrackDao.kt`, `Track.kt`, `ScannedTrack.kt`,
> `CatalogMappers.kt`, `SafDataSource.kt`/`DiscoveredFile`, `ScanMode.kt`, `CatalogRepositoryImplTest.kt`,
> `CatalogSynchronizerTest.kt`, `ScanFakes.kt`, `SonusDatabase.kt`, `coding-standards.md`.

### Diagnóstico: qué YA existe (no se reimplementa)

| AC | Escenario | Ya cubierto por (US-003/US-007) |
|----|-----------|---------------------------------|
| 3 | Alta de archivos nuevos | `CatalogSynchronizer.sync` inserta por `uri`; `added` en `ScanSummary` |
| 4 | Purga de tracks con URI ausente | `TrackDao.deleteWhereUriNotIn(uris)` en transacción; `purged` contado |
| 6 | Purga de dimensiones huérfanas | `Album/Genre/ArtistDao.purgeOrphans()`; centinelas `id=1` preservados |
| 7 | `ScanSummary` con 4 contadores | `sync` retorna `ScanSummary(added, purged, unsupported, orphanDimsPurged)` |
| 9 | No invención de datos | Nombres ausentes → `null`/centinela `id=1` (Invariante 4) |
| 10 | Atomicidad | Toda la operación en un único `database.withTransaction {}` |
| — | Background thread ([RNF-03]/[CT-08]) | `LibraryScanWorker : CoroutineWorker` + `dispatcherProvider.io` |

### Delta real de US-008 (lo que SÍ se implementa)

El `mode: ScanMode` **llega hasta `CatalogRepositoryImpl.synchronize()` pero hoy se ignora**: se extraen
los metadatos ID3 de *todos* los archivos descubiertos y se llama `sync(scanned)` sin distinguir modo.
US-008 implementa el diff real:

1. **Gate por `mtime` antes de extraer ID3 (ACs 1/2)**: en modo `INCREMENTAL`, un archivo cuyo
   `DiscoveredFile.lastModifiedMs` coincide con el `fileLastModifiedMs` almacenado en su `Track` **se omite
   sin re-extraer metadatos** (honra AC1: "ni sus metadatos son re-extraídos, y el ciclo completa en menor
   tiempo"). Un archivo con `mtime` distinto (modificado) o sin entrada previa (nuevo) sí se procesa.
2. **Purga sobre el conjunto descubierto completo (AC4)**: la purga debe usar **todas** las URIs
   descubiertas por SAF — incluidas las omitidas por no haber cambiado — para no purgar tracks vigentes.
   `sync` recibe explícitamente el conjunto de URIs descubiertas, desacoplado de la lista procesada.
3. **Modo `FULL` procesa todo (AC8)**: sin fingerprints previos, todos los archivos se procesan
   (comportamiento actual, preservado).

### Decisiones técnicas clave

1. **El gate de omisión vive en `CatalogRepositoryImpl`, no en `sync`**: la extracción ID3 ocurre en el
   bucle del repositorio (pipeline de escaneo); para honrar AC1 ("no re-extraer metadatos") la decisión de
   omitir debe tomarse **antes** de `id3DataSource.readMetadata`. El repositorio consulta las huellas
   (`uri → fileLastModifiedMs`) del catálogo a través de `CatalogSynchronizer.indexedFingerprints()`,
   manteniendo Room detrás de la capa de datos (el repositorio no inyecta DAOs, coherente con su diseño).
2. **`CatalogSynchronizer` sigue siendo el escritor determinista** (blueprint §3: "diff por
   `fileLastModifiedMs`/`uri`, altas, `MISSING`, purga por fidelidad + huérfanos en transacción"):
   `sync(processed, discoveredUris)` hace upsert de lo procesado, purga por `discoveredUris` y purga
   huérfanos, todo en la misma transacción. `added` cuenta solo inserciones.
3. **Huellas vía proyección Room ligera** (`SELECT uri, fileLastModifiedMs FROM track`): evita cargar
   entidades completas y no depende de `@MapInfo`; el mapa se arma en Kotlin.
4. **Comparación por igualdad de `mtime`**: se omite si `stored == discovered`; cualquier diferencia
   (incluida una menor por corrección de reloj del sistema de archivos) fuerza el re-proceso, más seguro
   que `>` estricto para "haya cambiado" (historia §Descripción).

### Fronteras de alcance (lo que NO entra en US-008)

- **AC5 — Purga en cascada de `PlaylistTrackCrossRef` / `QueueItem` / `PlaybackProgress`** → **DIFERIDO**
  (decisión PO, 2026-07-21). Esas tablas **no existen** en el esquema Room (v3, solo `SourceFolder`,
  `Artist`, `Genre`, `Album`, `Track`, `AppSettings`); son responsabilidad de las historias de Playlists,
  Cola y Progreso de Podcast. Misma frontera ya declarada por US-007 (`refinamiento.md` §Fronteras). El
  `onDelete = CASCADE` se configurará al crear cada tabla hija; la purga del `Track` (AC4) **sí** opera hoy.
- **Feedback visual del progreso / resumen (AC7 visual)** → **US-009**.
- **Selector de modo en la UI** → fuera de alcance (US-007 fijó `INCREMENTAL` como default del re-escaneo).

---

## Tareas de Implementación

### Fase 1 — Datos · Diff INCREMENTAL real (`:core:data`)

- [ ] **T1: Huellas del catálogo en `TrackDao`** — proyección `TrackFingerprint(uri, fileLastModifiedMs)`
  + `@Query("SELECT uri, fileLastModifiedMs FROM track") suspend fun fingerprints(): List<TrackFingerprint>`
  (Base: queries existentes de `TrackDao`) — `core/data/.../local/room/dao/TrackDao.kt`
- [ ] **T2: `CatalogSynchronizer` — gate de huellas + `sync` por conjunto descubierto** —
  (a) `suspend fun indexedFingerprints(): Map<String, Long>` que arma el mapa `uri → mtime` desde
  `trackDao.fingerprints()`; (b) cambiar la firma a
  `suspend fun sync(processed: List<ScannedTrack>, discoveredUris: List<String>): ScanSummary`: upsert de
  `processed`, purga con `discoveredUris` (`deleteWhereUriNotIn` / `deleteAll` si vacío), purga de huérfanos,
  todo en la transacción existente (Base: `CatalogSynchronizer.sync` actual) —
  `core/data/.../local/room/CatalogSynchronizer.kt`
- [ ] **T3: `CatalogRepositoryImpl` — gate por `mtime` según modo** — cargar
  `fingerprints = if (mode == INCREMENTAL) catalogSynchronizer.indexedFingerprints() else emptyMap()`;
  en el bucle, omitir la extracción ID3 cuando `mode == INCREMENTAL && fingerprints[file.uri] ==
  file.lastModifiedMs` (el resto se extrae y se agrega a `processed`); publicar progreso `Scanning` por cada
  archivo descubierto (procesado u omitido); llamar
  `catalogSynchronizer.sync(processed, discovered.map { it.second.uri })` (Base: `synchronize` actual) —
  `core/data/.../repository/CatalogRepositoryImpl.kt`

### Fase 2 — Pruebas (JUnit5 · MockK · Truth · Room instrumentado)

- [ ] **T4: `FakeId3DataSource` registra lecturas** — añadir `val readUris = mutableListOf<String>()`
  poblado en `readMetadata` para poder afirmar la NO re-extracción (AC1) —
  `core/data/src/test/.../fake/ScanFakes.kt`
- [ ] **T5: `CatalogRepositoryImplTest` (+casos US-008)** — ajustar el mock de `sync` a la firma de dos
  argumentos; nuevos casos: (a) `INCREMENTAL` con un archivo sin cambio (`mtime` == huella) → **no** aparece
  en `readUris` ni en `processed`, pero su URI **sí** en `discoveredUris` (AC1); (b) `INCREMENTAL` con archivo
  modificado (`mtime` distinto) → extraído y en `processed` (AC2); (c) archivo nuevo → extraído y en
  `processed` (AC3); (d) `FULL` → todos extraídos aunque existan huellas (AC8) —
  `core/data/src/test/.../repository/CatalogRepositoryImplTest.kt`
- [ ] **T6: `CatalogSynchronizerTest` (instrumentado, +casos US-008)** — actualizar las 3 pruebas existentes
  a `sync(processed, discoveredUris)`; nuevos casos: (a) un track cuya URI está en `discoveredUris` pero NO en
  `processed` (omitido por sin-cambio) **se preserva** — no purgado — mientras uno ausente de `discoveredUris`
  se purga, uno nuevo se inserta, y `ScanSummary` cuadra (AC1/AC4 escritura); (b) `indexedFingerprints()`
  devuelve el mapa `uri → fileLastModifiedMs` de los tracks persistidos —
  `core/data/src/androidTest/.../local/room/CatalogSynchronizerTest.kt`

### Fase 3 — Quality gates

- [ ] **T7: Ejecutar el gate** — `ktlintCheck`, `detekt`, `konsistTest`, `testDebugUnitTest`,
  `koverXmlReport` y verificación de aislamiento de red (sin `android.permission.INTERNET`) en verde (100%
  de tests JVM). El test instrumentado (`CatalogSynchronizerTest`) se documenta como verificación aparte si
  no hay emulador disponible.

---

## Checklist de refinamiento

- ☑ **Feature análoga leída completa**: US-003 (Motor de Biblioteca) + US-007 (cableado de `ScanMode`);
  código y tests actuales del `CatalogSynchronizer` y `CatalogRepositoryImpl` estudiados íntegros.
- ☑ **TODOS los artefactos identificados**: DAO (proyección + query), `CatalogSynchronizer` (huellas + nueva
  firma de `sync`), `CatalogRepositoryImpl` (gate por modo), fakes de prueba, tests unitario e instrumentado,
  quality gates. **Sin cambios de esquema Room** (no se añaden entidades ni migración; AC5 diferido) ni de DI.
- ☑ **Respeta arquitectura**: Room detrás de la capa de datos (repositorio no inyecta DAOs), errores como
  valor, dispatchers inyectados, transacción única (atomicidad), air-gapped intacto, sufijos por capa.
- ☑ **Incluye tests** (el proyecto los exige) y quality gates.
- ☑ **Fronteras de alcance explícitas**: AC5 (cascada) diferido a historias de Playlists/Cola/Podcast
  (tablas inexistentes); progreso visual → US-009; selector de modo UI → fuera de alcance.
