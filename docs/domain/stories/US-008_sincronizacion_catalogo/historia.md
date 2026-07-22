# Historia de Usuario

**Como** El Oyente,
**Quiero** que el Catálogo de mi biblioteca se sincronice de forma determinista con el estado real de mis archivos de audio en cada escaneo,
**Para** que la biblioteca refleje fielmente el sistema de archivos en todo momento: los archivos nuevos queden indexados, los eliminados desaparezcan del catálogo y de todas sus referencias (playlists, cola, marcadores), las dimensiones huérfanas se limpien, y los re-escaneos habituales sean eficientes al procesar únicamente los archivos que han cambiado.

## Descripción

El `CatalogSynchronizer` es el núcleo determinista del Motor de Biblioteca (contenedor C-04). Se ejecuta durante la fase `SYNCING` del ciclo de escaneo (`IDLE → SCANNING → SYNCING → IDLE`) y recibe como entrada el conjunto de URIs descubiertas por el recorrido SAF. A partir de ese conjunto, aplica el algoritmo de diferencia contra el estado actual del Catálogo Room:

- **Modo `INCREMENTAL`:** compara el `fileLastModifiedMs` de cada URI descubierta contra el valor almacenado en el `Track` correspondiente. Solo procesa los archivos cuyo `mtime` haya cambiado (nuevos o modificados); los demás se omiten para maximizar la eficiencia en re-escaneos habituales.
- **Modo `FULL`:** procesa todos los archivos descubiertos independientemente de su `mtime`, reconstruyendo el Catálogo de forma completa.

En ambos modos, tras procesar los archivos activos, identifica los `Track` cuyas URIs ya no están presentes en el conjunto descubierto, los marca como `MISSING` y los purga del Catálogo. La purga se ejecuta en cascada a través de las FKs de Room: las referencias en `PlaylistTrackCrossRef`, `QueueItem` y `PlaybackProgress` se eliminan automáticamente junto con el registro `Track`. Finalmente, elimina las entidades `Artist`, `Album` y `Genre` que queden sin tracks asociados, preservando siempre los registros centinela `id = 1`.

Ningún campo de metadato es inventado: la ausencia de etiqueta se representa con centinela `id = 1` para dimensiones normalizadas o `NULL` para campos textuales. La operación completa de sincronización (altas, bajas, purga de cascada y purga de huérfanos) se ejecuta en una única transacción Room para garantizar la atomicidad.

---

## Criterios de Aceptación

### Escenario 1: Modo INCREMENTAL — archivos sin cambio son omitidos

- **Dado** que el Catálogo contiene tracks con `fileLastModifiedMs` igual al `mtime` actual de sus archivos
- **Cuando** se ejecuta el `CatalogSynchronizer` en modo `INCREMENTAL`
- **Entonces** esos tracks no son re-procesados ni sus metadatos son re-extraídos, y el ciclo completa en menor tiempo que un `FULL` equivalente

### Escenario 2: Modo INCREMENTAL — archivos modificados son re-indexados

- **Dado** que un archivo de audio existente en el Catálogo tiene un `fileLastModifiedMs` en disco mayor al almacenado en su `Track`
- **Cuando** se ejecuta en modo `INCREMENTAL`
- **Entonces** ese archivo es re-procesado: sus metadatos ID3 son re-extraídos y el registro `Track` es actualizado con los nuevos valores (incluyendo recálculo de FKs `artistId`/`albumId`/`genreId`)

### Escenario 3: Alta de archivos nuevos

- **Dado** que se descubre una URI de audio que no tiene entrada en el Catálogo
- **Cuando** el `CatalogSynchronizer` la procesa (en cualquier modo)
- **Entonces** se crea un nuevo registro `Track` con `availability = AVAILABLE`, los metadatos ID3 extraídos, y los campos ausentes representados con centinela `id = 1` o `NULL` sin inventar ningún valor

### Escenario 4: Purga de tracks con URI desaparecida

- **Dado** que el Catálogo contiene tracks cuyas URIs ya no están presentes en el conjunto descubierto por el recorrido SAF
- **Cuando** el `CatalogSynchronizer` completa la fase de sincronización
- **Entonces** todos los registros `Track` con URI ausente son eliminados del Catálogo, y el contador `purged` del `ScanSummary` refleja el número de tracks purgados

### Escenario 5: Purga en cascada — referencias eliminadas automáticamente

- **Dado** que uno o más `Track` son purgados del Catálogo
- **Cuando** la transacción de sincronización se confirma en Room
- **Entonces** todas las referencias a esos tracks son eliminadas automáticamente por las FKs `CASCADE` de Room: sus entradas en `PlaylistTrackCrossRef` (vínculos de playlists), en `QueueItem` (cola de reproducción activa) y en `PlaybackProgress` (marcadores de reanudación de podcasts) desaparecen sin dejar rastros

### Escenario 6: Purga de dimensiones huérfanas

- **Dado** que tras purgar tracks, existen registros de `Artist`, `Album` o `Genre` que ya no son referenciados por ningún `Track`
- **Cuando** el `CatalogSynchronizer` ejecuta la limpieza de huérfanos
- **Entonces** esos registros son eliminados de sus respectivas tablas, y el contador `orphanDimsPurged` refleja el total. Los registros centinela `id = 1` de cada tabla **nunca** son eliminados

### Escenario 7: ScanSummary emitido al finalizar el ciclo

- **Dado** que el `CatalogSynchronizer` completó todas las fases de sincronización sin ser abortado
- **Cuando** el ciclo transita a `IDLE`
- **Entonces** se emite un `OperationResult.Success(ScanSummary)` con los cuatro contadores: `added` (tracks nuevos), `purged` (tracks eliminados por URI ausente), `unsupported` (archivos indexados como no reproducibles) y `orphanDimsPurged` (dimensiones huérfanas eliminadas)

### Escenario 8: Modo FULL — todos los archivos son procesados

- **Dado** que se solicita un escaneo en modo `FULL`
- **Cuando** el `CatalogSynchronizer` opera
- **Entonces** todos los archivos descubiertos son procesados independientemente de su `fileLastModifiedMs`, garantizando la reconstrucción completa y coherente del Catálogo

### Escenario 9: No invención de datos bajo ninguna circunstancia

- **Dado** cualquier archivo con metadatos parciales, vacíos o ausentes
- **Cuando** es procesado por el `CatalogSynchronizer`
- **Entonces** ningún campo (título, artista, álbum, género, tipo de contenido) es inferido, autocompletado ni generado; las dimensiones ausentes apuntan al centinela `id = 1` y el título ausente se almacena como `NULL`

### Escenario 10: Atomicidad de la sincronización

- **Dado** que el `CatalogSynchronizer` inicia la fase `SYNCING`
- **Cuando** ejecuta las operaciones de altas, bajas, purga en cascada y purga de huérfanos
- **Entonces** todas las operaciones forman una única transacción Room: si alguna falla, el Catálogo queda en el estado anterior sin cambios parciales aplicados

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Oyente — único agente humano del sistema, autoridad soberana absoluta
- **Permisos requeridos:** Permiso SAF de árbol (`treeUri`) persistido con `takePersistableUriPermission()` sobre cada carpeta fuente configurada ([RF-01])
- **Valor de negocio:** Materializa el Equilibrio de Organización (SDD §1.2) en su nivel más profundo: el `CatalogSynchronizer` es el mecanismo que transforma el resultado bruto del recorrido SAF en un Catálogo preciso y limpio. Sin esta sincronización determinista, el Catálogo acumularía entradas fantasma (archivos eliminados), perdería archivos nuevos, y contaminaría playlists y la cola con referencias a tracks inexistentes. El modo `INCREMENTAL` real activa la eficiencia prometida en la documentación del sistema, diferenciando re-escaneos habituales (rápidos, solo cambios) del escaneo fundacional completo (`FULL`).

### Reglas de Negocio

- **[RF-02]:** Extracción ID3 sin inventar datos; campos ausentes → centinela `id = 1` / `NULL`
- **[RF-03]:** Sincronización determinista del Catálogo: altas de archivos nuevos, purga de URIs desaparecidas y sus referencias, purga de dimensiones huérfanas
- **[RNF-03]:** Sincronización ejecutada en background thread dentro del `CoroutineWorker`; nunca bloquea el hilo principal
- **[RNF-08]:** Purga de dimensiones huérfanas tras cada sincronización para mantener huella de almacenamiento mínima
- **[Invariante 2]:** El Catálogo es un reflejo exacto del sistema de archivos; no se conservan entradas de archivos inexistentes
- **[Invariante 4]:** El Motor de Biblioteca nunca infiere, genera ni completa metadatos faltantes
- **[CT-03]:** Prohibido inferir o autocompletar metadatos ausentes
- **[CT-04]:** Fidelidad al sistema de archivos — purgar entradas al desaparecer el archivo físico
- **[CT-08]:** La interfaz nunca se bloquea; toda operación de sincronización ocurre fuera del hilo principal
- **Modo INCREMENTAL (diferido desde US-007):** el diff real por `fileLastModifiedMs` se implementa en esta historia; US-007 solo cableó el parámetro `ScanMode`
- **Purga en cascada (diferida desde US-006/US-007):** `PlaylistTrackCrossRef`, `QueueItem` y `PlaybackProgress` estaban pendientes de las FKs `CASCADE`; US-008 confirma y prueba este comportamiento

### Interfaz

Esta historia no tiene interfaz visual propia. El `CatalogSynchronizer` es un componente interno del Motor de Biblioteca (C-04) que opera en segundo plano durante la fase `SYNCING` del ciclo de escaneo. La representación visual del progreso y del resultado final del escaneo es responsabilidad exclusiva de **US-009 — Observar el estado y progreso del escaneo**.

### Sistemas Externos

- **Storage Access Framework (SAF) — Canal C5:** Las URIs de los archivos descubiertos por `SafDataSource` son la entrada principal del `CatalogSynchronizer`. Sin acceso de red ni servicios externos.
- **Room/SQLite (persistencia local — C-03):** Las operaciones de alta, baja y purga de huérfanos se ejecutan contra los DAOs de Room (`TrackDao`, `DimensionDao`) en transacciones atómicas. Las FKs `CASCADE` configuradas en el esquema Room materializan la purga automática de `PlaylistTrackCrossRef`, `QueueItem` y `PlaybackProgress`.
- **Sin integraciones externas:** Sistema autárquico ([Invariante 1 / RNF-06]). Ninguna llamada de red.

### Preview de Interfaz

No aplica. Historia de motor interno sin componente visual.

---

## Contexto y Referencias

**Arquitectura:**
- `docs/architecture/domain_and_state_model.md` — Entidades `Track`, `Artist`, `Album`, `Genre`, `SourceFolder`, `PlaylistTrackCrossRef`, `QueueItem`, `PlaybackProgress`; Ciclo del Proceso de Escaneo §5.3; Políticas de Depuración §6.2; Ciclo de Vida del Track §5.2
- `docs/architecture/interfaces_contract.md` — `TRG-LIB-03` (Ejecutar Escaneo / Re-escaneo), `TRG-LIB-04` (Progreso de Escaneo), `ScanState`, `ScanSummary`, `ScanMode`, `ERR_SCAN_ABORTED`
- `docs/architecture/architecture_blueprint.md` — `CatalogSynchronizer` (C-04, §3), `ScanStateEmitter`, `LibraryScanWorker`, `DimensionDao`, purga de huérfanos (§6.2)
- `docs/domain/definition/requirements_specification.md` — [RF-02], [RF-03], [RNF-03], [RNF-08]
- `docs/domain/definition/system_definition_document.md` — Equilibrio de Organización, Invariante 2, Invariante 4, Ciclo de Escaneo §1.3, Bucle de Coherencia del Catálogo §4.1

**Historias relacionadas:**
- **US-003** (Escaneo Fundacional) — donde reside la implementación inicial del `CatalogSynchronizer`; US-008 extiende su modo `INCREMENTAL` y confirma la purga en cascada
- **US-007** (Ejecutar Escaneo / Re-escaneo) — dispara el ciclo que desencadena US-008; diferenció el parámetro `ScanMode` sin implementar el diff real
- **US-009** (Observar el estado y progreso del escaneo) — cubre el feedback visual del `ScanState` emitido por US-008

**Lecciones aprendidas:** Ver `US-007/refinamiento.md` — el diff por `fileLastModifiedMs` y la purga en cascada de `PlaylistTrackCrossRef`/`QueueItem`/`PlaybackProgress` fueron diferidos explícitamente a US-008 al momento de implementar US-007.

---

## Definición de Terminado (Inicial)

- [ ] Funcionalidad implementada según los 10 criterios de aceptación
- [ ] Modo `INCREMENTAL` real: archivos con `fileLastModifiedMs` sin cambio son omitidos; archivos modificados son re-indexados
- [ ] Modo `FULL`: todos los archivos procesados independientemente de `fileLastModifiedMs`
- [ ] Purga de tracks con URI ausente: `purged` contabilizado en `ScanSummary`
- [ ] Purga en cascada verificada: `PlaylistTrackCrossRef`, `QueueItem` y `PlaybackProgress` eliminados por FK `CASCADE` al purgar el `Track`
- [ ] Purga de dimensiones huérfanas: `Artist`/`Album`/`Genre` sin tracks eliminados; centinelas `id = 1` preservados; `orphanDimsPurged` contabilizado
- [ ] `ScanSummary` emitido correctamente con los cuatro contadores
- [ ] Atomicidad verificada: ningún cambio parcial aplicado ante fallo de transacción
- [ ] No invención de datos: campos ausentes = centinela `id = 1` / `NULL` (Invariante 4)
- [ ] Sincronización ejecutada en background thread; hilo principal nunca bloqueado ([RNF-03] / [CT-08])
- [ ] Prueba instrumentada con Room in-memory: diff INCREMENTAL (skip, update, alta, purga), purga en cascada, purga de huérfanos, centinelas preservados
- [ ] Sin permiso `android.permission.INTERNET` compilado en el binario ([RNF-06] / [CT-01])
