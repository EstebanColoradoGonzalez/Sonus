# Historia de Usuario

**Como** El Oyente (agente humano soberano y único usuario del sistema),
**Quiero** filtrar las pistas del catálogo escribiendo texto libre que cruce simultáneamente título, nombre de artista y nombre de álbum,
**Para** localizar rápidamente el contenido que busco sin tener que navegar por las dimensiones taxonómicas de la biblioteca.

## Descripción

El Oyente necesita encontrar pistas específicas dentro de su biblioteca local de forma directa, sin conocer de antemano en qué género, artista o álbum están clasificadas. El filtro textual actúa como un atajo de navegación: reduce el espacio de resultados en tiempo real mientras el Oyente escribe, eliminando la necesidad de recorrer la jerarquía de carpetas y dimensiones del catálogo.

El filtro opera exclusivamente sobre el catálogo local (sin red, sin servicios externos) y puede combinarse con los filtros taxonómicos activos de US-010, intersectando ambas condiciones para afinar la búsqueda. La respuesta debe ser perceptiblemente inmediata para no romper la fluidez de la experiencia.

---

## Criterios de Aceptación

### Escenario 1: Filtro activo con coincidencias

- **Dado** que el catálogo contiene pistas indexadas en el Catálogo de Biblioteca
- **Cuando** el Oyente escribe texto en el campo de búsqueda y transcurre el debounce de aproximadamente 250–300 ms sin nuevas pulsaciones
- **Entonces** el sistema actualiza la lista de pistas mostrando únicamente aquellas cuyo título, nombre de artista o nombre de álbum contenga el texto ingresado (búsqueda insensible a mayúsculas/minúsculas), y la respuesta visual ocurre en menos de 500 ms

### Escenario 2: Sin coincidencias

- **Dado** que el Oyente ha ingresado texto en el campo de búsqueda
- **Cuando** ninguna pista del catálogo coincide con el término buscado en título, artista ni álbum
- **Entonces** el sistema muestra una lista vacía, sin mensaje de error, sin estados de carga visibles indefinidos, y sin ningún contenido inventado

### Escenario 3: Limpiar búsqueda

- **Dado** que el campo de búsqueda contiene texto activo
- **Cuando** el Oyente borra todo el texto del campo (queda vacío)
- **Entonces** el sistema restablece la vista del catálogo al estado sin filtro textual (equivalente a `textFilter = null`), mostrando todas las pistas que cumplan los filtros taxonómicos vigentes

### Escenario 4: Combinación con filtros taxonómicos activos

- **Dado** que el Oyente ha aplicado uno o más filtros taxonómicos (por ejemplo, un género o un tipo de contenido específico) desde US-010
- **Cuando** el Oyente escribe texto en el campo de búsqueda
- **Entonces** el sistema intersecta el filtro textual con los filtros taxonómicos activos, mostrando únicamente las pistas que satisfacen ambas condiciones de forma simultánea

### Escenario 5: Latencia aceptable sobre catálogo grande

- **Dado** que el catálogo contiene decenas de miles de pistas indexadas
- **Cuando** el Oyente escribe en el campo de búsqueda
- **Entonces** la interfaz nunca se bloquea, el hilo principal no ejecuta consultas de base de datos, y la respuesta visual al resultado filtrado ocurre en menos de 500 ms tras el debounce

### Escenario 6: Campo de búsqueda vacío al ingresar a la pantalla

- **Dado** que el Oyente navega a la pantalla de biblioteca
- **Cuando** la pantalla se carga por primera vez en esa sesión de navegación
- **Entonces** el campo de búsqueda aparece vacío y el catálogo muestra todos los resultados sin filtro textual

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Oyente — agente humano soberano, único usuario del sistema en Fase 1. Alta competencia tecnológica, estándares elevados de calidad de UX comparables a plataformas de streaming.
- **Permisos requeridos:** Ninguno adicional. El catálogo ya fue indexado en US-007/US-008 con los permisos SAF existentes. Esta historia es de solo lectura sobre el catálogo.
- **Valor de negocio:** Reducir la fricción de navegación cuando el Oyente sabe qué busca pero no recuerda en qué categoría taxonómica está clasificado. Complementa la navegación objetiva (metadatos) con recuperación directa por término libre.

### Reglas de Negocio

- El filtro textual opera exclusivamente sobre datos locales del catálogo; no hay consulta a servicios externos ni a internet (Invariante 1 — Autarquía Absoluta / CT-01).
- El campo de búsqueda vacío equivale a `textFilter = null`, que devuelve el catálogo completo sin filtro textual (no filtrado vacío ≠ "sin resultados").
- La búsqueda se aplica sobre tres campos: `Track.title`, `Artist.name`, `Album.name`. No se busca sobre género, año, número de pista ni duración.
- Las pistas cuyos campos sean el centinela ausente (`id = 1`, nombre vacío) no se incluyen en resultados de búsqueda textual sobre esa dimensión (ej. buscar "Rock" no recupera pistas sin artista cuyo nombre de artista sea vacío).
- La búsqueda no distingue mayúsculas de minúsculas.
- La ausencia de coincidencias devuelve lista vacía, nunca un error ni un estado de fallo (contrato TRG-NAV-01).
- El debounce de ~250–300 ms es obligatorio para evitar re-ejecutar consultas por cada pulsación del teclado y garantizar la latencia sub-500 ms sobre catálogos grandes (contrato interfaces §4.1 / RNF-01).
- No se persiste el historial de búsquedas ni ningún término escrito por el Oyente (CT-02 — cero datos comportamentales / RNF-07).
- La paginación del catálogo se mantiene activa durante la búsqueda; el sistema no carga todos los resultados en memoria de una sola vez (CT-07 / Restricción 4).

### Interfaz

La funcionalidad se ubica dentro de la pantalla principal de biblioteca (`LibraryScreen`), que es el destino de navegación central de EPIC-03. El campo de búsqueda es un control de entrada de texto visible dentro de esta pantalla, accesible sin navegar a ningún destino adicional.

El filtro textual convive con los controles de filtrado taxonómico de US-010 y el ordenamiento de US-012. No reemplaza ninguno de estos mecanismos; los complementa como una capa adicional de reducción del espacio de resultados.

#### Detalle de Interfaz de Usuario

- **Diseño general:** Campo de texto dentro de la pantalla de biblioteca, integrado en la barra de herramientas o inmediatamente debajo de ella. Debe ser visible sin scroll para que el Oyente pueda activarlo en cualquier momento durante la exploración del catálogo.
- **Campos y controles:** Un campo de entrada de texto libre con icono de búsqueda (lupa). Opcionalmente, un botón de limpieza (X) que aparece cuando hay texto para limpiar el campo con un tap.
- **Flujo de navegación visual:** El Oyente toca el campo → se activa el teclado → escribe texto → después del debounce (~250–300 ms) la lista de pistas debajo se actualiza en tiempo real → el Oyente puede limpiar el campo o descartar el teclado.
- **Mensajes y feedback:** No hay mensajes de error. Si la lista queda vacía, puede mostrarse un estado vacío visual (ilustración o texto informativo como "No se encontraron pistas"). No hay spinner de carga visible en condiciones normales gracias al debounce.

### Sistemas Externos

Ninguno. Esta historia opera íntegramente sobre el catálogo local Room/SQLite del dispositivo. No hay integración con red, servicios en la nube ni APIs remotas (Invariante 1).

### Preview de Interfaz

**Preview:** [`US-011.preview.md`](./US-011.preview.md) | **Formato:** Mermaid (diagrama de flujo de navegación)

---

## Contexto y Referencias

**Épica:** EPIC-03 — Exploración del Catálogo (Navegación y Descubrimiento)  
**Requisito funcional:** [RF-12] Navegación Taxonómica Multicapa — el campo `textFilter: String?` es parte del `BrowseQuery` definido en `TRG-NAV-01` (interfaces_contract §2.5)  
**Requisitos no funcionales:** [RNF-01] Latencia visual <500 ms | [RNF-03] Escaneo asíncrono / hilo principal libre | [RNF-07] Cero datos comportamentales  
**Restricciones transversales:** CT-01 (air-gapped) | CT-02 (cero datos comportamentales) | CT-07 (latencia sub-500 ms) | CT-08 (interfaz siempre responsiva)  
**Contrato técnico:** `TRG-NAV-01` (`BrowseQuery.textFilter`) | `LibraryViewModel` (debounce) | `CatalogRepositoryImpl` → `TrackDao` (LIKE indexado)  
**Nota arquitectónica (ADR-001):** El filtro textual se implementa con `LIKE` indexado en Room/SQLite. Búsqueda avanzada con relevancia requeriría FTS4/5, identificada como extensión futura.  
**Arquitectura:** `docs/architecture/architecture_blueprint.md`, `docs/architecture/interfaces_contract.md`, `docs/architecture/domain_and_state_model.md`  
**Historias relacionadas:** US-010 (Exploración taxonómica — comparte pantalla y BrowseQuery) | US-012 (Ordenamiento del catálogo — comparte pantalla y resultado observable)  
**Lecciones aprendidas:** El debounce no es opcional; sin él, catálogos de decenas de miles de pistas producirían consultas por cada tecla y romperían RNF-01. La lógica de debounce reside en `LibraryViewModel`, no en la Vista ni en el repositorio.

---

## Definición de Terminado (Inicial)

- [ ] El campo de búsqueda textual está visible y accesible en la pantalla de biblioteca sin scroll adicional
- [ ] El filtro se aplica con debounce de ~250–300 ms; no se ejecuta una consulta por cada pulsación del teclado
- [ ] La búsqueda es insensible a mayúsculas y cruza título, nombre de artista y nombre de álbum
- [ ] La lista se actualiza en menos de 500 ms tras el debounce (RNF-01)
- [ ] El hilo principal nunca ejecuta consultas a la base de datos; toda consulta corre fuera del Main Thread (RNF-03)
- [ ] La lista vacía se muestra correctamente cuando no hay coincidencias (sin error)
- [ ] Limpiar el campo de búsqueda restaura la vista completa del catálogo
- [ ] El filtro textual se intersecta correctamente con los filtros taxonómicos activos de US-010
- [ ] No se persiste ningún término de búsqueda ni historial de búsquedas (CT-02 / RNF-07)
- [ ] La paginación del catálogo se mantiene activa durante la búsqueda (CT-07 / Restricción 4)
- [ ] No hay llamadas de red ni permisos de internet involucrados (CT-01 / Invariante 1)
