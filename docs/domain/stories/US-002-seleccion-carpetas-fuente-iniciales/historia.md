# Historia de Usuario

**Como** El Oyente (agente humano soberano y único usuario de Sonus),
**Quiero** seleccionar una o varias carpetas de mi dispositivo como Carpetas Fuente durante la primera ejecución,
**Para** delimitar con precisión el universo de archivos de audio que Sonus descubrirá, incluyendo solo mi contenido relevante (p. ej. Música, Podcasts) y excluyendo audios no deseados (notas de voz, audios de mensajería, sonidos del sistema).

## Descripción

Esta historia es el segundo paso del flujo de primera ejecución (Fundación Soberana / EPIC-01) y ocurre después de conceder los permisos (`US-001`) y antes del escaneo fundacional (`US-003`). Materializa el **Apalancamiento 2** (SDD §4.1): la selección de Carpetas Fuente es el acto de configuración que, en segundos, **define la composición completa de la biblioteca**; una carpeta mal elegida contamina el Catálogo y una carpeta omitida deja contenido valioso fuera.

El Oyente agrega carpetas mediante el selector nativo del *Storage Access Framework* (SAF: `ACTION_OPEN_DOCUMENT_TREE`), único mecanismo de acceso al almacenamiento del sistema (ADR-003). Por cada carpeta elegida, Sonus **toma el permiso de árbol persistido** (`takePersistableUriPermission`) y registra un `SourceFolder` (`treeUri` único, `displayPath` legible, `dateAddedMs`) — contrato `TRG-LIB-01`. El `treeUri` persistido constituye la frontera de autorización efectiva por carpeta ([Restricción 6] / contrato §1.2); el sistema **no** solicita permisos de media *runtime* (`READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE`) ni de red (Invariante 1 / [RNF-06] / [CT-05]).

El Oyente puede acumular **múltiples** carpetas y revisarlas en una lista antes de continuar ([RF-01] "una o múltiples carpetas"). Para avanzar hacia el escaneo (`US-003`) se exige **al menos una** Carpeta Fuente registrada (Prerrequisito 4 del arranque, SDD §4.1: "sin esta configuración el sistema no sabe *dónde* buscar"): el control para continuar permanece deshabilitado mientras la lista esté vacía.

Esta historia **no** ejecuta el escaneo ni construye el Catálogo (eso es `US-003` / `TRG-LIB-03`), **no** gestiona las carpetas después del onboarding (agregar posterior es `US-005`; remover con purga en cascada es `US-006`) y **no** decide el tema visual ni marca el onboarding como completado (`US-004`). Su alcance es exclusivamente la **selección y registro** de las Carpetas Fuente iniciales.

---

## Criterios de Aceptación

### Escenario 1: Flujo Principal

- **Dado** que el Oyente se encuentra en la pantalla de selección de Carpetas Fuente del onboarding (tras `US-001`) y aún no ha registrado ninguna carpeta
- **Cuando** el Oyente pulsa "Agregar carpeta", el selector SAF del sistema (`ACTION_OPEN_DOCUMENT_TREE`) se abre, elige un directorio y el SO otorga el permiso persistible sobre su `treeUri`
- **Entonces** el sistema toma el permiso persistido (`takePersistableUriPermission`), registra un `SourceFolder` (`treeUri` único + `displayPath` legible + `dateAddedMs`) y la carpeta aparece en la lista de Carpetas Fuente seleccionadas, quedando habilitada la acción para continuar

### Escenario 2: Selección de múltiples carpetas

- **Dado** que el Oyente ya tiene al menos una Carpeta Fuente registrada en la lista
- **Cuando** repite la acción "Agregar carpeta" y elige otro directorio distinto (p. ej. primero "Música" y luego "Podcasts")
- **Entonces** el sistema registra cada carpeta adicional como un `SourceFolder` independiente y todas se muestran acumuladas en la lista, sin reemplazar a las anteriores ([RF-01])

### Escenario 3: Validaciones — obligatoriedad de al menos una carpeta

- **Dado** que el Oyente está en la pantalla de selección de Carpetas Fuente
- **Cuando** la lista de carpetas está vacía
- **Entonces** la acción para continuar hacia el escaneo (`US-003`) permanece **deshabilitada**; en el momento en que se registra la primera carpeta, la acción para continuar se habilita (Prerrequisito 4, SDD §4.1)

### Escenario 4: Casos Extremos — carpeta duplicada

- **Dado** que el Oyente ya registró una carpeta cuyo `treeUri` está en la lista
- **Cuando** vuelve a seleccionar exactamente la misma carpeta (mismo `treeUri`) en el selector SAF
- **Entonces** el sistema no crea un registro duplicado (unicidad de `SourceFolder.treeUri`), conserva la carpeta ya existente y comunica de forma no intrusiva que la carpeta ya estaba agregada (`ERR_DUPLICATE_SOURCE_FOLDER`, severidad WARNING)

### Escenario 5: Cancelación del selector / permiso no otorgado

- **Dado** que el Oyente abrió el selector SAF del sistema desde la pantalla de Carpetas Fuente
- **Cuando** cancela el selector sin elegir un directorio, o el SO no otorga el permiso persistible sobre el `treeUri` elegido
- **Entonces** el sistema regresa a la pantalla conservando las carpetas ya añadidas (sin registrar ninguna nueva) y muestra un aviso no intrusivo: en cancelación simple, que no se agregó ninguna carpeta; si el permiso fue denegado por el SO, que el acceso a la carpeta es necesario para descubrir el audio (`ERR_PERMISSION_DENIED`, severidad ERROR), sin reintentar el selector en bucle

### Escenario 6: Quitar una carpeta de la lista antes de continuar

- **Dado** que el Oyente agregó una carpeta por error y aún no ha avanzado al escaneo (no existe Catálogo todavía)
- **Cuando** la remueve de la lista de Carpetas Fuente seleccionadas
- **Entonces** el sistema elimina ese `SourceFolder`, libera su permiso SAF (`releasePersistableUriPermission`) y actualiza la lista; si la lista queda vacía, la acción para continuar vuelve a deshabilitarse (nota: esta remoción es *ligera*, sin purga en cascada porque aún no hay tracks; la remoción posterior con purga en cascada es `US-006`)

### Escenario 7: Confirmar la selección y transitar al escaneo

- **Dado** que el Oyente tiene al menos una Carpeta Fuente registrada
- **Cuando** pulsa la acción para continuar
- **Entonces** el sistema confirma que todos los `treeUri` conservan su permiso persistido y avanza al paso de escaneo fundacional (`US-003` / `TRG-LIB-03`), sin iniciar el escaneo dentro de esta historia

### Escenario 8: Autarquía verificable (solo SAF, sin media runtime ni red)

- **Dado** que el sistema accede al almacenamiento para registrar las Carpetas Fuente
- **Cuando** se inspeccionan los permisos y mecanismos de acceso empleados
- **Entonces** el único mecanismo de acceso es SAF con permiso de árbol persistido por carpeta; en ningún momento se solicita `READ_MEDIA_AUDIO`, `READ_EXTERNAL_STORAGE` ni `android.permission.INTERNET`, ni se emplea `MediaStore` (Invariante 1 / [RNF-06] / [CT-05] / ADR-003)

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Oyente — agente humano soberano y único usuario del sistema (Fase 1). Sin cuentas, roles secundarios ni jerarquías (Invariante 3).
- **Permisos requeridos:** Autorización de acceso **por carpeta** vía SAF (`takePersistableUriPermission` sobre el `treeUri`), que es la única frontera de autorización del acceso a archivos (contrato §1.2). **No** se solicitan permisos de media *runtime* (`READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE`), **ni** `INTERNET`, **ni** se usa `MediaStore`.
- **Valor de negocio:** Materializa el **Apalancamiento 2** (SDD §4.1): la selección de carpetas define el universo completo de la biblioteca. Es prerrequisito de arranque (Prerrequisito 4) e insumo directo del escaneo fundacional (`US-003`) y del Bucle de Crecimiento de Biblioteca.

### Reglas de Negocio

- **Obligatoriedad de ≥1 carpeta:** para avanzar al escaneo se requiere al menos una Carpeta Fuente registrada; la acción de continuar permanece deshabilitada con la lista vacía (SDD §4.1, Prerrequisito 4).
- **Cardinalidad múltiple:** el Oyente puede registrar una o varias carpetas en el onboarding ([RF-01]); se acumulan en una lista revisable.
- **Unicidad:** no se admiten Carpetas Fuente duplicadas — `SourceFolder.treeUri` es único; reintentar la misma carpeta produce `ERR_DUPLICATE_SOURCE_FOLDER` (WARNING) sin crear duplicado.
- **Acceso exclusivo por SAF (ADR-003 / [CT-05]):** el descubrimiento y acceso se realiza solo sobre las carpetas que el Oyente autoriza explícitamente; nunca se indexa todo el almacenamiento. La identidad de acceso es el `treeUri` persistido.
- **Cancelación soberana:** cancelar el selector es una acción válida; no bloquea la pantalla ni fuerza reintentos. La negativa de permiso del SO (`ERR_PERMISSION_DENIED`) se comunica y no se reintenta en bucle ([Restricción 6] / P3).
- **Remoción ligera pre-escaneo:** quitar una carpeta antes del escaneo solo elimina el `SourceFolder` y libera su permiso SAF (`releasePersistableUriPermission`), sin purga en cascada (aún no hay tracks). La remoción posterior con cascada es `US-006`.
- **Autarquía (Invariante 1):** jamás se solicita `INTERNET` ni se realiza comunicación de red durante la selección.
- **Frontera de alcance:** esta historia registra carpetas; **no** escanea ni construye el Catálogo (`US-003`), no gestiona carpetas post-onboarding (`US-005`/`US-006`), no marca el onboarding como completado (`US-004`).

### Interfaz

Segunda pantalla del flujo de primera ejecución (destino "onboarding" del `NavHost` Single-Activity + Navigation Compose, contenedor C-01), presentada después de la pantalla de permisos (`US-001`) y antes del escaneo fundacional (`US-003`).

#### Detalle de Interfaz de Usuario

- **Diseño general:** pantalla del onboarding con título, texto explicativo del beneficio (elegir dónde vive la música/podcasts y excluir lo no deseado), una **lista** de Carpetas Fuente seleccionadas (vacía al inicio con un *empty state* orientador), un botón para "Agregar carpeta" y una acción primaria para continuar al escaneo.
- **Campos y controles:** botón "Agregar carpeta" (abre el selector SAF `ACTION_OPEN_DOCUMENT_TREE`); ítems de lista mostrando el `displayPath` legible de cada carpeta, cada uno con una acción para quitarlo; botón primario "Continuar" (habilitado solo con ≥1 carpeta) que transita al escaneo.
- **Flujo de navegación visual:** onboarding → [pantalla de permisos `US-001`] → **[pantalla de selección de Carpetas Fuente]** → escaneo fundacional (`US-003`). Dentro de la pantalla: Agregar → selector SAF del SO → retorno con la carpeta añadida (o aviso si canceló/denegó).
- **Mensajes y feedback:** *empty state* cuando no hay carpetas; aviso no intrusivo al agregar una carpeta duplicada ("La carpeta ya estaba agregada"); aviso ante cancelación ("No se agregó ninguna carpeta") y ante permiso denegado ("Se necesita acceso a la carpeta para descubrir tu audio"); estado deshabilitado comunicado del botón "Continuar" mientras la lista esté vacía. Todos los textos se resuelven y localizan en la capa de presentación (i18n desacoplada de los datos).

### Sistemas Externos

- **Sistema Operativo Android — Storage Access Framework (canal C5):** `ACTION_OPEN_DOCUMENT_TREE` (selector de árbol), `DocumentFile`, `takePersistableUriPermission` / `releasePersistableUriPermission` sobre `treeUri`. Es el único proveedor de autorización de acceso a los archivos, por carpeta (contrato §1.2 / [Restricción 6]). Sin `MediaStore`, sin permisos de media *runtime*, sin red (air-gapped, [RNF-06]).

### Preview de Interfaz

**Preview:** [US-002.preview.md](./US-002.preview.md) | **Formato:** mermaid (flujo de navegación)

---

## Contexto y Referencias

**Arquitectura:**
- `docs/architecture/architecture_blueprint.md` — §1.2 (SAF como mecanismo único; permisos declarados), §1.3 (exclusiones: sin `MediaStore`, sin media runtime, sin `INTERNET`), §2.1 contenedor C-04 (Motor de Biblioteca) y C-01 (onboarding), ADR-003 (SAF en lugar de MediaStore), §4.1 [RF-01] (`AddSourceFolderUseCase` → `SourceFolderRepository` → `SafDataSource`).
- `docs/architecture/interfaces_contract.md` — `TRG-LIB-01` (Agregar Carpeta Fuente), §1.2 (autorización delegada al SO, SAF con permisos persistidos por carpeta), §3.2 (`ERR_PERMISSION_DENIED`, `ERR_DUPLICATE_SOURCE_FOLDER`).
- `docs/architecture/domain_and_state_model.md` — §2 (`SourceFolder`: `treeUri` único, `displayPath`, `dateAddedMs`), §6.1 (`SourceFolder` nace vacío en el Big Bang), §3 (relación `SourceFolder 1:N Track`, `onDelete = CASCADE` — relevante para `US-006`).
- `docs/domain/definition/system_definition_document.md` — §4.1 (Apalancamiento 2, Prerrequisito 4, secuencia de arranque paso 2), Invariantes 1 y 3, Restricción 6.
- `docs/domain/definition/requirements_specification.md` — [RF-01], [RNF-06], [Restricción 6].

**Historias relacionadas:** `US-001` (conceder permisos, precede), `US-003` (escaneo fundacional, sigue y consume las carpetas), `US-004` (marcar onboarding completado), `US-005` (agregar Carpeta Fuente post-onboarding), `US-006` (remover Carpeta Fuente con purga en cascada).

**Lecciones aprendidas:** N/A.

---

## Definición de Terminado (Inicial)

- [ ] Funcionalidad implementada según criterios de aceptación (agregar carpeta vía SAF, múltiples, persistir permiso, registrar `SourceFolder`)
- [ ] Validaciones funcionando correctamente (obligatoriedad de ≥1 carpeta para continuar, unicidad/duplicados, cancelación y permiso denegado, remoción ligera pre-escaneo)
- [ ] Mensajes implementados (empty state, duplicado, cancelación, permiso denegado, estado deshabilitado de "Continuar") y localizados en la capa de presentación
- [ ] Autarquía verificada (solo SAF por carpeta; sin `MediaStore`, sin permisos de media runtime, sin `INTERNET`)
