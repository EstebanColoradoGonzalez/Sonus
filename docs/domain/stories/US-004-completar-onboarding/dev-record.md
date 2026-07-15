## Dev Agent Record — Dev-Rápido

### Debug Log

| # | Tipo | Descripción | Resolución |
|---|------|-------------|------------|
| 1 | Regresión de compilación | Añadir `DomainError.SettingsPersistenceFailed` rompió el `when` exhaustivo (sin `else`) de `SourceFoldersViewModel.toNotice()` | Se agregó la rama defensiva `SettingsPersistenceFailed -> NotifyPermissionDenied`, coherente con el mapeo de errores no originables en add-folder |

### Completion Notes

- ⚡ **Dev-Rápido US-004**: Marcar el onboarding como completado. Se implementó el stack completo del singleton `AppSettings` (aún inexistente): dominio (`SettingsCommand.CompleteOnboarding`, puerto `SettingsRepository`, casos de uso `CompleteOnboardingUseCase`/`IsOnboardingCompletedUseCase`, `DomainError.SettingsPersistenceFailed`, enum `ThemePreference`), persistencia Room (`@Entity app_settings`, `SettingsDao` con UPDATE idempotente, `SettingsRepositoryImpl` con fallo mapeado a valor, DB v2→v3 + `MIGRATION_2_3` + seed del singleton `false`/`SYSTEM`), DI (`SettingsModule`, `DatabaseModule`) y presentación (`SonusAppViewModel` para gating de arranque + disparo fire-and-forget en el embudo único `SCAN → LIBRARY`, `SonusNavHost` parametrizado, `MainActivity` bloquea el render hasta resolver).
- **Cobertura de ACs**: 8/8 escenarios cubiertos por diseño y/o tests (idempotencia, gating de arranque, robustez de ambas ramas, degradación grácil, privacidad y autarquía).
- **Verificación**: `gradlew check` en verde (tests unitarios JVM de `:core:domain`/`:core:data`/`:app`, lint, konsist/arquitectura, `verifyNoInternetPermission`). Schema `3.json` regenerado; su `createSql` de `app_settings` coincide exactamente con `MIGRATION_2_3`.
- **Pendiente de ejecución en dispositivo/emulador** (no disponible en esta sesión): tests instrumentados `MigrationTest.migrate2To3` y `SettingsDaoTest` (escritos y compilando).

### File List

| Acción | Archivo | Descripción |
|--------|---------|-------------|
| C | `core/domain/.../command/SettingsCommand.kt` | Comando C1 con `CompleteOnboarding` (`TRG-CFG-02`) |
| C | `core/domain/.../model/ThemePreference.kt` | Enum de dominio (SYSTEM/LIGHT/DARK) |
| C | `core/domain/.../port/SettingsRepository.kt` | Puerto de configuración (onboarding) |
| M | `core/domain/.../error/DomainError.kt` | Nuevo subtipo `SettingsPersistenceFailed` |
| C | `core/domain/.../usecase/CompleteOnboardingUseCase.kt` | Cierra el onboarding (Esc.1/2/6) |
| C | `core/domain/.../usecase/IsOnboardingCompletedUseCase.kt` | Lectura de gating (Esc.3/5) |
| C | `core/data/.../entity/AppSettings.kt` | `@Entity` singleton `app_settings` |
| M | `core/data/.../converter/RoomTypeConverters.kt` | Converter de `ThemePreference` por nombre |
| C | `core/data/.../dao/SettingsDao.kt` | DAO con read + UPDATE idempotente |
| C | `core/data/.../repository/SettingsRepositoryImpl.kt` | Impl Room; fallo → `DomainError` |
| M | `core/data/.../room/SonusDatabase.kt` | Versión 3 + registro `AppSettings`/`settingsDao()` |
| M | `core/data/.../migration/Migrations.kt` | `MIGRATION_2_3` + `APP_SETTINGS_SINGLETON_SEED` |
| C | `core/data/schemas/...SonusDatabase/3.json` | Schema exportado v3 (generado por build) |
| M | `app/.../di/DatabaseModule.kt` | Migración v2→v3, seed en `onCreate`, `provideSettingsDao` |
| C | `app/.../di/SettingsModule.kt` | `@Binds SettingsRepositoryImpl → SettingsRepository` |
| C | `app/.../navigation/StartDestinationUiState.kt` | Estado de gating (Loading/Onboarding/Library) |
| C | `app/.../navigation/SonusAppViewModel.kt` | Gating de arranque + disparo `CompleteOnboarding` |
| M | `app/.../navigation/SonusNavHost.kt` | `startDestination` parametrizado + disparo en `SCAN → LIBRARY` |
| M | `app/.../MainActivity.kt` | Bloquea render hasta resolver el destino |
| M | `app/build.gradle.kts` | Deps `lifecycle-viewmodel-compose`, test `coroutines-test`/`mockk` |
| M | `feature/settings/.../onboarding/SourceFoldersViewModel.kt` | Rama del `when` para el nuevo error (fix de regresión) |
| C | `core/domain/test/.../fake/FakeSettingsRepository.kt` | Fake del puerto |
| C | `core/domain/test/.../usecase/CompleteOnboardingUseCaseTest.kt` | Éxito, idempotencia, propagación de fallo |
| C | `core/domain/test/.../usecase/IsOnboardingCompletedUseCaseTest.kt` | Default false / true tras completar |
| C | `core/data/test/.../repository/SettingsRepositoryImplTest.kt` | Éxito, mapeo de fallo (Esc.6), lectura |
| C | `core/data/androidTest/.../room/SettingsDaoTest.kt` | Default, marca e idempotencia (instrumentado) |
| M | `core/data/androidTest/.../room/MigrationTest.kt` | `migrate2To3` (instrumentado) |
| C | `app/test/.../navigation/SonusAppViewModelTest.kt` | Resolución de destino + emisión de comando |

### Métricas Dev-Rápido

- Tiempo sesión IA: 20 min
- Tareas manuales DoD: 0 min
- Tiempo total: 20 min
