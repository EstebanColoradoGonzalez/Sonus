# Historia de Usuario

**Como** El Oyente (agente humano soberano y único usuario de Sonus),
**Quiero** conceder a Sonus el permiso de notificaciones del sistema durante la primera ejecución,
**Para** que el reproductor pueda mostrarme un control persistente en la notificación y sobrevivir en segundo plano mientras uso otras aplicaciones o bloqueo la pantalla.

## Descripción

Esta historia inicia el flujo de primera ejecución (Fundación Soberana / EPIC-01). En dispositivos con Android 13 (API 33) o superior, Sonus solicita al Oyente el permiso *runtime* `POST_NOTIFICATIONS`, indispensable para publicar la notificación persistente que ancla el `Foreground Service` del Motor de Reproducción ([RF-13] / [RNF-04] / ADR-007).

La solicitud se presenta con una pantalla de *rationale* propia que explica el porqué del permiso antes de disparar el diálogo nativo del sistema, maximizando la calidad de la primera impresión (Apalancamiento 5, SDD §4.1).

El permiso es **opcional**: si el Oyente lo niega, el onboarding continúa hacia la selección de Carpetas Fuente aplicando degradación grácil (la reproducción funcionará, pero sin controles en la notificación hasta que el permiso se conceda), respetando la soberanía irrestricta del Oyente (Invariante 3) y la política de degradación comunicada ([Restricción 6] / principio P3).

Esta historia **no** gestiona el acceso al almacenamiento: el acceso a los archivos se autoriza por carpeta mediante SAF al seleccionar las Carpetas Fuente (`US-002`). Coherente con la autarquía del sistema (Invariante 1 / [RNF-06]), en ningún momento se solicita el permiso de Internet ni permisos de media *runtime* (`READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE`).

---

## Criterios de Aceptación

### Escenario 1: Flujo Principal

- **Dado** que el Oyente abre Sonus por primera vez (`AppSettings.onboardingCompleted = false`) en un dispositivo con Android 13 (API 33) o superior y el permiso `POST_NOTIFICATIONS` aún no ha sido concedido
- **Cuando** el sistema muestra la pantalla de *rationale* explicando por qué se necesitan las notificaciones, el Oyente pulsa el botón "Permitir notificaciones" (que dispara el diálogo nativo del SO) y concede el permiso
- **Entonces** el sistema registra el permiso como concedido y avanza automáticamente al siguiente paso del flujo de primera ejecución (selección de Carpetas Fuente, `US-002`)

### Escenario 2: Validaciones

- **Dado** que el Oyente está en la pantalla de permiso de notificaciones (API 33+) con el diálogo nativo del SO visible
- **Cuando** el Oyente niega el permiso (primera negación)
- **Entonces** el flujo de primera ejecución continúa igualmente hacia la selección de Carpetas Fuente (el permiso es opcional), sin bloquearse, y el sistema comunica de forma no intrusiva que el control en la notificación no estará disponible hasta concederlo, conservando un estado coherente

### Escenario 3: Casos Extremos

- **Dado** que el Oyente abre Sonus por primera vez en un dispositivo con Android inferior a 13 (API < 33), donde `POST_NOTIFICATIONS` no existe y la notificación se permite implícitamente
- **Cuando** se ejecuta el paso de permisos del onboarding
- **Entonces** el sistema omite automáticamente la solicitud (sin mostrar diálogo de permiso) y avanza directamente a la selección de Carpetas Fuente

### Escenario 4: Negación permanente

- **Dado** que el Oyente ya negó el permiso previamente y el SO lo marca como "no volver a preguntar" (o es una segunda negación en API 33+), de modo que el diálogo nativo ya no se muestra
- **Cuando** el Oyente pulsa "Permitir notificaciones" y el diálogo del SO no aparece
- **Entonces** el sistema muestra un mensaje indicando que debe habilitar el permiso manualmente y ofrece un enlace directo a los Ajustes de notificaciones del sistema, permitiendo continuar el onboarding sin reintentar el diálogo en bucle

### Escenario 5: Permiso ya concedido (idempotencia)

- **Dado** que el permiso `POST_NOTIFICATIONS` ya se encuentra concedido al llegar al paso (p. ej. reingreso al flujo o permiso persistido)
- **Cuando** se ejecuta el paso de permisos del onboarding
- **Entonces** el sistema detecta el permiso como concedido y avanza sin mostrar el diálogo ni la pantalla de *rationale*

### Escenario 6: Autarquía verificable (sin red ni media runtime)

- **Dado** que la app solicita permisos durante la primera ejecución
- **Cuando** se inspeccionan los permisos solicitados en runtime y los declarados en el manifiesto
- **Entonces** en ningún momento se solicita `android.permission.INTERNET` ni permisos de media *runtime* (`READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE`); el único permiso *runtime* solicitado es `POST_NOTIFICATIONS`, y los permisos `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_MEDIA_PLAYBACK` se declaran como permisos de instalación (Invariante 1 / [RNF-06] / ADR-003 / ADR-010)

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Oyente — agente humano soberano y único usuario del sistema (Fase 1). Sin cuentas, roles secundarios ni jerarquías (Invariante 3).
- **Permisos requeridos:** `POST_NOTIFICATIONS` (permiso *runtime*, solo API 33+). `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PLAYBACK` se declaran en instalación y no requieren diálogo. **No** se solicita `INTERNET` ni permisos de media *runtime*.
- **Valor de negocio:** Habilita el control persistente en segundo plano ([RF-13]) y la supervivencia del Motor de Reproducción frente a las políticas agresivas del SO ([RNF-04] / ADR-007). Es el primer paso del flujo de primera ejecución, punto de apalancamiento de la primera impresión (Apalancamiento 5, SDD §4.1).

### Reglas de Negocio

- **Permiso opcional (degradación grácil):** negar el permiso NO bloquea el onboarding; el flujo continúa hacia la selección de Carpetas Fuente ([Restricción 6] / P3 / Invariante 3).
- **Dependencia de versión de SO:** en Android < 13 (API < 33) el paso se omite automáticamente porque el permiso no existe y la notificación se permite implícitamente.
- **Negación permanente:** ante "no volver a preguntar" / segunda negación, se ofrece un enlace a los Ajustes del sistema y se continúa; nunca se reintenta el diálogo en bucle.
- **Idempotencia:** si el permiso ya está concedido, el paso se omite sin mostrar UI de permiso.
- **Autarquía (Invariante 1):** jamás se solicita `INTERNET` ni permisos de media *runtime*; el acceso a archivos se autoriza por SAF por carpeta en `US-002`, no en esta historia.
- **Efecto habilitador:** el permiso concedido habilita la notificación persistente del `Foreground Service mediaPlayback` ([RF-13] / [RNF-04] / ADR-007).

### Interfaz

Primera pantalla del flujo de primera ejecución (destino "onboarding" del `NavHost` Single-Activity + Navigation Compose, contenedor C-01), presentada antes de la selección de Carpetas Fuente. En dispositivos API < 33 este destino se omite en la navegación.

#### Detalle de Interfaz de Usuario

- **Diseño general:** pantalla de *rationale* a pantalla completa dentro del onboarding: título, texto explicativo del beneficio (control persistente en segundo plano), elemento visual/ícono, un botón primario y una acción secundaria para continuar sin conceder.
- **Campos y controles:** botón primario "Permitir notificaciones" (dispara el diálogo *runtime* del SO); acción secundaria "Omitir por ahora" (continúa el onboarding sin permiso); en el caso de negación permanente, botón "Abrir Ajustes" (enlaza a la configuración de notificaciones del sistema).
- **Flujo de navegación visual:** onboarding → [pantalla de permiso de notificaciones] → selección de Carpetas Fuente (`US-002`). En API < 33 el paso de permiso se salta y se navega directo a Carpetas Fuente.
- **Mensajes y feedback:** copy de *rationale* que explica el porqué; aviso no intrusivo al negar ("Podrás activar las notificaciones más tarde en Ajustes"); mensaje con enlace a Ajustes ante negación permanente. Todos los textos se resuelven y localizan en la capa de presentación (i18n desacoplada de los datos).

### Sistemas Externos

- **Sistema Operativo Android:** API de permisos *runtime* (contrato `RequestPermission`), diálogo nativo del SO e `Intent` hacia los Ajustes de notificaciones del sistema. Acoplado al `Foreground Service mediaPlayback` del Motor de Reproducción ([RNF-04] / ADR-007). Ninguna dependencia remota (air-gapped, [RNF-06]).

### Preview de Interfaz

**Preview:** [US-001.preview.md](./US-001.preview.md) | **Formato:** mermaid (flujo de navegación)

---

## Contexto y Referencias

**Arquitectura:**
- `docs/architecture/architecture_blueprint.md` — §1.2 (permisos declarados), §1.3 (exclusiones: sin `INTERNET`, sin media runtime), §2.1 contenedor C-02, ADR-007 (Foreground Service), ADR-010 (air-gapped verificable).
- `docs/architecture/interfaces_contract.md` — §1.2 (Autenticación/Autorización delegada al SO), `TRG-CFG-02` (Completar Onboarding).
- `docs/architecture/domain_and_state_model.md` — §2 / §6.1 (`AppSettings.onboardingCompleted`, singleton que fuerza el flujo de primera ejecución).
- `docs/domain/definition/system_definition_document.md` — §4.1 (Apalancamiento 5, secuencia de arranque, prerrequisito de permisos), Invariantes 1 y 3.
- `docs/domain/definition/requirements_specification.md` — [RF-01], [RF-13], [RNF-04], [RNF-06], [Restricción 6].

**Historias relacionadas:** `US-002` (selección de Carpetas Fuente), `US-003` (escaneo fundacional), `US-004` (marcar onboarding completado), `US-025` (Foreground Service), `US-026` (controles en notificación/pantalla de bloqueo).

**Lecciones aprendidas:** N/A (primera historia del proyecto).

---

## Definición de Terminado (Inicial)

- [ ] Funcionalidad implementada según criterios de aceptación
- [ ] Validaciones funcionando correctamente (permiso opcional, negación, negación permanente, API < 33, idempotencia)
- [ ] Mensajes implementados (rationale, aviso de negación, enlace a Ajustes) y localizados en presentación
