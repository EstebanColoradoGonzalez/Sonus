# Refinamiento Técnico (Developer)

**Autor**: Esteban Colorado González | **Fecha**: 2026-07-09

## Plan: Conceder los permisos del sistema (US-001)

**Arquitectura**: Arquitectura Limpia multi-módulo (Presentación → Dominio ← Datos),
MVVM + Single-Activity + Jetpack Compose + Hilt, 100% local (air-gapped).
Fundación del proyecto: US-001 es la primera historia, por lo que además del feature
se establece el esqueleto de los **9 módulos Gradle** y el **stack de calidad completo**
(ktlint · detekt · Konsist · Kover · JUnit5 · MockK · Turbine · verificación anti-INTERNET).

> **Nota de contexto**: No existe feature análoga implementada (primera historia). El "patrón"
> se extrae directamente del `architecture_blueprint.md` (§2.1, §3), `coding-standards.md`
> (§2–§5) y el `interfaces_contract.md` (§1.2, TRG-CFG-02). El código base actual es el
> scaffold Compose en el módulo único `app` (`MainActivity`, `ui/theme`), que se migra a `:app`.

### Decisión de flujo (mapa criterios → diseño)

| AC | Escenario | Punto de enforcement |
|----|-----------|----------------------|
| 1 | Concede permiso | `NotificationPermissionScreen` lanza diálogo → `OnboardingCommand.PermissionResult(granted=true)` → evento `NavigateToSourceFolders` |
| 2 | Negación simple | `PermissionResult(granted=false, permanentlyDenied=false)` → aviso no intrusivo + `NavigateToSourceFolders` |
| 3 | API < 33 | `EvaluateNotificationPermissionUseCase` → `Step.Skip` (gateway `isSupported()=false`) → navega directo |
| 4 | Negación permanente | `PermissionResult(granted=false, permanentlyDenied=true)` → estado `PermanentlyDenied` con "Abrir Ajustes" |
| 5 | Idempotencia (ya concedido) | `EvaluateNotificationPermissionUseCase` → `Step.Skip` (gateway `isGranted()=true`) → navega directo |
| 6 | Autarquía verificable | Manifiesto `:app`: solo `POST_NOTIFICATIONS` runtime + FGS install; **sin** INTERNET ni media runtime. Verificado por tarea Gradle `verifyNoInternetPermission` + test |

### Fronteras de alcance (lo que NO entra en US-001)

- **Persistencia de `AppSettings.onboardingCompleted` (Room) y el gating de arranque**: es US-004.
  En US-001 el `startDestination` del `NavHost` es `onboarding` de forma fija.
- **Selección de Carpetas Fuente (US-002)**: se navega a un **destino placeholder** temporal
  (`SourceFoldersPlaceholder`) que US-002 reemplazará.
- **`SettingsRepository` / `CompleteOnboardingUseCase`**: US-004.
- Módulos `:core:data` (Room/SAF/ID3), `:service:*`, `:feature:{library,player,playlists}`:
  se crean **vacíos** (esqueleto compilable) salvo lo que US-001 necesita en `:core:data`
  (solo `NotificationPermissionGatewayImpl`).

---

## Tareas de Implementación

### Fase 0 — Fundación de build y tooling

- [ ] **T1: Ampliar `gradle/libs.versions.toml`** — Hilt, hilt-navigation-compose, navigation-compose, lifecycle-viewmodel-compose, coroutines(+test), Turbine, MockK, JUnit5, Konsist, plugins ktlint/detekt/kover, KSP — `Sonus/gradle/libs.versions.toml`
- [ ] **T2: `settings.gradle.kts`** — incluir los 9 módulos (`:app`, `:core:domain`, `:core:data`, `:feature:library`, `:feature:player`, `:feature:playlists`, `:feature:settings`, `:service:playback`, `:service:indexer`) — `Sonus/settings.gradle.kts`
- [ ] **T3: Config de calidad raíz** — aplicar plugins ktlint/detekt/kover en root; `.editorconfig`, `.gitattributes`, `config/detekt/detekt.yml` (Base: `coding-standards.md` §6) — `Sonus/build.gradle.kts`, `Sonus/.editorconfig`, `Sonus/.gitattributes`, `Sonus/config/detekt/detekt.yml`

### Fase 1 — Esqueleto de módulos

- [ ] **T4: `:core:domain`** — `build.gradle.kts` (Kotlin puro/JVM, sin Android; coroutines + javax.inject) — `Sonus/core/domain/build.gradle.kts`
- [ ] **T5: `:core:data`** — `build.gradle.kts` (android lib, depende de `:core:domain` + Hilt) — `Sonus/core/data/build.gradle.kts`
- [ ] **T6: `:feature:settings`** — `build.gradle.kts` (android lib, Compose, Hilt, depende de `:core:domain`) — `Sonus/feature/settings/build.gradle.kts`
- [ ] **T7: Módulos esqueleto vacíos** — `:feature:{library,player,playlists}`, `:service:{playback,indexer}` con `build.gradle.kts` mínimo y paquete raíz — `Sonus/feature/*`, `Sonus/service/*`
- [ ] **T8: Migrar `:app`** — mover a `Sonus/app` dependencias de módulos; `build.gradle.kts` con Hilt/KSP, navigation-compose, `:core:domain`, `:core:data`, `:feature:settings`

### Fase 2 — Dominio (`:core:domain`)

- [ ] **T9: Puerto `NotificationPermissionGateway`** — `isSupported(): Boolean`, `isGranted(): Boolean` (Base: blueprint §3 "Puertos") — `.../core/domain/port/NotificationPermissionGateway.kt`
- [ ] **T10: Modelo `NotificationPermissionStep`** — `sealed interface` (`Skip`, `Request`) — `.../core/domain/model/NotificationPermissionStep.kt`
- [ ] **T11: `EvaluateNotificationPermissionUseCase`** — `operator fun invoke(): NotificationPermissionStep`; retorna `Skip` si `!isSupported()` o `isGranted()`, si no `Request` (Base: blueprint §3 "Casos de Uso") — `.../core/domain/usecase/EvaluateNotificationPermissionUseCase.kt`

### Fase 3 — Presentación (`:feature:settings`)

- [ ] **T12: `OnboardingUiState`** — `data class` inmutable con fase de permiso (`sealed interface OnboardingPermissionPhase`: `Rationale`, `SoftDenied`, `PermanentlyDenied`) — `.../feature/settings/presentation/onboarding/OnboardingUiState.kt`
- [ ] **T13: `OnboardingEvent`** — `sealed interface` one-shot (`NavigateToSourceFolders`, `LaunchSystemPermissionDialog`, `OpenNotificationSettings`) — `.../feature/settings/presentation/onboarding/OnboardingEvent.kt`
- [ ] **T14: `OnboardingCommand`** — `sealed interface` (`EvaluateStep`, `RequestPermission`, `Skip`, `PermissionResult(granted, permanentlyDenied)`, `OpenSettings`) — `.../feature/settings/presentation/onboarding/OnboardingCommand.kt`
- [ ] **T15: `OnboardingViewModel`** — `@HiltViewModel`; `StateFlow<OnboardingUiState>` + `Channel<OnboardingEvent>`; `onCommand(...)` `when` exhaustivo; delega en `EvaluateNotificationPermissionUseCase` (Base: coding-standards §3.2 MVVM) — `.../feature/settings/presentation/onboarding/OnboardingViewModel.kt`
- [ ] **T16: `NotificationPermissionScreen`** — `@Composable` rationale a pantalla completa; `rememberLauncherForActivityResult(RequestPermission)`; detecta negación permanente con `shouldShowRequestPermissionRationale`; renderiza fases; botones "Permitir/Omitir/Abrir Ajustes" vía `stringResource` — `.../feature/settings/presentation/onboarding/NotificationPermissionScreen.kt`
- [ ] **T17: Recursos de texto** — `strings.xml` (claves inglés `snake_case`, valores español: rationale, aviso, ajustes) — `Sonus/feature/settings/src/main/res/values/strings.xml`

### Fase 4 — Datos (`:core:data`)

- [ ] **T18: `NotificationPermissionGatewayImpl`** — `@Inject`, `@ApplicationContext`; `isSupported` = `Build.VERSION.SDK_INT >= TIRAMISU`; `isGranted` = `ContextCompat.checkSelfPermission(POST_NOTIFICATIONS)` (< 33 ⇒ `true` implícito) — `.../core/data/permission/NotificationPermissionGatewayImpl.kt`

### Fase 5 — Ensamblaje (`:app`)

- [ ] **T19: `SonusApplication`** — `@HiltAndroidApp` — `.../app/SonusApplication.kt`
- [ ] **T20: Hilt DI** — `@Module @Binds NotificationPermissionGateway → …Impl`; provee `EvaluateNotificationPermissionUseCase` — `.../app/di/PermissionModule.kt`
- [ ] **T21: `MainActivity`** — `@AndroidEntryPoint`; host de `SonusNavHost` (migra el scaffold actual) — `.../app/MainActivity.kt`
- [ ] **T22: `SonusNavHost`** — rutas `onboarding` (start) → `sourceFolders`; destino placeholder `SourceFoldersPlaceholder` temporal (US-002) — `.../app/navigation/SonusNavHost.kt`
- [ ] **T23: `AndroidManifest.xml` (`:app`)** — registrar `SonusApplication`; declarar `POST_NOTIFICATIONS` (runtime), `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK` (install); **sin** INTERNET ni media runtime (AC6) — `Sonus/app/src/main/AndroidManifest.xml`
- [ ] **T24: Verificación anti-INTERNET** — tarea Gradle `verifyNoInternetPermission` sobre el manifiesto mergeado, enganchada a `check` (Base: ADR-010) — `Sonus/app/build.gradle.kts`

### Fase 6 — Pruebas (JUnit5 · MockK · Turbine · coroutines-test)

- [ ] **T25: `FakeNotificationPermissionGateway`** — fake en memoria inspeccionable (source set de prueba) — `Sonus/core/domain/src/test/.../fake/FakeNotificationPermissionGateway.kt`
- [ ] **T26: `EvaluateNotificationPermissionUseCaseTest`** — Escenarios 3 (no soportado→Skip), 5 (concedido→Skip), soportado+no concedido→Request (3A) — `Sonus/core/domain/src/test/.../EvaluateNotificationPermissionUseCaseTest.kt`
- [ ] **T27: `OnboardingViewModelTest`** — Turbine sobre `uiState`/`events`: Escenario 1 (concede→navega), 2 (niega simple→aviso+navega), 4 (permanente→estado Ajustes), `EvaluateStep`→Skip navega, `Skip` navega — `Sonus/feature/settings/src/test/.../OnboardingViewModelTest.kt`
- [ ] **T28: `ArchitectureTest` (Konsist)** — dirección de dependencias + sufijos por capa (Base: coding-standards §6) — `Sonus/app/src/test/.../ArchitectureTest.kt`
- [ ] **T29: Test de autarquía (AC6)** — verifica que el manifiesto de `:app` no declara INTERNET y que `POST_NOTIFICATIONS` es el único permiso runtime — `Sonus/app/src/test/.../AutarkyManifestTest.kt`

### Fase 7 — Quality gates

- [ ] **T30: Ejecutar el gate** — `ktlintCheck`, `detekt`, `konsistTest`, `testDebugUnitTest`, `koverXmlReport`, `verifyNoInternetPermission` → todo en verde (100% de tests)

---

## Checklist de refinamiento

- ☑ Feature análoga: N/A (primera historia) → patrón extraído del blueprint + estándares (documentado)
- ☑ TODOS los artefactos identificados: build/tooling, dominio, presentación, datos, ensamblaje, manifiesto, tests
- ☑ Respeta arquitectura: capas, sufijos, dirección de dependencias, i18n desacoplada, air-gapped
- ☑ Incluye tests (el proyecto exige testing) y quality gates
- ☑ Fronteras de alcance explícitas (US-002/US-004 diferidos)
