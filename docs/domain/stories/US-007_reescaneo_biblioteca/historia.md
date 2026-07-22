# Historia de Usuario

**Como** El Oyente,
**Quiero** poder lanzar manualmente un escaneo o re-escaneo de todas mis carpetas fuente configuradas,
**Para** mantener mi biblioteca actualizada respecto al estado real de mis archivos de audio, de modo que el catálogo refleje fielmente cualquier cambio ocurrido en el sistema de archivos (archivos nuevos, eliminados o modificados) desde el último escaneo.

## Descripción

El Motor de Biblioteca recorre recursivamente todas las carpetas fuente vigentes mediante el Storage Access Framework (SAF), extrae las etiquetas ID3 de cada archivo de audio descubierto y sincroniza el Catálogo de forma determinista: incorpora los archivos nuevos (`Track.availability = AVAILABLE`), marca como `MISSING` los que ya no existen físicamente y los purga junto con todas sus referencias en playlists y en la cola de reproducción. Tras la sincronización, elimina las dimensiones huérfanas (`Artist`, `Album`, `Genre`) que hayan quedado sin tracks asociados, conservando siempre los registros centinela `id = 1`. Los campos de metadatos ausentes se representan con centinela `id = 1` o `NULL`, nunca con valores inventados.

El escaneo soporta dos modos: `INCREMENTAL` (diff por `fileLastModifiedMs`, más eficiente para re-escaneos habituales) y `FULL` (reconstrucción total del catálogo). Solo un escaneo puede estar activo a la vez (single-flight). Si la operación supera un segundo, reporta progreso determinista a través del flujo `ScanState` — la visualización de ese progreso es responsabilidad de **US-009**.

---

## Criterios de Aceptación

### Escenario 1: Escaneo exitoso con catálogo vacío (primera vez)

- **Dado** que el Oyente tiene al menos una carpeta fuente configurada con permiso SAF vigente y el catálogo está vacío
- **Cuando** lanza el escaneo (modo `INCREMENTAL` o `FULL`)
- **Entonces** el Motor de Biblioteca recorre recursivamente las carpetas, extrae metadatos ID3, registra cada archivo soportado como `Track.availability = AVAILABLE`, y el estado del ciclo transita `IDLE → SCANNING → SYNCING → IDLE` al finalizar exitosamente

### Escenario 2: Re-escaneo detecta y registra archivos nuevos

- **Dado** que el Oyente tiene archivos de audio en las carpetas fuente que no están en el catálogo
- **Cuando** el escaneo finaliza
- **Entonces** cada archivo nuevo queda registrado en el Catálogo con sus metadatos extraídos; los campos de metadato ausentes se representan con centinela `id = 1` o `NULL`, sin inventar ningún valor

### Escenario 3: Re-escaneo purga archivos eliminados

- **Dado** que archivos previamente indexados ya no existen físicamente en las carpetas fuente
- **Cuando** el escaneo finaliza
- **Entonces** las entradas correspondientes son purgadas del Catálogo, sus referencias en todas las playlists son eliminadas automáticamente, y sus entradas en la cola de reproducción activa también son purgadas

### Escenario 4: Purga de dimensiones huérfanas

- **Dado** que la sincronización eliminó tracks cuyas dimensiones (`Artist`, `Album`, `Genre`) ya no son referenciadas por ningún otro track
- **Cuando** el ciclo de `SYNCING` concluye
- **Entonces** los registros huérfanos de `Artist`, `Album` y `Genre` son eliminados, conservando siempre los centinelas `id = 1`

### Escenario 5: Resumen de escaneo al completar

- **Dado** que el escaneo completó sin ser abortado
- **Cuando** el ciclo transita a `IDLE`
- **Entonces** se emite un `OperationResult<ScanSummary>` con los contadores: `added` (tracks nuevos indexados), `purged` (tracks eliminados), `unsupported` (archivos no reproducibles), `orphanDimsPurged` (dimensiones huérfanas purgadas)

### Escenario 6: Single-flight — no escaneos concurrentes

- **Dado** que ya existe un escaneo activo (estado `SCANNING` o `SYNCING`)
- **Cuando** el Oyente o el sistema intenta lanzar un segundo escaneo
- **Entonces** la nueva solicitud es ignorada o fusionada con el ciclo activo sin iniciar un segundo proceso paralelo

### Escenario 7: Operación asíncrona — interfaz nunca bloqueada

- **Dado** que el escaneo está en curso (posiblemente sobre una biblioteca grande)
- **Cuando** el Motor de Biblioteca está procesando archivos
- **Entonces** el hilo principal de la interfaz nunca se bloquea; si la operación supera 1 segundo, se emite periódicamente `ScanState.Scanning(processed, total)` con progreso determinista

### Escenario 8: Degradación grácil ante permiso revocado o carpeta inaccesible

- **Dado** que una carpeta fuente tiene su permiso SAF revocado o se vuelve inaccesible durante el escaneo
- **Cuando** el Motor de Biblioteca intenta acceder a ella
- **Entonces** el escaneo emite `ScanState.Aborted(DomainError.ScanAborted(reason))` con la causa subyacente (ej. `ERR_PERMISSION_REVOKED`), se conserva el último Catálogo coherente, y no se inventan ni se purgan entradas por error de acceso transitorio

### Escenario 9: Archivos en formato no soportado

- **Dado** que se descubren archivos cuyo formato no puede ser decodificado por el Motor de Reproducción
- **Cuando** el escaneo los procesa
- **Entonces** se indexan con `Track.availability = UNSUPPORTED` (visibles en la biblioteca, no reproducibles) y se contabilizan en `ScanSummary.unsupported`

### Escenario 10: No invención de datos bajo ninguna circunstancia

- **Dado** cualquier archivo de audio con metadatos parciales, vacíos o ausentes
- **Cuando** el Motor de Biblioteca lo indexa
- **Entonces** ningún campo de metadato (título, artista, álbum, género, tipo de contenido) es inferido, autocompletado ni generado algorítmicamente; la ausencia se representa estrictamente con centinela `id = 1` para dimensiones normalizadas o `NULL` para campos textuales directos

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Oyente — único agente humano del sistema, autoridad soberana absoluta
- **Permisos requeridos:** Permiso SAF de árbol (`treeUri`) persistido mediante `takePersistableUriPermission()` sobre cada carpeta fuente configurada ([RF-01])
- **Valor de negocio:** Mantiene el Equilibrio de Organización (SDD §1.2): el Catálogo de Biblioteca es en todo momento un reflejo fiel de los archivos realmente presentes en el dispositivo. Sin este mecanismo, la biblioteca se degrada con el tiempo acumulando entradas fantasma y perdiendo archivos nuevos no indexados.

### Reglas de Negocio

- **[RF-02]:** Extracción ID3 sin inventar datos; campos ausentes → centinela `id = 1` / `NULL`
- **[RF-03]:** Sincronización determinista del Catálogo: altas de archivos nuevos, marcado de `MISSING`, purga de huérfanos
- **[RNF-03]:** Escaneo asíncrono en background thread; si supera 1 segundo, reportar progreso determinista; nunca bloquear el hilo principal
- **[Invariante 2]:** El Catálogo solo contiene entradas de archivos que existen físicamente en las carpetas fuente configuradas; no se conservan entradas fantasma
- **[Invariante 4]:** El Motor de Biblioteca nunca infiere, genera ni completa metadatos faltantes
- **[CT-03]:** Prohibido inferir o autocompletar metadatos ausentes
- **[CT-04]:** Fidelidad al sistema de archivos — purgar entradas al desaparecer el archivo
- **[CT-08]:** La interfaz nunca se bloquea; toda operación de I/O ocurre fuera del hilo principal
- **Single-flight (§4.1 interfaces_contract):** Solo un ciclo de escaneo activo a la vez; solicitudes concurrentes se ignoran o fusionan
- **Modos de escaneo:** `ScanMode.INCREMENTAL` (diff por `fileLastModifiedMs`) y `ScanMode.FULL` (reconstrucción total)

### Interfaz

La acción de lanzar el escaneo manual se ubica en la sección de configuración / gestión de carpetas fuente de la aplicación. El punto de entrada exacto en la UI (botón, menú, gesto) no está especificado en este ciclo de creación y se definirá en el análisis arquitectónico.

> **Gap conocido (diseño):** La ubicación precisa del control de re-escaneo en la jerarquía de pantallas no fue especificada en el intake. Se resuelve en la fase de Análisis Arquitectónico.

El feedback visual del progreso del escaneo (indicador de progreso, mensajes de estado, resumen al finalizar) es responsabilidad de **US-009 — Observar el estado y progreso del escaneo**.

### Sistemas Externos

- **Storage Access Framework (SAF) — Canal C5:** Único mecanismo de acceso al sistema de archivos. Lectura recursiva de directorios y extracción de bytes de audio vía URIs de árbol con permiso persistido. Sin acceso a red ni servicios externos.
- **Room/SQLite (persistencia local):** Escritura transaccional del Catálogo (`Track`, `Artist`, `Album`, `Genre`), purga de vínculos en `PlaylistTrackCrossRef`, `QueueItem` y `PlaybackProgress`.
- **Sin integraciones externas:** Sistema autárquico ([Invariante 1 / RNF-06]). Ninguna llamada de red, ningún servicio en la nube.

### Preview de Interfaz

No aplica en este nivel de historia. La representación visual del estado del escaneo se especifica en **US-009**.

---

## Contexto y Referencias

**Arquitectura:**
- `docs/architecture/domain_and_state_model.md` — Entidades `Track`, `SourceFolder`, `Artist`, `Album`, `Genre`; Ciclo del Proceso de Escaneo §5.3; Políticas de Depuración §6.2
- `docs/architecture/interfaces_contract.md` — `TRG-LIB-03` (Ejecutar Escaneo / Re-escaneo), `TRG-LIB-04` (Progreso de Escaneo), `ScanState`, `ScanSummary`, `ScanMode`, `ERR_SCAN_ABORTED`
- `docs/domain/definition/requirements_specification.md` — [RF-02], [RF-03], [RNF-03]
- `docs/domain/definition/system_definition_document.md` — Equilibrio de Organización, Invariante 2, Invariante 4, Ciclo de Escaneo §1.3, Apalancamiento 2

**Historias relacionadas:**
- **US-005** (Agregar Carpeta Fuente) — precondición: el escaneo requiere ≥ 1 carpeta fuente configurada con permiso vigente
- **US-006** (Remover Carpeta Fuente con purga en cascada) — puede disparar un re-escaneo posterior para verificar coherencia
- **US-008** (Sincronización determinista del Catálogo) — especifica en detalle el mecanismo de altas, bajas y purga de huérfanos que US-007 desencadena
- **US-009** (Observar el estado y progreso del escaneo) — cubre el feedback visual del progreso emitido por `ScanState.Scanning` / `ScanState.Finished` / `ScanState.Aborted`

**Lecciones aprendidas:** N/A (primera historia de este tipo en el proyecto)

---

## Definición de Terminado (Inicial)

- [ ] Funcionalidad implementada según los 10 criterios de aceptación
- [ ] Validaciones funcionando correctamente (single-flight, sin escaneos concurrentes)
- [ ] Mensajes de estado y error implementados (`ScanState`, `ScanSummary`, `ERR_SCAN_ABORTED`)
- [ ] Prueba unitaria del ciclo completo de escaneo (`IDLE → SCANNING → SYNCING → IDLE`)
- [ ] Prueba de sincronización determinista: alta de archivos nuevos, purga de `MISSING`, purga de dimensiones huérfanas
- [ ] Verificación de single-flight (segunda solicitud no genera escaneo paralelo)
- [ ] Verificación de no invención de datos: campos ausentes = centinela / NULL, no valores inferidos (Invariante 4)
- [ ] Escaneo ejecutado en background thread; hilo principal nunca bloqueado ([RNF-03] / [CT-08])
- [ ] Sin permiso `android.permission.INTERNET` compilado en el binario ([RNF-06] / [CT-01])
