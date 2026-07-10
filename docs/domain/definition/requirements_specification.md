# Documento de Requerimientos del Sistema (Funcionales y No Funcionales)

> Este documento traduce la definición abstracta del negocio (SDD) en mandatos accionables, evaluables y estrictos para las máquinas. Las descripciones deben ser atómicas y verificables para que el agente *Architect* y el *Peer-Reviewer* puedan usarlas como checklist.
>

## 1. Capacidades Funcionales Base (Requerimientos Funcionales)

### Dominio de Indexación y Descubrimiento (Motor de Biblioteca)

- **[RF-01] [Configuración de Carpetas Fuente]:** El sistema debe permitir al Oyente seleccionar, agregar y remover directorios específicos del sistema de archivos local que actuarán como origen exclusivo para el descubrimiento de archivos de audio.
- **[RF-02] [Escaneo y Extracción Local de ID3]:** El sistema debe iterar recursivamente sobre las carpetas fuente para identificar archivos de audio soportados y extraer sus etiquetas ID3 (título, artista, álbum, género, número de pista, año, carátula, tipo de contenido), asignando el valor explícito "Sin información" a los campos ausentes, sin realizar inferencias algorítmicas.
- **[RF-03] [Sincronización Determinista del Catálogo]:** El sistema debe actualizar el Catálogo reflejando el estado real del sistema de archivos: incorporando nuevos archivos descubiertos y eliminando automáticamente las entradas y referencias en playlists de aquellos archivos que ya no existan físicamente en las carpetas fuente.

### Dominio de Soberanía Informacional (Gestión de Metadatos y Entidades)

- **[RF-04] [Edición Directa de Metadatos Embebidos]:** El sistema debe permitir al Oyente modificar los valores de las etiquetas ID3 desde la interfaz visual y escribir estos cambios directamente en el archivo físico, propagando la actualización inmediatamente a la vista del Catálogo.
- **[RF-05] [Gestión de Agrupaciones (Playlists)]:** El sistema debe permitir al Oyente crear, renombrar, reordenar, poblar y eliminar colecciones lógicas de archivos de audio, almacenando estas referencias localmente.
- **[RF-06] [Eliminación Física con Confirmación]:** El sistema debe ejecutar la eliminación física permanente de un archivo de audio del dispositivo, previa confirmación explícita del Oyente, purgando simultáneamente su registro en el Catálogo y cualquier agrupación asociada.

### Dominio de Ejecución Acústica (Motor de Reproducción)

- **[RF-07] [Decodificación y Emisión Continua]:** El sistema debe decodificar archivos de audio locales y emitir el flujo de audio a través de las APIs del sistema operativo, permitiendo controles de reproducción estándar (play, pause, siguiente, anterior, seek progresivo).
- **[RF-08] [Gestión de Cola Multimodal]:** El sistema debe construir una secuencia de reproducción y permitir al Oyente alternar entre modos de ejecución: secuencial estricto, aleatorio (shuffle) y repetición (bucle).
- **[RF-09] [Tolerancia a Fallos de Pista]:** El sistema debe interceptar errores de decodificación o ausencia de archivos durante la reproducción activa y saltar automáticamente a la siguiente pista válida de la cola sin interrumpir el flujo general de la sesión.
- **[RF-10] [Gestión Activa del Foco de Audio]:** El sistema debe pausar automáticamente la reproducción o atenuar el volumen (ducking) al recibir señales del sistema operativo sobre la pérdida del foco de audio (llamadas, otras apps) y reanudar según las políticas de prioridad del SO.
- **[RF-11] [Corte de Seguridad Periférica]:** El sistema debe pausar instantáneamente la reproducción activa al interceptar el evento de desconexión del hardware de salida activo (audífonos cableados o dispositivos Bluetooth), requiriendo acción manual para reanudar.

### Dominio de Interfaz y Estado (Interacción)

- **[RF-12] [Navegación Taxonómica Multicapa]:** El sistema debe proveer una interfaz visual que permita filtrar y explorar el Catálogo intersectando las dimensiones extraídas (Género, Artista, Álbum, Tipo de contenido) y las Agrupaciones Personalizadas.
- **[RF-13] [Control Persistente de Segundo Plano]:** El sistema debe registrar un servicio persistente ante el SO y exponer una interfaz de control simplificada (notificación persistente/pantalla de bloqueo) que refleje el estado en tiempo real (metadatos de la pista actual, progreso y controles básicos) mientras la aplicación no esté en primer plano.
- **[RF-14] [Persistencia del Estado de Sesión]:** El sistema debe serializar y guardar localmente el estado exacto de la sesión activa (pista actual, posición temporal y cola pendiente) para permitir su restauración inmediata tras reinicios o cierres forzados por el SO.

## 2. Atributos de Calidad (Requerimientos No Funcionales)

### Categoría: Rendimiento y Latencia (Equilibrio de Organización y Soberanía)

- **[RNF-01] [Rendimiento - Latencia de Interacción Visual]:** El tiempo de respuesta entre cualquier comando de navegación del Oyente en la interfaz (exploración del catálogo, filtrado, transiciones de pantalla) y la actualización visual perceptible debe ser estrictamente sub-segundo (menor a 500ms).
- **[RNF-02] [Rendimiento - Latencia de Reproducción]:** El inicio de la emisión de flujo de audio, tras la emisión del comando "Reproducir" por parte del Oyente, debe ejecutarse en tiempo sub-segundo. Cualquier retraso provocado por la inicialización de decodificadores o negociación con el hardware debe ser imperceptible para no degradar la experiencia.
- **[RNF-03] [Rendimiento - Escaneo Asíncrono]:** Las operaciones de indexación y extracción de etiquetas ID3 realizadas por el Motor de Biblioteca deben ejecutarse en procesos de fondo (background threads) sin bloquear el hilo principal de la interfaz visual (Main/UI Thread). Si la operación toma más de 1 segundo (ej. en bibliotecas con miles de archivos), el sistema debe reportar el progreso de forma determinista para evitar la percepción de fallo.

### Categoría: Resiliencia y Continuidad Operativa (Equilibrio de Continuidad)

- **[RNF-04] [Resiliencia - Supervivencia ante el OS]:** El Motor de Reproducción debe implementar una estrategia robusta de defensa en profundidad para resistir las políticas agresivas de terminación de procesos de sistemas como MIUI o HyperOS. Esto exige obligatoriamente la ejecución mediante un proceso de alta prioridad (`Foreground Service`) anclado a una notificación persistente activa.
- **[RNF-05] [Recuperación - Restauración de Estado]:** Ante un evento inevitable de terminación forzada (OOM - Out of Memory kill), el sistema debe recuperar el estado completo de la sesión de reproducción (pista exacta, posición en milisegundos, composición de la cola y modo de reproducción) en menos de 2 segundos durante el próximo reinicio de la aplicación.

### Categoría: Seguridad, Privacidad y Aislamiento (Equilibrio de Soberanía)

- **[RNF-06] [Seguridad - Aislamiento de Red (Air-Gapped)]:** El binario final de la aplicación no debe contener, solicitar ni ejecutar permisos de acceso a Internet (`android.permission.INTERNET`). Ningún paquete de telemetría, reporte de errores (crashlytics) o petición a servidores externos puede ser compilado dentro de la aplicación base. La autarquía es absoluta.
- **[RNF-07] [Privacidad - Integridad de Datos Comportamentales]:** El sistema tiene estrictamente prohibido rastrear, persistir o procesar métricas de comportamiento del usuario (ej. frecuencia de escucha, tiempo de sesión, tasas de salto de pistas). Todo registro persistido debe limitarse exclusivamente a datos operativos necesarios (configuración, estado y catálogos).

### Categoría: Eficiencia y Portabilidad (Límites Operativos)

- **[RNF-08] [Eficiencia - Huella de Almacenamiento]:** El Catálogo de Biblioteca y las bases de datos relativas a metadatos, playlists y configuración deben estar altamente normalizados, minimizando el consumo de almacenamiento interno del dispositivo para priorizar el espacio físico destinado a los archivos de audio del usuario.
- **[RNF-09] [Portabilidad - Compatibilidad de Hardware Base]:** El sistema debe garantizar la funcionalidad completa y el cumplimiento de las invariantes en un entorno nativo de Android sin demandar la instalación de librerías propietarias externas, operando sobre las APIs de decodificación de medios provistas por la capa estándar del sistema operativo.

## 3. Glosario del Dominio (Ubiquitous Language)

- **[Oyente]:** El agente humano consciente y autoridad soberana del sistema, encargado de configurar, gobernar y consumir su colección local de audio.

- **[Archivo de Audio]:** La unidad atómica del sistema, consistente en un archivo digital físico almacenado en el almacenamiento local del dispositivo que contiene una pista codificada (sea música o podcast).

- **[Metadatos Embebidos]:** El conjunto de propiedades informacionales intrínsecas (etiquetas ID3 como título, artista, álbum, género, carátula, tipo) adheridas físicamente a un Archivo de Audio, las cuales dictan su clasificación objetiva.

- **[Motor de Biblioteca]:** El agente lógico subordinado responsable exclusivamente de escanear el sistema de archivos local, extraer los Metadatos Embebidos, construir el Catálogo y persistir las modificaciones físicas sobre los archivos.

- **[Motor de Reproducción]:** El agente lógico autónomo-operativo encargado de decodificar los Archivos de Audio, negociar el foco de audio con el sistema operativo y emitir el flujo de sonido al hardware de salida, manteniendo la persistencia en segundo plano.

- **[Catálogo de Biblioteca]:** El índice estructural e informacional que representa el estado organizado de todos los Archivos de Audio descubiertos por el sistema. Debe ser un reflejo exacto y fiel de la realidad del sistema de archivos local.

- **[Agrupación Personalizada (Playlist)]:** Una colección lógica, situacional y subjetiva creada por el Oyente que almacena referencias a Archivos de Audio existentes, permitiendo una organización independiente de los metadatos.

- **[Cola de Reproducción]:** La secuencia transitoria y efímera de Archivos de Audio pendientes de ser ejecutados por el Motor de Reproducción durante la sesión de escucha activa.

- **[Carpetas Fuente]:** Los directorios específicos del sistema de archivos del dispositivo, seleccionados explícitamente por el Oyente, que delimitan el perímetro exacto de escaneo para el Motor de Biblioteca.

- **[Equilibrios Sistémicos]:** Los tres estados fundamentales de homeostasis que el sistema está obligado a mantener: Organización (orden estructural), Continuidad (flujo ininterrumpido) y Soberanía (autarquía e independencia de redes externas).
