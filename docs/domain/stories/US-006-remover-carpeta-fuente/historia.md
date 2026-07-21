# Historia de Usuario

**Como** El Oyente (agente humano soberano y único usuario de Sonus),
**Quiero** remover una Carpeta Fuente de mi biblioteca ya en operación,
**Para** reducir el perímetro de descubrimiento y expulsar del Catálogo todo el contenido de audio indexado bajo esa carpeta, manteniendo la fidelidad entre el Catálogo y el estado actual de las carpetas autorizadas.

## Descripción

Esta historia es la hermana inversa de `US-005` (agregar carpeta) dentro de **EPIC-02 (Orden de la Biblioteca)** y sostiene el **Equilibrio de Organización** y la **Invariante 2 (Fidelidad al Sistema de Archivos)**. Materializa el lado destructivo del **Apalancamiento 2** (SDD §4.1): así como agregar una carpeta expande el universo de la biblioteca, removerla lo contrae de forma determinista y completa.

Al remover una Carpeta Fuente, el sistema ejecuta una **purga en cascada** completa: (1) elimina el `SourceFolder`, (2) en cascada por esquema (`onDelete = CASCADE`, modelo §3) elimina todos los `Track` descubiertos bajo esa carpeta, (3) en cascada eliminan sus `PlaylistTrackCrossRef`, `QueueItem` y `PlaybackProgress`, y (4) dispara la purga de dimensiones huérfanas (`Artist`/`Album`/`Genre` no referenciados por ningún `Track`, salvo centinela `id=1`). Finalmente, libera el permiso SAF de árbol (`releasePersistableUriPermission`) para no acumular permisos innecesarios en el SO.

Dado que la remoción de una carpeta con tracks indexados es **destructiva e irreversible**, la **Invariante 5 (Irreversibilidad Consciente)** exige confirmación explícita del Oyente antes de ejecutar la operación. El sistema comunica de forma transparente las consecuencias (cantidad de tracks y referencias en playlists afectadas) para que el Oyente decida con información completa.

Si hay pistas de la carpeta removida en la cola de reproducción activa, el CASCADE las elimina de `QueueItem`; el Motor de Reproducción adapta su comportamiento conforme a [RF-09] e **Invariante 6**.

**Decisiones de negocio de esta historia:**

- **Confirmación explícita obligatoria (Invariante 5).** El diálogo de confirmación es prerrequisito del contrato (`TRG-LIB-02`); invocar la operación sin `confirmed=true` produce `ERR_CONFIRMATION_REQUIRED`. La confirmación nunca se omite ni se autoejercita.
- **Comunicación del impacto antes de confirmar.** El sistema muestra cuántos tracks y cuántas referencias en playlists se eliminarán. El Oyente decide con datos reales (soberanía, Invariante 3).
- **Purga en cascada automática (Invariante 2).** Una vez confirmado, la eliminación del `SourceFolder` propaga por el esquema SQL (`CASCADE`) de forma transaccional: tracks, playlist-track cross refs, queue items y playback progress se eliminan en una sola operación atómica.
- **Purga de dimensiones huérfanas post-cascada.** Tras eliminar los tracks, los `Artist`/`Album`/`Genre` que queden sin referencias se purgan (excepto centinelas `id=1`), manteniendo la huella mínima ([RNF-08], modelo §6.2).
- **Liberación del permiso SAF.** `releasePersistableUriPermission` sobre el `treeUri` de la carpeta removida, para no acumular permisos SAF innecesarios en el SO.
- **La última Carpeta Fuente puede ser removida.** El Oyente es soberano (Invariante 3); la biblioteca puede quedar vacía. El diálogo lo comunica claramente.
- **No re-escaneo automático.** Remover una carpeta no dispara escaneo; el Catálogo simplemente se contrae.
- **Frontera de alcance.** Esta historia solo remueve la `SourceFolder` y ejecuta la purga en cascada. No ejecuta escaneo (`US-007`), no sincroniza el Catálogo por cambios externos de archivos (`US-008`), no es la selección inicial del onboarding (`US-002`), no agrega carpetas (`US-005`), y no observa el progreso del escaneo (`US-009`).

---

## Criterios de Aceptación

### Escenario 1: Flujo Principal — remover carpeta con confirmación explícita

- **Dado** que el Oyente se encuentra en la sección "Carpetas Fuente" de Configuración con al menos dos Carpetas Fuente registradas
- **Cuando** pulsa la acción "Quitar" en una carpeta, el sistema presenta el diálogo de confirmación con el impacto (nº de tracks y referencias en playlists afectadas), y el Oyente confirma explícitamente
- **Entonces** el sistema libera el permiso SAF (`releasePersistableUriPermission`), elimina el `SourceFolder` y todos sus tracks en cascada (incluyendo sus `PlaylistTrackCrossRef`, `QueueItem` y `PlaybackProgress`), purga las dimensiones huérfanas, y la carpeta desaparece de la lista (`TRG-LIB-02`, [RF-01])

### Escenario 2: El Catálogo se contrae — tracks de la carpeta eliminados

- **Dado** que la carpeta a remover tiene N tracks indexados en el Catálogo
- **Cuando** el Oyente confirma la remoción
- **Entonces** todos los N `Track` descubiertos bajo esa `SourceFolder` desaparecen del Catálogo; los tracks de otras Carpetas Fuente permanecen intactos (Invariante 2 / [RF-03])

### Escenario 3: Purga en cascada de playlists

- **Dado** que algunos tracks de la carpeta a remover están referenciados en una o más playlists
- **Cuando** el Oyente confirma la remoción
- **Entonces** todas las referencias a esos tracks en playlists (`PlaylistTrackCrossRef`) se eliminan automáticamente; las playlists que tenían referencias solo a esa carpeta quedan vacías pero **no se eliminan**; las playlists con pistas de otras carpetas conservan esas pistas con posiciones recompactadas ([RF-03], Invariante 2, modelo §3)

### Escenario 4: Purga de dimensiones huérfanas

- **Dado** que la carpeta removida era la única fuente de tracks asociados a ciertos `Artist`, `Album` o `Genre`
- **Cuando** se completa la purga en cascada
- **Entonces** los registros `Artist`, `Album` y `Genre` que queden sin referencias en ningún `Track` se eliminan, preservando únicamente los centinelas `id=1` y las dimensiones aún referenciadas por tracks de otras carpetas ([RNF-08], modelo §6.2)

### Escenario 5: Liberación del permiso SAF

- **Dado** que la Carpeta Fuente tenía un permiso de árbol persistido (`takePersistableUriPermission`)
- **Cuando** se elimina la carpeta
- **Entonces** el sistema invoca `releasePersistableUriPermission` sobre el `treeUri` de esa carpeta; el permiso ya no debe aparecer en los permisos SAF persistidos del SO

### Escenario 6: Cancelación del diálogo de confirmación

- **Dado** que el Oyente pulsó "Quitar" en una carpeta y el diálogo de confirmación está visible
- **Cuando** cancela el diálogo sin confirmar
- **Entonces** no se realiza ninguna eliminación, la carpeta y todos sus tracks permanecen intactos, y el sistema regresa a la lista sin cambios (soberanía, Invariante 3; `ERR_CONFIRMATION_REQUIRED` no se emite — la cancelación es una acción válida sin error)

### Escenario 7: Remoción con tracks en cola de reproducción activa

- **Dado** que hay pistas de la carpeta a remover en la cola de reproducción activa, incluyendo posiblemente la pista en reproducción
- **Cuando** el Oyente confirma la remoción
- **Entonces** los `QueueItem` correspondientes a los tracks eliminados desaparecen de la cola (CASCADE); si la pista actual era una de las eliminadas, el Motor de Reproducción avanza a la siguiente pista válida disponible o se detiene si la cola queda vacía, conforme a [RF-09] e Invariante 6; la sesión no colapsa

### Escenario 8: Remoción de la última Carpeta Fuente

- **Dado** que el Oyente tiene exactamente una Carpeta Fuente registrada
- **Cuando** intenta removerla y el diálogo comunica que esto dejará la biblioteca completamente vacía, y el Oyente confirma
- **Entonces** la operación se ejecuta dejando el Catálogo vacío y sin Carpetas Fuente; la interfaz refleja el estado de biblioteca vacía sin error (soberanía, Invariante 3)

### Escenario 9: Carpeta no encontrada (`ERR_ENTITY_NOT_FOUND`)

- **Dado** que el `folderId` solicitado ya no existe (inconsistencia temporal de estado entre vistas)
- **Cuando** se intenta removerla
- **Entonces** el sistema emite `ERR_ENTITY_NOT_FOUND` (ERROR) y la vista se refresca desde el flujo observable C2 para reflejar el estado actual real

### Escenario 10: Autarquía verificable (sin red)

- **Dado** que el sistema ejecuta la remoción de la carpeta y su purga en cascada
- **Cuando** se inspeccionan los mecanismos empleados
- **Entonces** toda la operación se ejecuta exclusivamente sobre persistencia local (Room/SQLite) y SAF; en ningún momento se solicita ni emplea `android.permission.INTERNET` ni comunicación de red (Invariante 1 / [RNF-06] / [CT-01])

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Oyente — agente humano soberano y único usuario del sistema (Fase 1). Sin cuentas, roles secundarios ni jerarquías (Invariante 3).
- **Permisos requeridos:** No se requieren permisos adicionales para remover; el permiso SAF existente (`treeUri` con `takePersistableUriPermission`) se **libera** (`releasePersistableUriPermission`) tras la eliminación. No se solicita `INTERNET` ni se usa `MediaStore`.
- **Valor de negocio:** Permite al Oyente reducir el perímetro de la biblioteca, eliminar fuentes de contenido no deseado y mantener la correspondencia fiel entre carpetas autorizadas y Catálogo. Materializa el lado destructivo del **Apalancamiento 2** (SDD §4.1) y sostiene el **Equilibrio de Organización** (Invariante 2).

### Reglas de Negocio

- **Confirmación explícita obligatoria (Invariante 5):** toda remoción debe estar precedida por un diálogo de confirmación que muestre el impacto; `confirmed=false` en el caso de uso produce `ERR_CONFIRMATION_REQUIRED`.
- **Comunicación del impacto:** el sistema presenta, previo a la confirmación, el número de tracks y referencias en playlists que se eliminarán, para que el Oyente decida con información completa.
- **Purga en cascada por esquema (`onDelete = CASCADE`, modelo §3):** la eliminación del `SourceFolder` propaga transaccionalmente a `Track`, `PlaylistTrackCrossRef`, `QueueItem` y `PlaybackProgress`.
- **Purga de dimensiones huérfanas:** tras la cascada, se eliminan `Artist`/`Album`/`Genre` sin referencias en ningún `Track`, preservando centinelas `id=1` ([RNF-08]).
- **Liberación del permiso SAF (`releasePersistableUriPermission`):** tras eliminar el `SourceFolder`, se libera el permiso de árbol para no acumular permisos innecesarios en el SO.
- **Playlists vacías se conservan:** las playlists que queden sin tracks tras la purga no se eliminan automáticamente (el Oyente decide qué hacer con ellas); solo sus referencias a tracks eliminados desaparecen.
- **Cola de reproducción adaptada automáticamente:** el CASCADE sobre `QueueItem` garantiza que los tracks removidos desaparezcan de la cola activa; el Motor de Reproducción maneja la continuidad conforme a [RF-09].
- **La última carpeta puede removerse (soberanía, Invariante 3):** el sistema no bloquea la operación; comunica las consecuencias y respeta la decisión del Oyente.
- **No re-escaneo automático:** remover una carpeta contrae el Catálogo de forma inmediata sin disparar re-escaneo.
- **Autarquía (Invariante 1):** jamás se solicita `INTERNET` ni se realiza comunicación de red durante la remoción.
- **Frontera de alcance:** esta historia remueve la `SourceFolder` y ejecuta la purga en cascada; no ejecuta escaneo (`US-007`), no sincroniza el Catálogo por cambios externos (`US-008`), no es la selección inicial del onboarding (`US-002`), no agrega carpetas (`US-005`), y no observa el progreso del escaneo (`US-009`).

### Interfaz

Acción disponible en **operación normal** (post-onboarding), fuera del `NavHost` de onboarding. La acción "Quitar" se expone en la pantalla de **Configuración** (destino `settings_source_folders` del `NavHost` Single-Activity, contenedor C-01), sección "Carpetas Fuente", sobre cada ítem de la lista de carpetas registradas. La operación la ejecuta el Motor de Biblioteca (C-04) sobre la persistencia local (Room).

#### Detalle de Interfaz de Usuario

- **Diseño general:** sección "Carpetas Fuente" que lista las carpetas ya registradas (cada una con su `displayPath` legible y una acción "Quitar" — ícono de eliminación o botón). Al pulsar "Quitar", aparece un **diálogo de confirmación** que comunica el impacto (nº de tracks y referencias en playlists afectadas) y ofrece "Confirmar" / "Cancelar".
- **Campos y controles:** lista de carpetas con acción "Quitar" por ítem; diálogo de confirmación con mensaje de impacto, botón "Confirmar" (destructivo) y botón "Cancelar"; aviso de éxito tras la eliminación.
- **Flujo de navegación visual:** Configuración → sección "Carpetas Fuente" → [Quitar] en una carpeta → diálogo de confirmación con impacto → [Confirmar] → carpeta eliminada de la lista + snackbar de confirmación. Si [Cancelar] → regreso a la lista sin cambios.
- **Mensajes y feedback:** snackbar de confirmación al remover exitosamente; descripción del impacto en el diálogo (p. ej. "Se eliminarán X pistas y sus referencias en Y playlists"); aviso si la carpeta era la última ("Tu biblioteca quedará vacía"); aviso de error no intrusivo ante `ERR_ENTITY_NOT_FOUND`. Todos los textos se resuelven y localizan en la capa de presentación (i18n desacoplada de los datos).

### Sistemas Externos

- **Sistema Operativo Android — Storage Access Framework (canal C5):** `releasePersistableUriPermission` sobre el `treeUri` de la carpeta removida. Único mecanismo de gestión de permisos de acceso a los archivos, por carpeta (contrato §1.2 / [Restricción 6]). Sin red (air-gapped, [RNF-06]).
- **Room (SQLite) — purga en cascada:** el `onDelete = CASCADE` declarado en la FK `Track.sourceFolderId → SourceFolder.id` (modelo §3) garantiza la propagación transaccional a `PlaylistTrackCrossRef`, `QueueItem` y `PlaybackProgress`.

### Preview de Interfaz

**Preview:** [US-006.preview.md](./US-006.preview.md) | **Formato:** mermaid (flujo de navegación)

---

## Contexto y Referencias

**Arquitectura:**
- `docs/architecture/architecture_blueprint.md` — §2.1 contenedor C-04 (Motor de Biblioteca) y C-01 (Configuración), §4.1 [RF-01] (`RemoveSourceFolderUseCase` → `SourceFolderRepository` → `SafDataSource`).
- `docs/architecture/interfaces_contract.md` — `TRG-LIB-02` (Remover Carpeta Fuente: `LibraryCommand.RemoveSourceFolder`, `OperationResult<Unit>`, `releasePersistableUriPermission`), §3.2 (`ERR_ENTITY_NOT_FOUND`, `ERR_CONFIRMATION_REQUIRED`).
- `docs/architecture/domain_and_state_model.md` — §3 (relación `SourceFolder 1:N Track`, `onDelete = CASCADE`), §6.2 (purga de dimensiones huérfanas: Artist/Album/Genre huérfanos tras escaneo / modificación / eliminación de SourceFolder).
- `docs/domain/definition/system_definition_document.md` — Invariantes 2, 3 y 5; §4.1 (Apalancamiento 2); Perturbación 5 (cambio en carpetas fuente).
- `docs/domain/definition/requirements_specification.md` — [RF-01], [RF-03], [RNF-06], [RNF-08], [Restricción 6].

**Historias relacionadas:** `US-005` (agregar Carpeta Fuente, hermana directa inversa), `US-002` (selección inicial de Carpetas Fuente en onboarding), `US-007` (ejecutar escaneo — no relacionado funcionalmente aquí, pero referencia por frontera), `US-008` (sincronización determinista del Catálogo), `US-009` (observar progreso del escaneo).

**Lecciones aprendidas:** N/A.

---

## Definición de Terminado (Inicial)

- [ ] Funcionalidad implementada según criterios de aceptación (remover carpeta con diálogo de confirmación, purga en cascada, liberación del permiso SAF, purga de dimensiones huérfanas)
- [ ] Diálogo de confirmación implementado con comunicación del impacto (nº de tracks y referencias en playlists)
- [ ] Purga en cascada verificada (tracks, PlaylistTrackCrossRef, QueueItem, PlaybackProgress)
- [ ] Purga de dimensiones huérfanas verificada (Artist/Album/Genre sin referencias, preservando centinelas id=1)
- [ ] Liberación del permiso SAF (`releasePersistableUriPermission`) verificada
- [ ] Comportamiento del Motor de Reproducción ante tracks eliminados en cola activa verificado ([RF-09], Invariante 6)
- [ ] Remoción de la última carpeta (biblioteca vacía) funciona correctamente
- [ ] Cancelación del diálogo no produce efectos secundarios
- [ ] Mensajes implementados (confirmación exitosa, impacto, última carpeta, error EntityNotFound) y localizados en la capa de presentación
- [ ] Autarquía verificada (sin red, sin `MediaStore`, sin permisos de media runtime, sin `INTERNET`)
