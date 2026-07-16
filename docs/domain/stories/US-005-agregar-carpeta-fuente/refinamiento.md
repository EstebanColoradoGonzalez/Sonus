# Refinamiento Técnico (Developer)

**Autor**: Esteban Colorado González | **Fecha**: 2026-07-15

## Plan: Agregar una Carpeta Fuente (post-onboarding)

**Arquitectura**: Clean Architecture 3 capas (Presentación → Dominio ← Datos), MVVM, Single-Activity, Hilt (blueprint §3; ADR-005/008). Comandos `sealed interface`, estado `StateFlow<UiState>` inmutable, eventos one-shot por `Channel`, errores como valor (`OperationResult`/`DomainError`, coding-standards §4.2/§4.3).

**Feature análoga leída completa**: US-002 (selección inicial de Carpetas Fuente en onboarding). Comparte por completo dominio y datos; solo cambia la presentación (contexto operativo vs. onboarding) y suma la regla de **solapamiento**.

### Reutilización directa (NO se modifica — preserva US-002)

- Dominio: `AddSourceFolderUseCase`, `ObserveSourceFoldersUseCase`, `SourceFolderRepository`, `SafPermissionGateway`, `LibraryCommand.AddSourceFolder`, `OperationResult`, `DomainError.{DuplicateSourceFolder, PermissionDenied}`, `Severity`, modelo `SourceFolder`, `TimeProvider`.
- Datos: `SourceFolderRepositoryImpl`, `SourceFolderDao` (índice único `treeUri`), `@Entity SourceFolder`, `SafPermissionGatewayImpl` (`takePersistableUriPermission`, `resolveDisplayPath`), mappers, `LibraryModule` (Hilt).
- Escenarios ya cubiertos por el dominio reutilizado: **E1** (alta), **E2** (preservación del Catálogo — el alta no toca `Track`), **E3** (duplicado exacto `treeUri` → `ERR_DUPLICATE_SOURCE_FOLDER`), **E5** (cancelación / `ERR_PERMISSION_DENIED`), **E7** (persistencia SAF, ya verificada en US-002), **E8** (autarquía: solo SAF, sin `INTERNET`/`MediaStore`/media runtime — invariante estructural del proyecto).

### Nuevo de US-005 (frontera de esta historia)

- **E4** Solapamiento (anidada/contenedora): advertir pero permitir → **nueva lógica de dominio**.
- **E6** Contenido pendiente de escanear: **comunicar** tras el alta (el disparo del escaneo es US-007, fuera de alcance).
- Presentación **post-onboarding**: pantalla de Configuración "Carpetas Fuente" (no existe aún) + acceso desde Biblioteca.
- **Fuera de alcance**: remover carpeta (US-006), ejecutar escaneo (US-007), sincronizar Catálogo (US-008), observar progreso (US-009).

### Decisión de diseño — Solapamiento

`AddSourceFolderUseCase` se conserva intacto (Success XOR Failure; el solapamiento **no** es un fallo: la carpeta se registra). El aviso de solapamiento es una **consulta no bloqueante** evaluada en dominio con una función pura sobre el `treeUri` codificado (ancestro/descendiente por prefijo `%2F`, sin decodificar → Kotlin puro, sin `android.*`/`java.net`). El ViewModel: (1) detecta solapamiento contra las carpetas existentes, (2) invoca `AddSourceFolderUseCase`, (3) en `Success` emite confirmación + aviso de solapamiento (si aplica) + aviso de contenido pendiente. Orden: la detección corre **antes** del alta para comparar solo contra carpetas previas.

### Tareas de Implementación

#### Fase 1 — Dominio (solapamiento)

- [ ] **T1: Función pura de comparación de árboles SAF** — `Sonus/core/domain/.../usecase/SourceFolderTreeUri.kt` — `object` con `overlaps(a, b): Boolean` (relación de ancestro/descendiente propia por prefijo `%2F` del document-id tras `/tree/`; excluye igualdad, que es duplicado). Kotlin puro. (Base: patrón de utilidades de dominio del proyecto)
- [ ] **T2: `DetectSourceFolderOverlapUseCase`** — `Sonus/core/domain/.../usecase/DetectSourceFolderOverlapUseCase.kt` — `@Inject constructor(repository: SourceFolderRepository)`; `suspend operator fun invoke(candidateTreeUri: String): Boolean` lee `observeAll().first()` y aplica `SourceFolderTreeUri.overlaps`. (Base: `AddSourceFolderUseCase.kt`) — depende de T1.

#### Fase 2 — Presentación (nuevo paquete `settings` operativo)

- [ ] **T3: `SettingsSourceFoldersUiState`** — `Sonus/feature/settings/.../presentation/settings/SettingsSourceFoldersUiState.kt` — `folders: List<SourceFolderUi>`, `hasPendingScanContent: Boolean`. (Base: `SourceFoldersUiState.kt`)
- [ ] **T4: `SettingsSourceFoldersCommand`** — mismo paquete — `AddFolderClicked`, `FolderPicked(treeUri)`, `SelectionCancelled` (sin `RemoveFolder`/`Continue`: fuera de alcance). (Base: `SourceFoldersCommand.kt`)
- [ ] **T5: `SettingsSourceFoldersEvent`** — mismo paquete — `LaunchFolderPicker`, `NotifyFolderAdded`, `NotifyOverlap`, `NotifyDuplicate`, `NotifyPermissionDenied`, `NotifySelectionCancelled`. (Base: `SourceFoldersEvent.kt`)
- [ ] **T6: `SettingsSourceFoldersViewModel`** — mismo paquete — `@HiltViewModel`; observa `ObserveSourceFoldersUseCase`; compone `DetectSourceFolderOverlapUseCase` + `AddSourceFolderUseCase`; en `Success` marca `hasPendingScanContent=true` y emite confirmación (+overlap si aplica); mapea `DomainError → Event`. (Base: `SourceFoldersViewModel.kt`) — depende de T2/T3/T4/T5.
- [ ] **T7: `SettingsSourceFoldersScreen`** — mismo paquete — dueña del launcher SAF `OpenDocumentTree`; lista de carpetas (solo lectura, con `displayPath`); botón "Agregar carpeta"; banner de "contenido pendiente de escanear" (afordancia de re-escaneo documentada como diferida a US-007); snackbars por evento. (Base: `SourceFoldersScreen.kt`) — depende de T6/T8.
- [ ] **T8: Recursos de texto** — `Sonus/feature/settings/src/main/res/values/strings.xml` — claves `settings_source_folders_*`: título, lista, add, confirmación de alta, aviso de solapamiento, duplicado, permiso denegado, cancelación, banner de contenido pendiente. Valor español, clave inglés `snake_case` (coding-standards §2.1).

#### Fase 3 — Navegación e integración (`:app` + `:feature:library`)

- [ ] **T9: Ruta `SETTINGS` en el grafo** — `Sonus/app/.../navigation/SonusNavHost.kt` — nueva `composable(SETTINGS)` que hospeda `SettingsSourceFoldersScreen`; navegación con back estándar. (Base: rutas `SOURCE_FOLDERS`/`LIBRARY` existentes)
- [ ] **T10: Acceso desde Biblioteca** — `Sonus/feature/library/.../presentation/LibraryLandingScreen.kt` — acción "Agregar carpeta"/Configuración que eleva `onNavigateToSettings` hacia el `NavHost`. (Base: elevación de eventos de `ScanScreen`)

> DI: `SettingsSourceFoldersViewModel` es `@HiltViewModel`; los casos de uso son `@Inject constructor` (dominio puro) → sin módulo Hilt nuevo. `DetectSourceFolderOverlapUseCase` se resuelve por constructor.

#### Fase 4 — Pruebas (obligatorio, coding-standards §5)

- [ ] **T11: `DetectSourceFolderOverlapUseCaseTest`** — `Sonus/core/domain/src/test/.../usecase/DetectSourceFolderOverlapUseCaseTest.kt` — JUnit5 + Truth + `FakeSourceFolderRepository`; casos: ancestro, descendiente, hermano (no solapa), no relacionado (no solapa), sin carpetas previas. Patrón 3A, nombres en backticks. (Base: `AddSourceFolderUseCaseTest.kt`)
- [ ] **T12: `SettingsSourceFoldersViewModelTest`** — `Sonus/feature/settings/src/test/.../presentation/settings/SettingsSourceFoldersViewModelTest.kt` — JUnit5 + MockK + Turbine; casos: `AddFolderClicked→LaunchFolderPicker`; alta OK → `NotifyFolderAdded` + `hasPendingScanContent=true`; alta con solapamiento → `NotifyOverlap`; `DuplicateSourceFolder→NotifyDuplicate`; `PermissionDenied→NotifyPermissionDenied`; cancelación → `NotifySelectionCancelled`. (Base: `SourceFoldersViewModelTest.kt`)

### Archivos relevantes (estudiados)

- `Sonus/core/domain/.../usecase/AddSourceFolderUseCase.kt` — patrón caso de uso + reutilización.
- `Sonus/core/domain/.../port/SourceFolderRepository.kt` — `observeAll()`/`exists()`.
- `Sonus/feature/settings/.../onboarding/SourceFolders{ViewModel,Screen,Command,Event,UiState}.kt` — patrón presentación completo a replicar.
- `Sonus/app/.../navigation/SonusNavHost.kt` — grafo Single-Activity.
- `Sonus/core/domain/src/test/.../AddSourceFolderUseCaseTest.kt` + fakes (`FakeSourceFolderRepository`, `FakeSafPermissionGateway`, `FixedTimeProvider`) — patrón de test de dominio.
- `Sonus/feature/settings/src/test/.../SourceFoldersViewModelTest.kt` — patrón de test de ViewModel (MockK + Turbine).
- `docs/architecture/interfaces_contract.md` §`TRG-LIB-01`, §3.2 (`ERR_DUPLICATE_SOURCE_FOLDER`/`ERR_PERMISSION_DENIED`).

### Checklist

☑ Feature análoga (US-002) leída completa | ☑ TODOS los artefactos identificados (dominio, datos, presentación, DI, navegación, tests, strings) | ☑ Respeta arquitectura (regla de dependencias, sufijos por capa, errores como valor) | ☑ Incluye tests (dominio + ViewModel) | ☑ Preserva US-002 sin cambios
