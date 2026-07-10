# Mapa y Manifiesto de Historias (Story Mapping Index)

> Este documento actúa como el enrutador central y el mapa topológico del progreso. No contiene los detalles de implementación de cada historia; su propósito es organizar las unidades de trabajo en épicas lógicas, definir el orden cronológico del "Viaje del Agente" y establecer las fases de entrega (Releases). Para ver los detalles, criterios de aceptación o dependencias, navega al archivo `.md` enlazado de cada historia.
>

## 1. Módulos y Épicas (El Viaje del Agente)

*Define las agrupaciones macro de funcionalidad o el orden cronológico estricto de la experiencia de usuario/sistema.*

Las épicas siguen el orden cronológico de la relación del Oyente con su biblioteca: cruza el umbral, el sistema ordena el contenido, el Oyente explora, reproduce, el sonido sobrevive al entorno, y finalmente el Oyente cura y gobierna su colección.

- **[EPIC-01] Fundación Soberana (Primera Ejecución):** Flujo de primera ejecución: concesión de permisos del sistema, selección inicial de Carpetas Fuente y escaneo fundacional de la biblioteca. Establece las condiciones de arranque y la primera impresión del sistema. ([RF-01])

- **[EPIC-02] Orden de la Biblioteca (Indexación y Catálogo):** El Motor de Biblioteca en operación: gestión de las Carpetas Fuente, escaneo y re-escaneo, extracción de metadatos ID3 sin inventar datos, y sincronización determinista del Catálogo (altas de archivos nuevos, marcado de ausentes y purga). Sostiene el Equilibrio de Organización. ([RF-01], [RF-02], [RF-03])

- **[EPIC-03] Exploración del Catálogo (Navegación y Descubrimiento):** Navegación taxonómica que cruza las dimensiones objetivas (Género, Artista, Álbum, Tipo de contenido) con las agrupaciones subjetivas (Playlists), con filtrado textual y ordenamiento. ([RF-12])

- **[EPIC-04] Continuidad del Sonido (Motor de Reproducción):** Reproducción de un contexto (álbum, artista, género, playlist o selección ad-hoc), controles de transporte (play/pausa, siguiente, anterior, seek, detener), modos de cola (secuencial, aleatorio reversible, repetición) y mutación de la cola en tiempo real. Sostiene el Equilibrio de Continuidad. ([RF-07], [RF-08])

- **[EPIC-05] Persistencia y Resiliencia en Segundo Plano:** Supervivencia del proceso ante el sistema operativo, control persistente en notificación y pantalla de bloqueo, gestión del foco de audio, corte de seguridad ante desconexión de la salida de audio, tolerancia a fallos de pista y restauración de la sesión tras una terminación forzada. ([RF-09], [RF-10], [RF-11], [RF-13], [RF-14])

- **[EPIC-06] Soberanía Informacional (Curación de la Biblioteca):** Edición de metadatos ID3, gestión de agrupaciones personalizadas (playlists) y eliminación física de archivos con confirmación explícita. Sostiene el Equilibrio de Soberanía del Oyente sobre su contenido. ([RF-04], [RF-05], [RF-06])

- **[EPIC-07] Preferencias y Gobierno del Sistema:** Preferencia de tema visual (SYSTEM / LIGHT / DARK) y ajustes operativos persistentes, estrictamente no comportamentales. (Configuración del Sistema; sin RF asociado)

## 2. Índice de Historias de Usuario (Story Router)

*Catálogo completo de historias estructurado por épicas. Cada ítem DEBE ser un enlace directo al archivo de la historia atómica.*

### [EPIC-01] Fundación Soberana (Primera Ejecución)

- **[US-001]** Conceder los permisos del sistema -> `./US-001_conceder_permisos.md` | **Estado:** `Todo`
- **[US-002]** Selección guiada de Carpetas Fuente iniciales -> `./US-002_carpetas_fuente_iniciales.md` | **Estado:** `Todo`
- **[US-003]** Escaneo fundacional y transición a la biblioteca -> `./US-003_escaneo_fundacional.md` | **Estado:** `Todo`
- **[US-004]** Marcar el onboarding como completado -> `./US-004_completar_onboarding.md` | **Estado:** `Todo`

### [EPIC-02] Orden de la Biblioteca (Indexación y Catálogo)

- **[US-005]** Agregar una Carpeta Fuente -> `./US-005_agregar_carpeta_fuente.md` | **Estado:** `Todo`
- **[US-006]** Remover una Carpeta Fuente (con purga en cascada) -> `./US-006_remover_carpeta_fuente.md` | **Estado:** `Todo`
- **[US-007]** Ejecutar escaneo / re-escaneo de la biblioteca -> `./US-007_reescaneo_biblioteca.md` | **Estado:** `Todo`
- **[US-008]** Sincronización determinista del Catálogo (altas, ausentes, purga de huérfanos) -> `./US-008_sincronizacion_catalogo.md` | **Estado:** `Todo`
- **[US-009]** Observar el estado y progreso del escaneo -> `./US-009_progreso_escaneo.md` | **Estado:** `Todo`

### [EPIC-03] Exploración del Catálogo (Navegación y Descubrimiento)

- **[US-010]** Explorar y filtrar el catálogo por dimensiones taxonómicas -> `./US-010_navegacion_taxonomica.md` | **Estado:** `Todo`
- **[US-011]** Buscar mediante filtro textual local -> `./US-011_filtro_textual.md` | **Estado:** `Todo`
- **[US-012]** Ordenar la vista del catálogo -> `./US-012_ordenamiento_catalogo.md` | **Estado:** `Todo`

### [EPIC-04] Continuidad del Sonido (Motor de Reproducción)

- **[US-013]** Reproducir un contexto y construir la cola -> `./US-013_reproducir_contexto.md` | **Estado:** `Todo`
- **[US-014]** Alternar reproducción / pausa -> `./US-014_play_pausa.md` | **Estado:** `Todo`
- **[US-015]** Avanzar y retroceder de pista (siguiente / anterior) -> `./US-015_siguiente_anterior.md` | **Estado:** `Todo`
- **[US-016]** Buscar posición dentro de la pista (seek) -> `./US-016_seek.md` | **Estado:** `Todo`
- **[US-017]** Detener la reproducción -> `./US-017_detener.md` | **Estado:** `Todo`
- **[US-018]** Alternar el modo de repetición (OFF / ONE / ALL) -> `./US-018_modo_repeticion.md` | **Estado:** `Todo`
- **[US-019]** Alternar el modo aleatorio reversible (shuffle) -> `./US-019_shuffle_reversible.md` | **Estado:** `Todo`
- **[US-020]** Encolar pistas (reproducir a continuación / añadir al final) -> `./US-020_encolar_pistas.md` | **Estado:** `Todo`
- **[US-021]** Remover una pista de la cola activa -> `./US-021_remover_de_cola.md` | **Estado:** `Todo`
- **[US-022]** Reordenar la cola activa -> `./US-022_reordenar_cola.md` | **Estado:** `Todo`
- **[US-023]** Observar el estado de reproducción (mini-reproductor y pantalla completa) -> `./US-023_observar_nowplaying.md` | **Estado:** `Todo`
- **[US-024]** Reanudar podcasts desde su marcador de progreso -> `./US-024_reanudacion_podcast.md` | **Estado:** `Todo`

### [EPIC-05] Persistencia y Resiliencia en Segundo Plano

- **[US-025]** Supervivencia mediante servicio en primer plano y notificación persistente -> `./US-025_foreground_service.md` | **Estado:** `Todo`
- **[US-026]** Comandos de transporte desde notificación y pantalla de bloqueo -> `./US-026_controles_media_session.md` | **Estado:** `Todo`
- **[US-027]** Gestión activa del foco de audio (ducking / pausa) -> `./US-027_foco_audio.md` | **Estado:** `Todo`
- **[US-028]** Corte de seguridad ante desconexión de la salida de audio -> `./US-028_becoming_noisy.md` | **Estado:** `Todo`
- **[US-029]** Tolerancia a fallos de pista con salto automático -> `./US-029_tolerancia_fallos_pista.md` | **Estado:** `Todo`
- **[US-030]** Persistencia y restauración de sesión tras terminación forzada -> `./US-030_restauracion_sesion.md` | **Estado:** `Todo`

### [EPIC-06] Soberanía Informacional (Curación de la Biblioteca)

- **[US-031]** Editar los metadatos ID3 de una pista -> `./US-031_editar_metadatos.md` | **Estado:** `Todo`
- **[US-032]** Crear una playlist -> `./US-032_crear_playlist.md` | **Estado:** `Todo`
- **[US-033]** Renombrar una playlist -> `./US-033_renombrar_playlist.md` | **Estado:** `Todo`
- **[US-034]** Poblar una playlist (agregar pistas) -> `./US-034_poblar_playlist.md` | **Estado:** `Todo`
- **[US-035]** Remover una pista de una playlist -> `./US-035_remover_de_playlist.md` | **Estado:** `Todo`
- **[US-036]** Reordenar las pistas de una playlist -> `./US-036_reordenar_playlist.md` | **Estado:** `Todo`
- **[US-037]** Eliminar una playlist -> `./US-037_eliminar_playlist.md` | **Estado:** `Todo`
- **[US-038]** Eliminar físicamente un archivo con confirmación explícita -> `./US-038_eliminar_archivo.md` | **Estado:** `Todo`

### [EPIC-07] Preferencias y Gobierno del Sistema

- **[US-039]** Fijar la preferencia de tema visual (SYSTEM / LIGHT / DARK) -> `./US-039_preferencia_tema.md` | **Estado:** `Todo`

## 3. Restricciones Transversales (RNF Implícitos)

*Lista de restricciones operativas o invariantes del sistema que NO se asignan a una historia específica porque permean toda la estructura. Los agentes de Desarrollo y QA (Peer-Reviewer) deben aplicar estos criterios como validadores implícitos en todas las historias.*

- **[CT-01] Aislamiento de red (air-gapped)** ([RNF-06] / Invariante 1): El sistema no establece comunicación de red ni declara el permiso de Internet. No incluye telemetría, analítica ni reporte de errores a servicios externos. No existe modo en línea ni funcionalidad remota.

- **[CT-02] Cero datos comportamentales** ([RNF-07] / Invariante 3): Prohibido crear, persistir o procesar métricas de comportamiento del Oyente (conteos de reproducción, historiales, tiempos de escucha, tasas de salto). Solo se persiste estado operativo: catálogo, configuración, sesión, playlists y marcador de reanudación.

- **[CT-03] No invención de datos** (Invariante 4 / [RF-02]): El sistema nunca infiere ni autocompleta metadatos ausentes. La ausencia se representa como tal; la etiqueta "Sin información" se muestra únicamente en la interfaz y no se persiste como dato.

- **[CT-04] Fidelidad al sistema de archivos** (Invariante 2): El Catálogo es un reflejo fiel del sistema de archivos. No se conservan entradas de archivos inexistentes: al desaparecer un archivo, se purga su registro junto con sus referencias en playlists y en la cola.

- **[CT-05] Acceso limitado a las Carpetas Fuente autorizadas** ([RF-01] / Invariante 3): El sistema solo accede a los archivos dentro de las Carpetas Fuente que el Oyente autoriza explícitamente; no indexa todo el almacenamiento del dispositivo. La autorización de acceso se delega al modelo de permisos del sistema operativo.

- **[CT-06] Gratuidad total** (Invariante 7): Ninguna funcionalidad puede introducir publicidad, muros de pago, funciones premium, suscripciones ni compras. Todas las capacidades están disponibles de forma completa desde el primer uso.

- **[CT-07] Latencia de interacción sub-500 ms** ([RNF-01]): Toda navegación, filtrado y transición de pantalla debe responder en menos de 500 ms, incluso sobre catálogos de decenas de miles de pistas.

- **[CT-08] Interfaz siempre responsiva** ([RNF-03]): La interfaz nunca se bloquea. El escaneo, la reproducción y toda operación de disco o decodificación se ejecutan fuera del hilo principal; las operaciones largas reportan progreso.

- **[CT-09] Huella de almacenamiento mínima** ([RNF-08]): El sistema minimiza su propio consumo de almacenamiento. Las carátulas no se guardan como archivos (se leen desde el propio audio) y no se acumulan históricos.

- **[CT-10] Degradación grácil ante fallos** (Invariantes 2 y 6): Ningún fallo local aborta la aplicación. Los fallos de una pista se absorben continuando la reproducción; los permisos revocados o carpetas inaccesibles degradan la función de forma comunicada, conservando el último estado coherente. Los mensajes al Oyente se resuelven y localizan en la interfaz.

- **[CT-11] Portabilidad sobre APIs estándar** ([RNF-09]): La funcionalidad se apoya exclusivamente en las APIs de la capa estándar del sistema operativo, sin librerías propietarias externas, y debe operar en cualquier dispositivo Android compatible.

## 4. Fases de Entrega (Release Plan / Slices)

*Define las fronteras de entrega, agrupando las historias indexadas en incrementos de valor funcional.*

### 4.1. Iteración Base (MVP / Release 1.0)

- **Objetivo de la Liberación:** Que el Oyente pueda arrancar el sistema por primera vez (permisos, Carpetas Fuente y escaneo), navegar su catálogo organizado por metadatos y reproducir su audio de forma continua e ininterrumpida, sobreviviendo a la terminación de procesos y a las perturbaciones del entorno (llamadas, desconexión de auriculares, pistas defectuosas).
- **Historias Incluidas:**
  - `[US-001]`, `[US-002]`, `[US-003]`, `[US-004]`, `[US-005]`, `[US-007]`, `[US-008]`, `[US-009]`, `[US-010]`, `[US-013]`, `[US-014]`, `[US-015]`, `[US-016]`, `[US-017]`, `[US-023]`, `[US-025]`, `[US-026]`, `[US-027]`, `[US-028]`, `[US-029]`, `[US-030]`

### 4.2. Iteración de Expansión (Release 1.1)

- **Objetivo de la Liberación:** Completar la experiencia de escucha (modos de cola, encolado y reanudación de podcasts) y la exploración (búsqueda textual y ordenamiento), añadir la gestión posterior de Carpetas Fuente y las preferencias visuales.
- **Historias Incluidas:**
  - `[US-006]`, `[US-011]`, `[US-012]`, `[US-018]`, `[US-019]`, `[US-020]`, `[US-021]`, `[US-022]`, `[US-024]`, `[US-039]`

### 4.3. Iteración de Soberanía Plena (Release 1.2)

- **Objetivo de la Liberación:** Habilitar la curación de la biblioteca —edición de metadatos, gestión completa de playlists y eliminación física de archivos— que activa los ciclos de refinamiento de metadatos, enriquecimiento de playlists y depuración de la colección.
- **Historias Incluidas:**
  - `[US-031]`, `[US-032]`, `[US-033]`, `[US-034]`, `[US-035]`, `[US-036]`, `[US-037]`, `[US-038]`
