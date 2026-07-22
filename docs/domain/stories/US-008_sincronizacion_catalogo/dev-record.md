## Dev Agent Record — Dev-Rápido

### Debug Log

| # | Tipo | Descripción | Resolución |
|---|------|-------------|------------|
| 1 | Entorno | Gradle detectaba Java 1.7 (`JAVA_HOME` legado) | Se fijó `JAVA_HOME` a JDK 17 (Eclipse Adoptium) para el gate |
| 2 | Alcance | AC5 (cascada) referencia tablas `PlaylistTrackCrossRef`/`QueueItem`/`PlaybackProgress` inexistentes en el esquema Room v3 | Diferido por decisión PO (2026-07-21), alineado con la frontera declarada en US-007 |

### Completion Notes

- ⚡ Dev-Rápido: implementado el **diff `INCREMENTAL` real** del `CatalogSynchronizer` (US-008). El
  `mode: ScanMode` que US-007 cableó hasta `CatalogRepositoryImpl` ahora es efectivo: en `INCREMENTAL`, un
  archivo cuyo `DiscoveredFile.lastModifiedMs` coincide con el `fileLastModifiedMs` persistido se **omite
  sin re-extraer ID3** (AC1); los modificados (AC2) y nuevos (AC3) se procesan. La purga usa el conjunto
  descubierto **completo**, preservando los omitidos vigentes (AC4). `FULL` procesa todo (AC8).
- El gate de omisión vive en `CatalogRepositoryImpl` (donde ocurre la extracción ID3, para honrar AC1);
  `CatalogSynchronizer` sigue siendo el escritor determinista y expone `indexedFingerprints()` (mapa
  `uri → mtime`) manteniendo Room detrás de la capa de datos.
- ACs ya satisfechos por US-003/US-007 y confirmados: altas, purga por fidelidad, purga de huérfanos,
  centinelas `id=1`/`NULL` (Invariante 4), `ScanSummary` de 4 contadores, atomicidad (transacción única),
  background thread ([RNF-03]).
- **AC5 (purga en cascada) DIFERIDO**: las tablas hijas no existen en el esquema (v3). Sin cambios de
  esquema Room ni migración en esta historia. Se resolverá vía `onDelete = CASCADE` al crearse cada tabla
  (historias de Playlists / Cola / Progreso de Podcast).
- Quality gate JVM en verde: `ktlintCheck`, `detekt`, `testDebugUnitTest` (`:core:data` debug+release),
  Konsist/arquitectura (`:app`), `verifyNoInternetPermission`, `koverXmlReport`. El test instrumentado
  (`CatalogSynchronizerTest`, Room in-memory) queda como verificación aparte (requiere emulador).

### File List

| Acción | Archivo | Descripción |
|--------|---------|-------------|
| Creado | `core/data/.../local/room/dao/TrackFingerprint.kt` | Proyección `uri → fileLastModifiedMs` para el diff INCREMENTAL |
| Modificado | `core/data/.../local/room/dao/TrackDao.kt` | Query `fingerprints()` que devuelve las huellas del catálogo |
| Modificado | `core/data/.../local/room/CatalogSynchronizer.kt` | `indexedFingerprints()` + `sync(processed, discoveredUris)` (purga por conjunto descubierto) |
| Modificado | `core/data/.../repository/CatalogRepositoryImpl.kt` | Gate por `mtime` según `ScanMode`: omite extracción ID3 de archivos sin cambio |
| Modificado | `core/data/src/test/.../fake/ScanFakes.kt` | `FakeId3DataSource.readUris` registra lecturas (aserción AC1) |
| Modificado | `core/data/src/test/.../repository/CatalogRepositoryImplTest.kt` | Casos INCREMENTAL (skip/modificado/nuevo) + FULL (AC8) + firma de dos args |
| Modificado | `core/data/src/androidTest/.../local/room/CatalogSynchronizerTest.kt` | Preservación de omitidos + `indexedFingerprints()` + firma de dos args |

### Métricas Dev-Rápido

- Tiempo sesión IA: 30 min
- Tareas manuales DoD: 0 min
- Tiempo total: 30 min
