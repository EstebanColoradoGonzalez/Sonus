# Refinamiento Técnico (Developer)

**Autor**: Esteban Colorado González | **Fecha**: 2026-07-21

## Plan: Observar el Estado y Progreso del Escaneo (US-009)

**Arquitectura**: Clean Architecture (Presentación → Dominio ← Datos), MVVM, Single-Activity (Compose). US-009 es **capa de presentación pura**: solo consume el contrato ya estabilizado `StateFlow<ScanState>` (`TRG-LIB-04`, canal C2) vía `ObserveScanStateUseCase`. No extiende el dominio ni ejecuta I/O.

### Contexto descubierto (feature análoga leída completa)

| Artefacto existente | Rol | Relevancia para US-009 |
| --- | --- | --- |
| `feature/library/.../scan/ScanViewModel.kt` + `ScanScreen.kt` | Pantalla de escaneo **fundacional (US-003)**, acoplada a la ruta `SCAN` del onboarding | Patrón de proyección `ScanState → UiState`. **NO reutilizable directo**: *inicia* el escaneo en `init`, autonavega a biblioteca y cierra onboarding al terminar. US-009 es observacional. |
| `core/domain/.../usecase/ObserveScanStateUseCase.kt` | Expone `StateFlow<ScanState>` (C2) | **Fuente única** de US-009. |
| `core/domain/.../model/ScanState.kt` / `ScanSummary.kt` | Estados `Idle/Scanning/Syncing/Finished/Aborted` + resumen de 4 contadores | `ScanSummary` ya tiene los 4 contadores que exige AC6. |
| `core/domain/.../usecase/RescanLibraryUseCase.kt` | Re-encola escaneo (single-flight, fire-and-forget) | Acción de "Reintentar" (AC7). |
| `feature/settings/.../settings/SettingsSourceFoldersViewModel.kt` | Dispara re-escaneo/altas fire-and-forget | Confirma explícitamente: *"progreso observado por US-009, no aquí"*. Punto de disparo. |
| `app/.../navigation/SonusNavHost.kt` | Grafo Single-Activity (rutas `ONBOARDING/SOURCE_FOLDERS/SCAN/LIBRARY/SETTINGS_SOURCE_FOLDERS`) | Punto de montaje del overlay global. |
| `core/data/.../scan/ScanStateEmitterImpl.kt` | `@Singleton` que retiene el último estado | **El estado terminal NO vuelve a `Idle`** → el overlay requiere lógica de descarte. |

### Decisión de diseño (coexistencia con US-003)

El overlay de US-009 se monta a nivel de app **dentro de `SonusNavHost`**, envolviendo el `NavHost` en un `Box`, y se activa **solo en las rutas post-onboarding** (`LIBRARY`, `SETTINGS_SOURCE_FOLDERS`). Durante el onboarding (`ONBOARDING/SOURCE_FOLDERS/SCAN`) queda suprimido, de modo que la ruta `SCAN` fundacional (US-003) sigue siendo la única dueña del progreso en ese flujo y **no hay doble render**. Montarlo dentro del `NavHost` da acceso al `navController` para la navegación de recuperación (AC7). El scrim a pantalla completa bloquea la navegación subyacente (AC2).

**Pasos**

1. Estado inmutable de UI del overlay — componente: `:feature:library` `presentation/scan/ScanProgressUiState.kt` — referencia: `scan/ScanUiState.kt`
2. Comandos del overlay (`Retry`, `Dismiss`) — componente: `presentation/scan/ScanProgressCommand.kt` — referencia: `scan/ScanCommand.kt` — depende de 1
3. ViewModel observacional (proyecta `ScanState`, lógica de descarte, `Retry`→`RescanLibraryUseCase`) — componente: `presentation/scan/ScanProgressViewModel.kt` — referencia: `scan/ScanViewModel.kt` — depende de 1,2
4. Overlay Compose bloqueante (scrim + tarjeta por fase, resumen de 4 contadores, recuperación en abort) — componente: `presentation/scan/ScanProgressOverlay.kt` — referencia: `scan/ScanScreen.kt` — depende de 1,3
5. Recursos de texto US-009 (claves `scan_progress_*`, valores en español) — componente: `feature/library/src/main/res/values/strings.xml` — depende de 4
6. Montaje global + gating por ruta + navegación de recuperación — componente: `app/.../navigation/SonusNavHost.kt` — referencia: patrón `composable(...)` existente — depende de 4
7. Tests unitarios del ViewModel — componente: `ScanProgressViewModelTest.kt` — referencia: `scan/ScanViewModelTest.kt` — depende de 3

### Archivos relevantes

- `feature/library/.../presentation/scan/ScanViewModel.kt` — referencia: patrón de reducción `ScanState → UiState` y canal de eventos
- `feature/library/.../presentation/scan/ScanScreen.kt` — referencia: render por fase, `LinearProgressIndicator` determinado/indeterminado
- `core/domain/.../usecase/ObserveScanStateUseCase.kt` / `RescanLibraryUseCase.kt` — referencia: contratos consumidos
- `app/.../navigation/SonusNavHost.kt` — referencia: grafo y rutas para gating/montaje
- `feature/library/.../scan/ScanViewModelTest.kt` — referencia: stack de test (JUnit5 + MockK + Turbine + coroutines-test)

### Checklist

☑ Feature análoga leída completa (US-003 scan presentation) | ☑ TODOS los artefactos identificados (dominio, emitter, disparadores, navegación, strings, tests) | ☑ Respeta arquitectura (presentación solo consume casos de uso; sin I/O ni red; `when` exhaustivo; strings externos)

---

### Tareas de Implementación

#### Fase 1 — Contrato de presentación (feature:library)

- [ ] **T1: `ScanProgressUiState` + `ScanProgressPhase` (HIDDEN/SCANNING/SYNCING/FINISHED/ABORTED) + `ScanResultUi` (4 contadores)** — `feature/library/src/main/java/com/estebancoloradogonzalez/sonus/feature/library/presentation/scan/ScanProgressUiState.kt` (Base: `scan/ScanUiState.kt`)
- [ ] **T2: `ScanProgressCommand` (`Retry`, `Dismiss`)** — `feature/library/.../presentation/scan/ScanProgressCommand.kt` (Base: `scan/ScanCommand.kt`)

#### Fase 2 — Lógica de presentación (feature:library)

- [ ] **T3: `ScanProgressViewModel` — observa `ObserveScanStateUseCase`, proyecta a `ScanProgressUiState`, descarta estado terminal (reaparece con nuevo `Scanning`), `Retry`→`RescanLibraryUseCase`; sin iniciar escaneo, sin I/O** — `feature/library/.../presentation/scan/ScanProgressViewModel.kt` (Base: `scan/ScanViewModel.kt`)

#### Fase 3 — UI (feature:library)

- [ ] **T4: `ScanProgressOverlay` — `Box` + scrim bloqueante; SCANNING (barra determinada/indeterminada + contador), SYNCING (actividad + mensaje), FINISHED (4 contadores + "Ver biblioteca"), ABORTED (causa + "Reintentar"/"Configurar carpetas"); HIDDEN no renderiza nada** — `feature/library/.../presentation/scan/ScanProgressOverlay.kt` (Base: `scan/ScanScreen.kt`)
- [ ] **T5: Recursos `scan_progress_*` en español** — `feature/library/src/main/res/values/strings.xml`

#### Fase 4 — Integración global (app)

- [ ] **T6: Envolver `NavHost` en `Box`, montar `ScanProgressOverlay` (hiltViewModel) con gating por ruta (`LIBRARY`/`SETTINGS_SOURCE_FOLDERS`); cablear "Ver biblioteca"→navegar LIBRARY, "Configurar carpetas"→navegar SETTINGS_SOURCE_FOLDERS, "Reintentar"→command, dismiss** — `app/src/main/java/com/estebancoloradogonzalez/sonus/navigation/SonusNavHost.kt`

#### Fase 5 — Pruebas

- [ ] **T7: `ScanProgressViewModelTest` — AC1 Idle→HIDDEN; AC3 Scanning total conocido; AC4 total null; AC5 Syncing; AC6 Finished con 4 contadores; AC7 Aborted con código + Retry invoca `RescanLibraryUseCase`; Dismiss→HIDDEN; nuevo Scanning tras dismiss→visible** — `feature/library/src/test/java/com/estebancoloradogonzalez/sonus/feature/library/presentation/scan/ScanProgressViewModelTest.kt` (Base: `scan/ScanViewModelTest.kt`)

### Cobertura de Criterios de Aceptación

| AC | Descripción | Cubierto por |
| --- | --- | --- |
| AC1 | Idle → oculto | T1 (HIDDEN), T4, T7 |
| AC2 | Scanning → aparece automáticamente, bloquea navegación | T4 (scrim), T6 (montaje global) |
| AC3 | Scanning con total → "X de Y" + barra determinada | T4, T7 |
| AC4 | Scanning total null → "X procesados" + indeterminada | T4, T7 |
| AC5 | Syncing → actividad + mensaje de catálogo | T3, T4, T7 |
| AC6 | Finished → 4 contadores + "Ver biblioteca" | T1, T3, T4, T7 |
| AC7 | Aborted → causa + "Reintentar"/"Configurar carpetas" | T3, T4, T6, T7 |
| AC8 | Audio no interrumpido | Diseño: overlay pasivo, no toca `PlaybackService` (sin cambios en servicio) |
| AC9 | Hilo principal nunca bloqueado | Diseño: solo observa `StateFlow`, sin I/O en presentación |
| AC10 | Actualización fluida del contador | T3 (proyección reactiva `update{}`), T4 |
