# Historia de Usuario

**Como** El Oyente (agente humano soberano y único usuario de Sonus),
**Quiero** agregar una nueva Carpeta Fuente a mi biblioteca ya en operación (fuera del flujo de primera ejecución),
**Para** expandir el perímetro de descubrimiento e incorporar contenido de audio nuevo, conservando intacto el Catálogo que ya tengo.

## Descripción

Esta historia abre el ciclo operativo de **gestión de Carpetas Fuente** dentro de EPIC-02 (Orden de la Biblioteca) y sostiene el **Bucle de Crecimiento de Biblioteca** (SDD §4.1): a medida que el Oyente agrega nuevas carpetas, el Motor de Biblioteca descubre más contenido y la biblioteca se vuelve más rica. Es la contraparte **post-onboarding** de `US-002` (que selecciona las Carpetas Fuente iniciales durante la primera ejecución): aquí el sistema **ya está operativo**, con un Catálogo poblado, y el Oyente decide sumar un directorio más. Materializa el **Apalancamiento 2** (SDD §4.1): la decisión de qué carpeta incluir define qué contenido se sumará al universo de la biblioteca.

El Oyente agrega la carpeta mediante el selector nativo del *Storage Access Framework* (SAF: `ACTION_OPEN_DOCUMENT_TREE`), único mecanismo de acceso al almacenamiento (ADR-003 / contrato §1.2). Por la carpeta elegida, Sonus **toma el permiso de árbol persistido** (`takePersistableUriPermission`) y registra un `SourceFolder` (`treeUri` único, `displayPath` legible, `dateAddedMs`) — contrato `TRG-LIB-01`. El `treeUri` persistido es la frontera de autorización efectiva por carpeta ([Restricción 6]); el sistema **no** solicita permisos de media *runtime* (`READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE`) ni de red (Invariante 1 / [RNF-06] / [CT-05]).

**Decisiones de negocio de esta historia:**

- **Ubicación en operación normal.** La gestión de Carpetas Fuente vive en la pantalla de **Configuración** (sección "Carpetas Fuente"), con un **acceso adicional desde la Biblioteca**. A diferencia de `US-002`, no forma parte del onboarding.
- **Solapamiento permitido con aviso.** Si la carpeta elegida se **solapa** con una Carpeta Fuente ya registrada (es subdirectorio o directorio contenedor de otra), el sistema **advierte** al Oyente pero **permite** el registro (soberanía del Oyente, Invariante 3). La no duplicación de pistas se garantiza en la **sincronización del Catálogo** por la **identidad natural del `Track` (su `uri`)**: un mismo archivo alcanzado por dos carpetas solapadas produce **un único `Track`** (`uri` único, modelo §1/§2).
- **Escaneo manual (frontera de alcance).** Esta historia **solo registra** la Carpeta Fuente; **no ejecuta el escaneo** que incorpora su contenido al Catálogo. El re-escaneo que descubre las pistas nuevas lo **inicia el Oyente explícitamente** (`US-007` / `TRG-LIB-03`), y su sincronización determinista corresponde a `US-008`. *(Nota de trazabilidad: el contrato `TRG-LIB-01` describe "encolar un escaneo" como comportamiento por defecto; por decisión soberana de negocio, en Sonus el disparo del escaneo se separa en `US-007` para mantener esta historia atómica y dar control explícito al Oyente. Esta historia deja la carpeta registrada y **comunica** que hay contenido pendiente de escanear.)*

Esta historia **no** escanea ni sincroniza el Catálogo (`US-007`/`US-008`), **no** remueve carpetas con purga en cascada (`US-006`), **no** es la selección inicial del onboarding (`US-002`) y **no** observa el progreso del escaneo (`US-009`). Su alcance es exclusivamente el **registro de una nueva Carpeta Fuente** en un sistema ya operativo.

---

## Criterios de Aceptación

### Escenario 1: Flujo Principal

- **Dado** que el Oyente ya completó el onboarding (`US-004`) y tiene una biblioteca operativa con al menos una Carpeta Fuente y un Catálogo poblado, y se encuentra en la sección "Carpetas Fuente" de Configuración
- **Cuando** pulsa "Agregar carpeta", el selector SAF del sistema (`ACTION_OPEN_DOCUMENT_TREE`) se abre, elige un directorio nuevo (no registrado) y el SO otorga el permiso persistible sobre su `treeUri`
- **Entonces** el sistema toma el permiso persistido (`takePersistableUriPermission`), registra un `SourceFolder` (`treeUri` único + `displayPath` legible + `dateAddedMs`) y la nueva carpeta aparece en la lista de Carpetas Fuente, conservando intactas las carpetas y el Catálogo preexistentes (`TRG-LIB-01`, [RF-01])

### Escenario 2: La biblioteca existente se preserva

- **Dado** que el Oyente tiene un Catálogo ya poblado con pistas descubiertas bajo carpetas previas
- **Cuando** agrega una nueva Carpeta Fuente
- **Entonces** el registro de la nueva carpeta **no altera ni reindexa** los `Track` existentes; las carpetas previas y sus pistas permanecen sin cambios, y la nueva carpeta se acumula como un `SourceFolder` independiente (no reemplaza a las anteriores — [RF-01], "una o múltiples carpetas")

### Escenario 3: Validaciones — carpeta duplicada (mismo `treeUri`)

- **Dado** que el Oyente ya tiene registrada una carpeta cuyo `treeUri` está en la lista
- **Cuando** vuelve a seleccionar exactamente la misma carpeta (mismo `treeUri`) en el selector SAF
- **Entonces** el sistema no crea un registro duplicado (unicidad de `SourceFolder.treeUri`), conserva la carpeta existente y comunica de forma no intrusiva que la carpeta ya estaba agregada (`ERR_DUPLICATE_SOURCE_FOLDER`, severidad WARNING)

### Escenario 4: Casos Extremos — carpeta solapada (anidada o contenedora)

- **Dado** que el Oyente ya tiene registrada una Carpeta Fuente (p. ej. `.../Audio`)
- **Cuando** selecciona una carpeta que se **solapa** con ella: un subdirectorio (p. ej. `.../Audio/Podcasts`) o un directorio contenedor (p. ej. `.../`)
- **Entonces** el sistema **advierte** del solapamiento de forma no intrusiva pero **permite** registrar la carpeta como un `SourceFolder` independiente (soberanía del Oyente, Invariante 3); se deja constancia de que la no duplicación de pistas queda garantizada en el escaneo posterior por la identidad natural `Track.uri` (un archivo alcanzado por dos carpetas produce un solo `Track`)

### Escenario 5: Cancelación del selector / permiso no otorgado

- **Dado** que el Oyente abrió el selector SAF del sistema desde la gestión de Carpetas Fuente
- **Cuando** cancela el selector sin elegir un directorio, o el SO no otorga el permiso persistible sobre el `treeUri` elegido
- **Entonces** el sistema regresa a la pantalla conservando las carpetas ya registradas (sin agregar ninguna nueva) y muestra un aviso no intrusivo: en cancelación simple, que no se agregó ninguna carpeta; si el permiso fue denegado por el SO, que el acceso a la carpeta es necesario para descubrir el audio (`ERR_PERMISSION_DENIED`, severidad ERROR), sin reintentar el selector en bucle ([Restricción 6] / Perturbación 3)

### Escenario 6: El escaneo es manual — contenido pendiente

- **Dado** que el Oyente acaba de registrar exitosamente una nueva Carpeta Fuente
- **Cuando** vuelve a la vista de la biblioteca
- **Entonces** el sistema **no** ha incorporado todavía las pistas de esa carpeta al Catálogo (el registro no dispara escaneo automático) y **comunica** que hay contenido pendiente de escanear, ofreciendo iniciar el re-escaneo de forma explícita (`US-007` / `TRG-LIB-03`); mientras el Oyente no lo inicie, el Catálogo permanece sin las pistas nuevas

### Escenario 7: Persistencia del registro tras reinicio

- **Dado** que el Oyente registró una nueva Carpeta Fuente
- **Cuando** cierra y vuelve a abrir la aplicación (o el SO termina y reinicia el proceso)
- **Entonces** la Carpeta Fuente registrada y su permiso de árbol persistido (`takePersistableUriPermission`) siguen vigentes y visibles en la lista, sin requerir volver a autorizarla (memoria durable en `SourceFolder`, modelo §2)

### Escenario 8: Autarquía verificable (solo SAF, sin media runtime ni red)

- **Dado** que el sistema accede al almacenamiento para registrar la nueva Carpeta Fuente
- **Cuando** se inspeccionan los permisos y mecanismos de acceso empleados
- **Entonces** el único mecanismo de acceso es SAF con permiso de árbol persistido por carpeta; en ningún momento se solicita `READ_MEDIA_AUDIO`, `READ_EXTERNAL_STORAGE` ni `android.permission.INTERNET`, ni se emplea `MediaStore` (Invariante 1 / [RNF-06] / [CT-05] / ADR-003)

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Oyente — agente humano soberano y único usuario del sistema (Fase 1). Sin cuentas, roles secundarios ni jerarquías (Invariante 3).
- **Permisos requeridos:** Autorización de acceso **por carpeta** vía SAF (`takePersistableUriPermission` sobre el `treeUri`), única frontera de autorización del acceso a archivos (contrato §1.2). **No** se solicitan permisos de media *runtime* (`READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE`), **ni** `INTERNET`, **ni** se usa `MediaStore`.
- **Valor de negocio:** Sostiene el **Bucle de Crecimiento de Biblioteca** (SDD §4.1) y materializa el **Apalancamiento 2**: agregar una carpeta amplía el universo de contenido descubrible en operación normal, sin rehacer el onboarding ni perder el Catálogo existente.

### Reglas de Negocio

- **Registro atómico, sin escaneo automático:** agregar una Carpeta Fuente **solo** da de alta el `SourceFolder` y toma su permiso persistido; el escaneo que incorpora las pistas nuevas lo inicia el Oyente explícitamente (`US-007`). El sistema comunica que hay contenido pendiente de escanear.
- **Cardinalidad múltiple:** el Oyente puede tener una o varias Carpetas Fuente; agregar una nueva la **acumula** sin reemplazar a las existentes ([RF-01]).
- **Unicidad exacta:** no se admiten Carpetas Fuente con el mismo `treeUri` — es único; reintentar la misma carpeta produce `ERR_DUPLICATE_SOURCE_FOLDER` (WARNING) sin crear duplicado.
- **Solapamiento: advertir y permitir:** una carpeta anidada dentro de (o contenedora de) otra ya registrada se **permite** con **aviso** al Oyente (soberanía, Invariante 3). La no duplicación de pistas se resuelve en la sincronización por la identidad natural `Track.uri` (un archivo → un solo `Track`, modelo §1/§2).
- **Preservación del Catálogo:** el alta de una carpeta **no** modifica, reindexa ni purga los `Track` ni dimensiones existentes.
- **Acceso exclusivo por SAF (ADR-003 / [CT-05]):** el descubrimiento y acceso se realiza solo sobre las carpetas que el Oyente autoriza explícitamente; nunca se indexa todo el almacenamiento. La identidad de acceso es el `treeUri` persistido.
- **Cancelación soberana:** cancelar el selector es una acción válida; no bloquea la pantalla ni fuerza reintentos. La negativa de permiso del SO (`ERR_PERMISSION_DENIED`) se comunica y no se reintenta en bucle ([Restricción 6]).
- **Autarquía (Invariante 1):** jamás se solicita `INTERNET` ni se realiza comunicación de red durante el registro.
- **Frontera de alcance:** esta historia registra una carpeta; **no** escanea ni sincroniza el Catálogo (`US-007`/`US-008`), **no** remueve carpetas con purga en cascada (`US-006`), **no** es la selección inicial del onboarding (`US-002`), **no** observa el progreso del escaneo (`US-009`).

### Interfaz

Acción disponible en **operación normal** (post-onboarding), fuera del `NavHost` de onboarding. Punto de entrada principal en la pantalla de **Configuración** (destino "settings" del `NavHost` Single-Activity, contenedor C-01), sección "Carpetas Fuente"; con un **acceso adicional desde la Biblioteca** (p. ej. desde un menú o el estado vacío de una carpeta). El registro de la carpeta lo ejecuta el Motor de Biblioteca (C-04) vía SAF.

#### Detalle de Interfaz de Usuario

- **Diseño general:** sección "Carpetas Fuente" que lista las carpetas ya registradas (cada una con su `displayPath` legible), un botón "Agregar carpeta" y, cuando corresponde, un aviso de "contenido pendiente de escanear" con una acción para iniciar el re-escaneo (`US-007`). Un acceso equivalente para "Agregar carpeta" se ofrece desde la Biblioteca.
- **Campos y controles:** botón "Agregar carpeta" (abre el selector SAF `ACTION_OPEN_DOCUMENT_TREE`); ítems de lista mostrando el `displayPath` de cada carpeta; acción para iniciar el re-escaneo (delega en `US-007`, fuera de esta HU). *(La acción de quitar carpeta con purga en cascada pertenece a `US-006`.)*
- **Flujo de navegación visual:** Configuración → sección "Carpetas Fuente" → [Agregar carpeta] → selector SAF del SO → retorno con la carpeta añadida (o aviso si canceló/denegó) → aviso "contenido pendiente de escanear". Ruta alterna: Biblioteca → [Agregar carpeta] → (mismo subflujo SAF).
- **Mensajes y feedback:** confirmación no intrusiva al registrar una carpeta; aviso al agregar una carpeta duplicada ("La carpeta ya estaba agregada"); aviso de solapamiento ("Esta carpeta se solapa con otra ya agregada; las pistas repetidas no se duplicarán"); aviso ante cancelación ("No se agregó ninguna carpeta") y ante permiso denegado ("Se necesita acceso a la carpeta para descubrir tu audio"); aviso de "contenido pendiente de escanear" con acción para iniciar el re-escaneo. Todos los textos se resuelven y localizan en la capa de presentación (i18n desacoplada de los datos).

### Sistemas Externos

- **Sistema Operativo Android — Storage Access Framework (canal C5):** `ACTION_OPEN_DOCUMENT_TREE` (selector de árbol), `DocumentFile`, `takePersistableUriPermission` sobre `treeUri`. Único proveedor de autorización de acceso a los archivos, por carpeta (contrato §1.2 / [Restricción 6]). Sin `MediaStore`, sin permisos de media *runtime*, sin red (air-gapped, [RNF-06]).

### Preview de Interfaz

**Preview:** [US-005.preview.md](./US-005.preview.md) | **Formato:** mermaid (flujo de navegación)

---

## Contexto y Referencias

**Arquitectura:**
- `docs/architecture/architecture_blueprint.md` — §1.2 (SAF como mecanismo único; permisos declarados), §1.3 (exclusiones: sin `MediaStore`, sin media runtime, sin `INTERNET`), §2.1 contenedor C-04 (Motor de Biblioteca) y C-01 (Configuración/Biblioteca), §4.1 [RF-01] (`AddSourceFolderUseCase` → `SourceFolderRepository` → `SafDataSource`), ADR-003 (SAF en lugar de MediaStore).
- `docs/architecture/interfaces_contract.md` — `TRG-LIB-01` (Agregar Carpeta Fuente), §1.2 (autorización delegada al SO, SAF con permisos persistidos por carpeta), §3.2 (`ERR_PERMISSION_DENIED`, `ERR_DUPLICATE_SOURCE_FOLDER`); `TRG-LIB-03` (escaneo, referenciado como frontera hacia `US-007`).
- `docs/architecture/domain_and_state_model.md` — §1 (identidad natural del `Track` = `uri`, base de la deduplicación ante solapamiento), §2 (`SourceFolder`: `treeUri` único, `displayPath`, `dateAddedMs`), §3 (relación `SourceFolder 1:N Track`, `onDelete = CASCADE` — relevante para `US-006`), §5.3 (ciclo de escaneo — relevante para `US-007`).
- `docs/domain/definition/system_definition_document.md` — §4.1 (Apalancamiento 2, Bucle de Crecimiento de Biblioteca), Invariantes 1 y 3, Restricción 6.
- `docs/domain/definition/requirements_specification.md` — [RF-01], [RNF-06], [Restricción 6].

**Historias relacionadas:** `US-002` (selección inicial de Carpetas Fuente en onboarding, contraparte fundacional), `US-006` (remover Carpeta Fuente con purga en cascada, hermana inversa), `US-007` (ejecutar escaneo / re-escaneo — consume la carpeta agregada), `US-008` (sincronización determinista del Catálogo), `US-009` (observar progreso del escaneo).

**Lecciones aprendidas:** N/A.

---

## Definición de Terminado (Inicial)

- [ ] Funcionalidad implementada según criterios de aceptación (agregar carpeta vía SAF, persistir permiso, registrar `SourceFolder`, preservar el Catálogo existente)
- [ ] Validaciones funcionando correctamente (unicidad exacta de `treeUri`/duplicado, solapamiento con aviso pero permitido, cancelación y permiso denegado)
- [ ] Punto de entrada disponible en Configuración y acceso adicional desde la Biblioteca
- [ ] Mensajes implementados (confirmación, duplicado, solapamiento, cancelación, permiso denegado, "contenido pendiente de escanear") y localizados en la capa de presentación
- [ ] El registro **no** dispara escaneo automático; se comunica el contenido pendiente y se ofrece iniciar el re-escaneo (`US-007`)
- [ ] Persistencia del registro y del permiso SAF verificada tras reinicio de la app
- [ ] Autarquía verificada (solo SAF por carpeta; sin `MediaStore`, sin permisos de media runtime, sin `INTERNET`)
