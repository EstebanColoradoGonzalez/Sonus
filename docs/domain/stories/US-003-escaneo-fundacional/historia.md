# Historia de Usuario

**Como** El Oyente (agente humano soberano y único usuario de Sonus),
**Quiero** iniciar el escaneo fundacional de las Carpetas Fuente seleccionadas y observar su progreso hasta que el Catálogo de Biblioteca quede construido,
**Para** ver mi colección de audio organizada por primera vez y poder comenzar a explorarla y reproducirla sin pasos adicionales.

## Descripción

Esta historia es el tercer paso del flujo de primera ejecución (Fundación Soberana / EPIC-01), que ocurre inmediatamente después de que el Oyente confirma la selección de Carpetas Fuente (`US-002`) y antes de que el onboarding se marque como completado (`US-004`).

En este momento el Motor de Biblioteca ejecuta el **escaneo fundacional**: recorre recursivamente cada Carpeta Fuente registrada vía SAF (`TRG-LIB-03`), extrae las etiquetas ID3 de cada archivo de audio descubierto sin inventar datos faltantes (Invariante 4), y construye el Catálogo de Biblioteca en Room. Los campos de metadatos ausentes reciben el centinela `id=1` (dimensiones normalizadas: Artist, Album, Genre) o se dejan como `NULL` (campos textuales directos como `title`); los archivos en formato no soportado se indexan como `UNSUPPORTED`; ningún archivo se omite silenciosamente.

El escaneo corre íntegramente en segundo plano (`CoroutineWorker` de WorkManager en `Dispatchers.IO` — ADR-006), sin bloquear el hilo principal de la interfaz ([RNF-03]). Si la operación supera 1 segundo — esperable en colecciones medianas o grandes — la interfaz emite el progreso de forma determinista (`ScanState.Scanning(processed, total)`) para que el Oyente perciba avance y no interprete el retraso como un fallo. Mientras el total de archivos no sea conocido (fase de enumeración), `total` puede ser `NULL`.

Al completarse (`ScanState.Finished`), el sistema muestra brevemente el `ScanSummary` (pistas añadidas, no soportadas, huérfanos purgados) y **transita automáticamente** a la vista principal de la biblioteca, donde el Oyente ve su colección organizada por primera vez, lista para explorar y reproducir. Este paso constituye el **Ciclo de Escaneo** (SDD §1.3): el Catálogo refleja fielmente el estado actual del sistema de archivos, sin entradas huérfanas ni archivos sin procesar.

Si durante el escaneo se pierde el permiso de alguna Carpeta Fuente o esta queda inaccesible, el sistema emite `ScanState.Aborted(ERR_SCAN_ABORTED)`, conserva el Catálogo en el último estado coherente y permite al Oyente volver a revisar las Carpetas Fuente.

Esta historia **no** gestiona el re-escaneo manual posterior (`US-007`), la sincronización incremental del Catálogo (`US-008`), el progreso observable desde la biblioteca (`US-009`) ni el marcado del onboarding como completado (`US-004`). Su alcance es exclusivamente el **escaneo de primera ejecución** y la transición a la biblioteca.

---

## Criterios de Aceptación

### Escenario 1: Flujo Principal — escaneo exitoso con progreso

- **Dado** que el Oyente completó la selección de ≥1 Carpeta Fuente en `US-002` y confirma continuar al escaneo
- **Cuando** el Motor de Biblioteca inicia el escaneo fundacional (`TRG-LIB-03`, `ScanMode.FULL`)
- **Entonces** la interfaz muestra una pantalla de progreso con indicador determinista (`ScanState.Scanning(processed, total)`); el escaneo corre íntegramente en segundo plano sin bloquear la UI ([RNF-03]); al finalizar se emite `ScanState.Finished` con el `ScanSummary` y la navegación transita automáticamente a la vista principal de la biblioteca

### Escenario 2: Escaneo rápido (biblioteca pequeña, < 1 s)

- **Dado** que las Carpetas Fuente contienen muy pocos archivos y el escaneo finaliza en menos de 1 segundo
- **Cuando** el Motor de Biblioteca termina
- **Entonces** el sistema puede transitar directamente a la biblioteca sin emitir una pantalla de progreso intermedia perceptible; la transición es fluida e imperceptible para el Oyente

### Escenario 3: Validaciones — archivos con metadatos ausentes o incompletos

- **Dado** que alguno de los archivos de audio descubiertos carece de una o varias etiquetas ID3 (título, artista, álbum, género)
- **Cuando** el Motor de Biblioteca lo indexa
- **Entonces** el campo ausente se asigna al centinela `id=1` (dimensiones normalizadas: Artist, Album, Genre) o se deja como `NULL` (campo `title`); **nunca** se infiere ni inventa dato alguno (Invariante 4); el archivo aparece en la biblioteca bajo etiquetas localizadas "Sin artista", "Sin álbum", "Sin género" (resueltas en la capa de presentación, no persistidas como literales)

### Escenario 4: Casos Extremos — archivos en formato no soportado

- **Dado** que alguno de los archivos descubiertos está en un formato que el Motor de Biblioteca no puede decodificar
- **Cuando** lo indexa
- **Entonces** se registra con `Track.availability = UNSUPPORTED` ([Restricción 2]); queda visible en la biblioteca pero no reproducible; el campo `ScanSummary.unsupported` refleja el conteo; el escaneo continúa con el resto de archivos sin interrupciones

### Escenario 5: Error de escaneo — carpeta inaccesible o permiso revocado

- **Dado** que durante el escaneo fundacional una Carpeta Fuente pierde su permiso SAF o queda inaccesible
- **Cuando** el Motor de Biblioteca detecta el error
- **Entonces** emite `ScanState.Aborted(DomainError.ScanAborted(reason))` con la causa subyacente (`ERR_PERMISSION_REVOKED` u otra); conserva el Catálogo en el último estado coherente construido hasta el momento; la interfaz comunica el error de forma no intrusiva y ofrece la opción de volver a la pantalla de Carpetas Fuente para revisar el acceso; **nunca** borra entradas ya indexadas por un error de acceso transitorio (P3 / SDD §4.2, Perturbación 5)

### Escenario 6: Cancelación del escaneo por el Oyente

- **Dado** que el Oyente decide interrumpir el escaneo en curso (p. ej. pulsando "Cancelar" en la pantalla de progreso)
- **Cuando** se dispara la cancelación
- **Entonces** el Motor de Biblioteca detiene el `CoroutineWorker` de forma limpia; el Catálogo queda en el estado construido hasta ese punto (sin purga de lo ya indexado); el Oyente puede reintentar el escaneo o volver a la pantalla de Carpetas Fuente

### Escenario 7: Autarquía — sin red ni permisos de media runtime

- **Dado** que el escaneo se ejecuta sobre los archivos de las Carpetas Fuente
- **Cuando** se inspeccionan los mecanismos de acceso y los permisos en runtime
- **Entonces** el único mecanismo de acceso es SAF con permisos de árbol persistidos por carpeta; en ningún momento se solicita `READ_MEDIA_AUDIO`, `READ_EXTERNAL_STORAGE` ni `android.permission.INTERNET`; no se accede a ningún servicio externo de metadatos ni de carátulas (Invariante 1 / [RNF-06] / ADR-003 / ADR-010)

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Oyente — agente humano soberano y único usuario del sistema (Fase 1). El Motor de Biblioteca es el agente lógico ejecutor del escaneo.
- **Permisos requeridos:** Permisos de árbol SAF persistidos por carpeta (`treeUri` de cada `SourceFolder`, concedidos en `US-002`). **No** se solicitan permisos de media *runtime* (`READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE`) ni `INTERNET`.
- **Valor de negocio:** Materializa el **Ciclo de Escaneo** (SDD §1.3) y el **Equilibrio de Organización** (SDD §1.2): el Catálogo de Biblioteca nace ordenado. Es el paso que transforma el estado de entropía inicial (archivos dispersos) en el estado final (biblioteca indexada, navegable y lista para reproducción), habilitando todos los flujos subsiguientes del sistema. Sin este paso no hay biblioteca; el sistema no puede cumplir su misión (SDD §1.2).

### Reglas de Negocio

- **Escaneo FULL fundacional:** el escaneo de primera ejecución es siempre `ScanMode.FULL`; no existe estado previo de Catálogo contra el que comparar.
- **No invención de datos (Invariante 4):** el Motor de Biblioteca nunca infiere ni autocompleta metadatos ausentes. La ausencia se representa con centinelas (`id=1` / `NULL`); la etiqueta localizada solo existe en presentación.
- **Fidelidad al sistema de archivos (Invariante 2):** el Catálogo refleja fielmente los archivos existentes en las Carpetas Fuente. Al finalizar el escaneo, no quedan entradas huérfanas ni archivos sin procesar (estado de cierre del Ciclo de Escaneo, SDD §1.3).
- **Progreso determinista ([RNF-03]):** si el escaneo supera 1 segundo, la UI **debe** emitir progreso (`ScanState.Scanning(processed, total?)`); `total` puede ser `NULL` durante la fase de enumeración recursiva.
- **Asincronía obligatoria ([RNF-03]):** el escaneo corre en `Dispatchers.IO` vía `CoroutineWorker` (WorkManager — ADR-006); jamás bloquea el hilo principal de la UI.
- **Degradación grácil ante fallos ([CT-10] / P3):** un permiso revocado o una carpeta inaccesible emite `ERR_SCAN_ABORTED`, conserva el Catálogo parcialmente construido y no borra entradas por error de acceso transitorio.
- **Archivos UNSUPPORTED visibles:** los archivos en formato no soportado se indexan (`Track.availability = UNSUPPORTED`) y son visibles en la biblioteca; no se omiten ni se lanzan errores de aplicación por ellos.
- **Huella de carátulas cero ([RNF-08] / [F-5]):** las carátulas **no** se persisten durante el escaneo; `Track.hasEmbeddedArtwork` se fija a `true/false` según la presencia de bytes embebidos; la imagen se lee *on-demand* desde el archivo, nunca se extrae a disco.
- **Single-flight ([CT-01] interno / contrato §4.1):** WorkManager garantiza una única instancia activa del escaneo a la vez; re-solicitudes concurrentes se fusionan en el ciclo en curso.
- **Frontera de alcance:** esta historia inicia el escaneo y gestiona la transición a la biblioteca; **no** marca el onboarding como completado (`US-004`), no gestiona re-escaneos manuales posteriores (`US-007`) ni la sincronización incremental (`US-008`).

### Interfaz

Tercera pantalla (o transición) del flujo de primera ejecución, presentada inmediatamente después de confirmar la selección de Carpetas Fuente (`US-002`). Pertenece al destino "onboarding" del `NavHost` Single-Activity + Navigation Compose (contenedor C-01).

#### Detalle de Interfaz de Usuario

- **Diseño general:** pantalla de progreso de escaneo a pantalla completa: título ("Organizando tu biblioteca" o similar), indicador de progreso determinista (barra o circular con contador `processed / total`), subtítulo con el nombre del archivo o carpeta actual en proceso, y una acción "Cancelar" accesible. Al finalizar, muestra brevemente el resumen (`ScanSummary`) antes de transitar a la biblioteca.
- **Campos y controles:** indicador de progreso (barra o anillo con `processed/total`; indeterminado mientras `total = NULL`); texto de estado del archivo en proceso; botón "Cancelar" (detiene el escaneo de forma limpia). En el estado `Finished`: resumen compacto con conteos (pistas encontradas, no soportadas) y transición automática a la biblioteca principal.
- **Flujo de navegación visual:** onboarding → [permisos `US-001`] → [selección Carpetas Fuente `US-002`] → **[pantalla de escaneo fundacional con progreso]** → **[biblioteca principal]**. En error (`Aborted`): [pantalla de escaneo] → aviso con opción de volver a Carpetas Fuente.
- **Mensajes y feedback:** título de progreso; subtítulo con archivo en curso (si disponible); en `Finished`: texto de éxito con conteos localizados; en `Aborted`: mensaje de error no intrusivo con causa y opción de reintentar o volver; en cancelación: mensaje confirmando que el Catálogo parcial se conserva. Todos los textos se resuelven y localizan en la capa de presentación (i18n desacoplada de los datos).

### Sistemas Externos

- **Sistema Operativo Android — Storage Access Framework (canal C5):** `DocumentFile` sobre los `treeUri` persistidos de cada `SourceFolder`; recorrido recursivo para descubrimiento de archivos de audio. Es el único canal de acceso al almacenamiento; sin `MediaStore`, sin permisos de media *runtime*, sin red.
- **Motor de Biblioteca — WorkManager (C-04):** `LibraryScanWorker` (`CoroutineWorker`) con `ExistingWorkPolicy.KEEP` (single-flight); `CatalogSynchronizer` ejecuta el diff y las altas; `ScanStateEmitter` publica el `ScanState` por `Flow` (C2).
- **Room/SQLite (C-03):** destino de escritura del Catálogo: tablas `Track`, `Artist`, `Album`, `Genre` (con centinelas `id=1` ya sembrados en el Big Bang — §6.1). El escaneo escribe en transacciones atómicas; purga de dimensiones huérfanas al finalizar ([RNF-08]).

### Preview de Interfaz

**Preview:** [US-003.preview.md](./US-003.preview.md) | **Formato:** mermaid (flujo de navegación)

---

## Contexto y Referencias

**Arquitectura:**
- `docs/architecture/architecture_blueprint.md` — §2.1 contenedor C-04 (Motor de Biblioteca, `LibraryScanWorker`, `CatalogSynchronizer`, `ScanStateEmitter`), ADR-003 (SAF), ADR-006 (WorkManager), ADR-010 (air-gapped verificable), §4.1 [RF-02]/[RF-03] (`ScanLibraryUseCase`), §4.2 [RNF-03].
- `docs/architecture/interfaces_contract.md` — `TRG-LIB-03` (Ejecutar Escaneo, `ScanMode`, `ScanSummary`), `TRG-LIB-04` (`ScanState`, progreso), §3.2 (`ERR_SCAN_ABORTED`, `ERR_PERMISSION_REVOKED`), §4.1 (single-flight, policy `KEEP`), §4.2 (presupuesto escaneo sin límite superior, progreso si > 1s).
- `docs/architecture/domain_and_state_model.md` — §2 (`Track`, `SourceFolder`, `Artist`, `Album`, `Genre`; centinelas `id=1`), §4 (`ContentType`, `TrackAvailability`), §5.3 (Ciclo de Vida del Motor de Biblioteca: `IDLE→SCANNING→SYNCING→IDLE`), §6.1 (Big Bang: tablas vacías + centinelas sembrados), §6.2 (política de purga de dimensiones huérfanas).
- `docs/domain/definition/system_definition_document.md` — §1.3 (Ciclo de Escaneo y estado de cierre), §4.1 (secuencia de arranque pasos 3–4, Apalancamiento 1 y 5, Retraso de escaneo inicial), §4.2 (Perturbaciones 1, 2 y 5 — absorción de anomalías), Invariantes 2 y 4.
- `docs/domain/definition/requirements_specification.md` — [RF-02], [RF-03], [RNF-03], [RNF-08], [RNF-06], [Restricción 2], [Restricción 3], [CT-08] (interfaz siempre responsiva), [CT-09] (huella mínima).

**Historias relacionadas:** `US-002` (selección de Carpetas Fuente, precede), `US-004` (marcar onboarding completado, sigue), `US-007` (re-escaneo manual posterior), `US-008` (sincronización incremental del Catálogo), `US-009` (observar progreso de escaneo desde la biblioteca).

**Lecciones aprendidas:** N/A.

---

## Definición de Terminado (Inicial)

- [ ] Funcionalidad implementada según criterios de aceptación (escaneo fundacional FULL, extracción ID3, construcción del Catálogo, transición a biblioteca)
- [ ] Validaciones funcionando correctamente (metadatos ausentes → centinela/NULL; UNSUPPORTED; error/aborto → catálogo coherente; cancelación limpia)
- [ ] Progreso determinista implementado (ScanState.Scanning con processed/total; umbral 1 s; asincronía garantizada — sin bloqueo de UI)
- [ ] Mensajes implementados (progreso, resumen al finalizar, aviso de aborto, cancelación) y localizados en la capa de presentación
- [ ] Autarquía verificada (solo SAF por carpeta; sin MediaStore, sin permisos de media runtime, sin INTERNET; carátulas no persistidas)
