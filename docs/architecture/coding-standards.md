# Documento de Estándares y Convenciones de Código

## 1. Propósito y Alcance general

Este documento establece las reglas obligatorias y las prácticas recomendadas para la escritura de código fuente en el proyecto. Su propósito es garantizar la legibilidad, la mantenibilidad y la uniformidad del código, mitigando la entropía técnica generada por los diferentes estilos de programación de los miembros del equipo. Este manual debe ser la principal referencia durante las revisiones de código (*Code Reviews*).

El proyecto es una aplicación **Android nativa en Kotlin**, 100% local (sin acceso a red), con **Arquitectura Limpia** en tres capas (presentación → dominio ← datos), patrón **Model–ViewModel–View** y enfoque **Single-Activity**. Sus módulos Gradle son `:core:domain`, `:core:data`, `:feature:library`, `:feature:player`, `:feature:playlists`, `:feature:settings`, `:service:playback`, `:service:indexer` y `:app`. Todos los ejemplos de este documento usan nombres reales del proyecto.

## 2. Estándares Obligatorios de Nomenclatura (Naming Conventions)

*Define las reglas estrictas sobre cómo se deben nombrar los distintos elementos del código para mantener una semántica universal.*

### 2.1. Idioma del Código Fuente

* **Código en inglés (obligatorio):** todos los identificadores (clases, interfaces, funciones, propiedades, parámetros, paquetes y módulos Gradle) y todos los comentarios y bloques `KDoc` se escriben en inglés. El español queda **prohibido dentro del código**.
* **Textos de interfaz en español, fuera del código:** toda cadena visible se escribe en español pero **jamás como literal incrustado** en Kotlin/Compose: se define en `res/values/strings.xml` y se referencia por clave (`R.string.*` / `stringResource(...)`). La **clave del recurso va en inglés `snake_case`** y su **valor en español**. Así el idioma queda desacoplado del código y se facilita la internacionalización futura.

  ```kotlin
  // ✅ CORRECTO — clave en inglés, valor en español fuera del código
  // strings.xml:  <string name="label_no_information">Sin información</string>
  Text(text = stringResource(R.string.label_no_information))

  // ❌ INCORRECTO — literal en español incrustado en el código
  Text(text = "Sin información")
  ```

* **Traducción de conceptos de negocio a término técnico:** cada concepto del dominio (español) tiene **un único** nombre técnico en inglés, usado de forma consistente en todo el código:

  | Concepto de dominio | Término técnico canónico (código) |
  |---|---|
  | Archivo de Audio / entrada del Catálogo | `Track` |
  | Metadatos Embebidos (ID3) | *tags* / `EditTags`, `Id3DataSource` |
  | Agrupación Personalizada | `Playlist` |
  | Carpeta Fuente | `SourceFolder` |
  | Cola de Reproducción | `Queue` / `QueueItem` |
  | Motor de Reproducción | `PlaybackService` / `SonusPlayer` |
  | Motor de Biblioteca | `LibraryScanWorker` / `CatalogSynchronizer` |
  | Catálogo de Biblioteca | `Catalog` (`CatalogRepository`) |

### 2.2. Convenciones de Capitalización (Casing)

Se adoptan las convenciones oficiales de Kotlin. La siguiente tabla es de cumplimiento obligatorio:

| Elemento | Convención | Ejemplo del proyecto |
|---|---|---|
| Paquetes | `minúsculas`, sin guiones ni `_`, un concepto por segmento | `com.ceiba.sonus.core.domain.model` |
| Módulos Gradle | `minúsculas`, jerárquicos con `:` | `:core:domain`, `:feature:library` |
| Clases, interfaces, `object`, `enum class`, anotaciones | `PascalCase` | `CatalogRepository`, `SonusPlayer`, `LibraryViewModel` |
| Funciones `@Composable` | `PascalCase` (excepción idiomática de Compose) | `MiniPlayerBar`, `ArtworkImage`, `LibraryScreen` |
| Funciones y propiedades | `camelCase` | `observeNowPlaying()`, `positionMs`, `currentQueueItemId` |
| Propiedad de respaldo mutable privada | `_camelCase` (único uso permitido del guion bajo inicial) | `_uiState` (`MutableStateFlow`) expuesta como `uiState` (`StateFlow`) |
| Parámetros y variables locales | `camelCase` | `trackId`, `treeUri`, `displayPath` |
| Constantes de compilación (`const val`) y `val` inmutables de nivel superior | `UPPER_SNAKE_CASE` | `PREVIOUS_RESTART_THRESHOLD_MS`, `SEARCH_DEBOUNCE_MS` |
| Constantes de `enum` | `UPPER_SNAKE_CASE` | `MUSIC`, `PODCAST`, `PLAY_NEXT`, `DATE_ADDED_DESC` |
| Subtipos de `sealed interface`/`class` (son **tipos**, no constantes) | `PascalCase` | `LossTransient`, `PlayPause`, `PlayContext` |
| Códigos de error (`String` estable) | `UPPER_SNAKE_CASE` con prefijo `ERR_` | `ERR_PERMISSION_DENIED`, `ERR_CONFIRMATION_REQUIRED` |
| Tablas y columnas Room (SQL) | `snake_case` en `tableName`; propiedad Kotlin mapeada en `camelCase` | tabla `source_folder` ↔ propiedad `sourceFolderId` |

```kotlin
// ✅ CORRECTO
class LibraryViewModel(private val browseCatalog: BrowseCatalogUseCase) : ViewModel() {
    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()
    companion object { private const val SEARCH_DEBOUNCE_MS = 300L }
}

// ❌ INCORRECTO — notación húngara, prefijo "I", guion bajo indebido, casing mixto
interface ICatalogRepo { fun Browse(mCon: String): List<track_view> }
```

* **Prohibiciones explícitas:** notación húngara (`strName`, `mCount`, `bFlag`), prefijo `I` en interfaces (`ICatalogRepository`), sufijo/`_` decorativo en tipos, y abreviaturas oscuras (`ctx`, `mgr`, `qmgr`) salvo las universalmente aceptadas (`id`, `uri`, `ms`, `db`).

### 2.3. Sufijos y Prefijos Estructurales

Kotlin **no usa prefijos** (nada de `I` para interfaces ni notación húngara); el rol se comunica mediante **sufijos obligatorios**. El sufijo declara la capa: un tipo con sufijo `UseCase` vive **siempre** en `:core:domain`; uno con sufijo `Impl` vive **siempre** en `:core:data`.

| Rol arquitectónico | Regla de nombre | Ejemplo | Capa / Módulo |
|---|---|---|---|
| Puerto (contrato de repositorio) | Interface **sin** prefijo `I`, nombrada por su rol | `CatalogRepository`, `PlaybackController`, `MetadataEditor`, `FileSystemGateway` | Dominio |
| Implementación de un puerto | Sufijo **`Impl`** | `CatalogRepositoryImpl`, `MetadataEditorImpl` | Datos |
| Caso de uso (uno por interacción del sistema) | Sufijo **`UseCase`** | `ScanLibraryUseCase`, `EditTrackTagsUseCase`, `DeleteFileUseCase` | Dominio |
| Comando (intención del Oyente) | `sealed interface` agrupado por dominio con sufijo **`Command`**; subtipos con verbo/sustantivo, **sin** sufijo | `LibraryCommand.AddSourceFolder`, `PlayerCommand.Enqueue`, `MetadataCommand.EditTags` | Dominio |
| DAO de Room | Sufijo **`Dao`** | `TrackDao`, `QueueDao`, `PlaybackStateDao` | Datos |
| Fuente de datos local | Sufijo **`DataSource`** | `SafDataSource`, `Id3DataSource`, `Media3PlayerDataSource` | Datos |
| ViewModel de pantalla | Sufijo **`ViewModel`** | `LibraryViewModel`, `PlayerViewModel`, `OnboardingViewModel` | Presentación |
| Estado / evento de UI observable | Sufijo **`State`** / **`Event`** | `LibraryUiState`, `NowPlayingState`, `ScanState`, `UiEvent` | Presentación / Dominio |
| `Worker` de WorkManager | Sufijo **`Worker`** | `LibraryScanWorker` | Servicio (indexer) |
| `Service` de Android | Sufijo **`Service`** | `PlaybackService` | Servicio (playback) |
| Colaborador especializado | Sufijo por rol: `Manager`, `Coordinator`, `Receiver`, `Callback`, `Mapper(s)`, `Converter(s)`, `Synchronizer`, `Emitter` | `AudioFocusManager`, `SessionPersistenceCoordinator`, `BecomingNoisyReceiver`, `MediaSessionCallback`, `EntityMappers`, `RoomTypeConverters`, `CatalogSynchronizer`, `ScanStateEmitter` | Servicio / Datos |
| Base de datos Room | Sufijo **`Database`** | `SonusDatabase` | Datos |
| `@Entity` de Room | Nombre de dominio **singular**, sin sufijo; puente N:M con sufijo **`CrossRef`** | `Track`, `Album`, `PlaybackState`, `PlaylistTrackCrossRef` | Datos |
| Alias de identidad (`typealias`) | Sufijo **`Id`** | `TrackId`, `PlaylistId`, `FolderId` | Dominio |
| Pantalla / componente Compose | Pantalla con sufijo **`Screen`**; componente reutilizable con nombre descriptivo | `LibraryScreen`, `MiniPlayerBar`, `ArtworkImage` | Presentación |

### 2.4. Nomenclatura de Archivos y Directorios

* **Un archivo `.kt` = un tipo público de nivel superior**, y el archivo se nombra **exactamente** como ese tipo en `PascalCase`: `Track.kt`, `LibraryViewModel.kt`, `CatalogRepositoryImpl.kt`.
* **Excepción idiomática de Kotlin:** una **jerarquía `sealed` cerrada** y sus subtipos pueden convivir en un solo archivo nombrado como el tipo raíz: `PlayerCommand.kt`, `DomainError.kt`, `PlaybackSource.kt`. Los archivos de **solo funciones de extensión/mapeo** se nombran por su propósito: `EntityMappers.kt`.
* **Paquetes:** en `minúscula`, una palabra por segmento, en **singular** por concepto, sin `_` ni `camelCase`. La raíz de paquete es `com.ceiba.sonus` y la organización sigue los módulos Gradle del proyecto:

  ```
  :core:domain     → com.ceiba.sonus.core.domain / { model, port, usecase, command, error }
  :core:data       → com.ceiba.sonus.core.data   / { local.room (dao, entity, converter),
                                                     local.saf, id3, media3, repository, mapper }
  :feature:library → com.ceiba.sonus.feature.library / { presentation (viewmodel, screen, component), di }
                     (idéntico patrón para :feature:player, :feature:playlists, :feature:settings)
  :service:playback→ com.ceiba.sonus.service.playback
  :service:indexer → com.ceiba.sonus.service.indexer
  :app             → com.ceiba.sonus.app  (MainActivity, SonusNavHost, grafo Hilt)
  ```

* **Módulos Gradle:** en `minúscula`, jerárquicos con `:` y con prefijo por naturaleza (`:core:`, `:feature:`, `:service:`, `:app`).
* **Recursos Android (`res/`):** nombres de archivo e `id` en `snake_case` minúscula (`ic_play_arrow`, `mini_player_progress`). Las **claves** de `strings.xml` en inglés `snake_case` y su **valor** en español. No existen recursos remotos ni carátulas de red.
* **Tablas Room:** el atributo `tableName` en `snake_case`: `track`, `artist`, `album`, `genre`, `source_folder`, `playlist`, `playlist_track`, `queue_item`, `playback_state`, `playback_progress`, `app_settings`.

## 3. Formato y Sintaxis del Código (Estilo)

*Establece las reglas visuales y tipográficas del código fuente para garantizar que las herramientas de control de versiones (Git) no generen conflictos por diferencias de formato.*

El estilo se verifica automáticamente con `ktlint` (formato) y `detekt` (olores de código), configurados desde un único `.editorconfig` en la raíz del repositorio. El formato que no pase `ktlint` **rompe el build** (ver Sección 6).

### 3.1. Indentación y Espaciado

* **Indentación:** **4 espacios**, nunca tabulaciones (`indent_style = space`, `indent_size = 4`). La indentación de continuación de una expresión partida es también de 4 espacios.
* **Límite de longitud de línea:** **120 caracteres** (`max_line_length = 120`). Al superarlo, partir por parámetro/argumento, un elemento por línea.
* **Llaves:** estilo **K&R** ("egipcio"): la llave de apertura al final de la línea de declaración; `else`/`catch`/`finally` en la misma línea que la llave de cierre. Toda estructura de control con cuerpo usa llaves salvo el `if` de una sola línea sin `else`.
* **Espaciado:** un espacio alrededor de operadores binarios y `=`, tras `,` y `:` (en tipos), y tras las palabras clave (`if (`, `when (`); sin espacio tras el nombre de función en su invocación. **Una** línea en blanco máximo entre miembros; sin líneas en blanco al inicio/fin de un bloque.
* **Comas finales (*trailing commas*):** **obligatorias** en construcciones multilínea (parámetros, argumentos, entradas de colección). Minimizan el ruido en los *diffs* de Git y previenen conflictos de *merge* al agregar elementos.
* **Higiene de archivo:** codificación **UTF-8**, fin de línea **LF** (`end_of_line = lf`, fijado también en `.gitattributes` para uniformidad Windows/Unix), **sin espacios en blanco al final** de línea (`trim_trailing_whitespace = true`) y **salto de línea final** obligatorio (`insert_final_newline = true`).
* **Preferencias idiomáticas:** cuerpos de expresión (`=`) para funciones de una sola expresión; una sentencia por línea; `val` por defecto.

```kotlin
// ✅ CORRECTO — 4 espacios, K&R, trailing comma, cuerpo de expresión
fun buildQueue(
    source: PlaybackSource,
    startTrackId: TrackId?,
    startShuffled: Boolean = false,
): OperationResult<Unit> = playContext(source, startTrackId, startShuffled)
```

### 3.2. Estructura Interna del Archivo

* **Sin cabecera de licencia/copyright:** el archivo comienza directamente con la declaración `package`.
* **Orden macro del archivo `.kt`:** (1) `package`; (2) bloque de `import`; (3) declaraciones de nivel superior. Se respeta la regla de **un tipo público por archivo** (con la excepción de las jerarquías `sealed`).
* **Orden interno de una clase:**
  1. Propiedades (primero la **propiedad de respaldo privada** `_x`, inmediatamente seguida de su versión pública `x`) y bloques `init`.
  2. Constructores secundarios (excepcionales en Kotlin).
  3. Funciones, agrupadas por **cohesión**: una función privada de ayuda se coloca junto a la función pública que la usa (la cohesión prima sobre el orden estricto público→privado).
  4. Clases anidadas.
  5. `companion object` **al final** (aloja `const` y *factories*).
* **Regla específica MVVM (presentación):** un `ViewModel` expone **estado inmutable primero** (`StateFlow<UiState>` público respaldado por un `MutableStateFlow` privado; eventos efímeros por `SharedFlow`/`Channel`), luego el punto de entrada de comandos (`onCommand(...)`) y por último los manejadores privados. El `ViewModel` **no** contiene reglas de negocio ni I/O: delega en casos de uso.

```kotlin
package com.ceiba.sonus.feature.library.presentation.viewmodel

import ...

class LibraryViewModel(
    private val browseCatalog: BrowseCatalogUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _events = Channel<UiEvent>(Channel.BUFFERED)
    val events: Flow<UiEvent> = _events.receiveAsFlow()

    fun onCommand(command: LibraryCommand) = when (command) {   // when exhaustivo, sin else
        is LibraryCommand.AddSourceFolder -> addSourceFolder(command)
        is LibraryCommand.Scan -> scan(command)
        // ...
    }

    private fun addSourceFolder(command: LibraryCommand.AddSourceFolder) { /* delega en el caso de uso */ }

    companion object {
        private const val SEARCH_DEBOUNCE_MS = 300L
    }
}
```

### 3.3. Organización de Importaciones / Dependencias

* **Bloque único ordenado alfabéticamente:** un **único bloque de imports ordenado alfabéticamente en orden ASCII, sin líneas en blanco ni agrupaciones** por origen. No se separan imports externos e internos; el orden lexicográfico los intercala de forma determinista.
* **Prohibido el import con comodín (`*`):** cada símbolo se importa explícitamente. `ktlint` rechaza `import androidx.compose.material3.*`.
* **Sin imports sin usar:** `detekt`/`ktlint` fallan ante imports no utilizados; el IDE debe tener activado "optimizar imports al guardar".
* **Alias de import (`as`)** solo para resolver colisiones de nombre reales (p. ej. dos `State` de paquetes distintos), nunca por conveniencia estética.

```kotlin
// ✅ CORRECTO — bloque único, alfabético ASCII, sin comodines ni líneas en blanco
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ceiba.sonus.core.domain.command.LibraryCommand
import com.ceiba.sonus.core.domain.usecase.BrowseCatalogUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

// ❌ INCORRECTO — comodín y grupos separados por líneas en blanco
import androidx.lifecycle.*

import com.ceiba.sonus.core.domain.usecase.BrowseCatalogUseCase
```

## 4. Reglas de Implementación Arquitectónica

*Traduce las decisiones tomadas en los Diagramas de Componentes y Clases a reglas de acoplamiento físico en el código.*

La regla de dependencia apunta **siempre hacia adentro**: `Presentación → Dominio ← Datos`. El **Dominio** es Kotlin puro (sin Android); la **capa de Datos** implementa sus puertos e integra los *frameworks*; la **Presentación** depende solo del Dominio. La frontera se protege con el grafo de módulos Gradle (un módulo no ve lo que no declara) y con reglas de arquitectura verificables (`Konsist`/`detekt`) que **rompen el build** ante una violación.

### 4.1. Dirección de Dependencias (Imports Prohibidos)

Cada módulo Gradle tiene una lista cerrada de lo que **le está prohibido importar**:

| Módulo | Puede depender de | **Imports PROHIBIDOS** |
|---|---|---|
| `:core:domain` (Dominio, Kotlin puro) | Solo Kotlin estándar, `kotlinx.coroutines`/`Flow`, `javax.inject` | `android.*`, `androidx.*`, `androidx.room.*`, `androidx.media3.*`, SAF/`DocumentFile`, Hilt-Android, Coil, WorkManager, y **cualquier** clase de `:core:data` o `:feature:*` |
| `:core:data` (Datos) | `:core:domain` + *frameworks* de infraestructura (Room, SAF, Media3, ID3) | Cualquier `:feature:*`; cualquier símbolo de Presentación (Compose, `ViewModel`) |
| `:feature:*` (Presentación) | `:core:domain`, `androidx.compose`, `androidx.lifecycle`, Hilt | `:core:data` (repos, DAOs, `@Entity`, DataSources); **otro `:feature:*`**; `androidx.room.*`, `androidx.media3.*`, SAF |
| `:service:playback` / `:service:indexer` (Hosts de infraestructura) | `:core:domain` + `:core:data` | Cualquier `:feature:*` (la UI nunca es dependencia de un servicio) |
| `:app` | Todos (ensambla el grafo Hilt) | — (único módulo que conoce todo; no contiene lógica de negocio) |

* **El `ViewModel` nunca importa un repositorio, DAO o *DataSource*:** solo consume **casos de uso** del dominio, inyectados por Hilt. Importar `CatalogRepositoryImpl` o `TrackDao` desde presentación es una violación de arquitectura.
* **La capa de Datos depende del Dominio, no al revés (inversión de dependencias):** los puertos (`CatalogRepository`, `MetadataEditor`, `FileSystemGateway`, …) se **definen** en `:core:domain` y se **implementan** en `:core:data` con el sufijo `Impl`. El cableado lo resuelve Hilt en `:app`.
* **Presentación solo conoce los canales de comando y estado:** la sesión de medios, el foco de audio, SAF y la decodificación son operados por `:service:*` y `:core:data`, nunca por una `@Composable` o un `ViewModel`.
* **Sin dependencias entre *features*:** la navegación entre pantallas se resuelve en `:app` (`SonusNavHost`); `:feature:library` no importa `:feature:player`.
* **Aislamiento de red:** está **prohibido** importar cualquier cliente de red (`java.net`, `okhttp`, `retrofit`), SDK de telemetría (Crashlytics, Sentry, Analytics) o declarar `android.permission.INTERNET`.

### 4.2. Manejo de Estados y Mutabilidad

* **Inmutabilidad por defecto:** `val` siempre; `var` solo con justificación local acotada. Los modelos de dominio y los `UiState` son `data class` **inmutables**; toda evolución de estado se hace con `.copy(...)`, nunca mutando en sitio.
* **Colecciones inmutables en superficies públicas:** exponer `List`/`Map` (solo lectura), jamás `MutableList`/`MutableMap`. La mutabilidad queda confinada al cuerpo de la función que la construye.
* **Estado observable con respaldo privado:** el estado se expone como `StateFlow<UiState>` **inmutable**, respaldado por un `MutableStateFlow` **privado**, actualizado atómicamente con `update { it.copy(...) }`. **Prohibido** exponer el flujo mutable.
* **Eventos efímeros fuera del estado:** avisos de una sola vez (errores, "pista omitida", fin de escaneo) viajan por `Channel`/`SharedFlow`, **nunca** dentro del `UiState`, para no re-emitirse ante recomposiciones o cambios de configuración.
* **Prohibido el estado global mutable:** sin `object`/`companion` con `var`, sin *singletons* mutables. Cada tipo de estado tiene un único dueño: el **durable** vive en Room; el **runtime** de reproducción vive en el `PlaybackService`; el **transitorio de UI** (scroll, foco) vive en `ViewModel`/`SavedStateHandle` y **no se persiste**.
* **Exhaustividad de tipos cerrados:** todo `when` sobre un `sealed`/`enum` es **exhaustivo, sin rama `else`**, para que agregar un caso falle en compilación.
* **Atomicidad y concurrencia:** las mutaciones que deben preservar contigüidad/unicidad (posiciones de cola y playlist) se ejecutan en **una única transacción Room** (`@Transaction`). No se comparte estado mutable entre hilos: la coordinación es por corrutinas + `Flow`; toda I/O corre **fuera del hilo principal** en un dispatcher de I/O **inyectado**, nunca en el hilo de la interfaz.
* **Persistencia de enums y ausencia de dato:** los enums se serializan por **nombre estable**, nunca por *ordinal*; la ausencia de metadato se representa con el **centinela `id = 1`** o `NULL`, **nunca** con un literal de presentación. Queda **prohibido** introducir columnas de comportamiento del usuario (`playCount`, `lastPlayedAt`, `skipCount`, …).

### 4.3. Gestión de Errores y Excepciones

* **Errores como valores, no como excepciones de control:** el dominio **nunca** propaga un `throw` a través de su frontera. Toda operación que puede fallar retorna `OperationResult<T>` (`Success`/`Failure`) con un `DomainError` **tipado**. Se consume con `when` exhaustivo, no con `try/catch` dispersos.
* **Captura solo en el borde:** las excepciones de infraestructura (SAF, Room, Media3, ID3) se capturan **exclusivamente en la capa de Datos** (repositorios/*data sources*) y se traducen a `DomainError` mediante un `ErrorMapper`. `try/catch` fuera de ese borde es una violación; **prohibido** el `catch (e: Exception)` genérico que silencia el fallo (*swallow*).
* **Error sin `message`:** `DomainError` expone `code` (`UPPER_SNAKE` con prefijo `ERR_`), `severity` y `recoverable`, **sin** campo `message`. El texto humano (en español) se resuelve en la capa de presentación a partir del `code`.
* **La `severity` decide el canal:** `INFO` → aviso efímero no bloqueante; `WARNING` → banner con estado coherente conservado; `ERROR` → la operación se aborta sin efecto colateral.
* **Continuidad sobre corrección:** los fallos de una **pista individual** (`ERR_TRACK_UNSUPPORTED`, `ERR_TRACK_MISSING`) son severidad `INFO`, disparan **salto automático** a la siguiente pista disponible y **no** abortan la sesión; solo se detiene si no queda ninguna válida.
* **Guardia de irreversibilidad:** toda operación destructiva exige `confirmed = true` como **precondición del contrato**; su ausencia devuelve `ERR_CONFIRMATION_REQUIRED`, jamás una ejecución silenciosa.
* **Cero telemetría en el fallo:** **prohibido** reportar errores a servicios externos (Crashlytics, Sentry, cualquier *sink* de red). El *logging*, de existir, es **estrictamente local y operativo** (p. ej. `android.util.Log` en *debug*), **nunca** registra datos de comportamiento del usuario ni sale del dispositivo.

```kotlin
// ✅ CORRECTO — error como valor, captura en el borde (Datos), mapeo a DomainError tipado
override suspend fun writeTags(command: EditTags): OperationResult<Unit> =
    try {
        id3DataSource.write(command)              // frontera de infraestructura
        OperationResult.Success(Unit)
    } catch (e: TagWriteException) {
        OperationResult.Failure(errorMapper.map(e))   // -> DomainError(ERR_TAG_WRITE_FAILED)
    }

// ❌ INCORRECTO — throw cruzando la frontera de dominio y catch genérico que silencia
suspend fun writeTags(command: EditTags) {
    try { id3DataSource.write(command) } catch (e: Exception) { /* swallow */ }
}
```

## 5. Estándares de Pruebas Automatizadas (Testing)

*Define las reglas inquebrantables para asegurar la calidad del código mediante la verificación automatizada.*

Stack de pruebas obligatorio: pruebas unitarias JVM con **JUnit5** + **kotlin.test**/**Truth** (aserciones); **MockK** para dobles (idiomático Kotlin, **no** Mockito); **kotlinx-coroutines-test** (`runTest`, `TestDispatcher`) para corrutinas; **Turbine** para verificar `Flow`/`StateFlow`. Las pruebas de DAO de Room corren como **instrumentadas** (`androidTest`) contra una base de datos **en memoria**. La cobertura se mide con **Kover**. Se sigue la **pirámide de pruebas**: la mayoría son unitarias sobre `:core:domain` (casos de uso) y los `ViewModel`; las instrumentadas se reservan para Room, Media3 y SAF.

### 5.1. Patrón de Estructura de Pruebas

* **Patrón 3A obligatorio (Arrange–Act–Assert):** toda prueba separa visiblemente sus tres fases con comentarios `// Arrange`, `// Act`, `// Assert` (equivalente a *Given–When–Then*). Prohibido intercalar acción y verificación.
* **Un comportamiento por prueba:** cada test valida **un** resultado observable; se evita la aserción múltiple no relacionada. Si un escenario exige varias verificaciones del mismo resultado, se agrupan (p. ej. `assertAll`).
* **Determinismo total:** ninguna prueba unitaria toca disco, SAF ni el reloj/aleatoriedad reales. El tiempo y la aleatoriedad se **inyectan** mediante *providers*, alimentados con valores fijos en la prueba.
* **Corrutinas y flujos:** las funciones `suspend` se prueban con `runTest`; el hilo principal se sustituye con un `TestDispatcher`. Los `Dispatchers` **se inyectan** (vía un `DispatcherProvider`), nunca se codifican con `Dispatchers.IO` fijo, para poder sustituirlos en prueba. Los `Flow`/`StateFlow` se verifican con **Turbine**.
* **Aislamiento por capa:** el caso de uso se prueba contra **dobles de sus puertos**; el `ViewModel` se prueba contra **dobles de sus casos de uso**, verificando las transiciones de `UiState` y la emisión de `UiEvent`.

```kotlin
@Test
fun `returns Failure ERR_CONFIRMATION_REQUIRED when confirmed is false`() = runTest {
    // Arrange
    val gateway = FakeFileSystemGateway()
    val useCase = DeleteFileUseCase(gateway)
    val command = DeleteFileBuilder().withConfirmed(false).build()   // Test Data Builder

    // Act
    val result = useCase(command)

    // Assert
    assertThat(result).isEqualTo(OperationResult.Failure(DomainError.ConfirmationRequired))
    assertThat(gateway.deletedUris).isEmpty()   // no borra nada
}
```

### 5.2. Nomenclatura de Casos de Prueba

* **Nombre en inglés entre *backticks*:** el nombre de la función de prueba se escribe en **inglés** entre acentos graves y describe **escenario + resultado esperado**, con la forma `` `<resultado esperado> when <escenario>` `` (o `should… when…`). Debe leerse como una afirmación de comportamiento.
  * ✅ `` `returns EmptyPlayableQueue when no track in context is available` ``
  * ✅ `` `skips to next track and keeps session alive when decode fails` ``
  * ❌ `test1`, `` `funciona bien` ``, `testDelete`.
* **Clase de prueba = *SUT* + sufijo `Test`:** la clase espeja a la unidad bajo prueba (*System Under Test*): `DeleteFileUseCaseTest`, `LibraryViewModelTest`. El archivo se ubica en el *source set* correspondiente (`test/` para JVM, `androidTest/` para instrumentadas) replicando el paquete de la clase probada.
* **Agrupación de escenarios:** los escenarios de una misma unidad se agrupan con `@Nested` (JUnit5) cuando aporta claridad.

### 5.3. Uso de Datos de Prueba (Mocking y Stubs)

* **Test Data Builder obligatorio:** los objetos de dominio (`Track`, `Playlist`, comandos como `EditTags`/`DeleteFile`) se construyen en las pruebas mediante ***builders*** con valores por defecto válidos y métodos `withX(...)` fluidos que solo sobrescriben lo relevante al escenario. Prohibido construir entidades a mano repetidamente (evita pruebas frágiles ante cambios de esquema). Los *builders* viven en un *source set* de prueba compartido.
* **Fakes sobre mocks para los puertos:** para los puertos de repositorio/*gateway* (`CatalogRepository`, `FileSystemGateway`, `SessionRepository`, …) se prefieren **implementaciones falsas en memoria** (`FakeXxx`) que preservan el comportamiento contractual, más robustas que un *mock* de interacciones.
* **MockK para colaboradores y verificación de interacción:** se usa **MockK** cuando interesa verificar que una interacción ocurrió (p. ej. que un caso de uso invocó `PlaybackController.next()`), con `verify { }`; `relaxed`/`relaxUnitFun` solo cuando reduce ruido sin ocultar el contrato. **Nunca** se *mockea* un tipo del que se depende por su valor (usar un *builder* real).
* **Cero recursos reales:** las pruebas unitarias **no** acceden a SAF, sistema de archivos real, Media3 ni a red. El acceso a Room se prueba con base de datos **en memoria** en `androidTest`; SAF y Media3 se sustituyen por *fakes* de sus *data sources*.
* **Sin lógica en los dobles:** un *fake* expone estado inspeccionable (p. ej. `deletedUris`, `savedState`) pero no reglas de negocio; la regla vive en la unidad bajo prueba, nunca duplicada en el doble.

## 6. Configuración de Herramientas

*Materializa como configuración ejecutable las reglas de las secciones anteriores. Estas herramientas son la única fuente de verdad del estilo: lo que no aprueban, rompe el build.*

* **Formatter / Linter de estilo — `ktlint`** (estilo oficial Kotlin): impone indentación, longitud de línea, import único ordenado sin comodines y *trailing commas*.
* **Análisis estático — `detekt`**: detecta olores de código, complejidad e imports sin uso; hospeda la regla de exhaustividad y las restricciones de manejo de errores.
* **Reglas de arquitectura — `Konsist`**: prueba (como test JVM) la dirección de dependencias y los sufijos por capa.
* **Cobertura — `Kover`**: reporte de cobertura nativo de Kotlin, sin dependencias externas.
* **Configuración base compartida — `.editorconfig`** (raíz del repositorio):

```ini
# .editorconfig — fuente única de formato (leída por ktlint y Android Studio)
root = true

[*]
charset = utf-8
end_of_line = lf
insert_final_newline = true
trim_trailing_whitespace = true

[*.{kt,kts}]
indent_style = space
indent_size = 4
max_line_length = 120
ktlint_code_style = ktlint_official
ij_kotlin_allow_trailing_comma = true
ij_kotlin_allow_trailing_comma_on_call_site = true
ij_kotlin_name_count_to_use_star_import = 2147483647          # desactiva imports con comodín
ij_kotlin_name_count_to_use_star_import_for_members = 2147483647
```

* **Fin de línea normalizado en Git — `.gitattributes`** (evita conflictos CRLF/LF en Windows/Unix):

```gitattributes
* text=auto eol=lf
*.kt   text eol=lf
*.kts  text eol=lf
```

* **Regla de análisis estático — `config/detekt/detekt.yml`** (extracto):

```yaml
style:
  ForbiddenComment:
    active: true
  UnusedImports:
    active: true
  WildcardImport:
    active: true
  MaxLineLength:
    maxLineLength: 120
naming:
  ClassNaming:
    classPattern: '[A-Z][A-Za-z0-9]*'      # PascalCase
  FunctionNaming:
    functionPattern: '[a-z][A-Za-z0-9]*'   # camelCase (los tests usan backticks)
exceptions:
  TooGenericExceptionCaught:
    active: true                            # prohíbe catch (e: Exception) genérico
  SwallowedException:
    active: true                            # prohíbe silenciar el fallo
```

* **Verificación de aislamiento de red:** el *build* falla si el manifiesto final (mergeado con dependencias) declara `android.permission.INTERNET`.

* **Tareas de Gradle** (invocadas en local y como *quality gate* obligatorio en CI antes de *merge*):

```bash
./gradlew ktlintCheck        # verifica formato
./gradlew ktlintFormat       # auto-corrige el formato
./gradlew detekt             # análisis estático
./gradlew konsistTest        # reglas de arquitectura: dependencias y sufijos
./gradlew testDebugUnitTest  # pruebas unitarias JVM
./gradlew koverXmlReport     # reporte de cobertura
./gradlew check              # meta-tarea: agrega todo lo anterior — es el quality gate de CI
```
