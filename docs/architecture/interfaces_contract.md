# Contrato de Interfaces e Interacciones

> Este documento define los "puertos" de comunicación del sistema. Debe detallar estrictamente cómo el entorno interactúa con el sistema (entradas) y cómo el sistema responde (salidas). **Debe mantenerse agnóstico a la presentación visual**, enfocándose puramente en la estructura de los datos, eventos, comandos o llamadas.
>

## 1. Protocolos y Canales de Comunicación

*Define los canales a través de los cuales el sistema escucha y emite información.*

> **Premisa rectora — Ausencia total de red.** Sonus es un sistema *air-gapped* por diseño (**Invariante 1 — Autarquía Absoluta** / **[RNF-06]**). En consecuencia, **no existe ningún protocolo de red** (HTTP, WebSockets, gRPC, sockets remotos) ni formato de intercambio *on-the-wire*. Todos los "puertos" del sistema son **locales** y se resuelven en tres planos: (a) **intra-proceso**, mediante flujo de datos reactivo entre las capas de Arquitectura Limpia bajo el patrón **Model–ViewModel–View** con enfoque **Single-Activity**; (b) **inter-proceso con el Sistema Operativo** (IPC vía *Binder*/callbacks: sesión de medios, foco de audio, servicio en primer plano); y (c) **contra el sistema de archivos** local a través del *Storage Access Framework* (SAF). Esta sección describe la **estructura** de esos puertos (comandos, eventos, estado), permaneciendo **agnóstica a la presentación visual**: no define pantallas ni layouts, sino los contratos de datos que cruzan cada frontera.

### 1.1. Inventario de Canales (Puertos del Sistema)

Materializa las cinco interfaces Sistema ↔ Entorno del SDD (§2.1) sobre mecanismos nativos de Android. La Interfaz Visual se desdobla en dos canales —comando (C1) y estado (C2)—, de ahí seis canales para cinco interfaces:

| # | Canal (Puerto) | Dirección | Mecanismo Android nativo | Naturaleza del intercambio | Trazabilidad |
|---|---|---|---|---|---|
| **C1** | **Comando del Oyente** (superficie de interacción) | Entrada: Oyente → Sistema | Invocación intra-proceso View → ViewModel (funciones sobre el `ViewModel`; sin serialización) | Comandos discretos modelados como objetos inmutables Kotlin (`sealed interface` de intenciones/acciones) | [RF-01, RF-04..RF-08, RF-12], SDD §3.1 "Comando del Oyente" |
| **C2** | **Estado del Sistema** (retroalimentación) | Salida: Sistema → Oyente | Flujo reactivo `StateFlow<UiState>` / `Flow` expuesto por el ViewModel; eventos de una sola vez (*one-shot*) vía canal separado | Estado inmutable observable (UiState) + eventos efímeros (errores, avisos) | [RNF-01], SDD §3.1 "Evento de Estado de Reproducción", Bucle de Control de Reproducción |
| **C3** | **Sesión de Medios** (control en segundo plano) | Bidireccional: Sistema ↔ SO ↔ Oyente | `MediaSession` (androidx.media3.session) publicando estado y recibiendo `MediaButton`/comandos de transporte desde notificación y pantalla de bloqueo | Estado de reproducción (metadatos de pista, posición) hacia el SO; comandos de transporte (play/pause/next/prev) hacia el sistema | [RF-13], SDD §2.1 "Controles en Notificación" |
| **C4** | **Negociación con el SO** (supervivencia y foco) | Bidireccional: Sistema ↔ SO | `Foreground Service` (tipo `mediaPlayback`) + callbacks de `AudioManager` (`AudioFocusRequest`, broadcast `ACTION_AUDIO_BECOMING_NOISY`) | Señales de ciclo de vida y foco de audio; el sistema reacciona (pausa/*ducking*/resume) a eventos que no origina | [RNF-04, RNF-05], [RF-10, RF-11], SDD §2.1 "Negociación con el SO", Perturbaciones 3/6/7 |
| **C5** | **Acceso al Sistema de Archivos** (descubrimiento y persistencia física) | Bidireccional: Sistema ↔ Sistema de archivos | *Storage Access Framework* (`ACTION_OPEN_DOCUMENT_TREE`, `DocumentFile`, URIs de árbol con permiso persistido) para lectura/escritura de bytes y etiquetas ID3 | Bytes de archivos de audio (lectura de metadatos ID3, escritura de modificaciones, eliminación física) | [RF-01, RF-02, RF-03, RF-04, RF-06], Invariante 2, SDD §2.1 "Acceso al Sistema de Archivos" |
| **C6** | **Salida de Audio** (emisión acústica) | Salida: Sistema → Hardware | APIs de decodificación/emisión de la capa estándar del SO (`MediaCodec`/`AudioTrack` a través del reproductor de medios de AndroidX) | Flujo de audio decodificado (PCM) entregado al *pipeline* de audio del dispositivo | [RF-07], [RNF-02, RNF-09], SDD §3.1 "Flujo de Audio Decodificado" |

**Nota de frontera arquitectónica (MVVM / Single-Activity).** El límite entre C1/C2 (intra-proceso) y el resto de canales (IPC/archivos) es deliberado: la capa de **presentación** (única `Activity` anfitriona + destinos de navegación internos) solo conoce C1 y C2. Los canales C3–C6 son operados por las capas de **dominio** y **datos** (repositorios, casos de uso, el `Service` del Motor de Reproducción y el Motor de Biblioteca), nunca directamente por la View. Esto preserva la agnosticidad a la presentación exigida por este documento y la separación de responsabilidades de la Arquitectura Limpia.

### 1.2. Canal Principal, Formato y Autorización

- **Canal Principal:** **Flujo de datos reactivo unidireccional intra-proceso** entre View y ViewModel (canales **C1 + C2**), que constituye la vía primaria de interacción del Oyente soberano. Los **comandos ascienden** (View → ViewModel, canal C1) y el **estado desciende** (ViewModel → View vía `StateFlow<UiState>`, canal C2). Este canal principal se complementa —no se sustituye— con el canal de control en segundo plano **C3 (MediaSession)**, que expone un subconjunto reducido de esos mismos comandos cuando la aplicación no está en primer plano ([RF-13]). No existe un canal de red porque el sistema es autárquico ([RNF-06] / Invariante 1).

- **Formato de Intercambio Base:** **Objetos inmutables en memoria de Kotlin**; no hay serialización a un formato textual de transporte (JSON/XML) porque no se cruza ninguna frontera de red ni de proceso remoto. Concretamente:
  - **Comandos (C1):** jerarquías `sealed interface` / `sealed class` que modelan cada intención del Oyente como un tipo cerrado y exhaustivo (habilita `when` sin rama `else`), coherente con los Dominios Cerrados del modelo de estado (§4 del *domain_and_state_model*).
  - **Estado y eventos (C2):** `data class` inmutable de UiState transportada por `StateFlow`; los avisos efímeros (errores de pista, fin de escaneo) viajan por un flujo *one-shot* separado (p. ej. `Channel`/`SharedFlow`) para no re-emitirse ante recomposiciones o cambios de configuración.
  - **Persistencia (C5, interno):** entidades **Room (SQLite)** para el Catálogo, playlists, configuración y estado de sesión; los enums se serializan por **nombre estable** de constante, nunca por *ordinal* (§1 del modelo de dominio).
  - **Metadatos (C5, externo):** etiquetas **ID3 binarias** leídas/escritas sobre los bytes del archivo (ID3v1/ID3v2, Vorbis Comments según formato — [Restricción 3]).
  - **Audio (C6):** **flujo binario decodificado (PCM)** gestionado por el *framework* de medios estándar; nunca se materializa como dato de aplicación.
  - **Carátulas:** **bytes embebidos** leídos *on-demand* desde el propio archivo (`hasEmbeddedArtwork = true`); huella en disco cero ([RNF-08], [F-5] del modelo de dominio). Nunca URLs remotas.

- **Autenticación/Autorización:** **No existe autenticación** en sentido clásico: el sistema es monousuario y el Oyente es la **autoridad soberana absoluta** (Invariante 3), sin cuentas, credenciales, tokens ni sesiones de identidad. La **autorización se delega íntegramente al modelo de permisos del Sistema Operativo Android**, que constituye la única frontera de control de acceso del sistema:
  - **Mecanismo único de acceso — SAF con permisos persistidos por carpeta:** el descubrimiento y el acceso a los archivos se realizan **exclusivamente** vía *Storage Access Framework*. Cada `SourceFolder.treeUri` conserva permiso de lectura/escritura de árbol mediante `takePersistableUriPermission()` ([RF-01], §2 del modelo de dominio). Es el **único** modelo de acceso al almacenamiento, coherente con la identidad natural del `Track` (URI de contenido SAF, `domain_and_state_model §1`). El permiso lo concede el Oyente en el flujo de primera ejecución (Apalancamiento 5, SDD §4.1; [Restricción 6]).
  - **Permisos de media *runtime* deliberadamente NO requeridos:** al descubrir por SAF (no por *MediaStore*), el sistema **no declara ni solicita** `READ_MEDIA_AUDIO` (API 33+) ni `READ_EXTERNAL_STORAGE` (legado). El permiso de árbol persistido es la única concesión de acceso a los archivos de audio; añadir permisos de media runtime sería redundante y divergiría del modelo SAF adoptado en el dominio.
  - **`POST_NOTIFICATIONS`** (API 33+) para la notificación persistente del control en segundo plano ([RF-13]).
  - **`FOREGROUND_SERVICE`** + **`FOREGROUND_SERVICE_MEDIA_PLAYBACK`** para el proceso de alta prioridad del Motor de Reproducción ([RNF-04]).
  - **Ausencia deliberada y verificable de `android.permission.INTERNET`:** su no declaración en el manifiesto es la garantía estructural de la autarquía y debe auditarse como criterio de aceptación ([RNF-06] / Invariante 1). Ningún canal de este documento requiere red.

  En síntesis: la "frontera de autorización" de Sonus **no es un intercambio de credenciales, sino el conjunto de permisos concedidos por el SO**; revocarlos (p. ej. el permiso de almacenamiento) cierra el canal C5 y degrada el sistema de forma grácil y comunicada, nunca de forma silenciosa ([Restricción 6], §5.3 del modelo de dominio).

## 2. Catálogo de Triggers e Interacciones

*Esta es la sección central. Por cada acción o evento que el sistema pueda recibir, define el contrato exacto de entrada y salida.*

### 2.0. Convenciones del Catálogo (Semántica común a todos los triggers)

Todo trigger se clasifica por su **canal de origen** (ver §1.1): **C1** (comando del Oyente, intra-proceso View→ViewModel), **C3** (comando de transporte vía `MediaSession`) o **C4/C5** (evento originado por el SO, el hardware o el motor). Reglas transversales:

- **Formato de entrada:** objeto inmutable Kotlin, **no JSON de red** (§1.2). Los comandos del Oyente se modelan como jerarquías `sealed interface` despachadas al ViewModel/caso de uso correspondiente. Los identificadores de entidad son la **clave subrogada** `id: Long` del modelo de dominio (`Track.id`, `Playlist.id`, `SourceFolder.id`).
- **Semántica de salida:** la mayoría de comandos son *fire-and-forget*: **no retornan un valor síncrono**; su efecto se refleja en el **estado observable** (Canal C2, `StateFlow<UiState>`). Las operaciones que pueden **fallar de forma significativa** (I/O de disco, escritura ID3, permisos) además emiten un desenlace puntual por el flujo *one-shot* de eventos, tipado como `OperationResult`. Los códigos de `DomainError` se catalogan en la **Sección 3**.
- **Envelope de resultado y comando base** (referenciado por los bloques siguientes):

```kotlin
// Desenlace puntual de una operación (viaja por el flujo one-shot de eventos, no por UiState).
sealed interface OperationResult<out T> {
    data class Success<out T>(val data: T) : OperationResult<T>
    data class Failure(val error: DomainError) : OperationResult<Nothing>   // DomainError -> Sección 3
}

typealias TrackId    = Long   // == Track.id  (identidad natural subyacente: Track.uri, §2 modelo de dominio)
typealias PlaylistId = Long   // == Playlist.id
typealias FolderId   = Long   // == SourceFolder.id
```

- **Trazabilidad:** cada trigger referencia su(s) requerimiento(s) de origen ([RF-xx]/[RNF-xx]), invariantes aplicables y la transición de estado que dispara en el `domain_and_state_model` (§5).

---

### 2.1. Módulo: Indexación y Descubrimiento (Motor de Biblioteca)

*Canales C1 (comando) y C5 (acceso a archivos). Gobierna el **Equilibrio de Organización**. Todo escaneo corre en *background thread* ([RNF-03]).*

#### `TRG-LIB-01`: Agregar Carpeta Fuente

- **Tipo de Trigger (Entrada):** Comando C1 `LibraryCommand.AddSourceFolder`, precedido por el selector SAF del SO (`ACTION_OPEN_DOCUMENT_TREE`) que devuelve el `treeUri` con permiso persistible.
- **Descripción:** Registra un directorio como origen de escaneo, toma el permiso persistido (`takePersistableUriPermission`) y **encola un escaneo** de la nueva carpeta ([RF-01]). Acto de apalancamiento crítico (SDD §4.1, Apalancamiento 2).

**Payload / Parámetros (Input):**

```kotlin
data class AddSourceFolder(
    val treeUri: String,      // Obligatorio. URI de árbol SAF devuelto por el selector del SO. Único (Index unique en SourceFolder.treeUri).
    val displayPath: String   // Obligatorio. Ruta legible derivada del treeUri para mostrar al Oyente.
)
```

**Respuesta / Salida (Output Esperado):**

- **Estado / Efecto:** alta de una fila `SourceFolder`; el catálogo comienza a poblarse tras el escaneo. Se observa vía C2 (lista de carpetas) y por el estado de escaneo (`TRG-LIB-04`).
- **Evento emitido:** `OperationResult<FolderId>`.

```kotlin
// Success -> FolderId de la carpeta creada.
// Failure(DomainError.PermissionDenied)        -> el SO no otorgó permiso persistible sobre el treeUri.
// Failure(DomainError.DuplicateSourceFolder)   -> el treeUri ya estaba registrado.
```

#### `TRG-LIB-02`: Remover Carpeta Fuente

- **Tipo de Trigger (Entrada):** Comando C1 `LibraryCommand.RemoveSourceFolder`.
- **Descripción:** Elimina una carpeta fuente. En cascada (`onDelete = CASCADE`, §3 modelo de dominio) purga todos los `Track` descubiertos bajo ella y, transitivamente, sus vínculos de playlist, cola y marcadores; a continuación dispara **purga de dimensiones huérfanas** (Artist/Album/Genre no referenciados, salvo centinela id=1) ([RF-01]/[RF-03], Invariante 2).

**Payload / Parámetros (Input):**

```kotlin
data class RemoveSourceFolder(
    val folderId: FolderId    // Obligatorio. Carpeta a remover. Debe existir.
)
```

**Respuesta / Salida (Output Esperado):**

- **Estado / Efecto:** baja de `SourceFolder` + purga en cascada; el catálogo observable (C2) se contrae. Se libera el permiso SAF (`releasePersistableUriPermission`).
- **Evento emitido:** `OperationResult<Unit>` — `Failure(DomainError.EntityNotFound)` si `folderId` no existe.

#### `TRG-LIB-03`: Ejecutar Escaneo / Re-escaneo

- **Tipo de Trigger (Entrada):** Comando C1 `LibraryCommand.Scan`. Disparadores: primera ejecución (onboarding), re-escaneo manual del Oyente, o cambio en el conjunto de carpetas fuente. Transición `IDLE → SCANNING` (§5.3 modelo de dominio).
- **Descripción:** Recorre recursivamente las carpetas fuente vigentes vía SAF, extrae etiquetas ID3 ([RF-02]) y **sincroniza el Catálogo de forma determinista**: da de alta archivos nuevos, marca/purga los inexistentes (`MISSING`) y elimina sus referencias en playlists ([RF-03], Invariante 2). Campos ausentes → centinela id=1 / `NULL`, **nunca inferidos** (Invariante 4).

**Payload / Parámetros (Input):**

```kotlin
data class Scan(
    val mode: ScanMode = ScanMode.INCREMENTAL   // Opcional. FULL = reconstrucción total; INCREMENTAL = diff por fileLastModifiedMs.
)
enum class ScanMode { FULL, INCREMENTAL }
```

**Respuesta / Salida (Output Esperado):**

- **Estado / Efecto:** el Catálogo (`Track` + dimensiones) queda como reflejo fiel del sistema de archivos; estado del ciclo `SCANNING → SYNCING → IDLE` observable por `TRG-LIB-04`.
- **Evento emitido:** al cierre, `OperationResult<ScanSummary>`.

```kotlin
data class ScanSummary(
    val added: Int,          // Tracks nuevos indexados.
    val purged: Int,         // Tracks MISSING eliminados del catálogo.
    val unsupported: Int,    // Archivos indexados como UNSUPPORTED ([Restricción 2]).
    val orphanDimsPurged: Int// Artist/Album/Genre huérfanos eliminados ([RNF-08]).
)
// Failure(DomainError.ScanAborted(reason)) -> escaneo interrumpido; 'reason' = causa subyacente (p. ej. PermissionRevoked por [Restricción 6],
//   o carpeta inaccesible). Se conserva el último catálogo coherente (§5.3). ERR_SCAN_ABORTED envuelve la causa en 'details' (ver §3.2).
```

#### `TRG-LIB-04`: Progreso de Escaneo *(salida / evento de estado)*

- **Tipo de Trigger (Entrada):** No es un comando; es el **canal de salida C2** que emite el progreso del ciclo de escaneo. Obligatorio si la operación supera 1 s ([RNF-03]) para evitar la percepción de fallo.
- **Descripción:** Publica de forma continua el estado del Motor de Biblioteca hacia la capa de presentación.

**Payload / Parámetros (Input):** *(N/A — solo salida)*

**Respuesta / Salida (Output Esperado):**

- **Código de Éxito / Estado:** flujo `StateFlow<ScanState>` (Canal C2).

```kotlin
sealed interface ScanState {
    data object Idle : ScanState
    data class Scanning(val processed: Int, val total: Int?) : ScanState  // total NULL mientras se enumera; progreso determinista ([RNF-03]).
    data object Syncing : ScanState
    data class Finished(val summary: ScanSummary) : ScanState
    data class Aborted(val reason: DomainError) : ScanState               // p. ej. permiso revocado.
}
```

---

### 2.2. Módulo: Soberanía Informacional (Metadatos, Playlists y Eliminación)

*Canal C1 (comando) + C5 (escritura física). Materializa la **soberanía del Oyente** (Invariante 3) y la **no invención de datos** (Invariante 4).*

#### `TRG-META-01`: Editar Metadatos de Pista

- **Tipo de Trigger (Entrada):** Comando C1 `MetadataCommand.EditTags`.
- **Descripción:** Escribe las etiquetas ID3 modificadas **directamente en el archivo físico** ([RF-04]) vía SAF y propaga el cambio al Catálogo: recalcula FKs `artistId/albumId/genreId` (creando dimensiones si no existen) y **purga dimensiones que queden huérfanas** (Bucle de Coherencia del Catálogo, SDD §4.1). Transición `AVAILABLE → AVAILABLE` (§5.2). La UI refleja el cambio de forma optimista y persiste en segundo plano.

**Payload / Parámetros (Input):**

```kotlin
data class EditTags(
    val trackId: TrackId,           // Obligatorio. Pista objetivo.
    val title: String?,             // Opcional. NULL = fijar ausente (sentinela §1 modelo de dominio). No se envía => sin cambio.
    val artistName: String?,        // Opcional. Cadena vacía/blanco => centinela id=1. No se envía => sin cambio.
    val albumName: String?,         // Opcional. Idem centinela.
    val genreName: String?,         // Opcional. Idem centinela.
    val trackNumber: Int?,          // Opcional. NULL admitido.
    val releaseYear: Int?,          // Opcional. NULL admitido.
    val contentType: ContentType?   // Opcional. MUSIC|PODCAST|UNKNOWN. Permite corregir la frontera semántica ([RF-04]).
) // Semántica de campos "no enviados": patrón parcial (solo se mutan los campos presentes en el intent concreto).
```

**Respuesta / Salida (Output Esperado):**

- **Estado / Efecto:** etiqueta ID3 reescrita en disco + fila `Track` y dimensiones actualizadas; la vista del catálogo (C2) refleja la nueva clasificación.
- **Evento emitido:** `OperationResult<Unit>`.

```kotlin
// Failure(DomainError.TagWriteFailed)     -> el formato no admite escritura de ese tag ([Restricción 3]) o falló la I/O.
// Failure(DomainError.FileUnavailable)    -> el archivo ya no existe (=> pasa a MISSING, §5.2).
// Failure(DomainError.PermissionRevoked)  -> sin permiso de escritura SAF sobre la carpeta origen.
```

#### `TRG-PLST-01`: Crear Playlist

- **Tipo de Trigger (Entrada):** Comando C1 `PlaylistCommand.Create`.
- **Descripción:** Crea una agrupación personalizada vacía ([RF-05]). Organización subjetiva independiente de los metadatos.

**Payload / Parámetros (Input):**

```kotlin
data class CreatePlaylist(
    val name: String   // Obligatorio. No vacío tras trim. No se exige unicidad global (el Oyente es soberano).
)
```

**Respuesta / Salida (Output Esperado):**

- **Estado / Efecto:** alta de fila `Playlist`; visible en C2.
- **Evento emitido:** `OperationResult<PlaylistId>` — `Failure(DomainError.ValidationBlankName)` si el nombre queda vacío.

#### `TRG-PLST-02`: Renombrar Playlist

- **Tipo de Trigger (Entrada):** Comando C1 `PlaylistCommand.Rename`.
- **Descripción:** Cambia el nombre y actualiza `updatedAtMs` ([RF-05]).

**Payload / Parámetros (Input):**

```kotlin
data class RenamePlaylist(
    val playlistId: PlaylistId,  // Obligatorio. Debe existir.
    val newName: String          // Obligatorio. No vacío tras trim.
)
```

**Respuesta / Salida (Output Esperado):**

- **Evento emitido:** `OperationResult<Unit>` — `EntityNotFound` | `ValidationBlankName`.

#### `TRG-PLST-03`: Poblar Playlist (Agregar Pistas)

- **Tipo de Trigger (Entrada):** Comando C1 `PlaylistCommand.AddTracks`.
- **Descripción:** Anexa una o más pistas al final de la playlist, asignando `position` contigua desde el máximo actual ([RF-05]/[F-8]). Operación **idempotente**: los `trackId` ya presentes se ignoran. La **clave primaria compuesta `(playlistId, trackId)`** de `PlaylistTrackCrossRef` (§2 modelo de dominio) hace que una pista aparezca **como máximo una vez** por playlist — los duplicados son imposibles por esquema.

**Payload / Parámetros (Input):**

```kotlin
data class AddTracksToPlaylist(
    val playlistId: PlaylistId,      // Obligatorio.
    val trackIds: List<TrackId>      // Obligatorio. No vacío. Orden de la lista = orden de anexado. Los ya presentes se ignoran (PK compuesta).
)
```

**Respuesta / Salida (Output Esperado):**

- **Estado / Efecto:** altas en `PlaylistTrackCrossRef` con posiciones contiguas; `updatedAtMs` actualizado.
- **Evento emitido:** `OperationResult<Int>` (nº de pistas efectivamente añadidas) — `EntityNotFound` si la playlist o algún track no existe.

#### `TRG-PLST-04`: Remover Pista de Playlist

- **Tipo de Trigger (Entrada):** Comando C1 `PlaylistCommand.RemoveTrack`.
- **Descripción:** Quita el vínculo pista↔playlist y **recompacta posiciones** para mantener contigüidad sin huecos ([F-8]). No afecta al `Track` ni al archivo.

**Payload / Parámetros (Input):**

```kotlin
data class RemoveTrackFromPlaylist(
    val playlistId: PlaylistId,  // Obligatorio.
    val trackId: TrackId         // Obligatorio.
)
```

**Respuesta / Salida (Output Esperado):**

- **Evento emitido:** `OperationResult<Unit>` — idempotente (remover un vínculo inexistente es `Success`).

#### `TRG-PLST-05`: Reordenar Pistas de Playlist

- **Tipo de Trigger (Entrada):** Comando C1 `PlaylistCommand.Reorder`.
- **Descripción:** Reescribe las posiciones de la playlist en **una sola transacción**, preservando contigüidad y unicidad `(playlistId, position)` ([F-8]).

**Payload / Parámetros (Input):**

```kotlin
data class ReorderPlaylist(
    val playlistId: PlaylistId,          // Obligatorio.
    val orderedTrackIds: List<TrackId>   // Obligatorio. Permutación EXACTA del contenido actual (misma cardinalidad y elementos).
)
```

**Respuesta / Salida (Output Esperado):**

- **Evento emitido:** `OperationResult<Unit>` — `Failure(DomainError.ReorderMismatch)` si la lista no es una permutación exacta del contenido actual.

#### `TRG-PLST-06`: Eliminar Playlist

- **Tipo de Trigger (Entrada):** Comando C1 `PlaylistCommand.Delete`.
- **Descripción:** Borra la playlist; en cascada elimina sus vínculos `PlaylistTrackCrossRef` (no los tracks) ([RF-05], §3 modelo de dominio).

**Payload / Parámetros (Input):**

```kotlin
data class DeletePlaylist(
    val playlistId: PlaylistId   // Obligatorio.
)
```

**Respuesta / Salida (Output Esperado):**

- **Evento emitido:** `OperationResult<Unit>` — `EntityNotFound`.

#### `TRG-FILE-01`: Eliminar Archivo Físico *(acción destructiva irreversible)*

- **Tipo de Trigger (Entrada):** Comando C1 `LibraryCommand.DeleteFile`, **precedido obligatoriamente por confirmación explícita del Oyente** (Invariante 5). La confirmación es un prerrequisito del contrato, no un parámetro opcional.
- **Descripción:** Elimina físicamente el archivo del dispositivo vía SAF y purga en cascada su `Track`, vínculos de playlist, `QueueItem` y `PlaybackProgress` ([RF-06], Invariante 2). Transición `AVAILABLE|UNSUPPORTED → (purgado)` (§5.2). Tras purgar, dispara limpieza de dimensiones huérfanas.

**Payload / Parámetros (Input):**

```kotlin
data class DeleteFile(
    val trackId: TrackId,        // Obligatorio. Pista/archivo a eliminar.
    val confirmed: Boolean       // Obligatorio. DEBE ser true; el caso de uso rechaza la operación si es false (Invariante 5).
)
```

**Respuesta / Salida (Output Esperado):**

- **Estado / Efecto:** archivo borrado del sistema de archivos + purga en cascada; si la pista estaba en la cola activa, el motor salta a la siguiente ([RF-09]).
- **Evento emitido:** `OperationResult<Unit>`.

```kotlin
// Failure(DomainError.ConfirmationRequired) -> confirmed=false (guardia de Invariante 5).
// Failure(DomainError.PermissionRevoked)    -> sin permiso SAF de borrado.
// Failure(DomainError.FileUnavailable)      -> el archivo ya no existía (se purga el registro igualmente => coherencia Invariante 2).
```

---

### 2.3. Módulo: Ejecución Acústica (Motor de Reproducción)

*Canal C1/C3 (comandos de transporte) → efecto sobre `PlaybackState` (singleton persistido) y la máquina de estados runtime `PlaybackStatus` (§5.1). Gobierna el **Equilibrio de Continuidad** e **Invariante 6**. Al abandonar una pista `PODCAST` (pausa, detención o salto), se persiste su marcador `PlaybackProgress` (§2 modelo de dominio).*

#### `TRG-PLAY-01`: Reproducir Contexto (Construir Cola)

- **Tipo de Trigger (Entrada):** Comando C1 `PlayerCommand.PlayContext`.
- **Descripción:** Construye la **Cola de Reproducción** a partir de un contexto de origen (álbum, artista, género, playlist, o selección ad-hoc) e inicia la reproducción desde una pista dada. Persiste `QueueItem` (doble orden `originalPosition`/`playbackPosition`, [F-3]) y actualiza `PlaybackState`. Transición `IDLE/STOPPED → PREPARING → PLAYING` (§5.1).

**Payload / Parámetros (Input):**

```kotlin
data class PlayContext(
    val source: PlaybackSource,      // Obligatorio. Define el conjunto y su orden secuencial base.
    val startTrackId: TrackId?,      // Opcional. Pista inicial; NULL => primera del contexto.
    val startShuffled: Boolean = false
)
sealed interface PlaybackSource {
    data class Album(val albumId: Long) : PlaybackSource
    data class Artist(val artistId: Long) : PlaybackSource
    data class Genre(val genreId: Long) : PlaybackSource
    data class PlaylistRef(val playlistId: PlaylistId) : PlaybackSource
    data class AdHoc(val trackIds: List<TrackId>) : PlaybackSource   // Selección explícita del Oyente.
}
```

**Respuesta / Salida (Output Esperado):**

- **Estado / Efecto:** cola persistida + `PlaybackState.currentQueueItemId` apuntando a la pista inicial; emisión de audio (C6) en tiempo sub-segundo ([RNF-02]).
- **Evento emitido:** `OperationResult<Unit>` — `Failure(DomainError.EmptyPlayableQueue)` si ninguna pista del contexto es `AVAILABLE`.

#### `TRG-PLAY-02`: Alternar Play / Pausa

- **Tipo de Trigger (Entrada):** Comando C1 o C3 `PlayerCommand.TogglePlayPause` (equivale a `MediaButton` PLAY/PAUSE desde notificación/lockscreen, [RF-13]).
- **Descripción:** Conmuta entre `PLAYING` y `PAUSED`. Al pausar persiste `resumeStatus=PAUSED`, `positionMs` y, si es PODCAST, `PlaybackProgress` (§5.1).

**Payload / Parámetros (Input):** `object TogglePlayPause` *(sin parámetros; opera sobre la pista actual)*

**Respuesta / Salida (Output Esperado):**

- **Estado / Efecto:** transición de estado reflejada en C2/C3 (mini-reproductor y notificación) y persistida en `PlaybackState`.
- **Evento emitido:** ninguno (fire-and-forget); *no-op* seguro si el estado es `IDLE`/`STOPPED`.

#### `TRG-PLAY-03` / `TRG-PLAY-04`: Siguiente / Anterior

- **Tipo de Trigger (Entrada):** Comandos C1/C3 `PlayerCommand.Next` / `PlayerCommand.Previous`.
- **Descripción:** Avanza/retrocede según `playbackPosition` respetando `RepeatMode`/`shuffle` ([RF-08]). `Previous` reinicia la pista actual si `positionMs` supera un umbral (p. ej. 3 s), si no salta a la anterior. Transición `→ PREPARING` (§5.1).

**Payload / Parámetros (Input):** `object Next` / `object Previous`

**Respuesta / Salida (Output Esperado):**

- **Estado / Efecto:** nuevo `currentQueueItemId`, `positionMs=0`, nueva emisión de audio. En `RepeatMode.OFF` sin siguiente ⇒ `STOPPED`.
- **Evento emitido:** ninguno; salto automático de pistas no `AVAILABLE` conforme a [RF-09].

#### `TRG-PLAY-05`: Buscar Posición (Seek)

- **Tipo de Trigger (Entrada):** Comando C1/C3 `PlayerCommand.SeekTo`.
- **Descripción:** Reposiciona el cursor de reproducción dentro de la pista actual sin cambiar de estado (`PLAYING`/`PAUSED` se preservan; §5.1).

**Payload / Parámetros (Input):**

```kotlin
data class SeekTo(
    val positionMs: Long   // Obligatorio. 0 <= positionMs <= Track.durationMs. Fuera de rango => se clampa.
)
```

**Respuesta / Salida (Output Esperado):**

- **Estado / Efecto:** `PlaybackState.positionMs` actualizado; C2/C3 reflejan la nueva posición.
- **Evento emitido:** ninguno.

#### `TRG-PLAY-06`: Detener

- **Tipo de Trigger (Entrada):** Comando C1/C3 `PlayerCommand.Stop`.
- **Descripción:** Cierra la sesión activa: `→ STOPPED`, persiste `resumeStatus=STOPPED` y libera el foco de audio ([RF-07]). El Foreground Service puede finalizar si no hay sesión pausada pendiente.

**Payload / Parámetros (Input):** `object Stop`

**Respuesta / Salida (Output Esperado):**

- **Estado / Efecto:** motor en `STOPPED`; notificación retirada o degradada.
- **Evento emitido:** ninguno.

#### `TRG-PLAY-07`: Fijar Modo de Repetición

- **Tipo de Trigger (Entrada):** Comando C1/C3 `PlayerCommand.SetRepeatMode`.
- **Descripción:** Establece `RepeatMode` (OFF|ONE|ALL) y lo persiste en `PlaybackState` ([RF-08]).

**Payload / Parámetros (Input):**

```kotlin
data class SetRepeatMode(
    val mode: RepeatMode   // Obligatorio. OFF | ONE | ALL (§4 modelo de dominio).
)
```

**Respuesta / Salida (Output Esperado):**

- **Estado / Efecto:** `PlaybackState.repeatMode` actualizado; reflejado en C2/C3.
- **Evento emitido:** ninguno.

#### `TRG-PLAY-08`: Fijar Aleatorio (Shuffle)

- **Tipo de Trigger (Entrada):** Comando C1/C3 `PlayerCommand.SetShuffle`.
- **Descripción:** Activa/desactiva shuffle de forma **reversible** ([F-3]): al activar recalcula `playbackPosition` (permutación) preservando `originalPosition`; al desactivar restaura `playbackPosition ← originalPosition`. Persiste `shuffleEnabled`.

**Payload / Parámetros (Input):**

```kotlin
data class SetShuffle(
    val enabled: Boolean   // Obligatorio. true = permutar la cola; false = volver al orden secuencial estable.
)
```

**Respuesta / Salida (Output Esperado):**

- **Estado / Efecto:** cola reordenada por `playbackPosition`, puntero `currentQueueItemId` preservado; `PlaybackState.shuffleEnabled` persistido.
- **Evento emitido:** ninguno.

#### `TRG-QUEUE-01`: Encolar Pista(s) (Añadir a la cola / Reproducir a continuación)

- **Tipo de Trigger (Entrada):** Comando C1 `PlayerCommand.Enqueue`.
- **Descripción:** Inserta una o más pistas en la **cola activa sin reconstruirla** (SDD §3.2: la cola "puede ser modificada en tiempo real"). Modo `PLAY_NEXT` inserta justo después del puntero `currentQueueItemId`; `APPEND` añade al final. Reescribe `originalPosition`/`playbackPosition` de los items afectados en una sola transacción, preservando contigüidad ([F-8]). A diferencia de las playlists, la cola **admite el mismo `trackId` repetido** (§2/§3 modelo de dominio).

**Payload / Parámetros (Input):**

```kotlin
data class Enqueue(
    val trackIds: List<TrackId>,     // Obligatorio. No vacío.
    val mode: EnqueueMode = EnqueueMode.APPEND
)
enum class EnqueueMode { PLAY_NEXT, APPEND }
```

**Respuesta / Salida (Output Esperado):**

- **Estado / Efecto:** altas en `QueueItem` con órdenes recompactados; `currentQueueItemId` inalterado. Si la cola estaba vacía con el motor en `IDLE`, **no** arranca reproducción automáticamente (soberanía del Oyente, Invariante 3).
- **Evento emitido:** `OperationResult<Unit>` — `Failure(DomainError.EntityNotFound)` si algún `trackId` no existe.

#### `TRG-QUEUE-02`: Remover de la Cola

- **Tipo de Trigger (Entrada):** Comando C1 `PlayerCommand.RemoveFromQueue`.
- **Descripción:** Elimina un `QueueItem` de la cola activa y **recompacta** ambos órdenes ([F-8]). Si el item removido es el actual (`currentQueueItemId`), el puntero avanza a la siguiente posición reproducible (o `STOPPED` si la cola queda vacía).

**Payload / Parámetros (Input):**

```kotlin
data class RemoveFromQueue(
    val queueItemId: Long   // Obligatorio. Identifica la POSICIÓN de cola (QueueItem.id), no el trackId (evita ambigüedad con repetidos, [F-2]).
)
```

**Respuesta / Salida (Output Esperado):**

- **Estado / Efecto:** baja de `QueueItem` + recompactación; puntero reajustado si procede.
- **Evento emitido:** `OperationResult<Unit>` — idempotente si el item ya no existe.

#### `TRG-QUEUE-03`: Reordenar la Cola

- **Tipo de Trigger (Entrada):** Comando C1 `PlayerCommand.ReorderQueue`.
- **Descripción:** Reescribe el orden **efectivo** (`playbackPosition`) de la cola en una sola transacción, preservando contigüidad y unicidad ([F-8]). El puntero `currentQueueItemId` se conserva (apunta a la posición, no al índice, [F-2]). Interacción con shuffle: el reordenamiento manual redefine el orden efectivo vigente; `originalPosition` (orden secuencial base) permanece intacto para la reversibilidad de shuffle ([F-3]).

**Payload / Parámetros (Input):**

```kotlin
data class ReorderQueue(
    val orderedQueueItemIds: List<Long>   // Obligatorio. Permutación EXACTA de los QueueItem.id de la cola actual.
)
```

**Respuesta / Salida (Output Esperado):**

- **Evento emitido:** `OperationResult<Unit>` — `Failure(DomainError.ReorderMismatch)` si no es permutación exacta de la cola actual.

---

### 2.4. Módulo: Interacciones Originadas por el Entorno (SO / Hardware / Motor)

*Triggers **no iniciados por el Oyente** (Canales C4/C6 y eventos internos del motor). El sistema es el **consumidor**: reacciona para preservar los equilibrios de Continuidad y Soberanía.*

#### `TRG-ENV-01`: Cambio de Foco de Audio

- **Tipo de Trigger (Entrada):** Evento C4 — callback `AudioManager.OnAudioFocusChangeListener` del SO.
- **Descripción:** El SO notifica ganancia/pérdida del foco de audio (llamada, otra app, notificación). El motor reacciona según [RF-10] / Perturbación 6 (§4.2 SDD).

**Payload / Parámetros (Input):**

```kotlin
sealed interface AudioFocusEvent {
    data object LossTransient : AudioFocusEvent           // Pérdida breve => ducking (atenúa volumen, sigue PLAYING).
    data object LossTransientCanDuck : AudioFocusEvent    // Ducking explícito.
    data object LossPermanent : AudioFocusEvent           // Pérdida prolongada => PAUSED (no autoreanuda).
    data object Gain : AudioFocusEvent                    // Recupera foco => restaura volumen o reanuda si venía de pérdida transitoria.
}
```

**Respuesta / Salida (Output Esperado):**

- **Estado / Efecto:** transición `PLAYING → PLAYING(ducking)` o `PLAYING → PAUSED` según el tipo (§5.1). El audio de Sonus **nunca se superpone** a una app con prioridad ([RF-10]).
- **Evento emitido:** actualización de estado por C2/C3.

#### `TRG-ENV-02`: Desconexión de Salida de Audio (Becoming Noisy)

- **Tipo de Trigger (Entrada):** Evento C4 — broadcast `AudioManager.ACTION_AUDIO_BECOMING_NOISY` (auriculares desconectados / Bluetooth desvinculado).
- **Descripción:** **Pausa instantánea** de la reproducción para evitar emisión inesperada por el altavoz ([RF-11] / Perturbación 7). Requiere reanudación **manual** del Oyente.

**Payload / Parámetros (Input):** *(broadcast sin datos de payload relevantes)*

**Respuesta / Salida (Output Esperado):**

- **Estado / Efecto:** `PLAYING → PAUSED` (persistido). No hay autoreanudación al reconectar.
- **Evento emitido:** actualización de estado por C2/C3.

#### `TRG-ENV-03`: Comando de Transporte de Medios (MediaButton)

- **Tipo de Trigger (Entrada):** Evento C3 — `MediaSession.Callback` desde la notificación persistente, la pantalla de bloqueo o auriculares con botón ([RF-13]).
- **Descripción:** Traduce comandos de transporte del SO a los comandos internos del módulo 2.3 (play/pause/next/previous/seek/stop). Es el **espejo en segundo plano** del Canal C1.

**Payload / Parámetros (Input):**

```kotlin
sealed interface MediaButtonCommand {
    data object PlayPause : MediaButtonCommand   // -> TRG-PLAY-02
    data object Next : MediaButtonCommand        // -> TRG-PLAY-03
    data object Previous : MediaButtonCommand    // -> TRG-PLAY-04
    data class SeekTo(val positionMs: Long) : MediaButtonCommand  // -> TRG-PLAY-05
    data object Stop : MediaButtonCommand        // -> TRG-PLAY-06
}
```

**Respuesta / Salida (Output Esperado):**

- **Estado / Efecto:** idéntico al del comando interno equivalente; el `MediaSession` publica el nuevo `PlaybackState` al SO.
- **Evento emitido:** actualización de metadatos y estado en la sesión de medios (C3).

#### `TRG-ENV-04`: Fin o Fallo de Pista *(evento interno del motor)*

- **Tipo de Trigger (Entrada):** Evento interno del Motor de Reproducción (callback del reproductor de medios): pista completada, error de decodificación o archivo ausente.
- **Descripción:** Núcleo de la **Invariante 6** y [RF-09]. Ante fin normal avanza según `RepeatMode`; ante error marca el `Track` (`UNSUPPORTED`/`MISSING`) y **salta automáticamente a la siguiente pista `AVAILABLE`**, sin interrumpir la sesión. Solo cae a `STOPPED` si no queda ninguna pista válida.

**Payload / Parámetros (Input):**

```kotlin
sealed interface TrackEndEvent {
    data object Completed : TrackEndEvent                     // Fin normal => siguiente según RepeatMode/shuffle.
    data class DecodeError(val trackId: TrackId) : TrackEndEvent   // => Track.availability = UNSUPPORTED; salta.
    data class FileMissing(val trackId: TrackId) : TrackEndEvent   // => Track.availability = MISSING; salta y marca para purga.
}
```

**Respuesta / Salida (Output Esperado):**

- **Estado / Efecto:** transición `PLAYING/PREPARING → ERROR → PREPARING(siguiente)` o `→ STOPPED` (§5.1); el `Track` afectado cambia de disponibilidad (§5.2).
- **Evento emitido:** aviso *one-shot* no bloqueante al Oyente (p. ej. "pista omitida"), sin detener el flujo de escucha.

#### `TRG-ENV-05`: Terminación Forzada y Restauración de Sesión

- **Tipo de Trigger (Entrada):** Evento C4 — el SO termina el proceso (OOM kill, políticas MIUI/HyperOS); posteriormente, reinicio de la aplicación.
- **Descripción:** Al reiniciar, el sistema **restaura la sesión** releyendo `PlaybackState` + `QueueItem` (ordenados por `playbackPosition`) y reposicionando con `currentQueueItemId`, en **< 2 s** ([RNF-05] / Perturbación 3). Transición `IDLE → restauración`. **Sin autoplay:** se restaura según `resumeStatus` (`PAUSED`/`STOPPED`), nunca en `PLAYING`.

**Payload / Parámetros (Input):** *(no hay comando; el disparador es el ciclo de vida del proceso)*

**Respuesta / Salida (Output Esperado):**

- **Estado / Efecto:** pista, `positionMs`, cola (doble orden) y modo restaurados; motor listo para reanudar desde `positionMs`.
- **Evento emitido:** estado de sesión rehidratado publicado por C2/C3.

---

### 2.5. Módulo: Navegación y Observación de Estado (Consulta)

*Canal C1 (consulta) → Canal C2 (flujo observable). Sirve al **Equilibrio de Organización** y a la latencia sub-500ms ([RNF-01]). Puramente estructural — sin definir presentación visual.*

#### `TRG-NAV-01`: Consultar / Filtrar Catálogo

- **Tipo de Trigger (Entrada):** Consulta C1 `LibraryQuery.Browse` (observada, no imperativa: retorna un flujo que se re-emite ante cambios del catálogo).
- **Descripción:** Intersecta las dimensiones taxonómicas (Género, Artista, Álbum, Tipo) y las agrupaciones (Playlist) para producir la vista filtrada ([RF-12]). Se apoya en los índices de navegación del modelo de dominio para cumplir [RNF-01].

**Payload / Parámetros (Input):**

```kotlin
data class BrowseQuery(
    val contentType: ContentType? = null,   // Filtro por tipo (MUSIC/PODCAST/UNKNOWN); NULL = todos.
    val genreId: Long? = null,               // Filtro por género (FK). NULL = todos.
    val artistId: Long? = null,              // Filtro por artista de la PISTA (Track.artistId, ver [F-7]).
    val albumId: Long? = null,               // Filtro por álbum.
    val playlistId: PlaylistId? = null,      // Restringe a los tracks de una agrupación.
    val availability: TrackAvailability? = null, // p. ej. ocultar UNSUPPORTED. NULL = todos.
    val sort: TrackSort = TrackSort.TITLE_ASC,   // Orden de presentación (dato NO comportamental).
    val textFilter: String? = null          // Filtro textual local sobre título/artista/álbum. NULL = sin filtro.
)
enum class TrackSort { TITLE_ASC, TITLE_DESC, ALBUM_ASC, ARTIST_ASC, DATE_ADDED_DESC, TRACK_NUMBER_ASC }
```

**Respuesta / Salida (Output Esperado):**

- **Código de Éxito / Estado:** flujo `Flow<List<TrackView>>` (Canal C2), donde `TrackView` es el modelo de dominio proyectado para presentación (los centinelas id=1 y `title=NULL` se resuelven a la etiqueta localizada en la capa de presentación, §1). La ausencia de coincidencias devuelve **lista vacía**, no un error.

#### `TRG-OBS-01`: Observar Estado de Reproducción

- **Tipo de Trigger (Entrada):** Suscripción C2 `PlayerQuery.ObserveNowPlaying` (canal de salida continuo).
- **Descripción:** Expone el estado en tiempo real del Motor de Reproducción a la presentación (mini-reproductor y pantalla completa) y, en paralelo, alimenta el `MediaSession` (C3) para la notificación/lockscreen ([RF-13]). Alimenta el Bucle de Control de Reproducción (SDD §4.1).

**Payload / Parámetros (Input):** *(N/A — solo salida)*

**Respuesta / Salida (Output Esperado):**

- **Código de Éxito / Estado:** flujo `StateFlow<NowPlayingState>` (Canal C2).

```kotlin
data class NowPlayingState(
    val status: PlaybackStatus,      // IDLE|PREPARING|PLAYING|PAUSED|STOPPED|ERROR (runtime, §4/§5.1). NO se persiste.
    val currentTrack: TrackView?,    // Pista actual proyectada; NULL si IDLE/cola vacía.
    val positionMs: Long,            // Posición actual (ms).
    val durationMs: Long,            // Duración de la pista actual (ms).
    val repeatMode: RepeatMode,      // OFF|ONE|ALL.
    val shuffleEnabled: Boolean,     // Estado de aleatorio.
    val queueSize: Int               // Nº de items en la cola activa.
)
```

---

### 2.6. Módulo: Configuración del Sistema (AppSettings)

*Canal C1 → persistencia local en el singleton `AppSettings` (§2/§6.1 modelo de dominio). Preferencias **operativas** del Oyente; jamás datos comportamentales ([RNF-07] / Invariante 3).*

#### `TRG-CFG-01`: Fijar Preferencia de Tema

- **Tipo de Trigger (Entrada):** Comando C1 `SettingsCommand.SetTheme`.
- **Descripción:** Actualiza `AppSettings.themePreference`. Preferencia visual pura, no comportamental.

**Payload / Parámetros (Input):**

```kotlin
data class SetTheme(
    val theme: ThemePreference   // Obligatorio. SYSTEM | LIGHT | DARK (§4 modelo de dominio).
)
```

**Respuesta / Salida (Output Esperado):**

- **Estado / Efecto:** `AppSettings.themePreference` persistido; el tema efectivo se propaga vía C2.
- **Evento emitido:** ninguno (*fire-and-forget*).

#### `TRG-CFG-02`: Completar Flujo de Primera Ejecución (Onboarding)

- **Tipo de Trigger (Entrada):** Comando C1 `SettingsCommand.CompleteOnboarding`.
- **Descripción:** Marca `AppSettings.onboardingCompleted = true` al cerrar el flujo de primera ejecución (permisos → selección de Carpetas Fuente → escaneo inicial; Apalancamiento 5, SDD §4.1). **Idempotente**: fijarlo cuando ya es `true` es *no-op*.

**Payload / Parámetros (Input):** `object CompleteOnboarding` *(sin parámetros)*

**Respuesta / Salida (Output Esperado):**

- **Estado / Efecto:** `onboardingCompleted = true` persistido; los arranques posteriores omiten el flujo de primera ejecución.
- **Evento emitido:** ninguno.

## 3. Manejo de Errores y Excepciones

*Define la estructura estandarizada que el sistema devolverá cuando una interacción falle, y los códigos específicos de error que las interfaces consumidoras deben manejar.*

### 3.0. Filosofía de Manejo de Errores (Principios Rectores)

El tratamiento de fallos en Sonus no es genérico: está subordinado a las invariantes del dominio. Cinco principios lo gobiernan:

- **P1 — Errores como valores, no como excepciones de control.** El error **nunca** cruza la frontera de dominio como `throw`. Las excepciones de infraestructura (SAF, Room, reproductor de medios) se **capturan en el borde** (repositorios/data sources) y se **mapean** a un `DomainError` tipado, transportado por `OperationResult.Failure` (§2.0). El dominio y la presentación operan sobre **valores exhaustivos** (`when` sin `else`), no sobre `try/catch` dispersos. Coherente con la Arquitectura Limpia (§1 modelo de dominio).
- **P2 — Continuidad sobre corrección (Invariante 6 / [RF-09]).** Los fallos de una **pista individual** (formato no soportado, archivo ausente, error de decodificación) **no abortan la sesión**: se absorben con salto automático a la siguiente pista `AVAILABLE`. Estos "errores" son de severidad `INFO` (aviso no bloqueante), no `ERROR`. Solo se detiene la reproducción si no queda ninguna pista válida.
- **P3 — Degradación grácil, nunca invención (Invariantes 2 y 4).** Ausencia de datos, permisos parciales o carpetas inaccesibles **degradan la funcionalidad de forma comunicada**, conservando el último estado coherente. El sistema jamás inventa metadatos ni borra por un error de acceso transitorio (§5.3 modelo de dominio).
- **P4 — Guardia de irreversibilidad (Invariante 5).** Toda operación destructiva exige confirmación explícita como **precondición del contrato**; su ausencia produce un `DomainError.ConfirmationRequired`, no una ejecución silenciosa.
- **P5 — Cero telemetría en el fallo ([RNF-06] / [RNF-07] / Invariante 1).** **Ningún error se reporta a servicios externos** (prohibido Crashlytics, Sentry, reportes remotos). El registro, de existir, es **estrictamente local** y **operativo**: nunca perfila hábitos del Oyente ni incluye datos de comportamiento. El `message` humano **no viaja dentro del error**: se resuelve en presentación desde el `code` (i18n desacoplada del dato, §1 del modelo de dominio).

### 3.1. Estructura Estándar de Error

*Formato unificado que siempre se devolverá en caso de fallo, sin importar el trigger. Consistente con el Formato de Intercambio de §1.2: objeto inmutable Kotlin, no JSON de red.*

```kotlin
// Contrato base de fallo. Viaja SIEMPRE dentro de OperationResult.Failure (§2.0).
sealed class DomainError(
    val code: String,          // Código estable MAYÚSCULAS_SNAKE. Es la CLAVE; nunca se traduce ni se persiste como literal.
    val severity: Severity,    // Determina el canal de notificación (aviso efímero, banner, diálogo).
    val recoverable: Boolean   // true = el Oyente puede corregir/reintentar; false = estructural (requiere cambio de contexto).
) {
    abstract val details: ErrorDetails?   // Contexto tipado opcional (id de entidad, campo inválido). NUNCA datos comportamentales.
    // NOTA: no hay campo 'message'. El texto legible se RESUELVE en la capa de presentación a partir de 'code'
    //       (localización, §1). Para logging LOCAL de desarrollo puede derivarse un texto técnico desde 'code' + 'details'.
}

enum class Severity {
    INFO,      // Aviso no bloqueante (p. ej. pista omitida). La sesión continúa (P2).
    WARNING,   // Degradación parcial (p. ej. carpeta inaccesible); se conserva estado coherente (P3).
    ERROR      // La operación solicitada se aborta sin efecto colateral; el sistema permanece íntegro.
}

// Detalle tipado y acotado del fallo. Sin URLs, sin identificadores de telemetría ([RNF-06]).
sealed interface ErrorDetails {
    data class Entity(val kind: String, val id: Long) : ErrorDetails        // Entidad implicada (p. ej. "Track", 42).
    data class Field(val name: String, val constraint: String) : ErrorDetails // Campo/validación fallida.
    data class Io(val cause: IoCauseCode) : ErrorDetails                     // Causa de I/O local acotada (enum, no stacktrace remoto).
    data class Cause(val error: DomainError) : ErrorDetails                  // Error subyacente envuelto (p. ej. ERR_SCAN_ABORTED envolviendo ERR_PERMISSION_REVOKED).
}
enum class IoCauseCode { NOT_FOUND, ACCESS_DENIED, WRITE_FAILED, DECODE_FAILED, UNKNOWN_LOCAL }
```

### 3.2. Diccionario de Códigos de Error

*Catálogo completo de los `DomainError` referenciados en la Sección 2, agrupados por categoría. La columna "Acción sugerida" es para el **consumidor** (capa de presentación), nunca implica reporte a red (P5).*

**Categoría A — Permisos del Sistema Operativo / SAF** *([Restricción 6], [RF-01])*

| Código (App) | Sev. | Escenario de Fallo (Trigger origen) | Acción Sugerida (Consumidor) |
| --- | --- | --- | --- |
| `ERR_PERMISSION_DENIED` | ERROR | El SO no otorgó el permiso persistible sobre el `treeUri` al agregar carpeta (`TRG-LIB-01`). | Reabrir el selector SAF y explicar por qué el permiso es necesario; no reintentar en bucle. |
| `ERR_PERMISSION_REVOKED` | WARNING | Permiso SAF revocado durante edición o borrado (`TRG-META-01`, `TRG-FILE-01`). En escaneo (`TRG-LIB-03`) se emite **envuelto** dentro de `ERR_SCAN_ABORTED`. | Conservar el último catálogo coherente; solicitar re-concesión desde configuración (§5.3). |

**Categoría B — Validación e Integridad de Entidades** *(Invariantes 2, 5)*

| Código (App) | Sev. | Escenario de Fallo (Trigger origen) | Acción Sugerida (Consumidor) |
| --- | --- | --- | --- |
| `ERR_ENTITY_NOT_FOUND` | ERROR | El `id` referenciado (folder/playlist/track) no existe (`TRG-LIB-02`, `TRG-PLST-02/03/06`). | Refrescar la vista desde el flujo observable (C2); la entidad pudo ser purgada por un escaneo. |
| `ERR_DUPLICATE_SOURCE_FOLDER` | WARNING | El `treeUri` ya estaba registrado como carpeta fuente (`TRG-LIB-01`). | Informar que la carpeta ya existe; no crear duplicado. |
| `ERR_VALIDATION_BLANK_NAME` | ERROR | Nombre de playlist vacío tras `trim` (`TRG-PLST-01`, `TRG-PLST-02`). | Bloquear la confirmación hasta que el nombre no esté vacío. `details = Field("name", "not_blank")`. |
| `ERR_REORDER_MISMATCH` | ERROR | La lista de reordenamiento no es permutación exacta del contenido actual (`TRG-PLST-05`). | Descartar el reordenamiento y re-sincronizar desde el estado actual de la playlist. |
| `ERR_CONFIRMATION_REQUIRED` | ERROR | Se invocó eliminación física con `confirmed=false` (`TRG-FILE-01`). Guardia de **Invariante 5**. | Mostrar diálogo de confirmación explícita antes de reintentar; jamás autoconfirmar. |

**Categoría C — Sistema de Archivos e I/O Local** *(Invariante 2, [Restricción 2/3])*

| Código (App) | Sev. | Escenario de Fallo (Trigger origen) | Acción Sugerida (Consumidor) |
| --- | --- | --- | --- |
| `ERR_FILE_UNAVAILABLE` | WARNING | El archivo objetivo ya no existe en disco al editar o borrar (`TRG-META-01`, `TRG-FILE-01`). | El `Track` pasa a `MISSING` (§5.2); informar y ofrecer re-escaneo. El registro se purga por fidelidad (Invariante 2). |
| `ERR_TAG_WRITE_FAILED` | ERROR | El formato no admite escritura del tag o falló la I/O de la etiqueta ID3 (`TRG-META-01`, [Restricción 3]). | Revertir el cambio optimista en la UI; informar que ese campo no es editable para ese formato. |
| `ERR_STORAGE_IO` | ERROR | Fallo genérico de I/O local no clasificado (lectura SAF, transacción Room). | Reintentar una vez; si persiste, informar sin bloquear el resto de la app. `details = Io(UNKNOWN_LOCAL)`. |

**Categoría D — Reproducción** *(Invariante 6 / [RF-07], [RF-08], [RF-09] — mayormente absorbidos por P2)*

| Código (App) | Sev. | Escenario de Fallo (Trigger origen) | Acción Sugerida (Consumidor) |
| --- | --- | --- | --- |
| `ERR_EMPTY_PLAYABLE_QUEUE` | ERROR | Ninguna pista del contexto solicitado es `AVAILABLE` (`TRG-PLAY-01`). | Informar que no hay pistas reproducibles; no entrar en `PREPARING`. |
| `ERR_TRACK_UNSUPPORTED` | INFO | Pista en formato no decodificable durante la reproducción (`TRG-ENV-04`). | **Salto automático** a la siguiente (P2); aviso efímero "pista omitida". Marca `Track = UNSUPPORTED`. |
| `ERR_TRACK_MISSING` | INFO | El archivo desapareció al intentar reproducirlo (`TRG-ENV-04`). | **Salto automático** (P2); marca `Track = MISSING` para purga en el próximo escaneo. |
| `ERR_SCAN_ABORTED` | WARNING | Escaneo interrumpido por permiso revocado o carpeta inaccesible (`TRG-LIB-03`/`TRG-LIB-04`). **Envuelve** la causa subyacente (p. ej. `ERR_PERMISSION_REVOKED`) en `details`. | Emitir `ScanState.Aborted(reason)`; conservar catálogo previo; permitir reintento manual. |

**Nota de exhaustividad.** Los `INFO` de la Categoría D **no son fallos de una interacción del Oyente**, sino eventos internos del motor que el sistema **resuelve por sí mismo** (Invariante 6); se listan aquí por completitud del contrato, pero no interrumpen el flujo ni exigen acción correctiva del Oyente. No existe categoría de errores de red, autenticación, sesión expirada ni servidor: son **estructuralmente imposibles** en un sistema autárquico ([RNF-06] / Invariante 1).

## 4. Limitaciones y Restricciones de Interfaz (Rate Limits / Throttling)

*Define las barreras de protección de las interfaces para evitar abusos o sobrecargas sistémicas.*

> **No existe *rate limiting* de red.** Al ser monousuario, soberano (Invariante 3) y sin red ([RNF-06]), Sonus no tiene cuotas por cliente, penalizaciones `HTTP 429` ni bloqueos temporales. Las "barreras de protección" de este apartado son los límites reales de un sistema local reactivo: (a) **coalescencia** de comandos de alta frecuencia para no saturar el hilo principal ([RNF-01]); (b) **presupuestos de latencia** derivados de los RNF; y (c) **acotación de recursos** frente a las restricciones físicas del dispositivo ([Restricción 1/4]).

### 4.1. Control de Frecuencia de Comandos (Coalescencia, no Throttling punitivo)

*El sistema nunca rechaza un comando del Oyente por frecuencia; los fusiona o descarta de forma transparente para preservar la responsividad. No hay penalización.*

| Interacción | Mecanismo de control | Parámetro de referencia | Justificación |
| --- | --- | --- | --- |
| **Filtro textual / búsqueda** (`TRG-NAV-01`) | *Debounce* de la consulta observable | ~250–300 ms de inactividad antes de consultar | Evita re-ejecutar la consulta por cada pulsación; garantiza [RNF-01] sobre catálogos grandes ([Restricción 4]). |
| **Seek / arrastre de barra de progreso** (`TRG-PLAY-05`) | *Throttle* + *seek* final al soltar | Emisión intermedia ≤ ~60 ms; commit al liberar | Reposicionar el decodificador en cada píxel saturaría el motor; se aplica el valor final. |
| **Play/Pause, Next, Previous** (`TRG-PLAY-02/03/04`) | Coalescencia idempotente del último estado | Colapsar ráfagas < ~300 ms | Pulsaciones repetidas rápidas convergen al estado final sin encolar transiciones intermedias. |
| **Reordenar playlist / cola** (`TRG-PLST-05` / `TRG-QUEUE-03`) | *Batching* en una sola transacción | 1 transacción Room por gesto completo | Contigüidad y unicidad de posiciones se garantizan atómicamente ([F-8]). |
| **Escaneo** (`TRG-LIB-03`) | *Single-flight*: un escaneo activo a la vez | Re-solicitudes se ignoran o encolan como uno solo | Evita escaneos concurrentes sobre las mismas carpetas; el ciclo `SCANNING` es exclusivo (§5.3). |

**Penalización por exceso:** **ninguna.** No hay bloqueo, ni código `429`, ni *cooldown* impuesto al Oyente. El "exceso" se absorbe por coalescencia/descarte silencioso del evento redundante. La única exclusión activa es el *single-flight* del escaneo, que **no penaliza** sino que reutiliza el ciclo en curso.

### 4.2. Presupuestos de Latencia (Timeouts y SLAs internos)

*El sistema no aborta interacciones por "timeout de red" (no hay red). En su lugar, se compromete con presupuestos de latencia derivados de los RNF; superarlos dispara comunicación de progreso, nunca cancelación silenciosa.*

| Interacción | Presupuesto (objetivo) | Comportamiento al superarlo | Trazabilidad |
| --- | --- | --- | --- |
| **Respuesta de navegación/filtrado** (`TRG-NAV-01`, C2) | **< 500 ms** (sub-segundo estricto) | Es un objetivo de diseño (índices del modelo de dominio), no un abort. Degradación se trata como defecto de rendimiento. | [RNF-01] |
| **Inicio de reproducción** (`TRG-PLAY-01`, `PREPARING → PLAYING`) | **Sub-segundo** | Si la preparación excede el umbral, mostrar estado `PREPARING` visible; nunca congelar la UI. | [RNF-02] |
| **Escaneo de biblioteca** (`TRG-LIB-03/04`) | Sin límite superior duro | Si supera **1 s**, emitir `ScanState.Scanning(processed, total)` con progreso determinista. Corre en *background thread*; jamás bloquea el hilo principal. | [RNF-03] |
| **Restauración de sesión tras OOM** (`TRG-ENV-05`) | **< 2 s** en el reinicio | Rehidratar `PlaybackState` + `QueueItem` dentro del presupuesto; sin autoplay. | [RNF-05] |
| **Persistencia de edición de metadatos** (`TRG-META-01`) | Imperceptible (UI optimista) | Reflejar el cambio en UI de inmediato; persistir en segundo plano. Si falla, revertir con `ERR_TAG_WRITE_FAILED` (§3.2). | SDD §4.1 (Retraso de persistencia) |

**Tiempo Máximo de Ejecución (Timeout):** las operaciones locales de larga duración (escaneo, edición masiva) **no se abortan por reloj**; se ejecutan de forma asíncrona con reporte de progreso ([RNF-03]) y son **cancelables por el Oyente** (p. ej. detener un escaneo), no por un temporizador del sistema. La única "cancelación" legítima proviene de la voluntad del Oyente o de la revocación de permisos del SO (`ERR_SCAN_ABORTED`), nunca de un umbral temporal arbitrario.

### 4.3. Acotación de Recursos y Capacidad

*Límites impuestos por el entorno físico (no por la interfaz), que el sistema respeta para no degradar la continuidad ni la huella.*

- **Volumen del catálogo:** el diseño se orienta a **decenas de miles de `Track`** ([Restricción 4]); la interfaz de navegación (`TRG-NAV-01`) debe **paginar/virtualizar** los resultados (p. ej. `PagingSource` de Room) para mantener [RNF-01], sin cargar la colección completa en memoria.
- **Huella de almacenamiento propia:** el sistema **no** persiste carátulas como archivos ni acumula históricos ([RNF-08], [F-5]); las imágenes se leen *on-demand* y se cachean **solo en memoria**. La cola (`QueueItem`) se reconstruye por sesión, no se acumula.
- **Concurrencia interna:** el Motor de Biblioteca opera en *background threads*; el Motor de Reproducción vive en un **Foreground Service** de alta prioridad ([RNF-04]). La presentación (única `Activity`) **jamás** ejecuta I/O de disco ni decodificación en el hilo principal — esta es la barrera de protección más importante contra el bloqueo de la interfaz.
- **Límite físico no gobernado por Sonus:** el tamaño máximo de la biblioteca lo impone el **almacenamiento del dispositivo** ([Restricción 1]); el sistema no lo controla, solo opera eficientemente dentro de él.
