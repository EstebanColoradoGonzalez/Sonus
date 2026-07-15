# Historia de Usuario

**Como** El Oyente (agente humano soberano y único usuario de Sonus),
**Quiero** que, una vez completado el flujo de primera ejecución (permisos, Carpetas Fuente y escaneo fundacional), el sistema registre de forma persistente que el onboarding ya finalizó,
**Para** que en los arranques posteriores la aplicación me lleve directamente a mi biblioteca sin repetir la configuración inicial.

## Descripción

Esta historia es el **cuarto y último paso** del flujo de primera ejecución (Fundación Soberana / EPIC-01), que ocurre inmediatamente después de que el Motor de Biblioteca completa el escaneo fundacional (`US-003`) y el sistema transita a la vista principal de la biblioteca.

En este momento el sistema materializa el **Apalancamiento 5 (SDD §4.1)**: el flujo de primera ejecución es una ventana temporal que ocurre una sola vez pero cuyo impacto es permanente. Marcar el onboarding como completado es el acto que cierra esa ventana. Concretamente, el sistema emite el comando `SettingsCommand.CompleteOnboarding` (`TRG-CFG-02`), que a través del `CompleteOnboardingUseCase` fija `AppSettings.onboardingCompleted = true` en el singleton de configuración (`AppSettings`, fila `id = 1`) persistido en Room vía `SettingsRepository` → `SettingsDao`. La operación es **idempotente**: fijarla cuando ya vale `true` es un *no-op* sin efecto observable.

El disparo es **robusto respecto al camino recorrido**: el onboarding se marca como completado en la transición a la biblioteca sin importar la rama que la haya originado — escaneo con pantalla de progreso (`US-003`, Escenario 1) o biblioteca pequeña que transita en menos de 1 segundo (`US-003`, Escenario 2). En ambos casos el efecto se aplica **una sola vez** gracias a la idempotencia, garantizando un estado coherente.

El cierre del onboarding es un **efecto de estado silencioso**: no introduce pantalla, mensaje ni elemento visual propio. El feedback perceptible ya lo provee la transición a la biblioteca de `US-003`. Esto respeta la **huella mínima** ([RNF-08]) y evita fricción innecesaria en la primera impresión.

El valor persistido es **estrictamente operativo**: `onboardingCompleted` es un interruptor de arranque, no un dato comportamental. No cuenta ejecuciones, no registra fechas de uso, no perfila al Oyente ([RNF-07] / Invariante 3). En arranques posteriores, el contenedor de navegación (`SonusNavHost`) lee `onboardingCompleted` al inicio y, cuando es `true`, fija el destino de arranque directamente en la biblioteca, **omitiendo** los destinos de permisos, Carpetas Fuente y escaneo. Cuando es `false` (primera ejecución o dato aún sin sembrar como `true`), el arranque sigue el flujo de onboarding completo.

Esta historia **no** implementa el escaneo (`US-003`), la selección de Carpetas Fuente (`US-002`) ni la concesión de permisos (`US-001`); tampoco gestiona la revocación posterior de permisos ni el re-escaneo. Su alcance es exclusivamente la **persistencia del cierre del onboarding** y el **gating del destino de arranque** en función de ese estado.

---

## Criterios de Aceptación

### Escenario 1: Flujo Principal — cierre del onboarding al transitar a la biblioteca

- **Dado** que el Oyente completó los pasos previos del flujo de primera ejecución (permisos `US-001`, Carpetas Fuente `US-002`, escaneo fundacional `US-003`) y el sistema transita a la vista principal de la biblioteca
- **Cuando** se produce la transición a la biblioteca
- **Entonces** el sistema emite `SettingsCommand.CompleteOnboarding` (`TRG-CFG-02`) y persiste `AppSettings.onboardingCompleted = true` en el singleton `id = 1` vía `SettingsRepository`; la transición a la biblioteca ocurre de forma normal, sin bloquearse por la operación de persistencia

### Escenario 2: Idempotencia — marcar cuando ya está completado

- **Dado** que `AppSettings.onboardingCompleted` ya vale `true`
- **Cuando** se vuelve a emitir `SettingsCommand.CompleteOnboarding`
- **Entonces** la operación es un *no-op*: el estado permanece en `true`, no se produce ningún efecto secundario observable ni error, y no se emite evento alguno (contrato `TRG-CFG-02`, *fire-and-forget*)

### Escenario 3: Gating de arranque — arranques posteriores omiten el onboarding

- **Dado** que en un arranque previo el onboarding quedó marcado como completado (`onboardingCompleted = true`)
- **Cuando** el Oyente vuelve a abrir la aplicación
- **Entonces** `SonusNavHost` lee `onboardingCompleted` al inicio y fija el destino de arranque directamente en la biblioteca, omitiendo los destinos de permisos (`US-001`), Carpetas Fuente (`US-002`) y escaneo (`US-003`); el Oyente ve su biblioteca sin repetir la configuración inicial

### Escenario 4: Robustez del disparo — ambos caminos de transición marcan una sola vez

- **Dado** que la transición a la biblioteca puede originarse por dos ramas: escaneo con pantalla de progreso (`US-003`, Escenario 1) o biblioteca pequeña que transita en menos de 1 segundo (`US-003`, Escenario 2)
- **Cuando** se alcanza la biblioteca por cualquiera de las dos ramas
- **Entonces** `onboardingCompleted` queda en `true` de forma consistente y el efecto se aplica **una sola vez** (gracias a la idempotencia del Escenario 2), sin depender de la rama recorrida

### Escenario 5: Validación — primera ejecución mantiene el flujo completo

- **Dado** que es la primera ejecución de la aplicación y `AppSettings.onboardingCompleted` vale `false` (valor por defecto del singleton sembrado en el Big Bang, §6.1 modelo de dominio)
- **Cuando** el sistema arranca
- **Entonces** `SonusNavHost` fija el destino de arranque en el flujo de onboarding (permisos → Carpetas Fuente → escaneo), sin omitir ningún paso; el onboarding solo se marcará como completado al concluir el flujo (Escenario 1)

### Escenario 6: Degradación grácil — fallo al persistir el estado

- **Dado** que al intentar persistir `onboardingCompleted = true` ocurre un fallo en la capa de almacenamiento (Room)
- **Cuando** el `SettingsRepository` detecta el error
- **Entonces** el fallo se captura en el borde y se mapea a un `DomainError` tipado transportado por `OperationResult.Failure` (P1 / §3); **nunca** cruza la frontera de dominio como excepción de control; la transición a la biblioteca no se aborta por este fallo (degradación grácil, P3); el estado incoherente se resolverá en el próximo intento, dado que la operación es idempotente y reintentable

### Escenario 7: Privacidad — estado operativo, no dato comportamental

- **Dado** que el sistema persiste el cierre del onboarding
- **Cuando** se inspecciona lo que se almacena
- **Entonces** el único dato persistido es el interruptor booleano `onboardingCompleted` en el singleton `AppSettings`; **no** se registran conteos de ejecución, fechas de uso, historiales ni métrica alguna de comportamiento del Oyente ([RNF-07] / Invariante 3); el cierre del onboarding es un efecto silencioso sin UI propia ([RNF-08])

### Escenario 8: Autarquía — sin red durante la operación

- **Dado** que el cierre del onboarding se persiste localmente
- **Cuando** se inspeccionan los mecanismos empleados
- **Entonces** la operación se resuelve íntegramente con recursos locales del dispositivo (Room/SQLite); en ningún momento se establece comunicación de red ni se reporta el evento a servicio externo alguno (Invariante 1 / [RNF-06])

---

## Información Recopilada

### Usuario y Contexto

- **Tipo de usuario:** El Oyente — agente humano soberano y único usuario del sistema (Fase 1). El `SettingsRepository` es el agente lógico que persiste el estado de configuración.
- **Permisos requeridos:** Ninguno adicional. No se solicitan permisos del sistema operativo para persistir configuración local; los permisos de árbol SAF ya fueron concedidos en pasos previos (`US-001`/`US-002`).
- **Valor de negocio:** Cierra el **flujo de primera ejecución** (Apalancamiento 5, SDD §4.1), transformando una ventana de configuración de una sola vez en un estado permanente. Evita que el Oyente repita permisos, selección de Carpetas Fuente y escaneo en cada arranque, reduciendo la fricción a cero tras el primer uso y consolidando la primera impresión de un sistema que "recuerda" su estado. Sin este paso, el sistema no distinguiría un arranque inicial de uno recurrente.

### Reglas de Negocio

- **Cierre del onboarding (Apalancamiento 5, SDD §4.1):** el onboarding se marca como completado al concluir el flujo de primera ejecución (permisos → Carpetas Fuente → escaneo inicial), en la transición a la biblioteca.
- **Idempotencia (`TRG-CFG-02`):** emitir `CompleteOnboarding` cuando `onboardingCompleted` ya vale `true` es un *no-op*; sin efecto secundario ni error.
- **Disparo robusto por ambas ramas:** el efecto se aplica en la transición a la biblioteca tanto si vino de un escaneo con progreso como de una biblioteca pequeña (<1s), garantizando un único resultado coherente.
- **Efecto silencioso ([RNF-08]):** el cierre del onboarding no introduce pantalla, mensaje ni elemento visual propio; el feedback ya lo aporta la transición a la biblioteca (`US-003`).
- **Gating de arranque:** en cada arranque, el sistema decide el destino inicial según `onboardingCompleted` — `true` → biblioteca directa (omite permisos/Carpetas Fuente/escaneo); `false` → flujo de onboarding completo.
- **Estado operativo, no comportamental ([RNF-07] / Invariante 3):** `onboardingCompleted` es un interruptor de arranque; no se persiste ningún dato de comportamiento (conteos, fechas, historiales).
- **Singleton de configuración:** `AppSettings` es una fila única `id = 1`; el valor por defecto es `onboardingCompleted = false`, sembrado en el Big Bang (§6.1 modelo de dominio).
- **Errores como valores (P1 / P3):** un fallo de persistencia se captura en el borde, se mapea a `DomainError` vía `OperationResult.Failure` y degrada de forma grácil sin abortar la transición a la biblioteca; la operación es reintentable por ser idempotente.
- **Frontera de alcance:** esta historia persiste el cierre del onboarding y gobierna el destino de arranque; **no** implementa permisos (`US-001`), Carpetas Fuente (`US-002`) ni escaneo (`US-003`), ni gestiona la revocación posterior de permisos o el re-escaneo.

### Interfaz

Esta historia **no introduce una pantalla propia**. Opera como un efecto de estado transversal al flujo de primera ejecución del `NavHost` Single-Activity + Navigation Compose (contenedor C-01): (1) al **transitar** desde el escaneo (`US-003`) a la biblioteca, dispara la persistencia del cierre del onboarding; y (2) al **arrancar** la aplicación, condiciona cuál es el destino inicial (`onboarding` vs. `library`) según `onboardingCompleted`. El cambio observable para el Oyente es únicamente la ausencia del flujo de onboarding en arranques posteriores.

### Sistemas Externos

- **Room/SQLite (C-03):** destino de escritura del estado; tabla `app_settings`, singleton `id = 1`, campo `onboardingCompleted`. Escritura vía `SettingsDao` a través de `SettingsRepository` (contrato §2.6). Lectura en el arranque para el gating de navegación.
- **Capa de Presentación — `SonusNavHost` (C-01):** consume `onboardingCompleted` para fijar el `startDestination` del grafo de navegación (biblioteca vs. onboarding). El `OnboardingViewModel`/flujo emite `SettingsCommand.CompleteOnboarding` al cerrar el flujo.
- **Caso de Uso — `CompleteOnboardingUseCase`:** orquesta el comando `TRG-CFG-02` sobre el `SettingsRepository` (blueprint §4, módulo "Configuración y Onboarding").

### Preview de Interfaz

**Preview:** [US-004.preview.md](./US-004.preview.md) | **Formato:** mermaid (flujo de decisión de arranque y disparo de cierre)

---

## Contexto y Referencias

**Arquitectura:**
- `docs/architecture/architecture_blueprint.md` — §2.1 contenedor C-01 (capa de presentación, `SonusNavHost`, `OnboardingViewModel`), §2.6-relacionado `SettingsRepository` (preferencias y onboarding), §4 módulo "Configuración y Onboarding" (`CompleteOnboardingUseCase` → `SettingsRepository` → `SettingsDao`, `TRG-CFG-01/02`), ADR-005 (Single-Activity + Navigation Compose).
- `docs/architecture/interfaces_contract.md` — `TRG-CFG-02` (Completar Flujo de Primera Ejecución: comando `SettingsCommand.CompleteOnboarding`, idempotencia, efecto en arranques posteriores), §2.6 (módulo AppSettings, canal C1), §3.0 (P1 errores como valores, P3 degradación grácil, P5 cero telemetría).
- `docs/architecture/domain_and_state_model.md` — §2 (`AppSettings` singleton `id = 1`, campo `onboardingCompleted: Boolean` `@default(false)`), §6.1 (Big Bang: fila singleton con `onboardingCompleted = false`, `themePreference = SYSTEM`).
- `docs/domain/definition/system_definition_document.md` — §4.1 (secuencia de arranque, Apalancamiento 5 — flujo de primera ejecución), §5.1 Invariante 1 (autarquía) e Invariante 3 (soberanía y privacidad; sin datos comportamentales).
- `docs/domain/definition/requirements_specification.md` — [RNF-06] (air-gapped), [RNF-07] (integridad de datos comportamentales), [RNF-08] (huella de almacenamiento mínima).

**Historias relacionadas:** `US-001` (permisos, primer paso del onboarding), `US-002` (Carpetas Fuente, segundo paso), `US-003` (escaneo fundacional y transición a biblioteca, precede directamente), `US-039` (preferencia de tema, comparte el módulo `AppSettings` / `TRG-CFG-01`).

**Lecciones aprendidas:** N/A.

---

## Definición de Terminado (Inicial)

- [ ] Funcionalidad implementada según criterios de aceptación (persistencia de `onboardingCompleted = true` en la transición a biblioteca vía `TRG-CFG-02`; gating del destino de arranque)
- [ ] Idempotencia verificada (re-emitir `CompleteOnboarding` con estado ya `true` es *no-op*)
- [ ] Gating de arranque funcionando (arranques posteriores con `onboardingCompleted = true` van directo a biblioteca; primera ejecución mantiene el flujo completo)
- [ ] Robustez del disparo verificada (ambas ramas de transición de `US-003` marcan el estado una sola vez)
- [ ] Degradación grácil ante fallo de persistencia (error como valor `DomainError`/`OperationResult.Failure`; no aborta la transición; reintentable)
- [ ] Privacidad y huella verificadas (solo estado operativo, sin dato comportamental [RNF-07]; efecto silencioso sin UI propia [RNF-08])
- [ ] Autarquía verificada (persistencia local Room; sin red, sin reporte externo — Invariante 1 / [RNF-06])
