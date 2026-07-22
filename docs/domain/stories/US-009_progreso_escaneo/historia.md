# Historia de Usuario

**Como** El Oyente,
**Quiero** ver en tiempo real el estado y progreso de cualquier operación de escaneo activa mediante una pantalla dedicada que se presenta mientras el ciclo se ejecuta,
**Para** tener certeza de que el sistema está trabajando activamente sobre mi biblioteca, no interpretar el procesamiento como un fallo, y conocer el resultado final (archivos añadidos, purgados, no soportados y dimensiones huérfanas eliminadas) una vez concluido el escaneo.

## Descripción

US-009 materializa la retroalimentación visual del ciclo de escaneo del Motor de Biblioteca. Cuando se dispara un escaneo —ya sea por re-escaneo manual (US-007), por el escaneo fundacional (US-003) o por cualquier cambio en las carpetas fuente— el sistema transita por los estados `SCANNING → SYNCING → IDLE` emitiendo un `StateFlow<ScanState>` (canal C2, `TRG-LIB-04`). Esta historia cubre la capa de presentación que consume ese flujo y lo traduce a una pantalla dedicada que bloquea la navegación principal mientras el ciclo está activo.

La pantalla de progreso:

- **Aparece automáticamente** en cuanto el estado transita de `Idle` a `Scanning`, sin intervención del Oyente.
- **Muestra contadores deterministas** durante el estado `Scanning`: el número de archivos procesados (`processed`) y, cuando está disponible, el total estimado (`total`). Si `total` es `null` (mientras el Motor de Biblioteca aún enumera los archivos), muestra solo el contador de procesados sin denominador.
- **Indica la fase `Syncing`** con un indicador de actividad sin contadores numéricos, comunicando que el sistema está reconciliando el Catálogo (aplicando altas, bajas, purga en cascada y purga de huérfanos).
- **Presenta el `ScanSummary` completo** al concluir (`Finished`): tracks añadidos, tracks purgados, archivos no soportados y dimensiones huérfanas eliminadas. El Oyente puede cerrar la pantalla o ser redirigido a la biblioteca.
- **Comunica el aborto** si el ciclo termina en `Aborted`: muestra la causa (ej. permiso revocado) y ofrece una acción recuperable (reintento o navegación a configuración de carpetas fuente).

La UI es completamente pasiva: solo observa el `StateFlow<ScanState>` publicado por el `ScanStateEmitter` del Motor de Biblioteca (C-04) y renderiza el estado correspondiente. Ninguna operación de fondo ni de I/O se ejecuta en el hilo de presentación. La reproducción de audio activa no se interrumpe durante el escaneo.

---

## Criterios de Aceptación

### Escenario 1: Estado IDLE — pantalla de progreso no visible

- **Dado** que no hay ningún ciclo de escaneo activo (`ScanState.Idle`)
- **Cuando** el Oyente navega por cualquier parte de la aplicación
- **Entonces** la pantalla de progreso del escaneo no se muestra; no interfiere con la navegación normal

### Escenario 2: Inicio del escaneo — pantalla aparece automáticamente

- **Dado** que el sistema dispara un escaneo y el estado transita a `ScanState.Scanning`
- **Cuando** el `StateFlow<ScanState>` emite el primer valor `Scanning`
- **Entonces** la pantalla de progreso se presenta automáticamente bloqueando la navegación principal, y muestra el indicador de progreso con el contador de archivos procesados

### Escenario 3: Progreso con total conocido

- **Dado** que el Motor de Biblioteca ya terminó de enumerar los archivos y `ScanState.Scanning(processed, total)` tiene `total` distinto de `null`
- **Cuando** la pantalla de progreso observa el estado
- **Entonces** muestra ambos valores en formato "X de Y archivos procesados", permitiendo al Oyente estimar el tiempo restante

### Escenario 4: Progreso con total desconocido

- **Dado** que el Motor de Biblioteca aún está enumerando archivos y `ScanState.Scanning(processed, total)` tiene `total = null`
- **Cuando** la pantalla de progreso observa el estado
- **Entonces** muestra únicamente el contador de archivos procesados ("X archivos procesados") sin denominador, y el indicador de progreso opera en modo indeterminado

### Escenario 5: Fase de sincronización

- **Dado** que el recorrido SAF ha concluido y el estado transita a `ScanState.Syncing`
- **Cuando** la pantalla de progreso observa el cambio de estado
- **Entonces** muestra un indicador de actividad sin contadores numéricos y un mensaje que comunica que el Catálogo está siendo actualizado (altas, bajas y purga en curso)

### Escenario 6: Escaneo finalizado — ScanSummary visible

- **Dado** que el ciclo completa todas las fases y el estado transita a `ScanState.Finished(summary)`
- **Cuando** la pantalla de progreso observa el estado `Finished`
- **Entonces** muestra el `ScanSummary` completo con los cuatro contadores: tracks añadidos (`added`), tracks purgados (`purged`), archivos no reproducibles (`unsupported`) y dimensiones huérfanas eliminadas (`orphanDimsPurged`), y ofrece al Oyente la opción de cerrar la pantalla o ir a la biblioteca

### Escenario 7: Escaneo abortado — causa comunicada y acción recuperable

- **Dado** que el ciclo es interrumpido y el estado transita a `ScanState.Aborted(reason)`
- **Cuando** la pantalla de progreso observa el estado `Aborted`
- **Entonces** muestra la causa del aborto (ej. permiso revocado sobre una carpeta fuente) y presenta una acción recuperable: reintentar el escaneo o navegar a la configuración de carpetas fuente para resolver el problema

### Escenario 8: Reproducción de audio no interrumpida durante el escaneo

- **Dado** que el Motor de Reproducción está activo y emitiendo audio
- **Cuando** un escaneo se dispara y la pantalla de progreso bloquea la navegación
- **Entonces** la reproducción de audio continúa sin pausas ni interrupciones; la pantalla de progreso no afecta al Motor de Reproducción ni al Foreground Service

### Escenario 9: Hilo principal nunca bloqueado

- **Dado** que un escaneo sobre una biblioteca grande (miles de archivos) está en curso
- **Cuando** la pantalla de progreso actualiza su estado al recibir nuevas emisiones del `StateFlow<ScanState>`
- **Entonces** la UI responde a gestos del sistema (ej. botón Atrás del SO) sin congelarse; toda operación del Motor de Biblioteca ocurre en background threads y la pantalla solo renderiza el estado recibido

### Escenario 10: Actualización fluida del contador de progreso

- **Dado** que el Motor de Biblioteca está en estado `Scanning` y emite actualizaciones periódicas del contador `processed`
- **Cuando** la pantalla de progreso recibe cada nueva emisión del flujo
- **Entonces** el contador se actualiza de forma visible y fluida, sin saltos bruscos ni congelamiento de la UI, dando retroalimentación continua de que el sistema está operando

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Oyente — único agente humano del sistema, autoridad soberana absoluta
- **Permisos requeridos:** Ninguno adicional. Los permisos SAF ya fueron concedidos en US-001/US-002/US-005. Esta historia es puramente de observación
- **Valor de negocio:** Materializa el cierre perceptual del Ciclo de Escaneo (SDD §1.3). Sin esta historia, el escaneo ocurre en silencio: el Oyente no tiene visibilidad del progreso y puede interpretar el procesamiento como un cuelgue o fallo del sistema. La pantalla de progreso es el puente entre la operación técnica del Motor de Biblioteca (US-008) y la experiencia percibida del Oyente. Cumple directamente [RNF-03]: "si la operación toma más de 1 segundo, el sistema debe reportar el progreso de forma determinista para evitar la percepción de fallo". El SDD identifica el escaneo inicial como un "retraso en el sistema" cuyo impacto solo se mitiga comunicando el progreso (SDD §4.1, Retrasos del Sistema)

### Reglas de Negocio

- **[RNF-03]:** Si el escaneo supera 1 s, se emite `ScanState.Scanning(processed, total)` con progreso determinista desde el background thread. La pantalla de progreso es la concreción visual de este requerimiento
- **[CT-08]:** La interfaz nunca se bloquea. La pantalla de progreso actualiza su estado reactivamente; ninguna operación de I/O ocurre en el hilo principal
- **[CT-01]:** Sin integraciones de red. La pantalla solo consume el `StateFlow<ScanState>` local
- **[Invariante 6]:** El Motor de Reproducción no se interrumpe. La pantalla de progreso bloquea la navegación de la biblioteca pero no afecta al Foreground Service de reproducción
- **Patrón de pantalla bloqueante:** La pantalla de progreso sigue el mismo patrón del escaneo fundacional (US-003): mientras el ciclo está activo, la navegación principal queda bloqueada. El Oyente no puede interactuar con el catálogo hasta que el escaneo concluye (`Finished`) o aborta (`Aborted`)
- **Progreso determinista:** El contador `processed/total` se actualiza con cada archivo procesado. Si `total = null`, el indicador opera en modo indeterminado hasta que el Motor de Biblioteca completa la enumeración

### Interfaz

La pantalla de progreso es un destino de navegación dedicado (o un overlay a pantalla completa) que se presenta automáticamente al iniciar cualquier ciclo de escaneo. Bloquea la navegación hacia la biblioteca y demás pantallas mientras el escaneo está activo.

#### Detalle de Interfaz de Usuario

- **Diseño general:** Pantalla/overlay a pantalla completa centrada en el progreso. Sin barra de navegación inferior ni acceso al catálogo mientras el escaneo está activo. Misma identidad visual que el escaneo fundacional de US-003
- **Campos y controles:**
  - Indicador de fase actual (`Scanning` / `Syncing`)
  - Contador de progreso: "X de Y archivos" (cuando `total` conocido) o "X archivos procesados" (cuando `total = null`)
  - Barra de progreso determinada (cuando `total` conocido) o indeterminada (cuando `total = null`)
  - Al finalizar (`Finished`): tarjeta de resumen con los 4 contadores del `ScanSummary` + botón "Ver biblioteca"
  - Al abortar (`Aborted`): mensaje de causa + botones "Reintentar" y "Configurar carpetas"
- **Flujo de navegación visual:** `Disparador de escaneo (US-007 / US-005 / US-006)` → `Pantalla de Progreso (Scanning)` → `Pantalla de Progreso (Syncing)` → `Pantalla de Resumen (Finished)` → `Biblioteca`. En caso de aborto: `Pantalla de Progreso (Scanning / Syncing)` → `Pantalla de Error (Aborted)` → `Reintento` o `Configuración`
- **Mensajes y feedback:**
  - Estado `Scanning`: "Escaneando biblioteca… X de Y archivos" / "Escaneando biblioteca… X archivos procesados"
  - Estado `Syncing`: "Actualizando catálogo…"
  - Estado `Finished`: "Escaneo completado — [N] pistas añadidas · [N] purgadas · [N] no soportadas"
  - Estado `Aborted`: "Escaneo interrumpido — [causa localizada]" + acciones recuperables

### Sistemas Externos

- **`StateFlow<ScanState>` (Canal C2, `TRG-LIB-04`):** única fuente de datos de la pantalla. Emitido por el `ScanStateEmitter` del Motor de Biblioteca (C-04). La UI solo observa; no escribe ni modifica ningún estado
- **Sin integraciones externas:** sistema autárquico ([Invariante 1 / RNF-06]). Ninguna llamada de red

### Preview de Interfaz

```
┌─────────────────────────────────────────┐
│                                         │
│         🔍 Escaneando biblioteca        │
│                                         │
│   ████████████████░░░░░░░  1.240 / 2.100│
│         archivos procesados             │
│                                         │
│   Fase: Recorriendo carpetas fuente     │
│                                         │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│                                         │
│   ✅ Escaneo completado                 │
│                                         │
│   ＋ 48  pistas añadidas                │
│   ✕  12  pistas purgadas               │
│   ⚠   3  archivos no soportados        │
│   🗑   5  dimensiones huérfanas         │
│                                         │
│          [ Ver biblioteca ]             │
│                                         │
└─────────────────────────────────────────┘
```

---

## Contexto y Referencias

**Arquitectura:**
- `docs/architecture/domain_and_state_model.md` — Ciclo del Proceso de Escaneo §5.3 (`IDLE → SCANNING → SYNCING → IDLE`)
- `docs/architecture/interfaces_contract.md` — `TRG-LIB-04` (Progreso de Escaneo), `ScanState`, `ScanSummary`; §4.2 Presupuestos de Latencia (escaneo > 1s)
- `docs/architecture/architecture_blueprint.md` — `ScanStateEmitter` (C-04), `LibraryScanWorker`, contenedor C-01 (Presentación / Jetpack Compose)
- `docs/domain/definition/requirements_specification.md` — [RNF-03] (Escaneo Asíncrono con reporte de progreso)
- `docs/domain/definition/system_definition_document.md` — Retraso "Escaneo inicial de biblioteca" (SDD §4.1), Equilibrio de Organización (SDD §1.2), Ciclo de Escaneo §1.3

**Historias relacionadas:**
- **US-003** (Escaneo Fundacional) — define el patrón de pantalla de progreso bloqueante; US-009 extiende ese patrón a re-escaneos manuales y cambios de carpetas fuente
- **US-007** (Ejecutar Escaneo / Re-escaneo) — dispara el ciclo que US-009 observa; la pantalla de progreso se presenta automáticamente al confirmar el re-escaneo
- **US-008** (Sincronización Determinista del Catálogo) — emite el `ScanState`/`ScanSummary` que US-009 consume y presenta al Oyente
- **US-005 / US-006** (Agregar / Remover Carpeta Fuente) — también disparan escaneos que US-009 debe observar

**Lecciones aprendidas:** US-008 explicita que "La representación visual del progreso y del resultado final del escaneo es responsabilidad exclusiva de US-009". El contrato del `ScanState` ya está definido y estabilizado en `TRG-LIB-04`; esta historia solo consume ese contrato sin extenderlo.

---

## Definición de Terminado (Inicial)

- [ ] Funcionalidad implementada según los 10 criterios de aceptación
- [ ] Pantalla de progreso visible al dispararse cualquier escaneo (US-003, US-007, cambio de carpetas fuente)
- [ ] Estado `Scanning` con `total` conocido: muestra "X de Y archivos" con barra determinada
- [ ] Estado `Scanning` con `total = null`: muestra "X archivos procesados" con barra indeterminada
- [ ] Estado `Syncing`: indicador de actividad sin contadores, mensaje de actualización de catálogo
- [ ] Estado `Finished`: `ScanSummary` completo con los 4 contadores y botón "Ver biblioteca"
- [ ] Estado `Aborted`: causa localizada + botones "Reintentar" y "Configurar carpetas"
- [ ] Pantalla bloqueante: sin acceso a la navegación de biblioteca mientras escaneo activo
- [ ] Motor de Reproducción no interrumpido (audio sigue activo durante el escaneo)
- [ ] Hilo principal nunca bloqueado; UI responde a gestos del SO en todo momento ([RNF-03] / [CT-08])
- [ ] Sin permiso `android.permission.INTERNET` compilado en el binario ([RNF-06] / [CT-01])
