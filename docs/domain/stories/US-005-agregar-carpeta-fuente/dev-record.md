## Dev Agent Record — Dev-Rápido

### Debug Log

| # | Tipo | Descripción | Resolución |
|---|------|-------------|------------|
| 1 | Entorno | `JAVA_HOME` del shell apuntaba a JDK 1.7; Gradle/AGP requieren 17. | Se ejecutó Gradle con `JAVA_HOME` = Eclipse Adoptium JDK 17.0.18. |
| 2 | Build | Tarea `konsistTest` inexistente en el proyecto. | Quality gate ejecutado con `ktlintCheck` + `detekt` (Konsist aún no cableado). |
| 3 | Código | Import faltante de `Row` y dependencia de iconos Material no garantizada en `:feature:settings`. | Se agregó el import y se reemplazó el ícono de navegación por un `TextButton` "Volver". |

### Completion Notes

- ⚡ Dev-Rápido: **US-005 — Agregar una Carpeta Fuente** (post-onboarding). El Oyente agrega una `SourceFolder` vía SAF (`ACTION_OPEN_DOCUMENT_TREE` + `takePersistableUriPermission`) desde una nueva sección "Carpetas Fuente" en Configuración, con acceso adicional desde la Biblioteca.
- **Reutilización total del dominio/datos de US-002** (`AddSourceFolderUseCase`, `SafPermissionGateway`, `SourceFolderRepository`, `ObserveSourceFoldersUseCase`, `DomainError`) sin modificarlos → escenarios E1, E2, E3, E5, E7, E8 cubiertos.
- **Nuevo de US-005**: detección de **solapamiento** (E4) como función pura de dominio + caso de uso (advierte pero permite; la deduplicación real la garantiza `Track.uri` en el escaneo de US-007/US-008); aviso de **contenido pendiente de escanear** (E6, sin disparar escaneo — frontera hacia US-007); presentación post-onboarding y navegación.
- **Fuera de alcance respetado**: remover carpeta (US-006), ejecutar escaneo (US-007), sincronizar (US-008), progreso (US-009).
- **Verificación**: `:core:domain:test` y `:feature:settings:testDebugUnitTest` PASS; `ktlintCheck` + `detekt` PASS; compilación de `:app` y `:feature:library` PASS (incluye cableado Hilt vía KSP).

### File List

| Acción | Archivo | Descripción |
|--------|---------|-------------|
| Creado | `Sonus/core/domain/.../usecase/SourceFolderTreeUri.kt` | Función pura: solapamiento por prefijo `%2F` de `treeUri` SAF. |
| Creado | `Sonus/core/domain/.../usecase/DetectSourceFolderOverlapUseCase.kt` | Caso de uso: detecta solapamiento contra carpetas registradas. |
| Creado | `Sonus/feature/settings/.../presentation/settings/SettingsSourceFoldersUiState.kt` | Estado de UI + proyección `SourceFolderUi` + flag `hasPendingScanContent`. |
| Creado | `Sonus/feature/settings/.../presentation/settings/SettingsSourceFoldersCommand.kt` | Comandos C1 (add / picked / cancelled). |
| Creado | `Sonus/feature/settings/.../presentation/settings/SettingsSourceFoldersEvent.kt` | Eventos one-shot (added, overlap, duplicate, denied, cancelled, launch picker). |
| Creado | `Sonus/feature/settings/.../presentation/settings/SettingsSourceFoldersViewModel.kt` | `@HiltViewModel`: compone overlap + add; mapea `DomainError`. |
| Creado | `Sonus/feature/settings/.../presentation/settings/SettingsSourceFoldersScreen.kt` | Pantalla Compose: launcher SAF, lista, banner pendiente, snackbars. |
| Creado | `Sonus/core/domain/src/test/.../usecase/DetectSourceFolderOverlapUseCaseTest.kt` | Tests de solapamiento (ancestro/descendiente/hermano/prefijo/duplicado/vacío). |
| Creado | `Sonus/feature/settings/src/test/.../presentation/settings/SettingsSourceFoldersViewModelTest.kt` | Tests del ViewModel (MockK + Turbine): E1/E3/E4/E5/E6. |
| Modificado | `Sonus/feature/settings/src/main/res/values/strings.xml` | Claves `settings_source_folders_*` (español). |
| Modificado | `Sonus/app/.../navigation/SonusNavHost.kt` | Ruta `settings_source_folders` + acceso desde Biblioteca. |
| Modificado | `Sonus/feature/library/.../presentation/LibraryLandingScreen.kt` | Botón "Agregar carpeta" → navega a gestión de Carpetas Fuente. |
| Modificado | `Sonus/feature/library/src/main/res/values/strings.xml` | Clave `library_add_source_folder`. |
| Creado | `docs/domain/stories/US-005-agregar-carpeta-fuente/refinamiento.md` | Plan técnico (step-01). |
| Modificado | `docs/domain/stories/US-005-agregar-carpeta-fuente/index.md` | Fase Refinamiento Técnico → ✅ Completada. |

### Métricas Dev-Rápido

- Tiempo sesión IA: 23 min
- Tareas manuales DoD: 0 min
- Tiempo total: 23 min
