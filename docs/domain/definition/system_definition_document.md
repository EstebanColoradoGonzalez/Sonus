# Documento de Definición del Sistema

> Este documento es la fuente de verdad absoluta del negocio. Debe redactarse utilizando lenguaje natural y conceptos de la Teoría General de Sistemas. **No incluir jerga técnica, nombres de bases de datos, frameworks o lenguajes de programación.**
>

## 1. Visión y Propósito (El Por qué)

### 1.1. Contexto y Nivel de Entropía

*Describe el estado de desorden, ineficiencia o necesidad latente en el entorno antes de la intervención de este sistema. ¿Qué caos viene a organizar?*

- **Problema actual:** El entorno actual del consumo de audio personal en dispositivos móviles presenta un estado de entropía elevado que se manifiesta en cuatro dimensiones simultáneas:
  - **Entropía en la soberanía del usuario.** El modelo dominante de la industria ha desplazado el control de la experiencia de escucha hacia plataformas centralizadas de streaming. El usuario no posee sus archivos; los alquila. Las herramientas disponibles que sí operan sobre archivos locales imponen barreras de monetización (muros de pago para funciones básicas, publicidad intrusiva) o requieren conexión a servicios externos, generando una dependencia estructural que anula la autonomía del individuo sobre su propia biblioteca de audio.
  - **Entropía en la organización del contenido.** Los archivos de audio almacenados localmente en un dispositivo existen en un estado de desorden inherente. Los metadatos embebidos en dichos archivos (etiquetas ID3: título, artista, género, carátula) frecuentemente son incorrectos, incompletos o inexistentes. Esta corrupción informacional se propaga: cualquier intento de categorización automática produce resultados caóticos. A esto se suma la ausencia de una frontera semántica clara entre tipos de contenido radicalmente distintos — música (piezas cortas, consumo aleatorio) y podcasts (piezas largas, consumo secuencial) — que conviven en el mismo espacio de almacenamiento sin diferenciación, degradando la experiencia de escucha cuando se mezclan indiscriminadamente.
  - **Entropía en la continuidad operativa.** El entorno de ejecución (específicamente dispositivos con sistemas operativos que implementan políticas agresivas de gestión de recursos, como MIUI/HyperOS de Xiaomi) introduce una perturbación constante: la terminación forzada de procesos en segundo plano. Este comportamiento del entorno rompe la expectativa fundamental de que un reproductor de audio mantenga la reproducción continua cuando el usuario interactúa con otras aplicaciones o bloquea la pantalla, generando una experiencia fragmentada e impredecible.
  - **Entropía en la interfaz de interacción.** Las herramientas locales existentes presentan interfaces de usuario que no han evolucionado al ritmo de los estándares visuales y de interacción establecidos por las plataformas de streaming modernas. Esto crea una disonancia cognitiva: el usuario que está habituado a la fluidez, la intuitividad y la estética contemporánea de aplicaciones como Spotify se encuentra con interfaces anticuadas, confusas o funcionalmente limitadas cuando intenta gestionar su contenido local.
- **Impacto del problema:** El resultado neto de estas cuatro dimensiones de entropía es que **no existe una herramienta que permita al usuario ejercer soberanía completa sobre su biblioteca de audio local con una experiencia de calidad comparable a las plataformas centralizadas**, obligándolo a elegir entre control (reproductores locales deficientes) o experiencia (plataformas de streaming con dependencia externa).

### 1.2. Propósito Central (La Misión Sistémica)

*La declaración inquebrantable de lo que el sistema debe lograr o mantener en equilibrio (homeostasis), independientemente de su medio de ejecución.*

- **Misión:** **Sonus existe para gobernar la relación entre un agente humano y su colección finita de archivos de audio almacenados localmente, garantizando que dicha relación se mantenga en un estado de orden, continuidad y control soberano.**

La homeostasis que el sistema debe mantener opera en tres equilibrios simultáneos:

1. **Equilibrio de Organización:** El sistema debe mantener la biblioteca de audio en un estado permanente de orden estructurado, donde cada pieza de contenido esté correctamente identificada, categorizada y accesible según su naturaleza (música vs. podcast), su género, su artista y sus metadatos asociados. Este orden debe ser resiliente ante la incorporación de nuevos archivos con metadatos deficientes.
2. **Equilibrio de Continuidad:** El sistema debe garantizar que el flujo de reproducción de audio, una vez iniciado por el agente, se mantenga ininterrumpido e independiente de las acciones concurrentes del agente sobre otros sistemas del dispositivo o del estado de la pantalla. La reproducción es un proceso que, una vez activado, debe resistir perturbaciones externas.
3. **Equilibrio de Soberanía:** El sistema debe operar en completa autarquía respecto a sistemas externos (servidores, redes, servicios en la nube). Toda la información, el procesamiento y la experiencia deben residir y ejecutarse dentro de los límites del dispositivo local. La funcionalidad del sistema no debe degradarse bajo ninguna condición de conectividad.

Esta misión es agnóstica al medio tecnológico: conceptualmente, Sonus es un sistema de gobernanza de biblioteca de audio personal, ya sea que se materialice como una aplicación móvil, una herramienta de escritorio o cualquier otro artefacto.

**Propuesta de Transformación (Estado Inicial → Estado Final).** El sistema garantiza la siguiente transformación:

| Dimensión | Estado Inicial (Entrada) | Estado Final (Salida) |
|---|---|---|
| **Archivos de audio** | Conjunto desestructurado de archivos en almacenamiento local, con metadatos potencialmente corruptos, incompletos o ausentes, sin diferenciación semántica entre tipos de contenido | Biblioteca organizada, categorizada por tipo (música/podcast), género, artista y álbum, con metadatos verificados y corregidos por el agente, y carátulas asociadas |
| **Experiencia de escucha** | Fragmentada, impredecible, interrumpida por el entorno operativo, sin contexto visual ni controles persistentes | Continua, estable, con retroalimentación visual rica (carátula, progreso, controles), mantenida en segundo plano con controles accesibles desde notificaciones |
| **Interacción del usuario** | Dispersa entre múltiples herramientas deficientes, con interfaces inconsistentes y fricción alta | Centralizada en un punto único de interacción con interfaz moderna, intuitiva y estéticamente coherente |
| **Control sobre metadatos** | Requiere herramientas externas (computador) para consultar o modificar etiquetas ID3 | Edición directa desde el mismo sistema, con propagación inmediata del cambio a la organización de la biblioteca |

**Equifinalidad — Múltiples vías hacia el estado final.** El sistema contempla vías alternativas para alcanzar el estado de orden deseado:

- **Organización por metadatos (vía principal):** El sistema estructura la biblioteca leyendo y utilizando las etiquetas ID3 embebidas en los archivos (género, artista, álbum, tipo de contenido). Esta es la vía primaria de categorización: la estructura objetiva de la biblioteca se deriva de las propiedades intrínsecas de cada archivo de audio. Cuando los metadatos son completos y correctos, el sistema genera automáticamente una taxonomía navegable por género, artista, álbum y tipo (música vs. podcast).
- **Organización por agrupaciones personalizadas del agente (vía subjetiva):** El sistema permite al agente humano crear sus propias colecciones lógicas (playlists o carpetas personalizadas) para agrupar archivos de audio según criterios enteramente subjetivos y situacionales, independientes de los metadatos. Un archivo puede pertenecer simultáneamente a su categoría por metadatos (ej. "Rock") y a múltiples agrupaciones personales (ej. "Para correr", "Favoritos del mes"). Esta vía otorga al agente una segunda dimensión de organización que no depende de la calidad de los metadatos y refleja su intención personal de consumo.
- **Corrección manual de metadatos por el agente (vía de refinamiento):** El sistema permite que el agente humano intervenga directamente para consultar y modificar los metadatos embebidos en los archivos de audio. Este mecanismo actúa como vía de refinamiento progresivo: cada corrección realizada por el agente mejora la calidad de la vía principal (organización por metadatos), propagándose inmediatamente a la estructura de la biblioteca. Es el puente que permite al agente transformar el desorden informacional de los metadatos en orden estructural.

Estas tres vías no son mutuamente excluyentes; se complementan y refuerzan mutuamente para alcanzar el mismo estado final de biblioteca organizada. La vía de metadatos provee la estructura objetiva, las agrupaciones personalizadas proveen la organización subjetiva, y la corrección manual mejora continuamente la calidad de la primera. Esta convergencia otorga al sistema una robustez inherente ante la variabilidad de la calidad de los datos de entrada y la diversidad de contextos de uso del agente.

### 1.3. Estados de Cierre e Indicadores de Eficacia

*Condiciones medibles que indican que el sistema cumple su propósito central y mitiga la entropía inicial. (Métricas de éxito y valor aportado).*

El sistema opera en ciclos continuos sin un "estado final absoluto" — su propósito es mantener equilibrios, no alcanzar un destino. Sin embargo, se identifican ciclos operativos con estados de cierre definidos y parámetros para medir la eficacia del sistema en su misión de mitigar la entropía.

**Ciclos Operativos y sus Estados de Cierre:**

| Ciclo | Estado de Cierre | Condición de cierre exitoso |
|---|---|---|
| **Ciclo de Escaneo** | Catálogo actualizado | Todas las carpetas fuente han sido recorridas, todos los archivos de audio descubiertos están indexados con sus metadatos, y el Catálogo refleja fielmente el estado actual del sistema de archivos. No quedan archivos sin procesar ni entradas huérfanas en el Catálogo. |
| **Ciclo de Reproducción** | Cola agotada o detenida por el Oyente | El Motor de Reproducción ha reproducido todas las pistas de la cola sin interrupciones no deseadas, o el Oyente ha detenido voluntariamente la reproducción. El estado de la sesión (última pista, posición) queda persistido. |
| **Ciclo de Edición de Metadatos** | Metadato persistido y Catálogo coherente | El cambio solicitado por el Oyente ha sido escrito en el archivo de audio, el Catálogo refleja el nuevo valor, y la organización visible de la biblioteca es consistente con la modificación. |
| **Ciclo de Gestión de Playlist** | Playlist persistida | La creación, modificación o eliminación de una agrupación personalizada ha sido almacenada y es visible en la interfaz. |
| **Ciclo de Eliminación de Archivo** | Archivo eliminado y sistema limpio | El archivo ha sido eliminado del sistema de archivos, removido del Catálogo, y todas las referencias en playlists y cola de reproducción han sido eliminadas automáticamente. |

**Indicadores de Eficacia (¿Está Sonus cumpliendo su misión?):**

| Indicador | Qué mide | Señal de equilibrio | Señal de desequilibrio |
|---|---|---|---|
| **Integridad del Catálogo** | Correspondencia entre Catálogo y sistema de archivos | Cada entrada del Catálogo apunta a un archivo existente; cada archivo de audio en las carpetas fuente tiene una entrada en el Catálogo | Entradas huérfanas en el Catálogo, archivos no indexados en las carpetas fuente |
| **Continuidad de Reproducción** | Tiempo de reproducción ininterrumpida vs. interrupciones no deseadas | Sesiones de escucha completas sin cortes por terminación del SO, sin saltos por archivos inaccesibles | Cortes frecuentes, pérdida de estado de reproducción, necesidad de reiniciar manualmente |
| **Completitud de Metadatos** | Proporción de archivos con metadatos completos y correctos vs. archivos con campos vacíos o "Sin información" | Tendencia decreciente de archivos sin metadatos a lo largo del tiempo (por efecto del bucle de refinamiento) | Proporción estancada o creciente de archivos sin metadatos, indicando que el Oyente no percibe valor en corregirlos |
| **Cobertura de Playlists** | Proporción de archivos de la biblioteca que pertenecen a al menos una agrupación personalizada | Crecimiento orgánico de playlists y de archivos referenciados en ellas | Playlists vacías, archivos que nunca son incluidos en ninguna agrupación |
| **Latencia de Interacción** | Tiempo entre un comando del Oyente y la respuesta perceptible del sistema | Respuestas sub-segundo para navegación y controles de reproducción; escaneo con indicador de progreso | Retrasos perceptibles en navegación, tiempos de carga visibles, interfaz que no responde a gestos inmediatamente |

## 2. Entorno y Fronteras (El Dónde)

### 2.1. Fronteras e Interfaces

*Define la "piel" del sistema. Qué está bajo la jurisdicción del sistema y qué pertenece al exterior.*

- **Dentro del sistema (In-Scope):**
  - El Catálogo de Biblioteca y su lógica de indexación.
  - El Motor de Reproducción y la gestión de la cola de reproducción.
  - La interfaz visual de interacción con el Oyente.
  - La lógica de lectura y escritura de metadatos embebidos.
  - La gestión de agrupaciones personalizadas (playlists).
  - La configuración y preferencias del sistema.
  - La persistencia local de toda la información operativa del sistema.
- **Fuera del sistema (Out-of-Scope):**
  - El sistema de archivos del dispositivo (Sonus lee de él, escribe metadatos y puede eliminar archivos por orden del Oyente, pero no gobierna su estructura general).
  - El sistema operativo del dispositivo y sus políticas de gestión de recursos (MIUI/HyperOS).
  - El hardware de audio del dispositivo (altavoz, auriculares, Bluetooth).
  - Las aplicaciones externas que compiten por recursos del dispositivo.
  - Cualquier red, servidor, servicio en la nube o sistema externo (están fuera y **permanecen fuera**; Sonus no establece comunicación con ellos bajo ninguna circunstancia).

**Interfaces (Puntos de contacto Sistema ↔ Entorno):**

| Interfaz | Tipo | Dirección | Protocolo/Mecanismo |
|---|---|---|---|
| **Acceso al Sistema de Archivos** | Bidireccional | Sistema ↔ Entorno | Lectura, escritura y eliminación de archivos de audio desde las carpetas fuente configuradas por el Oyente. El sistema solicita permisos de acceso al almacenamiento del dispositivo para leer archivos y sus metadatos, escribir modificaciones de metadatos, y eliminar archivos cuando el Oyente lo ordene con confirmación explícita. |
| **Salida de Audio** | Salida | Sistema → Entorno | El Motor de Reproducción emite el flujo de audio decodificado hacia el hardware de audio del dispositivo a través de las APIs de audio del sistema operativo. |
| **Interfaz Visual (Pantalla)** | Bidireccional | Sistema ↔ Oyente | Superficie visual táctil con un modelo de interacción multi-capa que permite al Oyente navegar la biblioteca mientras mantiene visibilidad y control persistente sobre la reproducción activa. Las capas de interacción son: (1) **Navegación de biblioteca** — pantallas de exploración, filtrado y gestión del contenido organizado por metadatos o playlists; (2) **Mini-reproductor persistente** — componente compacto siempre visible durante la navegación que muestra la pista actual, su progreso y controles básicos (play/pause, siguiente), permitiendo al Oyente controlar la reproducción sin abandonar la pantalla de biblioteca en la que se encuentra; (3) **Pantalla de reproducción completa** — vista expandida dedicada a la pista en reproducción, con carátula prominente, barra de progreso interactiva, controles completos y contexto de la cola activa. Las transiciones entre estas capas deben ser fluidas y emular los estándares de interacción de aplicaciones de streaming modernas como Spotify. |
| **Controles en Notificación / Pantalla de Bloqueo** | Bidireccional | Sistema ↔ Oyente | Interfaz reducida expuesta a través del sistema de notificaciones del SO, que permite al Oyente controlar la reproducción sin abrir la interfaz visual completa. El sistema publica su estado; el Oyente emite comandos básicos (play, pause, siguiente, anterior). |
| **Negociación con el SO (Supervivencia)** | Bidireccional | Sistema ↔ Entorno | El sistema solicita al sistema operativo la capacidad de mantener un proceso persistente en segundo plano (Foreground Service) y gestiona las señales del SO que amenazan con interrumpir la reproducción. |

### 2.2. Agentes y Roles

*Identifica a los ejecutores autónomos (humanos, lógicos, mecánicos) que interactúan con el sistema, incluyendo sus perfiles y niveles de autoridad.*

El sistema identifica los siguientes agentes autónomos que interactúan dentro de sus fronteras:

- **El Oyente (Agente Humano Principal):**
  - **Naturaleza:** Agente humano consciente, único usuario del sistema en Fase 1.
  - **Perfil:** Individuo con alta competencia tecnológica y estándares elevados de calidad en experiencia de uso. Posee una colección personal de archivos de audio almacenados localmente en su dispositivo, que incluye tanto música como podcasts.
  - **Capacidades dentro del sistema:**
    - Explorar, navegar y filtrar la biblioteca de audio organizada por las dimensiones objetiva (metadatos) y subjetiva (agrupaciones personales). El filtrado permite al Oyente reducir el conjunto visible de contenido aplicando criterios específicos (ej. solo podcasts, solo un género determinado, solo un artista).
    - Iniciar, pausar, reanudar, detener, avanzar y retroceder la reproducción de cualquier pieza de audio.
    - Controlar el orden de reproducción (secuencial, aleatorio, repetición).
    - Consultar y modificar los metadatos embebidos en los archivos de audio (etiquetas ID3: título, artista, álbum, género, carátula, tipo de contenido).
    - Crear, editar, eliminar y poblar agrupaciones personalizadas (playlists).
    - Eliminar archivos de audio del dispositivo directamente desde el sistema, con confirmación explícita previa.
    - Configurar las carpetas fuente desde las cuales el sistema descubre archivos de audio (una o múltiples carpetas seleccionadas por el Oyente).
  - **Nivel de autoridad:** Soberano absoluto. El Oyente es la máxima autoridad dentro del sistema. Toda decisión de organización, reproducción y configuración emana exclusivamente de su voluntad. No existe ningún otro agente con capacidad de anular, restringir o condicionar sus acciones.
  - **Heurísticas de decisión:** El Oyente decide qué escuchar basándose en contexto situacional (estado de ánimo, actividad, momento del día). No sigue un patrón predecible; por ello, el sistema debe ofrecer múltiples puntos de entrada a la biblioteca y no imponer un flujo de navegación rígido.
- **El Motor de Biblioteca (Agente Lógico):**
  - **Naturaleza:** Agente autónomo no consciente, ejecutado como proceso lógico dentro del sistema.
  - **Perfil:** Responsable de descubrir, indexar y estructurar los archivos de audio presentes en el almacenamiento local del dispositivo.
  - **Capacidades dentro del sistema:**
    - Escanear recursivamente las carpetas fuente configuradas por el Oyente para descubrir archivos de audio compatibles.
    - Leer y extraer los metadatos embebidos (etiquetas ID3) de cada archivo descubierto.
    - Construir y mantener actualizado un índice estructurado de la biblioteca (el catálogo).
    - Detectar cambios en el sistema de archivos (archivos nuevos, eliminados o movidos) y propagar esos cambios al catálogo.
    - Persistir las modificaciones de metadatos realizadas por el Oyente de vuelta a los archivos de audio.
    - Ejecutar la eliminación física de archivos de audio del sistema de archivos cuando el Oyente lo ordene, previa confirmación explícita, y propagar la eliminación al Catálogo y a todas las agrupaciones que lo referenciaban.
  - **Nivel de autoridad:** Ejecutor subordinado. Opera exclusivamente bajo las reglas definidas por el sistema y las configuraciones establecidas por el Oyente. No toma decisiones autónomas sobre la organización; solo ejecuta la indexación según los metadatos encontrados y las correcciones indicadas.
  - **Heurísticas de decisión:** Ante un archivo sin metadatos o con metadatos ambiguos, el Motor de Biblioteca lo indexa con la información disponible (incluyendo valores vacíos o desconocidos) y lo presenta al Oyente sin intentar inferir o inventar datos faltantes.
- **El Motor de Reproducción (Agente Lógico):**
  - **Naturaleza:** Agente autónomo no consciente, ejecutado como proceso lógico persistente dentro del sistema.
  - **Perfil:** Responsable de decodificar y emitir el flujo de audio, manteniendo la reproducción continua e ininterrumpida independientemente del estado del dispositivo.
  - **Capacidades dentro del sistema:**
    - Decodificar archivos de audio en los formatos soportados y emitir el flujo de audio al hardware de salida del dispositivo.
    - Mantener la reproducción activa en segundo plano, resistiendo las políticas de terminación de procesos del sistema operativo del dispositivo.
    - Gestionar la cola de reproducción (siguiente, anterior, aleatorio, repetición).
    - Exponer controles de reproducción a través de las interfaces del sistema operativo (notificaciones, controles en pantalla de bloqueo).
    - Reportar el estado de reproducción en tiempo real (pista actual, progreso, duración) a la interfaz visual del sistema.
  - **Nivel de autoridad:** Ejecutor subordinado autónomo-operativo. Obedece las instrucciones de reproducción del Oyente, pero opera con autonomía para mantener la continuidad del flujo de audio sin intervención constante del usuario. Es el único agente con capacidad de interactuar con el hardware de audio del dispositivo.
  - **Heurísticas de decisión:** Si la pista actual finaliza, avanza automáticamente a la siguiente pista en la cola según el modo de reproducción activo. Si no hay más pistas, se detiene y reporta el fin de la cola. Ante un error de decodificación de un archivo, salta a la siguiente pista y notifica el evento sin interrumpir la experiencia de escucha.

## 3. Ontología: Entidades Centrales (El Qué)

### 3.1. Entidades y Activos

*Elementos principales que son manipulados, consumidos o transformados por el sistema. (Ej. "Jugador", "Factura", "Partida", "Cliente").*

- **Archivo de Audio:** Unidad atómica del sistema. Un archivo digital almacenado en el sistema de archivos local del dispositivo que contiene una pista de audio codificada (música o podcast). Es el activo fundamental que justifica la existencia de Sonus. *(Naturaleza: físico-digital.)*
- **Metadatos Embebidos (Etiquetas ID3):** Conjunto de propiedades informacionales adheridas al archivo de audio: título, artista, álbum, género, número de pista, año, carátula (imagen), y tipo de contenido. Pueden ser correctos, incompletos, erróneos o ausentes. *(Naturaleza: informacional.)*
- **Agrupación Personalizada (Playlist):** Colección lógica definida por el Oyente que agrupa referencias a archivos de audio según criterios subjetivos. Un archivo puede pertenecer a cero o múltiples agrupaciones simultáneamente. Tiene un nombre y un orden definido por el usuario. *(Naturaleza: informacional.)*

**Elementos en tránsito (Flujos).** Además de los activos anteriores, el sistema transporta los siguientes elementos entre sus componentes y el entorno:

| Elemento en tránsito | Descripción | Origen → Destino |
|---|---|---|
| **Flujo de Audio Decodificado** | Señal de audio digital procesada y lista para ser emitida al hardware de salida. | Motor de Reproducción → Hardware de audio del dispositivo |
| **Evento de Estado de Reproducción** | Información en tiempo real sobre la pista actual, posición de progreso, duración total y modo de reproducción. | Motor de Reproducción → Interfaz visual del sistema |
| **Comando del Oyente** | Instrucciones emitidas por el agente humano (reproducir, pausar, siguiente, editar metadato, crear playlist, etc.). | Oyente → Sistema |
| **Modificación de Metadatos** | Cambio en una o más etiquetas ID3 de un archivo, iniciado por el Oyente y persistido por el Motor de Biblioteca. | Oyente → Motor de Biblioteca → Archivo de Audio |
| **Resultado de Escaneo** | Conjunto de archivos de audio descubiertos con sus metadatos extraídos tras un escaneo de las carpetas fuente. | Sistema de archivos → Motor de Biblioteca → Catálogo |
| **Orden de Eliminación de Archivo** | Instrucción confirmada por el Oyente para eliminar permanentemente un archivo de audio del dispositivo. El Motor de Biblioteca ejecuta la eliminación física del archivo en el sistema de archivos, lo remueve del Catálogo y elimina automáticamente todas las referencias a dicho archivo en las agrupaciones personalizadas. | Oyente → Motor de Biblioteca → Sistema de archivos |

### 3.2. Stocks (Acumulaciones)

*Inventarios físicos o abstractos que se acumulan o agotan en un momento dado. (Ej. "Saldo en cuenta", "Puntos de vida", "Inventario en bodega").*

| Stock | Descripción | Naturaleza |
|---|---|---|
| **Archivo de Audio** | Unidad atómica del sistema. Un archivo digital almacenado en el sistema de archivos local del dispositivo que contiene una pista de audio codificada (música o podcast). Es el activo fundamental que justifica la existencia de Sonus. | Físico-digital |
| **Metadatos Embebidos (Etiquetas ID3)** | Conjunto de propiedades informacionales adheridas al archivo de audio: título, artista, álbum, género, número de pista, año, carátula (imagen), y tipo de contenido. Pueden ser correctos, incompletos, erróneos o ausentes. | Informacional |
| **Catálogo de Biblioteca** | Índice estructurado que representa el estado actual de todos los archivos de audio descubiertos por el Motor de Biblioteca, junto con sus metadatos extraídos. Es la representación interna del orden de la biblioteca. | Informacional |
| **Agrupación Personalizada (Playlist)** | Colección lógica definida por el Oyente que agrupa referencias a archivos de audio según criterios subjetivos. Un archivo puede pertenecer a cero o múltiples agrupaciones simultáneamente. Tiene un nombre y un orden definido por el usuario. | Informacional |
| **Cola de Reproducción** | Secuencia ordenada de archivos de audio pendientes de reproducción en la sesión activa. Es efímera por naturaleza: se construye cuando el Oyente inicia la reproducción y puede ser modificada en tiempo real. | Informacional-transitoria |
| **Configuración del Sistema** | Conjunto de parámetros persistentes que gobiernan el comportamiento del sistema: rutas de las carpetas fuente, preferencias de visualización, último estado de reproducción. | Informacional |

## 4. Dinámica y Retroalimentación (El Cómo Sistémico)

### 4.1. Flujos y Bucles de Retroalimentación

*Cómo circulan la información y las acciones para generar crecimiento (refuerzo) o mantener el equilibrio (balance).*

**Condiciones de Arranque.** Para que el sistema rompa su inercia inicial y comience a operar, los siguientes prerrequisitos deben estar satisfechos:

*Prerrequisitos del Entorno:*

1. **Dispositivo operativo:** Un dispositivo Android funcional con sistema operativo compatible, hardware de audio operativo y almacenamiento local accesible.
2. **Archivos de audio existentes:** Al menos un archivo de audio en un formato compatible debe existir en el almacenamiento local del dispositivo. Sin archivos de audio, el sistema puede arrancar pero no puede cumplir su propósito central — la biblioteca estará vacía y los motores de reproducción e indexación no tendrán materia prima sobre la cual operar.
3. **Permisos concedidos:** El sistema operativo debe haber otorgado al sistema los permisos de acceso al almacenamiento local. Sin estos permisos, la interfaz entre el sistema y el sistema de archivos está cerrada y el Motor de Biblioteca no puede operar.

*Prerrequisitos de Configuración (Primera ejecución):*

4. **Carpetas fuente definidas:** El Oyente debe indicar al sistema cuáles son las carpetas fuente desde las cuales descubrir archivos de audio (puede ser una o múltiples carpetas). Este es el acto fundacional que define el perímetro de escaneo del Motor de Biblioteca. La selección de carpetas específicas permite al Oyente incluir solo los directorios relevantes (ej. carpeta de Música, carpeta de Podcasts) y excluir contenido no deseado (notas de voz, audios de mensajería, sonidos del sistema). Sin esta configuración, el sistema no sabe *dónde* buscar.

*Secuencia de arranque.* Una vez satisfechos los prerrequisitos, el arranque se ejecuta en el siguiente orden:

1. El sistema solicita y verifica permisos de acceso al almacenamiento.
2. El Oyente configura las carpetas fuente (si es la primera ejecución) o el sistema carga la configuración persistida.
3. El Motor de Biblioteca ejecuta un escaneo inicial de todas las carpetas fuente configuradas, descubriendo todos los archivos de audio y extrayendo sus metadatos.
4. El Catálogo de Biblioteca se construye y se presenta al Oyente a través de la interfaz visual.
5. El Motor de Reproducción se posiciona en estado inactivo, listo para recibir el primer comando de reproducción del Oyente.
6. El sistema alcanza su estado operativo nominal: biblioteca indexada, interfaz visible, reproducción disponible.

El sistema opera con flujos circulares, no lineales. Se identifican los siguientes bucles:

- **Bucles de Refuerzo (amplificación positiva):**
  - **Bucle de Refinamiento de Metadatos:** Cuantos más metadatos corrige el Oyente → mejor organizada está la biblioteca → más fácil es para el Oyente descubrir archivos con metadatos incorrectos → más correcciones realiza. Este bucle de refuerzo genera un ciclo virtuoso donde la calidad del catálogo mejora progresivamente con el uso. La acción de corregir produce un entorno que facilita más correcciones.
  - **Bucle de Enriquecimiento de Playlists:** Cuantas más agrupaciones personalizadas crea el Oyente → más contextos de escucha tiene cubiertos → más natural se vuelve la interacción con el sistema → mayor es la probabilidad de que cree nuevas agrupaciones para nuevos contextos. El sistema se vuelve más valioso con cada playlist creada.
  - **Bucle de Crecimiento de Biblioteca:** A medida que el Oyente incorpora más archivos de audio a las carpetas fuente o agrega nuevas carpetas fuente → el Motor de Biblioteca descubre y cataloga más contenido → la biblioteca se vuelve más rica y diversa → el Oyente tiene más incentivos para seguir alimentándola. (Nota: este bucle tiene un límite natural impuesto por la capacidad de almacenamiento del dispositivo — ver Sección 5.2).
  - **Bucle de Depuración de Biblioteca:** El Oyente identifica archivos no deseados en la biblioteca → los elimina directamente desde el sistema (con confirmación) → la biblioteca se vuelve más limpia y relevante → la experiencia de navegación mejora → el Oyente identifica más fácilmente otros archivos que debería eliminar. Este bucle de refuerzo permite al Oyente depurar progresivamente su colección sin recurrir a herramientas externas.
- **Bucles de Balance (regulación homeostática):**
  - **Bucle de Coherencia del Catálogo:** Cuando el Oyente modifica los metadatos de un archivo → el Motor de Biblioteca propaga el cambio al Catálogo → la organización visible de la biblioteca se actualiza inmediatamente → el Oyente percibe el resultado y puede evaluar si el cambio fue correcto. Si no fue correcto, puede volver a modificar. Este bucle actúa como termostato: la retroalimentación visual inmediata permite al Oyente ajustar hasta alcanzar el estado deseado, frenando cambios innecesarios una vez que la organización es satisfactoria.
  - **Bucle de Control de Reproducción:** El Motor de Reproducción emite eventos de estado (pista actual, progreso) → la interfaz visual refleja estos eventos → el Oyente percibe el estado y decide si intervenir (pausar, saltar, cambiar modo) → el Motor de Reproducción ajusta su comportamiento → emite nuevos eventos. Este bucle mantiene el equilibrio entre la autonomía operativa del Motor de Reproducción y el control soberano del Oyente.
  - **Bucle de Supervivencia en Segundo Plano:** El SO amenaza con terminar el proceso → el sistema negocia persistencia (Foreground Service, notificación persistente) → si la negociación es exitosa, la reproducción continúa → el sistema mantiene activa la señal de negociación. Si el SO revoca la persistencia, el sistema detecta la interrupción y se prepara para restaurar el estado cuando sea posible. Este bucle regula la tensión constante entre las políticas de ahorro de recursos del SO y la necesidad de continuidad del Motor de Reproducción.

**Retrasos en el Sistema (Delays).** Se identifican los siguientes cuellos de botella temporales entre acción y percepción del resultado:

| Retraso | Descripción | Causa | Impacto potencial |
|---|---|---|---|
| **Escaneo inicial de biblioteca** | Entre el momento en que el Oyente configura las carpetas fuente y el momento en que la biblioteca completa se presenta organizada en la interfaz. | La lectura recursiva de todas las carpetas fuente y la extracción de metadatos de cada archivo consume tiempo proporcional al volumen total de la biblioteca. | Si la biblioteca es grande (miles de archivos) o las carpetas fuente son numerosas, el Oyente podría percibir un sistema "vacío" o "incompleto" durante el escaneo. El sistema debe comunicar el progreso para evitar que el Oyente interprete el retraso como un fallo. |
| **Persistencia de metadatos modificados** | Entre el momento en que el Oyente confirma un cambio de metadatos y el momento en que el archivo de audio refleja físicamente ese cambio y el catálogo se actualiza. | La escritura de etiquetas ID3 en el archivo de audio implica operación de I/O en disco. | El retraso es generalmente imperceptible para archivos individuales, pero podría acumularse en ediciones masivas. El sistema debe reflejar el cambio en la interfaz inmediatamente (optimistamente) y persistir en segundo plano. |
| **Inicio de reproducción** | Entre el momento en que el Oyente toca "reproducir" y el momento en que el audio comienza a sonar. | Decodificación inicial del archivo, inicialización del flujo de audio, negociación con el hardware de audio. | Debe ser mínimo (sub-segundo). Un retraso perceptible aquí rompe la fluidez de la experiencia y genera la percepción de un sistema lento. |
| **Detección de archivos nuevos** | Entre el momento en que un archivo de audio nuevo aparece en alguna de las carpetas fuente y el momento en que el Catálogo lo refleja. | El sistema no tiene control en tiempo real sobre el sistema de archivos externo; depende de re-escaneos periódicos o activados por el usuario. | El Oyente podría no ver archivos recién añadidos hasta el próximo escaneo. El sistema debería ofrecer un mecanismo de re-escaneo manual como mitigación. |

**Puntos de Apalancamiento (Leverage Points).** Los siguientes son los puntos dentro de la estructura del sistema donde una intervención mínima produce un impacto desproporcionado:

- **Apalancamiento 1: La calidad de los metadatos embebidos (Nivel alto de impacto).** Este es el punto de apalancamiento más poderoso del sistema. La calidad de las etiquetas ID3 en los archivos de audio determina directamente la calidad de toda la organización objetiva de la biblioteca. Un solo cambio en el campo "género" de un archivo propaga un cambio en cómo ese archivo aparece en la navegación por géneros del Catálogo. Si el Oyente dedica un esfuerzo inicial a corregir metadatos de forma masiva, el impacto en la experiencia global de navegación es transformador — una biblioteca con metadatos completos y correctos se organiza sola de forma impecable. Inversamente, metadatos masivamente deficientes degradan la utilidad de todo el eje de organización objetiva, forzando al Oyente a depender exclusivamente de playlists manuales.
- **Apalancamiento 2: La selección de carpetas fuente (Nivel alto de impacto).** La decisión de qué carpetas incluir como fuente determina la composición completa de la biblioteca. Incluir una carpeta incorrecta (ej. la carpeta de audios de WhatsApp) contamina toda la biblioteca con contenido no deseado. Excluir una carpeta relevante deja contenido valioso fuera del sistema. Este acto de configuración inicial, que toma segundos, define el universo de contenido sobre el cual opera todo el sistema.
- **Apalancamiento 3: La estrategia de supervivencia en segundo plano (Nivel crítico de impacto).** La implementación técnica específica de cómo el Motor de Reproducción negocia con el SO para mantenerse activo determina si el sistema cumple o falla en su Equilibrio de Continuidad. Un cambio en la estrategia de Foreground Service, en la prioridad del proceso, o en el manejo de señales del SO puede significar la diferencia entre reproducción ininterrumpida y cortes constantes. Este es un punto de apalancamiento técnico donde decisiones de implementación pequeñas tienen consecuencias masivas en la experiencia percibida.
- **Apalancamiento 4: La velocidad de respuesta de la interfaz (Nivel medio-alto de impacto).** La percepción del Oyente sobre la calidad del sistema está desproporcionadamente influenciada por la latencia de la interfaz. Un sistema con biblioteca perfecta y reproducción estable pero con una interfaz que tarda 500ms en responder a cada gesto será percibido como "lento" e "inferior" a las plataformas de streaming. La fluidez de la interfaz es un multiplicador de la percepción de calidad sobre todas las demás capacidades del sistema.
- **Apalancamiento 5: El flujo de primera ejecución (Nivel medio de impacto).** La primera interacción del Oyente con el sistema — permisos, selección de carpetas fuente, escaneo inicial — define la primera impresión y establece las condiciones iniciales de toda la experiencia posterior. Un flujo de primera ejecución que sea claro, rápido y que comunique progreso durante el escaneo inicial genera confianza. Un flujo confuso, lento o silencioso genera abandono inmediato. Este momento es una ventana de apalancamiento temporal: ocurre una sola vez pero su impacto es permanente.

**Propiedades Emergentes.** Los siguientes comportamientos globales emergen del sistema en funcionamiento continuo y no pueden deducirse observando a los agentes o entidades de forma aislada:

- **Emergencia 1: La Biblioteca Personalizada como Reflejo del Oyente.** Con el uso sostenido, la combinación de metadatos corregidos, playlists creadas y archivos depurados produce un artefacto que ningún agente individual creó intencionalmente: una biblioteca de audio que es un reflejo fiel de los gustos, hábitos y contextos de vida del Oyente. Esta biblioteca personalizada no es el resultado de un algoritmo de recomendación ni de una curación editorial — es la sedimentación orgánica de miles de micro-decisiones del Oyente (corregir un género aquí, crear una playlist allá, eliminar un archivo que ya no le interesa). El sistema no diseña esta personalización; la facilita y la preserva.
- **Emergencia 2: Ritualización de la Escucha.** Cuando el sistema cumple consistentemente sus tres equilibrios (organización, continuidad, soberanía), emerge un patrón de comportamiento en el Oyente que no se puede predecir observando los componentes: la escucha de audio se ritualiza. El Oyente desarrolla rutinas asociadas al sistema — la playlist de la mañana, los podcasts del transporte, la música para concentrarse. Estas rutinas no están codificadas en el sistema; emergen de la confiabilidad y accesibilidad que el sistema provee. Si el sistema falla en continuidad (cortes frecuentes) o en organización (no encuentra lo que busca), la ritualización no ocurre y el sistema se vuelve prescindible.
- **Emergencia 3: Entropía Decreciente Progresiva.** A medida que los bucles de refuerzo operan (refinamiento de metadatos, enriquecimiento de playlists, depuración de biblioteca), la entropía global del sistema decrece progresivamente sin una intervención deliberada de "limpieza total". Cada sesión de uso deja la biblioteca en un estado ligeramente más ordenado que la sesión anterior. Este fenómeno de auto-organización gradual es una propiedad emergente de la interacción entre las tres vías de equifinalidad y los bucles de balance — ningún agente individual lo produce, pero el sistema como totalidad lo exhibe.
- **Emergencia 4: Tensión Permanente con el Entorno Operativo.** Del enfrentamiento continuo entre el Motor de Reproducción (que necesita persistir) y el SO (que necesita liberar recursos) emerge una dinámica de tensión permanente que no se resuelve — solo se gestiona. Esta tensión no es un defecto del sistema; es una propiedad inherente de operar dentro de un entorno hostil a procesos de larga duración. El rendimiento percibido por el Oyente es el resultado neto de esta negociación constante e invisible.

### 4.2. Absorción de Anomalías (Resiliencia)

*Mecanismos de contingencia del sistema ante perturbaciones externas o internas para recuperar el equilibrio.*

Aplicando la Ley de la Variedad Requerida, el sistema debe disponer de mecanismos de control cuya complejidad iguale o supere la complejidad de las perturbaciones que puede enfrentar. Se identifican las siguientes perturbaciones y sus mecanismos de absorción:

- **Perturbación 1: Archivos de audio corruptos o en formato no soportado.**
  - *Variedad de la perturbación:* Un archivo puede estar truncado, codificado en un formato desconocido, o dañado internamente.
  - **Mecanismo:** El Motor de Biblioteca indexa el archivo con los metadatos que pueda extraer, marcándolo como potencialmente problemático. El Motor de Reproducción, al intentar decodificarlo, detecta el error, **salta automáticamente a la siguiente pista** de la cola, y notifica al Oyente del problema sin interrumpir el flujo de escucha. El sistema no colapsa ante un archivo individual defectuoso.
- **Perturbación 2: Metadatos ausentes, incompletos o incoherentes.**
  - *Variedad de la perturbación:* Un archivo puede carecer completamente de etiquetas ID3, tener campos vacíos, o contener valores inconsistentes (ej. género "Unknown").
  - **Mecanismo:** El Motor de Biblioteca **nunca inventa datos**. Indexa el archivo con la información disponible, utilizando valores explícitos de "Sin información" para campos ausentes. El archivo aparece en la biblioteca bajo categorías genéricas ("Sin género", "Sin artista") hasta que el Oyente decida corregirlo. La organización degrada de manera grácil, no catastrófica.
- **Perturbación 3: Terminación forzada del proceso por el SO (MIUI/HyperOS).**
  - *Variedad de la perturbación:* El sistema operativo puede terminar el proceso del Motor de Reproducción en cualquier momento para liberar recursos.
  - **Mecanismo:** El sistema implementa una estrategia de defensa en profundidad: (1) se registra como Foreground Service con notificación persistente para minimizar la probabilidad de terminación, (2) persiste periódicamente el estado de reproducción (pista actual, posición, cola) para poder restaurarlo en caso de terminación, (3) si es terminado y reiniciado, restaura el último estado conocido y notifica al Oyente. La resiliencia ante esta perturbación es crítica dado el perfil del dispositivo objetivo.
- **Perturbación 4: Desaparición de archivos del sistema de archivos.**
  - *Variedad de la perturbación:* Un archivo previamente indexado puede ser eliminado, movido o renombrado fuera del control de Sonus (por el usuario desde un explorador de archivos, otra aplicación, o por conexión USB al computador).
  - **Mecanismo:** Cuando el Motor de Reproducción intenta reproducir un archivo que ya no existe, detecta el error y salta a la siguiente pista sin interrumpir el flujo de escucha. En el siguiente escaneo, el Motor de Biblioteca elimina del Catálogo las entradas cuyos archivos ya no existen en el sistema de archivos, y **elimina automáticamente todas las referencias a esos archivos en las agrupaciones personalizadas (playlists)**. El sistema solo muestra archivos que existen físicamente en el dispositivo; no conserva rastros de archivos inexistentes.
- **Perturbación 5: Cambio en las carpetas fuente o reorganización masiva de archivos.**
  - *Variedad de la perturbación:* El Oyente puede agregar, quitar o modificar las carpetas fuente configuradas, o reorganizar externamente la estructura de archivos dentro de ellas.
  - **Mecanismo:** El sistema ejecuta un re-escaneo completo de todas las carpetas fuente vigentes, reconstruyendo el Catálogo contra el nuevo estado del sistema de archivos. Las playlists se preservan en la medida en que los archivos referenciados sigan existiendo (identificados por ruta); las referencias a archivos que ya no existen se eliminan automáticamente. Si un archivo cambió de ubicación pero mantiene el mismo nombre y metadatos, el sistema lo trata como un archivo nuevo (no intenta hacer coincidencia heurística, manteniéndose predecible).
- **Perturbación 6: Interrupción del foco de audio por otra aplicación.**
  - *Variedad de la perturbación:* Otra aplicación del dispositivo (llamada telefónica entrante, app de navegación con instrucciones de voz, otra app de audio) solicita el foco de audio del sistema operativo, lo cual puede ocurrir en cualquier momento durante la reproducción activa.
  - **Mecanismo:** El Motor de Reproducción monitorea las señales de cambio de foco de audio del SO. Cuando otra aplicación solicita el foco, el Motor de Reproducción pausa la reproducción (o reduce el volumen temporalmente si la interrupción es transitoria). Cuando el foco de audio es devuelto, el Motor de Reproducción puede reanudar la reproducción automáticamente según el tipo de interrupción (ej. reanudar tras una notificación breve, pero no tras una llamada prolongada). El audio de Sonus nunca se superpone al de una aplicación con prioridad de audio.
- **Perturbación 7: Desconexión de la salida de audio activa.**
  - *Variedad de la perturbación:* El Oyente desconecta los auriculares con cable, o el dispositivo Bluetooth de audio se desvincula, se apaga o sale del alcance durante la reproducción activa.
  - **Mecanismo:** El Motor de Reproducción detecta la señal de desconexión de salida de audio (broadcast `BECOMING_NOISY` en Android) y pausa automáticamente la reproducción para evitar que el audio se emita inesperadamente por el altavoz del dispositivo. La reproducción permanece pausada hasta que el Oyente la reanude explícitamente. Este comportamiento protege al Oyente de situaciones socialmente incómodas (ej. música sonando por el altavoz en un lugar público).

**Capacidad de Adaptación (Aprendizaje Sistémico).** Sonus es un sistema de Fase 1 diseñado para un usuario único y un dispositivo específico, pero su arquitectura debe contemplar mecanismos de adaptación que le permitan evolucionar sin reescritura:

- *Adaptación a Cambios en el Entorno Operativo:*
  - **Evolución del SO:** Las políticas de gestión de recursos de Android, MIUI y HyperOS cambian con cada versión. La estrategia de supervivencia en segundo plano (Apalancamiento 3) no puede ser estática — debe ser un componente aislable y actualizable que pueda ajustarse a nuevas restricciones del SO sin afectar al resto del sistema. El día que MIUI cambie su política de Foreground Services, solo este componente debería necesitar modificación.
  - **Evolución de formatos de audio:** Nuevos formatos de audio y esquemas de metadatos pueden surgir. El Motor de Reproducción y el Motor de Biblioteca deben estar diseñados para que agregar soporte para un nuevo formato sea una extensión, no una modificación estructural.
  - **Cambio de dispositivo:** El Oyente puede migrar de un Xiaomi 14T Pro a otro dispositivo Android con políticas de gestión de recursos diferentes. El sistema debe poder operar en cualquier dispositivo Android compatible sin modificaciones, ajustando únicamente la estrategia de supervivencia al perfil del nuevo SO.
- *Adaptación a la Escala (Preparación para Fases 2 y 3):*
  - **De usuario único a múltiples usuarios (Fase 2):** La arquitectura actual asume un solo Oyente. Para soportar múltiples usuarios (ej. compartir con amigos), el sistema necesitaría evolucionar su modelo de Configuración y Playlists hacia estructuras por perfil. Esta evolución debe ser posible sin alterar los Motores de Biblioteca ni de Reproducción.
  - **De dispositivo específico a distribución amplia (Fase 3):** Para publicación en tienda de aplicaciones, el sistema debe poder adaptarse a diferentes tamaños de pantalla, densidades de píxeles y capacidades de hardware sin reescritura de la lógica de dominio. La interfaz visual debe ser el componente que absorba esta variabilidad, manteniendo intactos los agentes lógicos.
- *Lo que Sonus NO aprende:* El sistema no implementa aprendizaje automático, algoritmos de recomendación, ni patrones de uso inferidos. No analiza los hábitos del Oyente para sugerir contenido, no predice qué quiere escuchar, y no reorganiza la biblioteca basándose en frecuencia de uso. Esta es una decisión deliberada derivada de la Invariante 3 (Soberanía Irrestricta del Oyente): el sistema es una herramienta obediente, no un asistente anticipatorio. Su adaptación es estructural y arquitectónica, nunca comportamental ni predictiva.

## 5. Leyes y Restricciones (Los Límites)

### 5.1. Reglas Absolutas (Invariantes)

*Leyes fundamentales e inquebrantables del dominio operativo; si se rompen, invalidan el modelo.*

Las siguientes leyes son inquebrantables. Si alguna se viola, el sistema deja de ser Sonus y se convierte en otra cosa:

- **Invariante 1: Autarquía Absoluta (Cero Dependencias Externas).** El sistema no establece, bajo ninguna circunstancia ni para ninguna funcionalidad, comunicación con servidores externos, servicios en la nube, APIs remotas o cualquier recurso que requiera conexión de red. Toda operación — descubrimiento, indexación, reproducción, edición de metadatos, gestión de playlists, configuración — se ejecuta exclusivamente con recursos locales del dispositivo. No existe un "modo online" ni una "funcionalidad premium remota". Si esta invariante se rompe, se invalida la necesidad fundamental de soberanía y privacidad total (N1).
- **Invariante 2: Fidelidad al Sistema de Archivos.** El Catálogo de Biblioteca es un reflejo fiel de la realidad del sistema de archivos. El sistema solo presenta al Oyente archivos de audio que existen físicamente en las carpetas fuente configuradas. No conserva entradas fantasma, referencias a archivos eliminados, ni rastros de contenido que ya no existe. Si un archivo desaparece del almacenamiento, desaparece del sistema — del Catálogo, de las playlists, de la cola de reproducción. La verdad del sistema de archivos es la verdad de Sonus.
- **Invariante 3: Soberanía Irrestricta del Oyente y Privacidad Absoluta.** El Oyente es la única autoridad sobre el sistema. Ningún proceso lógico, heurística interna o mecanismo automático puede tomar decisiones que contradigan, restrinjan o anulen la voluntad del Oyente. El sistema no impone curaciones editoriales, recomendaciones no solicitadas, ni reorganizaciones automáticas que el usuario no haya iniciado explícitamente. La organización de la biblioteca es resultado de los metadatos existentes y de las acciones deliberadas del Oyente, nunca de inferencias del sistema. Adicionalmente, el sistema no recopila, analiza, almacena ni procesa datos de comportamiento del Oyente (frecuencia de escucha, patrones de uso, historiales de reproducción con fines analíticos, métricas de interacción). La información que el sistema persiste se limita estrictamente a lo necesario para su operación funcional (configuración, playlists, estado de reproducción, catálogo), nunca para perfilar al usuario.
- **Invariante 4: No Invención de Datos.** El Motor de Biblioteca nunca infiere, genera, ni completa metadatos faltantes. Si un archivo no tiene género, su género es "Sin información", no una suposición algorítmica. Si no tiene carátula, se muestra sin carátula. La integridad informacional prevalece sobre la estética. Solo el Oyente tiene la autoridad de completar o corregir metadatos.
- **Invariante 5: Irreversibilidad Consciente.** Toda acción destructiva e irreversible (eliminación de un archivo de audio del dispositivo) requiere confirmación explícita del Oyente antes de ejecutarse. El sistema nunca elimina archivos de audio de forma autónoma ni silenciosa. La eliminación automática aplica exclusivamente a registros internos del sistema (entradas del Catálogo, referencias en playlists) cuando el archivo físico ya no existe, pero nunca al archivo físico en sí.
- **Invariante 6: Continuidad de Reproducción como Prioridad Operativa.** Una vez que el Oyente inicia la reproducción, el Motor de Reproducción tiene como mandato mantener el flujo de audio activo e ininterrumpido. Ningún error individual (archivo corrupto, formato no soportado, archivo eliminado) debe detener la reproducción de toda la cola. El sistema siempre intenta avanzar a la siguiente pista disponible antes de rendirse.
- **Invariante 7: Gratuidad Total y No Monetización.** Sonus es una herramienta gratuita, sin monetización de ningún tipo. No existe ni existirá publicidad, muros de pago, funcionalidades premium, suscripciones, compras dentro de la aplicación, ni ningún mecanismo de extracción de valor económico del Oyente. Todas las funcionalidades del sistema están disponibles de forma completa e irrestricta desde el primer uso. Si esta invariante se rompe, Sonus se convierte en parte del problema que justificó su creación — las herramientas que imponen barreras de monetización sobre funciones básicas.

### 5.2. Límites de Capacidad y Restricciones

*Restricciones finitas (físicas, lógicas o temporales) bajo las que el sistema está forzado a operar.*

El sistema opera bajo las siguientes restricciones finitas que determinan su techo operativo:

- **Restricción 1: Capacidad de Almacenamiento del Dispositivo.** La biblioteca de audio está limitada por el espacio de almacenamiento disponible en el dispositivo. Sonus no controla este límite — es una restricción impuesta por el entorno. El sistema no puede expandir la biblioteca más allá de lo que el almacenamiento físico permite. Debe operar de forma eficiente para no consumir espacio significativo con sus propios datos internos (Catálogo, configuración, datos de playlists), maximizando el espacio disponible para los archivos de audio del Oyente.
- **Restricción 2: Formatos de Audio Soportados.** El sistema solo puede decodificar y reproducir archivos de audio en los formatos que su Motor de Reproducción sea capaz de procesar. Los archivos en formatos no soportados serán indexados por el Motor de Biblioteca (si tienen metadatos extraíbles) pero no podrán ser reproducidos. Esta restricción debe ser comunicada al Oyente de forma transparente.
- **Restricción 3: Formatos de Metadatos Soportados.** La capacidad de lectura y escritura de metadatos está limitada a los estándares de etiquetado soportados (ID3v1, ID3v2, Vorbis Comments, etc., según el formato del archivo). Archivos con esquemas de metadatos propietarios o no estándar podrían no ser completamente legibles o editables.
- **Restricción 4: Recursos Computacionales del Dispositivo.** El escaneo de la biblioteca, la extracción de metadatos y la decodificación de audio consumen CPU, memoria RAM y operaciones de I/O. Bibliotecas extremadamente grandes (decenas de miles de archivos) podrían degradar la fluidez del escaneo inicial y la navegación del Catálogo. El sistema debe gestionar estos recursos de forma eficiente, priorizando la responsividad de la interfaz y la continuidad de la reproducción sobre la completitud inmediata del escaneo.
- **Restricción 5: Políticas del Sistema Operativo.** Las restricciones más agresivas provienen del entorno operativo — específicamente las políticas de MIUI/HyperOS respecto a procesos en segundo plano. El sistema puede negociar persistencia, pero no puede garantizarla al 100% porque no controla las decisiones del SO. Esta es la restricción más impredecible y la que más amenaza el Equilibrio de Continuidad.
- **Restricción 6: Permisos del Sistema Operativo.** El sistema depende de que el Oyente otorgue permisos de acceso al almacenamiento. Si estos permisos son revocados, el sistema pierde su capacidad de operar sobre los archivos de audio. No puede forzar la concesión de permisos — solo puede solicitarlos y explicar por qué son necesarios.
