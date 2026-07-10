# Modelo de Dominio y Estado

> Este documento define la arquitectura estructural de la memoria del sistema y el ciclo de vida de sus entidades. Actúa simultáneamente como Modelo Entidad-Relación, Diccionario de Datos y Máquina de Estados. Se debe utilizar una sintaxis declarativa (pseudo-código, TypeScript types o Prisma schema) para definir las estructuras, utilizando los comentarios inline como el diccionario de datos.
>

> **Fuentes de verdad:** `docs/domain/definition/system_definition_document.md` (SDD) y `docs/domain/definition/requirements_specification.md` (RF/RNF).
> **Contexto tecnológico:** Aplicación móvil Android nativa (Kotlin), 100% local sin red, Arquitectura Limpia con patrón **Model–ViewModel–View** y enfoque **Single-Activity**. La sintaxis declarativa se expresa en **Kotlin/Room (SQLite)**, la persistencia local nativa del sistema.
>
> **Alcance de este documento.** Define la **memoria durable** del sistema (lo que sobrevive a un reinicio o a una terminación forzada del SO — ver [RNF-05]) y los **ciclos de vida** de sus entidades. El estado transitorio de UI (scroll, foco, animaciones) vive en `ViewModel`/`StateFlow` de la capa de presentación y **no** se persiste. El límite entre ambos es la tabla `PlaybackState`: es el puente que permite restaurar la sesión tras un OOM kill.

## 1. Convenciones Base (Dominios Universales)

*Define las normas absolutas aplicadas en el almacenamiento y manejo de la información en todo el sistema para garantizar la homeostasis de los datos.*

- **Identidad y claves:** Toda entidad persistida usa una clave subrogada interna `id: Long` (`@PrimaryKey(autoGenerate = true)`). La **identidad natural** de un `Track` es su **URI de contenido persistido** (Storage Access Framework), no su ruta textual ni sus metadatos. Esto materializa la **Invariante 2 (Fidelidad al Sistema de Archivos)**: la verdad del track es su existencia física, no su etiqueta.
- **Indexación para navegación:** Toda columna usada como eje de filtrado taxonómico ([RF-12]) o como clave foránea debe estar **indexada**, para garantizar la latencia sub-500ms de **[RNF-01]** sobre catálogos de decenas de miles de filas ([Restricción 4]). Room exige, además, índice explícito en toda columna FK.
- **Manejo de Tiempos y Fechas:** Todos los instantes se almacenan como **epoch en milisegundos UTC** (`Long`). Solo se persisten marcas de tiempo **operativas** (fecha de alta en el catálogo, fecha de última modificación del archivo en disco para detección de cambios). Queda **terminantemente prohibido** persistir marcas de tiempo de comportamiento de escucha (ver anti-convención abajo).
- **Manejo de Estados Lógicos:** Uso estricto de `Boolean` (`true`/`false`), **no nulos** para lógicas binarias, siempre con valor por defecto explícito (`@ColumnInfo(defaultValue = "0")`).
- **Manejo de Valores de Alta Precisión (posiciones y duraciones de audio):** Toda posición temporal y duración de reproducción se almacena como **enteros de milisegundos** (`Long`), nunca como punto flotante de segundos. Esto evita la deriva por coma flotante y garantiza la restauración exacta de la posición exigida por **[RNF-05]** ("posición en milisegundos").
- **Ausencia de datos (No Invención — Invariante 4 / [RF-02]):** La ausencia de un metadato **no se representa con un literal de presentación persistido**, sino con un **centinela a nivel de datos**, y el texto visible ("Sin información") se **renderiza en la capa de presentación**. Dos mecanismos según el tipo de campo:
  - *Dimensiones normalizadas* (Artista, Álbum, Género): la ausencia apunta al **registro semilla reservado `id = 1`** de cada tabla (ver §6.1), cuya `name` es una cadena vacía sentinela; la UI mapea `id = 1` → etiqueta localizada.
  - *Campos textuales directos* (p. ej. `title`): la ausencia se representa con `NULL`; la UI renderiza la etiqueta localizada.
  - **Motivación:** (a) preserva la Invariante 4 (el sistema nunca infiere ni inventa); (b) mantiene distinguible un archivo *sin etiqueta* de uno legítimamente etiquetado con el texto "Sin información"; (c) desacopla el idioma de los datos, habilitando la internacionalización de Fase 3 sin migración de contenido.
- **Anti-convención — Prohibición de Datos Comportamentales ([RNF-07] / Invariante 3):** Está **arquitectónicamente prohibido** introducir columnas o tablas que perfilen al Oyente. Quedan expresamente vetadas, entre otras: `playCount`, `lastPlayedAt`, `skipCount`, `totalListeningTimeMs`, `sessionHistory`, `favoriteScore`. La memoria del sistema se limita a datos **operativos** (catálogo, configuración, estado de sesión, playlists). Ver la nota de frontera en `PlaybackProgress` (§2) sobre por qué la posición de reanudación es estado operativo y no comportamental.
- **Aislamiento de red ([RNF-06] / Invariante 1):** Ninguna entidad contiene URLs remotas, tokens, identificadores de dispositivo con fines de telemetría, ni referencias a recursos de red. Las carátulas se resuelven desde bytes embebidos en el archivo o desde almacenamiento local.
- **Conversión de tipos (Room):** Todos los `enum` de §4 requieren un `@TypeConverter` (o `@TypeConverters` a nivel de base de datos) que los serialice a `String` estable (el nombre de la constante, nunca su ordinal, para resistir reordenamientos futuros del enum).
- **Persistencia y Arquitectura Limpia:** Los modelos de la capa de **dominio** (data classes puras de Kotlin) son agnósticos a la persistencia. Las declaraciones `@Entity` de esta sección pertenecen a la **capa de datos** (Room) y se mapean a/desde el dominio vía repositorios. El catálogo, las playlists y la configuración residen en una única base de datos SQLite local; el estado de reproducción se materializa en la tabla `PlaybackState`.

## 2. Esquema de Estructuras y Diccionario Integrado

*Inventario ontológico del sistema. Define las entidades fundamentales, sus propiedades y relaciones. Los comentarios junto a cada propiedad actúan como el Diccionario de Datos estricto (indicando propósito, límites y valores por defecto).*

Kotlin / Room (SQLite)

```kotlin
// ==========================================
// ENTIDAD: Track  (materializa "Archivo de Audio" + "Metadatos Embebidos" + una entrada del "Catálogo de Biblioteca")
// PROPÓSITO: Unidad atómica del sistema. Representa un archivo de audio físico descubierto en las
//            Carpetas Fuente, junto con las etiquetas ID3 extraídas. El conjunto de Tracks ES el Catálogo.
// ==========================================
@Entity(
    tableName = "track",
    indices = [
        Index(value = ["uri"], unique = true),                   // Identidad natural = URI física (Invariante 2)
        Index("artistId"), Index("albumId"),                     // [F-1] Índices de navegación taxonómica ([RF-12]/[RNF-01])
        Index("genreId"), Index("sourceFolderId")                // [F-1] Room exige índice en toda columna FK
    ],
    foreignKeys = [
        ForeignKey(Artist::class, ["id"], ["artistId"], onDelete = RESTRICT),
        ForeignKey(Album::class,  ["id"], ["albumId"],  onDelete = RESTRICT),
        ForeignKey(Genre::class,  ["id"], ["genreId"],  onDelete = RESTRICT),
        ForeignKey(SourceFolder::class, ["id"], ["sourceFolderId"], onDelete = CASCADE)
    ]
)
data class Track(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,           // PK subrogada interna.
    val uri: String,                                             // Obligatorio, ÚNICO. URI de contenido persistido (SAF). Identidad natural del track.
    val title: String?,                                          // Título ID3. NULL = etiqueta ausente; la UI renderiza "Sin información" ([RF-02], §1).
    val artistId: Long,                                          // FK -> Artist. Intérprete de LA PISTA (puede diferir del artista del álbum: "feat.", compilaciones). Ausente => id=1.
    val albumId: Long,                                           // FK -> Album. Álbum al que pertenece la pista. Ausente => id=1.
    val genreId: Long,                                           // FK -> Genre. Género de la pista. Ausente => id=1. (Nota: modelo mono-género, ver §4/F-12.)
    val sourceFolderId: Long,                                    // FK -> SourceFolder. Carpeta fuente que originó el descubrimiento del track.
    val contentType: ContentType,                                // Enum. MUSIC | PODCAST | UNKNOWN. Frontera semántica música/podcast (§4).
    val trackNumber: Int?,                                       // Opcional. Nº de pista dentro del álbum. NULL admitido (dato numérico posicional, no clasificador).
    val releaseYear: Int?,                                       // Opcional. Año de publicación. NULL si ausente (dato numérico, no clasificador).
    val durationMs: Long,                                        // Obligatorio. Duración total en milisegundos (entero de alta precisión, §1).
    val hasEmbeddedArtwork: Boolean,                             // [F-5/F-6] Obligatorio. true si el archivo trae carátula embebida. La imagen NO se persiste: se lee on-demand desde 'uri'. @default(false).
    val availability: TrackAvailability,                         // Enum. Estado de disponibilidad (ver Máquina de Estados §5.2). @default(AVAILABLE).
    val fileLastModifiedMs: Long,                                // Obligatorio. mtime del archivo (epoch ms UTC). Insumo para detección de cambios en re-escaneo ([RF-03]).
    val dateAddedMs: Long                                        // Obligatorio. Fecha de alta en el catálogo (epoch ms UTC). Dato OPERATIVO, no comportamental.
)

// ==========================================
// ENTIDAD: Artist   (dimensión normalizada de "Metadatos Embebidos")
// PROPÓSITO: Eje objetivo de organización por intérprete. Derivado del escaneo; minimiza la huella ([RNF-08]).
// ==========================================
@Entity(tableName = "artist", indices = [Index(value = ["name"], unique = true)])
data class Artist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,           // PK. id=1 reservado: registro centinela de artista ausente (§6.1). name="" ; la UI lo rotula.
    val name: String                                             // Obligatorio, ÚNICO. Nombre del artista. Cadena vacía SOLO en el registro centinela id=1 (§1).
)

// ==========================================
// ENTIDAD: Album   (dimensión normalizada de "Metadatos Embebidos")
// PROPÓSITO: Eje objetivo de organización por álbum. Derivado del escaneo.
//            [F-5/F-6] La carátula del álbum NO se almacena: se resuelve on-demand desde una pista representativa con hasEmbeddedArtwork=true.
// ==========================================
@Entity(
    tableName = "album",
    indices = [Index(value = ["name", "artistId"], unique = true), Index("artistId")],
    foreignKeys = [ForeignKey(Artist::class, ["id"], ["artistId"], onDelete = RESTRICT)]
)
data class Album(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,           // PK. id=1 reservado: registro centinela de álbum ausente (§6.1).
    val name: String,                                            // Obligatorio. Nombre del álbum. Cadena vacía SOLO en el registro centinela id=1.
    val artistId: Long                                           // FK -> Artist. Artista del ÁLBUM (distingue álbumes homónimos de distintos artistas; ver nota topológica §3).
)

// ==========================================
// ENTIDAD: Genre   (dimensión normalizada de "Metadatos Embebidos")
// PROPÓSITO: Eje objetivo de organización por género. Derivado del escaneo.
// ==========================================
@Entity(tableName = "genre", indices = [Index(value = ["name"], unique = true)])
data class Genre(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,           // PK. id=1 reservado: registro centinela de género ausente (§6.1).
    val name: String                                             // Obligatorio, ÚNICO. Nombre del género. Cadena vacía SOLO en el registro centinela id=1.
)

// ==========================================
// ENTIDAD: SourceFolder   (materializa "Carpetas Fuente")
// PROPÓSITO: Directorio elegido explícitamente por el Oyente que delimita el perímetro de escaneo ([RF-01]).
//            Punto de apalancamiento crítico: define el universo completo de la biblioteca (SDD §4.1 Apalancamiento 2).
// ==========================================
@Entity(tableName = "source_folder", indices = [Index(value = ["treeUri"], unique = true)])
data class SourceFolder(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,           // PK.
    val treeUri: String,                                         // Obligatorio, ÚNICO. URI de árbol SAF con permiso persistido de lectura/escritura.
    val displayPath: String,                                     // Obligatorio. Ruta legible para mostrar al Oyente en la configuración.
    val dateAddedMs: Long                                       // Obligatorio. Fecha en que el Oyente agregó la carpeta (epoch ms UTC, dato operativo).
)

// ==========================================
// ENTIDAD: Playlist   (materializa "Agrupación Personalizada")
// PROPÓSITO: Colección lógica subjetiva del Oyente. Organización independiente de los metadatos ([RF-05]).
// ==========================================
@Entity(tableName = "playlist")
data class Playlist(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,           // PK.
    val name: String,                                            // Obligatorio. Nombre definido por el Oyente. Editable.
    val createdAtMs: Long,                                       // Obligatorio. Fecha de creación (epoch ms UTC, dato operativo).
    val updatedAtMs: Long                                        // Obligatorio. Última modificación estructural de la playlist (renombrar/reordenar/poblar).
)

// ==========================================
// ENTIDAD: PlaylistTrackCrossRef   (tabla puente N:M ordenada)
// PROPÓSITO: Resuelve la pertenencia "un track pertenece a 0..N playlists" con orden definido por el Oyente.
// ==========================================
@Entity(
    tableName = "playlist_track",
    primaryKeys = ["playlistId", "trackId"],
    indices = [
        Index("trackId"),
        Index(value = ["playlistId", "position"], unique = true)  // [F-8] Orden determinista: no se admiten dos posiciones iguales en una misma playlist.
    ],
    foreignKeys = [
        ForeignKey(Playlist::class, ["id"], ["playlistId"], onDelete = CASCADE),  // Borrar playlist => borra sus vínculos (no los tracks).
        ForeignKey(Track::class,    ["id"], ["trackId"],    onDelete = CASCADE)   // Borrar/purgar track => se elimina de toda playlist (Invariante 2 / [RF-06]).
    ]
)
data class PlaylistTrackCrossRef(
    val playlistId: Long,                                        // FK -> Playlist.
    val trackId: Long,                                           // FK -> Track.
    val position: Int                                            // Obligatorio. Orden del track dentro de la playlist (>= 0, contiguo). Reordenar reescribe posiciones (ver §3, nota reordenamiento).
)

// ==========================================
// ENTIDAD: QueueItem   (materializa "Cola de Reproducción" — cola única de la sesión activa)
// PROPÓSITO: Secuencia de la sesión activa. Se PERSISTE para restaurar la sesión tras un OOM kill ([RF-14]/[RNF-05]).
//            [F-3] Guarda DOS órdenes para que alternar shuffle sea reversible y la restauración determinista.
// ==========================================
@Entity(
    tableName = "queue_item",
    indices = [
        Index("trackId"),
        Index(value = ["originalPosition"], unique = true),      // [F-3][F-8] Orden secuencial estable, único.
        Index(value = ["playbackPosition"], unique = true)       // [F-3][F-8] Orden efectivo (con shuffle aplicado), único.
    ],
    foreignKeys = [ForeignKey(Track::class, ["id"], ["trackId"], onDelete = CASCADE)]  // Track purgado => sale de la cola.
)
data class QueueItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,           // PK. Referenciado por PlaybackState.currentQueueItemId como puntero de reproducción.
    val trackId: Long,                                           // FK -> Track. Pista encolada (puede repetirse el mismo trackId en distintos QueueItem).
    val originalPosition: Int,                                   // [F-3] Obligatorio. Orden secuencial estable ([RF-08] modo secuencial). Base a la que se vuelve al desactivar shuffle.
    val playbackPosition: Int                                    // [F-3] Obligatorio. Orden efectivo de reproducción. Igual a originalPosition si shuffle=OFF; permutado si shuffle=ON.
)

// ==========================================
// ENTIDAD: PlaybackState   (SINGLETON — estado del Motor de Reproducción / puente durable)
// PROPÓSITO: Snapshot de la sesión activa. Única fuente para restaurar reproducción en < 2s tras terminación forzada ([RNF-05]).
// ==========================================
@Entity(tableName = "playback_state")
data class PlaybackState(
    @PrimaryKey val id: Int = 1,                                 // SINGLETON. Siempre id=1 (fila única, §6.1).
    val currentQueueItemId: Long?,                               // [F-2] Puntero AUTORITATIVO a la posición actual en la cola (QueueItem.id). NULL si la cola está vacía. Resuelve la ambigüedad de tracks repetidos.
    val positionMs: Long,                                        // Obligatorio. Posición exacta de reproducción en ms (§1). @default(0).
    val repeatMode: RepeatMode,                                  // Enum. OFF | ONE | ALL ([RF-08]). @default(OFF).
    val shuffleEnabled: Boolean,                                 // Obligatorio. true si el modo aleatorio está activo ([RF-08]). @default(false).
    val resumeStatus: ResumeStatus                               // [F-10] Enum resumible persistido: STOPPED | PAUSED. NUNCA se persiste un estado transitorio (PLAYING/PREPARING). Al restaurar no hay autoplay. @default(STOPPED).
)

// ==========================================
// ENTIDAD: PlaybackProgress   (posición de reanudación por pista)
// PROPÓSITO: [F-11] Recuerda la posición donde el Oyente dejó una pista de consumo secuencial largo (PODCAST),
//            para retomarla. Se pobla EXCLUSIVAMENTE para tracks contentType=PODCAST.
// FRONTERA [RNF-07]: NO es dato comportamental. No cuenta reproducciones, no mide hábitos, no perfila. Es estado
//            operativo de una única pista (equivalente a la posición de un marcador de lectura), coherente con la
//            soberanía del Oyente.
// ==========================================
@Entity(
    tableName = "playback_progress",
    foreignKeys = [ForeignKey(Track::class, ["id"], ["trackId"], onDelete = CASCADE)]  // Track purgado => se olvida su marcador.
)
data class PlaybackProgress(
    @PrimaryKey val trackId: Long,                               // PK y FK -> Track. Un único marcador por pista.
    val positionMs: Long                                         // Obligatorio. Última posición conocida (ms). Se actualiza al pausar/salir de un PODCAST.
)

// ==========================================
// ENTIDAD: AppSettings   (SINGLETON — materializa "Configuración del Sistema": preferencias)
// PROPÓSITO: Parámetros persistentes de comportamiento/visualización. Las Carpetas Fuente viven en su propia tabla.
// ==========================================
@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 1,                                 // SINGLETON. Siempre id=1.
    val onboardingCompleted: Boolean,                            // true tras completar el flujo de primera ejecución (SDD §4.1 Apalancamiento 5). @default(false).
    val themePreference: ThemePreference                         // Enum. SYSTEM | LIGHT | DARK. Preferencia visual. @default(SYSTEM).
)
```

## 3. Matriz de Relaciones y Topología

*Define explícitamente las reglas matemáticas de existencia, dependencia y cardinalidad entre las entidades definidas en la sección anterior, así como el comportamiento de las eliminaciones.*

| **Entidad Origen** | **Cardinalidad** | **Entidad Destino** | **Verbo / Naturaleza** | **Regla de Integridad (Comportamiento en Cascada)** |
| --- | --- | --- | --- | --- |
| `Artist` | `1 : N` | `Album` | "Publica" | `RESTRICT`: un artista no se borra si tiene álbumes. Los artistas son derivados; se purgan solo al quedar huérfanos tras un escaneo (§6.2). |
| `Artist` | `1 : N` | `Track` | "Interpreta" | `RESTRICT`. El track siempre apunta a un artista válido (real o centinela id=1). |
| `Album` | `1 : N` | `Track` | "Agrupa" | `RESTRICT`. Idem; purga de álbumes huérfanos tras escaneo. |
| `Genre` | `1 : N` | `Track` | "Clasifica" | `RESTRICT`. Idem; purga de géneros huérfanos tras escaneo. |
| `SourceFolder` | `1 : N` | `Track` | "Origina / Delimita" | `CASCADE`: quitar una Carpeta Fuente elimina del Catálogo todos los tracks descubiertos bajo ella, y en cascada sus vínculos de playlist, cola y marcadores ([RF-01]/[RF-03], Invariante 2). Debe dispararse purga de dimensiones huérfanas a continuación (§6.2). |
| `Playlist` | `N : M` | `Track` | "Referencia (subjetiva)" | Vía `PlaylistTrackCrossRef`. `CASCADE` en ambos extremos: borrar la playlist borra los vínculos (no los tracks); purgar el track lo elimina de toda playlist ([RF-05]/[RF-06]). |
| `Track` | `1 : N` | `PlaylistTrackCrossRef` | "Es referenciado por" | `CASCADE`. Materializa la eliminación automática de referencias de la Invariante 2. |
| `Track` | `1 : N` | `QueueItem` | "Es encolado como" | `CASCADE`: un track purgado desaparece de la cola activa (coherente con [RF-09] y salto de pista ausente). |
| `Track` | `1 : 0..1` | `PlaybackProgress` | "Tiene marcador de reanudación" | `CASCADE`. El marcador (solo podcasts) se olvida al purgar la pista. |
| `QueueItem` | `1 : 0..1` | `PlaybackState` | "Es la posición actual de" | `currentQueueItemId` **nullable**, `SET NULL` semántico gestionado por la app. `[F-2]` El puntero de reproducción apunta a una **posición** de la cola, no a un track (evita ambigüedad con tracks repetidos). |
| `AppSettings` | `1 : 1` | *(sistema)* | "Configura" | Singleton. No cascada. |

**Notas topológicas:**

- **`[F-7]` Doble artista (pista vs. álbum):** `Track.artistId` es el **intérprete de la pista**; `Album.artistId` es el **artista del álbum**. En compilaciones o colaboraciones ("feat.") pueden diferir. Consecuencia para [RF-12]: "artista → álbumes" se resuelve por `Album.artistId` y "artista → pistas" por `Track.artistId`; la UI debe elegir la relación correcta según el contexto de navegación para no producir resultados aparentemente inconsistentes.
- **`[F-8]` Reordenamiento y contigüidad:** Tanto `PlaylistTrackCrossRef.position` como los órdenes de `QueueItem` se mantienen **contiguos desde 0 y sin huecos**. Reordenar/insertar/eliminar reescribe las posiciones afectadas dentro de una única transacción. La unicidad `(playlistId, position)` y la de los órdenes de cola lo garantizan a nivel de esquema.
- **`[F-3]` Reversibilidad del shuffle:** Al activar shuffle se recalcula `playbackPosition` (permutación) preservando `originalPosition`; al desactivarlo, `playbackPosition ← originalPosition`. La restauración tras OOM reconstruye la cola por `playbackPosition` y reposiciona el puntero con `currentQueueItemId`.
- **Doble eje de organización (Equifinalidad, SDD §1.2):** El eje **objetivo** (`Artist`/`Album`/`Genre` → `Track`) coexiste con el eje **subjetivo** (`Playlist` N:M `Track`). Un mismo track pertenece simultáneamente a su clasificación por metadatos y a 0..N playlists — sin conflicto.
- **Efimeridad vs. durabilidad:** `QueueItem` y `PlaybackState` son conceptualmente transitorios (SDD §3.2), pero se **persisten deliberadamente** como excepción controlada para cumplir la restauración de sesión ([RF-14]/[RNF-05]). Se reconstruyen/vacían al iniciar una nueva sesión.
- **`[F-13]` Mutación de la cola en caliente:** además de construirse al iniciar reproducción, la cola admite **modificación en tiempo real** durante la sesión activa — encolar (añadir al final / reproducir a continuación), remover y reordenar — conforme a SDD §3.2 ("puede ser modificada en tiempo real"). Toda mutación reescribe `originalPosition`/`playbackPosition` de los items afectados en **una única transacción**, preservando contigüidad y unicidad ([F-8]). El puntero `PlaybackState.currentQueueItemId` apunta a la **posición** (no al índice) y se conserva ante estas mutaciones, evitando ambigüedad con `trackId` repetidos ([F-2]). El orden secuencial base (`originalPosition`) solo se altera por reordenamiento explícito, preservando la reversibilidad del shuffle ([F-3]).
- **Preparación multi-perfil (Fase 2 — SDD §Adaptación):** El modelo actual asume un único Oyente. La evolución a múltiples perfiles se logra añadiendo `profileId` a `Playlist`, `AppSettings`, `PlaybackState` y `PlaybackProgress` — **sin tocar** `Track`, `Artist`, `Album`, `Genre` ni los motores. Se documenta aquí como costura de migración prevista, no implementada en Fase 1.

## 4. Dominios Cerrados (Enums y Catálogos)

*Conjuntos de valores estáticos predefinidos que limitan la entrada de datos, previniendo la entropía y asegurando la consistencia semántica.*

Kotlin

```kotlin
// Frontera semántica entre tipos de contenido (SDD §1.1: música vs. podcast).
enum class ContentType {
    MUSIC     // Pieza corta, consumo aleatorio. Habilita shuffle como modo natural.
    PODCAST   // Pieza larga, consumo secuencial. Elegible para marcador de reanudación (PlaybackProgress).
    UNKNOWN   // Etiqueta de tipo ausente. NO se infiere (Invariante 4). El Oyente puede corregirla ([RF-04]).
}

// Estado de disponibilidad de un Track frente a la realidad del sistema de archivos (Invariante 2).
enum class TrackAvailability {
    AVAILABLE     // Archivo existe y es reproducible por el Motor de Reproducción.
    UNSUPPORTED   // Formato no decodificable ([Restricción 2]): detectado en escaneo o al fallar la decodificación durante la reproducción (§5.2). Visible, no reproducible.
    MISSING       // Detectado como inexistente en reproducción; pendiente de purga en el próximo escaneo ([RF-03]).
}

// Modo de reproducción de la cola ([RF-08]).
enum class RepeatMode {
    OFF   // Al agotar la cola, la reproducción se detiene (cierre del Ciclo de Reproducción, SDD §1.3).
    ONE   // Repite indefinidamente la pista actual.
    ALL   // Al agotar la cola, reinicia desde el principio.
}

// [F-10] Estado RESUMIBLE del motor que se persiste (subconjunto de PlaybackStatus). Nunca se persisten
// estados transitorios (PLAYING/PREPARING/ERROR): al restaurar tras OOM no debe haber autoplay ([RNF-05]).
enum class ResumeStatus {
    STOPPED   // Sesión cerrada por el Oyente.
    PAUSED    // Sesión abierta pausada; se restaura lista para reanudar desde positionMs.
}

// Estado RUNTIME (en memoria, ViewModel/Service) del Motor de Reproducción — NO se persiste; ver Máquina de Estados §5.1.
enum class PlaybackStatus {
    IDLE       // Inactivo, sin pista cargada. Estado de arranque nominal (SDD §4.1, secuencia de arranque paso 5).
    PREPARING  // Decodificando/inicializando el flujo antes de emitir sonido ([RNF-02]).
    PLAYING    // Emitiendo flujo de audio al hardware.
    PAUSED     // Pausado por el Oyente, por pérdida de foco, o por desconexión de periférico.
    STOPPED    // Detenido explícitamente por el Oyente; sesión cerrada.
    ERROR      // Fallo irrecuperable de la pista actual; dispara salto a la siguiente ([RF-09]).
}

// Preferencia de tema visual (dato de configuración; no comportamental).
enum class ThemePreference {
    SYSTEM  // Sigue el tema del sistema operativo.
    LIGHT
    DARK
}
```

> **`[F-12]` Limitación conocida — mono-género:** `Track` referencia un único `Genre`. Algunos esquemas ID3 admiten múltiples géneros; el modelo adopta uno solo, coherente con la organización por género único del SDD. De requerirse multi-género en el futuro, se introduciría una tabla puente `Track` N:M `Genre` sin alterar el resto del esquema.

## 5. Máquina de Estados (Ciclos de Vida)

*Para las entidades que mutan con el tiempo, esta sección define estrictamente los estados posibles y las transiciones legales. Evita estados huérfanos o transiciones imposibles.*

### 5.1. Ciclo de Vida de: Motor de Reproducción (`PlaybackStatus` runtime; snapshot en `PlaybackState`)

Es el ciclo de vida **crítico** del sistema: gobierna el **Equilibrio de Continuidad** (SDD §1.2) y la **Invariante 6 (Continuidad como Prioridad Operativa)**. El estado transita en memoria (`PlaybackStatus`); solo su subconjunto **resumible** (`ResumeStatus`) + `positionMs` + puntero de cola se persiste en `PlaybackState`. Se refleja en tiempo real en el Foreground Service, la notificación persistente ([RF-13]) y la UI (mini-reproductor y pantalla completa).

- **Estado Inicial (Nacimiento):** `IDLE` (motor posicionado tras el arranque, sin pista cargada).
- **Estados Finales (Terminación):** No hay estado final absoluto — el motor es cíclico (SDD §1.3). `STOPPED` es el cierre voluntario de una sesión, desde el cual siempre se puede reiniciar el ciclo.

| **Estado Origen** | **Evento / Trigger (Acción)** | **Estado Destino (Resultado)** | **Condiciones / Validaciones Previas** |
| --- | --- | --- | --- |
| `IDLE` | Oyente pulsa "Reproducir" | `PREPARING` | La cola tiene ≥ 1 `QueueItem` con `Track.availability = AVAILABLE`. Si el track es `PODCAST`, `positionMs` se inicializa desde `PlaybackProgress` si existe. |
| `PREPARING` | Decodificador listo, hardware negociado | `PLAYING` | Inicio sub-segundo ([RNF-02]). Se adquiere el foco de audio. |
| `PREPARING` | Error de decodificación / archivo ausente | `ERROR` | Marca el `Track` como `UNSUPPORTED` o `MISSING`. |
| `PLAYING` | Oyente pulsa "Pausa" | `PAUSED` | Persiste `resumeStatus=PAUSED`, `positionMs`, y `PlaybackProgress` si es PODCAST. |
| `PLAYING` | Pérdida transitoria de foco (notificación) | `PLAYING` *(ducking)* | Atenúa volumen sin cambiar de estado ([RF-10]). |
| `PLAYING` | Pérdida prolongada de foco (llamada, otra app de audio) | `PAUSED` | El audio nunca se superpone a una app con prioridad ([RF-10] / Perturbación 6). |
| `PLAYING` | Desconexión de salida (`ACTION_AUDIO_BECOMING_NOISY`) | `PAUSED` | Pausa instantánea; requiere reanudación **manual** ([RF-11] / Perturbación 7). |
| `PLAYING` | Fin de pista y hay siguiente en cola | `PREPARING` | Avanza según `RepeatMode`/`shuffle` usando `playbackPosition`. Se persiste `positionMs = 0` y nuevo `currentQueueItemId`. |
| `PLAYING` | Fin de pista sin siguiente y `RepeatMode = OFF` | `STOPPED` | Cierre del Ciclo de Reproducción (SDD §1.3). Se persiste el estado. |
| `PLAYING` / `PREPARING` | Error de la pista actual | `ERROR` → `PREPARING` | **Invariante 6 / [RF-09]:** salta automáticamente a la siguiente pista `AVAILABLE`; solo cae a `STOPPED` si no queda ninguna. |
| `PAUSED` | Oyente pulsa "Reproducir" / retorno de foco reanudable | `PLAYING` | Reanuda desde `positionMs` persistido. |
| `PAUSED` / `PLAYING` | Oyente pulsa "Detener" | `STOPPED` | Persiste `resumeStatus=STOPPED`. |
| `STOPPED` / `PAUSED` / `PLAYING` | Oyente selecciona "Siguiente" / "Anterior" / "Seek" | `PREPARING` / *(mismo)* | "Seek" actualiza `positionMs` sin salir de `PLAYING`/`PAUSED`. |
| *(cualquiera)* | **Terminación forzada del SO (OOM kill)** | `IDLE` → restauración | Al reiniciar, se relee `PlaybackState` + `QueueItem` (por `playbackPosition`) y se reposiciona con `currentQueueItemId`, restaurando pista, `positionMs`, cola y modo en **< 2s** ([RNF-05] / Perturbación 3). **Sin autoplay:** se restaura según `resumeStatus` (`PAUSED`/`STOPPED`), nunca en `PLAYING`. |

### 5.2. Ciclo de Vida de: `Track` (Disponibilidad frente al Sistema de Archivos)

Gobierna el **Equilibrio de Organización** y la **Invariante 2 (Fidelidad al Sistema de Archivos)**.

- **Estado Inicial (Nacimiento):** `AVAILABLE` (al ser descubierto e indexado con metadatos extraíbles y formato soportado).
- **Estados Finales (Terminación):** Purga (el registro se **elimina** del Catálogo; no hay estado "fantasma" — Invariante 2).

| **Estado Origen** | **Evento / Trigger (Acción)** | **Estado Destino (Resultado)** | **Condiciones / Validaciones Previas** |
| --- | --- | --- | --- |
| *(no existe)* | Escaneo descubre archivo con formato soportado | `AVAILABLE` | Metadatos extraídos; campos ausentes → centinela id=1 / `NULL` ([RF-02], Invariante 4, §1). |
| *(no existe)* | Escaneo descubre archivo con formato **no** soportado | `UNSUPPORTED` | Indexado pero no reproducible ([Restricción 2]); visible en biblioteca. |
| `AVAILABLE` | Motor intenta reproducir y el archivo no existe | `MISSING` | Dispara salto a la siguiente pista ([RF-09] / Perturbación 4). |
| `AVAILABLE` | Motor intenta reproducir y falla la **decodificación** (formato no soportado detectado en runtime / archivo corrupto) | `UNSUPPORTED` | Coherente con el error en `PREPARING` de §5.1. El track permanece **visible pero no reproducible** ([Restricción 2]); dispara salto a la siguiente pista ([RF-09] / Invariante 6). |
| `AVAILABLE` | Oyente edita metadato ([RF-04]) | `AVAILABLE` | Se reescribe la etiqueta ID3 en disco; se recalculan FKs `artistId`/`albumId`/`genreId` y se purgan dimensiones huérfanas (Bucle de Coherencia del Catálogo, SDD §4.1). |
| `AVAILABLE` / `UNSUPPORTED` | Oyente confirma **eliminación física** ([RF-06]) | *(purgado)* | **Invariante 5:** requiere confirmación explícita. Se borra el archivo, el registro `Track` y en cascada sus vínculos de playlist, cola y marcador. |
| `MISSING` | Próximo escaneo confirma ausencia | *(purgado)* | Se elimina del Catálogo y de todas las playlists automáticamente ([RF-03] / Invariante 2). |
| `MISSING` | Próximo escaneo re-descubre el archivo en la misma URI | `AVAILABLE` | Reaparición del archivo; se re-habilita para reproducción. |

### 5.3. Ciclo del Proceso de Escaneo (Motor de Biblioteca)

Ciclo operativo, no entidad persistida, pero define transiciones observables por la UI (indicador de progreso, [RNF-03]). Estado inicial `IDLE`; sin estado final (cíclico).

| **Estado Origen** | **Evento / Trigger** | **Estado Destino** | **Condiciones / Validaciones** |
| --- | --- | --- | --- |
| `IDLE` | Primera ejecución / re-escaneo manual / cambio de Carpetas Fuente | `SCANNING` | Debe existir ≥ 1 `SourceFolder` con permiso vigente ([RF-01]). Corre en background thread ([RNF-03]). |
| `SCANNING` | Recorrido recursivo completado | `SYNCING` | Reporta progreso determinista si supera 1s ([RNF-03]). |
| `SYNCING` | Altas, bajas y purga de huérfanos aplicadas | `IDLE` (Catálogo actualizado) | Estado de cierre del Ciclo de Escaneo (SDD §1.3): sin archivos sin procesar ni entradas huérfanas. |
| `SCANNING` / `SYNCING` | Permiso revocado / carpeta inaccesible ([Restricción 6]) | `IDLE` (con aviso) | Notifica al Oyente; conserva el último Catálogo coherente. No inventa ni borra por error de acceso. |

## 6. Datos Semilla y Retención (Termodinámica del Dato)

*Reglas de inicialización y envejecimiento de la memoria del sistema.*

### 6.1. Condiciones de Inicialización (Big Bang)

Registros que deben existir en el momento cero del despliegue (creados en la migración/`onCreate` de la base de datos Room):

- **`Artist`, `Album`, `Genre` — Registro centinela reservado:** Cada una de estas tablas se siembra con **una fila fija `id = 1`** cuyo `name` es una cadena vacía sentinela. Es el destino por defecto de las FKs de cualquier `Track` cuya etiqueta ID3 correspondiente esté ausente. La **capa de presentación** renderiza `id = 1` como la etiqueta localizada ("Sin artista", "Sin álbum", "Sin género"). Materializa la **Invariante 4** sin inventar datos y sin acoplar el idioma a los datos (§1). Para `Album` semilla, `artistId = 1`. **Estos registros nunca se purgan.**
- **`PlaybackState` — Fila singleton:** Una única fila `id = 1` con `currentQueueItemId = NULL`, `positionMs = 0`, `repeatMode = OFF`, `shuffleEnabled = false`, `resumeStatus = STOPPED`. Es el motor en su estado de arranque nominal (SDD §4.1, paso 5).
- **`AppSettings` — Fila singleton:** Una única fila `id = 1` con `onboardingCompleted = false`, `themePreference = SYSTEM`. `onboardingCompleted = false` fuerza el flujo de primera ejecución (permisos → selección de Carpetas Fuente → escaneo inicial; Apalancamiento 5, SDD §4.1).
- **`SourceFolder`, `Track`, `Playlist`, `QueueItem`, `PlaybackProgress`:** **Vacíos** en el Big Bang. La biblioteca nace vacía; se puebla exclusivamente por acción del Oyente (agregar carpetas) y del Motor de Biblioteca (escaneo). Ningún contenido precargado (coherente con la autarquía, Invariante 1, y la soberanía, Invariante 3).

### 6.2. Volumen y Depuración (Purge / Archiving)

- **Tasa de Crecimiento Esperada:**
  - `Track`: **1 registro por archivo de audio** en las Carpetas Fuente. Es la tabla dominante en volumen; acotada por la capacidad de almacenamiento del dispositivo ([Restricción 1], Bucle de Crecimiento de Biblioteca). Diseño orientado a decenas de miles de filas ([Restricción 4]).
  - `Artist`/`Album`/`Genre`: crecimiento sublineal (varias pistas comparten dimensión). Normalización exigida por **[RNF-08]** para minimizar la huella.
  - `PlaylistTrackCrossRef`: crece con la actividad de curación del Oyente (Bucle de Enriquecimiento de Playlists).
  - `QueueItem`: acotado al tamaño de la sesión activa; se reconstruye por sesión.
  - `PlaybackProgress`: acotado al número de podcasts con reanudación pendiente; nunca crece con la música.
  - `PlaybackState`/`AppSettings`: **cardinalidad fija = 1** (singletons). No crecen.
- **`[F-5]` Huella de carátulas:** Las carátulas **no se persisten** como archivos propios. Se leen *on-demand* desde los bytes embebidos del archivo de audio (`hasEmbeddedArtwork = true`), delegando el cacheo en memoria a la capa de imagen (p. ej. Coil/Glide con la `uri` del track). Huella en disco = **cero**, honrando [RNF-08]. La carátula de álbum se resuelve desde una pista representativa del álbum con `hasEmbeddedArtwork = true`.
- **Políticas de Depuración:**
  - **Purga por fidelidad (Invariante 2 / [RF-03]):** Cada escaneo elimina los `Track` en estado `MISSING` (archivos inexistentes), y en cascada sus vínculos en `PlaylistTrackCrossRef`, `QueueItem` y su `PlaybackProgress`. No se conservan entradas fantasma.
  - **Purga de dimensiones huérfanas:** Tras cada escaneo, tras cada edición de metadatos ([RF-04]) y tras eliminar una `SourceFolder`, se eliminan los `Artist`/`Album`/`Genre` que ya no son referenciados por ningún `Track` — **excepto los registros centinela `id = 1`**. Mantiene la huella mínima ([RNF-08]).
  - **Vaciado de cola:** `QueueItem` se purga/reconstruye al iniciar una nueva sesión de reproducción; no se acumula histórico de colas.
  - **Retención de datos comportamentales: PROHIBIDA (`0 días`).** El sistema **no genera** registros de comportamiento del Oyente ([RNF-07] / Invariante 3), por lo que no existe política de archivado ni caducidad para ellos: simplemente **no se crean**. `PlaybackProgress` no es una excepción: guarda una única posición operativa por pista (marcador de reanudación), no un historial ni una métrica de hábitos. La única "memoria" del sistema es su estado operativo actual, nunca su historia de uso.
