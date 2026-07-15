# Refinamiento Técnico (Developer)

**Autor**: Esteban Colorado González | **Fecha**: 2026-07-15

## Plan: Marcar el onboarding como completado (US-004)

**Arquitectura del proyecto** (patrón identificado y a replicar):
Clean Architecture multi-módulo Kotlin/Android. Un caso de uso vertical se materializa como:
`SettingsCommand` (dominio, C1) → `UseCase` (dominio) → `Repository` (puerto en `:core:domain`) →
`RepositoryImpl` (Room en `:core:data`) → `Dao` + `@Entity` → seeding/migración versionada → binding Hilt en `:app`.
Errores como valores vía `OperationResult`/`DomainError` (P1). Feature análoga completa estudiada: **SourceFolder** (US-002)
y la **migración/seeding de catálogo** (US-003).

**Feature análoga leída (referencias):**
- Comando: `core/domain/.../command/LibraryCommand.kt`
- Puerto + Use Case: `core/domain/.../port/SourceFolderRepository.kt`, `.../usecase/AddSourceFolderUseCase.kt`, `.../usecase/VerifySourceFoldersReadyUseCase.kt`
- Errores/resultado: `core/domain/.../error/DomainError.kt`, `.../result/OperationResult.kt`
- Entity/DAO/Impl/Mapper: `core/data/.../entity/SourceFolder.kt`, `.../dao/SourceFolderDao.kt`, `.../repository/SourceFolderRepositoryImpl.kt`, `.../converter/RoomTypeConverters.kt`
- DB/migración/seeding: `core/data/.../room/SonusDatabase.kt`, `.../migration/Migrations.kt`; `app/.../di/DatabaseModule.kt`, `.../di/LibraryModule.kt`
- Presentación/navegación: `app/.../navigation/SonusNavHost.kt`, `app/.../MainActivity.kt`, `feature/library/.../scan/ScanViewModel.kt` + `ScanScreen.kt`
- Tests: `AddSourceFolderUseCaseTest.kt`, `FakeSourceFolderRepository.kt`, `CatalogRepositoryImplTest.kt`, `SourceFolderDaoTest.kt`, `MigrationTest.kt`, `ScanViewModelTest.kt`

**Decisiones de diseño clave:**
1. El puerto `SettingsRepository` expone primitivos (`completeOnboarding(): OperationResult<Unit>`, `isOnboardingCompleted(): Boolean`) — no se introduce modelo de dominio `AppSettings` (YAGNI; lo añadirá US-039 al gestionar tema). Sin fuga de Room al dominio.
2. La `@Entity app_settings` incluye **ambos** campos del modelo (`onboardingCompleted`, `themePreference`) y se siembra como singleton `id=1` (`false`, `SYSTEM`) conforme al Big Bang §6.1, evitando churn de migración en US-039. `themePreference` requiere el enum de dominio `ThemePreference` + `TypeConverter` por **nombre** (no ordinal).
3. **Idempotencia** (Esc.2): la escritura es `UPDATE app_settings SET onboardingCompleted=1 WHERE id=1` — naturalmente idempotente.
4. **Degradación grácil** (Esc.6): `SettingsRepositoryImpl` captura el fallo de Room en el borde y lo mapea a `DomainError.SettingsPersistenceFailed` (`OperationResult.Failure`); el disparo desde presentación es **fire-and-forget** → la transición a biblioteca nunca se bloquea ni aborta.
5. **Disparo robusto** (Esc.1/Esc.4): el comando se emite en el **único embudo** `SCAN → LIBRARY` (`ScanScreen.onNavigateToLibrary`), cableado en `SonusNavHost`. Ambas ramas de US-003 (escaneo con progreso y biblioteca <1s) pasan por ahí. Los arranques recurrentes (Esc.3) arrancan directo en `LIBRARY` sin atravesar `SCAN`, por lo que **no** re-disparan.
6. **Gating de arranque** (Esc.3/Esc.5): `SonusAppViewModel` resuelve el `startDestination` (`Loading → Onboarding | Library`) leyendo `isOnboardingCompleted` al inicio; `MainActivity` no renderiza el grafo hasta resolver; `SonusNavHost` recibe el `startDestination` parametrizado.
7. **Privacidad/Autarquía** (Esc.7/Esc.8): solo se persiste un booleano operativo, sin dato comportamental; persistencia 100% local (Room), sin red. Cubierto por diseño y por los tests de arquitectura existentes (`AutarkyManifestTest`, `NoMediaStoreTest`).

**Checklist**
- ☑ Feature análoga leída completa (SourceFolder + migración/seeding US-003)
- ☑ TODOS los artefactos identificados (entity, DAO, converter, migración, seed, DI, schema export, tests)
- ☑ Respeta arquitectura (Clean, errores como valores, dominio puro, DI en composition root)

---

### Tareas de Implementación

#### Fase 1 — Dominio (`:core:domain`)
- [ ] **T1: `SettingsCommand` (sealed interface con `CompleteOnboarding`, C1 / `TRG-CFG-02`)** — `core/domain/.../command/SettingsCommand.kt` (Base: `command/LibraryCommand.kt`)
- [ ] **T2: Enum de dominio `ThemePreference` (SYSTEM/LIGHT/DARK)** — `core/domain/.../model/ThemePreference.kt` (Base: `model/ContentType.kt`)
- [ ] **T3: Puerto `SettingsRepository` (`completeOnboarding(): OperationResult<Unit>`, `isOnboardingCompleted(): Boolean`)** — `core/domain/.../port/SettingsRepository.kt` (Base: `port/SourceFolderRepository.kt`)
- [ ] **T4: `DomainError.SettingsPersistenceFailed` (WARNING, recoverable, `ErrorDetails.Io(WRITE_FAILED)`)** — `core/domain/.../error/DomainError.kt` (MODIFICAR)
- [ ] **T5: `CompleteOnboardingUseCase` (delega en el puerto; Esc.1/2/6)** — `core/domain/.../usecase/CompleteOnboardingUseCase.kt` (Base: `usecase/AddSourceFolderUseCase.kt`)
- [ ] **T6: `IsOnboardingCompletedUseCase` (lectura de gating; Esc.3/5)** — `core/domain/.../usecase/IsOnboardingCompletedUseCase.kt` (Base: `usecase/VerifySourceFoldersReadyUseCase.kt`)

#### Fase 2 — Persistencia (`:core:data`)
- [ ] **T7: `@Entity app_settings` (singleton `id=1`, `onboardingCompleted`, `themePreference`)** — `core/data/.../entity/AppSettings.kt` (Base: `entity/SourceFolder.kt`)
- [ ] **T8: `TypeConverter` de `ThemePreference` por nombre** — `core/data/.../converter/RoomTypeConverters.kt` (MODIFICAR)
- [ ] **T9: `SettingsDao` (`isOnboardingCompleted(): Boolean?`, `markOnboardingCompleted()` idempotente por UPDATE)** — `core/data/.../dao/SettingsDao.kt` (Base: `dao/SourceFolderDao.kt`)
- [ ] **T10: `SettingsRepositoryImpl` (mapea fallo Room → `DomainError`; `?: false` si no hay fila)** — `core/data/.../repository/SettingsRepositoryImpl.kt` (Base: `repository/SourceFolderRepositoryImpl.kt`)
- [ ] **T11: `SonusDatabase` → versión 3, registrar `AppSettings` + `settingsDao()`** — `core/data/.../room/SonusDatabase.kt` (MODIFICAR)
- [ ] **T12: `MIGRATION_2_3` + `APP_SETTINGS_SINGLETON_SEED` (crea tabla + siembra singleton `false`/`SYSTEM`)** — `core/data/.../migration/Migrations.kt` (MODIFICAR)

#### Fase 3 — Inyección de Dependencias (`:app`)
- [ ] **T13: `DatabaseModule` → `addMigrations(MIGRATION_1_2, MIGRATION_2_3)`, seed del singleton en `onCreate`, `provideSettingsDao`** — `app/.../di/DatabaseModule.kt` (MODIFICAR)
- [ ] **T14: `SettingsModule` → `@Binds SettingsRepositoryImpl → SettingsRepository`** — `app/.../di/SettingsModule.kt` (Base: `di/LibraryModule.kt`)

#### Fase 4 — Presentación: gating + disparo (`:app`)
- [ ] **T15: `SonusAppViewModel` (resuelve `startDestination` vía `IsOnboardingCompletedUseCase`; `completeOnboarding()` fire-and-forget vía `CompleteOnboardingUseCase`) + estado `StartDestinationUiState` (Loading/Onboarding/Library)** — `app/.../navigation/SonusAppViewModel.kt` (Base: `feature/library/.../scan/ScanViewModel.kt`)
- [ ] **T16: `SonusNavHost` parametrizado con `startDestination` + cableado del disparo en `SCAN → LIBRARY` (Esc.1/4)** — `app/.../navigation/SonusNavHost.kt` (MODIFICAR)
- [ ] **T17: `MainActivity` bloquea el render hasta resolver el gating y pasa estado + callback al NavHost** — `app/.../MainActivity.kt` (MODIFICAR)

#### Fase 5 — Tests (obligatorio, 100% verde)
- [ ] **T18: `FakeSettingsRepository`** — `core/domain/test/.../fake/FakeSettingsRepository.kt` (Base: `fake/FakeSourceFolderRepository.kt`)
- [ ] **T19: `CompleteOnboardingUseCaseTest` (éxito, idempotencia doble emisión, propagación de Failure)** — `core/domain/test/.../usecase/CompleteOnboardingUseCaseTest.kt` (Base: `AddSourceFolderUseCaseTest.kt`)
- [ ] **T20: `IsOnboardingCompletedUseCaseTest` (false por defecto Esc.5; true tras completar Esc.3)** — `core/domain/test/.../usecase/IsOnboardingCompletedUseCaseTest.kt`
- [ ] **T21: `SettingsRepositoryImplTest` (éxito→Unit; idempotencia; DAO lanza→`Failure(SettingsPersistenceFailed)` Esc.6)** — `core/data/test/.../repository/SettingsRepositoryImplTest.kt` (Base: `CatalogRepositoryImplTest.kt`)
- [ ] **T22: `SettingsDaoTest` (androidTest: marca idempotente + lectura refleja estado)** — `core/data/androidTest/.../room/SettingsDaoTest.kt` (Base: `SourceFolderDaoTest.kt`)
- [ ] **T23: `MigrationTest.migrate2To3` (tabla `app_settings` creada + singleton sembrado `false`/`SYSTEM`)** — `core/data/androidTest/.../room/MigrationTest.kt` (MODIFICAR)
- [ ] **T24: `SonusAppViewModelTest` (Loading→Onboarding si false / →Library si true; `completeOnboarding()` invoca el use case)** — `app/test/.../navigation/SonusAppViewModelTest.kt` (Base: `ScanViewModelTest.kt`)

#### Fase 6 — Verificación de build
- [ ] **T25: Compilar y correr tests JVM (`:core:domain`, `:core:data`, `:app`); regenerar schema exportado `3.json`; confirmar 100% verde**

---

### Trazabilidad Criterios de Aceptación → Tareas

| Escenario | Cobertura |
| --- | --- |
| 1 — Cierre en transición | T5, T9, T10, T16, T19, T21 |
| 2 — Idempotencia | T9 (UPDATE), T19, T21, T22 |
| 3 — Gating recurrente | T6, T15, T16, T17, T20, T24 |
| 4 — Robustez ambas ramas | T16 (embudo único SCAN→LIBRARY) |
| 5 — Primera ejecución flujo completo | T12 (seed `false`), T6, T15, T20, T23, T24 |
| 6 — Degradación grácil | T4, T10, T15 (fire-and-forget), T21 |
| 7 — Privacidad | T7 (solo booleano), tests de arquitectura existentes |
| 8 — Autarquía | T10 (Room local), `AutarkyManifestTest`/`NoMediaStoreTest` existentes |
