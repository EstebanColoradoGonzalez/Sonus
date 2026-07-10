## Dev Agent Record — Dev-Rápido

### Debug Log

| # | Tipo | Descripción | Resolución |
|---|------|-------------|------------|
| 1 | Config detekt | `FunctionNaming` marcaba los `@Composable` (PascalCase) y `MagicNumber` los `dp`/colores del theme | `ignoreAnnotated: ['Composable']` en `FunctionNaming`/`MagicNumber` + `ignorePropertyDeclaration` en `detekt.yml` |
| 2 | Config ktlint | `function-naming` rechazaba los `@Composable` | `ktlint_function_naming_ignore_when_annotated_with = Composable` en `.editorconfig` |
| 3 | Dependencias | AndroidX (activity 1.12.4, core-ktx 1.18.0, lifecycle 2.10.0) exigían `compileSdk 36` (no instalado) | Bajadas a versiones compatibles con `compileSdk 35` (activity 1.9.3, core-ktx 1.15.0, lifecycle 2.8.7, test-ext 1.2.1, espresso 3.6.1) |
| 4 | Test | `AutarkyManifestTest` fallaba porque el comentario del manifiesto menciona `READ_MEDIA_AUDIO`/`READ_EXTERNAL_STORAGE` (documentando su ausencia) | El test elimina los comentarios XML antes de asertar (verifica declaraciones reales, no documentación) |
| 5 | Limpieza | `ExampleUnitTest`/`ExampleInstrumentedTest` (JUnit4) del scaffold chocaban con la plataforma JUnit5 | Eliminados |

### Completion Notes

- ⚡ Dev-Rápido: **US-001 — Conceder permisos del sistema (onboarding de notificaciones)**. Se estableció la fundación multi-módulo del proyecto (9 módulos Gradle: `:app`, `:core:{domain,data}`, `:feature:{library,player,playlists,settings}`, `:service:{playback,indexer}`) y el stack de calidad completo (ktlint · detekt · Konsist · Kover · JUnit5 · MockK · Turbine · verificación anti-INTERNET), y sobre esa base se implementó la pantalla de *rationale* + solicitud del permiso `POST_NOTIFICATIONS`.
- Cobertura de los 6 criterios de aceptación:
  - **AC1** (concede) y **AC2** (negación simple + aviso no intrusivo) — `OnboardingViewModel.handlePermissionResult` + snackbar.
  - **AC3** (API < 33) y **AC5** (idempotencia) — `EvaluateNotificationPermissionUseCase` → `Skip`.
  - **AC4** (negación permanente) — detección con `shouldShowRequestPermissionRationale` en el borde Compose → estado `PermanentlyDenied` con enlace a Ajustes.
  - **AC6** (autarquía) — manifiesto solo con `POST_NOTIFICATIONS` runtime + FGS install, sin INTERNET ni media runtime; verificado por `verifyNoInternetPermission` (Gradle) y `AutarkyManifestTest`.
- **Quality gate (`./gradlew check`) en verde**: compilación de los 9 módulos, ktlint, detekt, Konsist, Kover verify, lint y 17 tests unitarios (3 dominio + 8 ViewModel + 3 Konsist + 3 autarquía).
- **Fronteras diferidas** (documentadas): persistencia de `AppSettings.onboardingCompleted` y gating de arranque → US-004; selección de Carpetas Fuente → destino placeholder que reemplaza US-002.

### File List

| Acción | Archivo | Descripción |
|--------|---------|-------------|
| Modificado | `Sonus/gradle/libs.versions.toml` | Catálogo: Hilt, KSP, navigation, coroutines, JUnit5, MockK, Turbine, Truth, Konsist, ktlint/detekt/kover, javax.inject; versiones AndroidX alineadas a compileSdk 35 |
| Modificado | `Sonus/settings.gradle.kts` | Inclusión de los 9 módulos |
| Modificado | `Sonus/build.gradle.kts` | Plugins de calidad, `subprojects` ktlint/detekt, agregación Kover |
| Creado | `Sonus/.editorconfig` | Formato ktlint (4 espacios, 120 cols, sin comodines, Composable exento) |
| Creado | `Sonus/.gitattributes` | Normalización EOL LF |
| Creado | `Sonus/config/detekt/detekt.yml` | Reglas detekt (naming, magic number, excepciones) |
| Creado | `Sonus/core/domain/build.gradle.kts` | Módulo dominio (Kotlin puro/JVM) |
| Creado | `Sonus/core/data/build.gradle.kts` | Módulo datos (android lib + Hilt) |
| Creado | `Sonus/feature/settings/build.gradle.kts` | Módulo presentación settings/onboarding (Compose + Hilt) |
| Creado | `Sonus/feature/library/build.gradle.kts` | Esqueleto |
| Creado | `Sonus/feature/player/build.gradle.kts` | Esqueleto |
| Creado | `Sonus/feature/playlists/build.gradle.kts` | Esqueleto |
| Creado | `Sonus/service/playback/build.gradle.kts` | Esqueleto |
| Creado | `Sonus/service/indexer/build.gradle.kts` | Esqueleto |
| Modificado | `Sonus/app/build.gradle.kts` | Hilt/KSP, deps de módulos, JUnit5, tarea `verifyNoInternetPermission` |
| Creado | `core/domain/.../port/NotificationPermissionGateway.kt` | Puerto de permiso de notificaciones |
| Creado | `core/domain/.../model/NotificationPermissionStep.kt` | Modelo `Skip`/`Request` |
| Creado | `core/domain/.../usecase/EvaluateNotificationPermissionUseCase.kt` | Decisión skip/request |
| Creado | `core/data/.../permission/NotificationPermissionGatewayImpl.kt` | Implementación Android del puerto |
| Creado | `feature/settings/.../onboarding/OnboardingUiState.kt` | Estado inmutable + fases |
| Creado | `feature/settings/.../onboarding/OnboardingEvent.kt` | Eventos one-shot |
| Creado | `feature/settings/.../onboarding/OnboardingCommand.kt` | Comandos de UI |
| Creado | `feature/settings/.../onboarding/OnboardingViewModel.kt` | ViewModel (MVVM) |
| Creado | `feature/settings/.../onboarding/NotificationPermissionScreen.kt` | Pantalla Compose de rationale + permiso |
| Creado | `feature/settings/src/main/res/values/strings.xml` | Textos i18n (es) |
| Creado | `app/.../SonusApplication.kt` | `@HiltAndroidApp` |
| Creado | `app/.../di/PermissionModule.kt` | Binding Hilt puerto→impl |
| Modificado | `app/.../MainActivity.kt` | `@AndroidEntryPoint` + host del NavHost |
| Creado | `app/.../navigation/SonusNavHost.kt` | Grafo Single-Activity + placeholder Carpetas Fuente |
| Modificado | `app/src/main/AndroidManifest.xml` | `SonusApplication` + permisos (POST_NOTIFICATIONS, FGS) |
| Modificado | `app/src/main/res/values/strings.xml` | String del placeholder |
| Creado | `core/domain/src/test/.../fake/FakeNotificationPermissionGateway.kt` | Fake del puerto |
| Creado | `core/domain/src/test/.../usecase/EvaluateNotificationPermissionUseCaseTest.kt` | Tests del caso de uso |
| Creado | `feature/settings/src/test/.../onboarding/OnboardingViewModelTest.kt` | Tests del ViewModel (Turbine) |
| Creado | `app/src/test/.../architecture/ArchitectureTest.kt` | Reglas Konsist |
| Creado | `app/src/test/.../architecture/AutarkyManifestTest.kt` | Verificación de autarquía (AC6) |
| Eliminado | `app/.../ExampleUnitTest.kt` | Scaffold JUnit4 |
| Eliminado | `app/.../ExampleInstrumentedTest.kt` | Scaffold JUnit4 |

### Métricas Dev-Rápido

- Tiempo sesión IA: 22 min
- Tareas manuales DoD: 0 min
- Tiempo total: 22 min
