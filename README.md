# Sonus

> Reproductor de audio **100 % local, soberano y sin red** (*air-gapped*) para Android. Sonus gobierna la relación entre una persona y su colección de audio almacenada en el dispositivo, manteniéndola en orden, con reproducción continua y bajo control absoluto del usuario.

> **Estado:** en definición / pre-implementación. El repositorio contiene la documentación de negocio y arquitectura, el mapa de historias y la primera historia de usuario. El proyecto de código Android está inicializado pero **aún no tiene lógica implementada**.

## Qué hace

Sonus resuelve el desorden del audio local (metadatos incompletos, dependencia de plataformas de streaming, cortes de reproducción en segundo plano, interfaces anticuadas) ofreciendo una experiencia de calidad sin ceder soberanía ni privacidad. Se sostiene sobre tres equilibrios: **Organización**, **Continuidad** y **Soberanía**.

Capacidades previstas (ver `docs/domain/definition/requirements_specification.md`):

- **Biblioteca:** selección de Carpetas Fuente, escaneo e indexación de etiquetas ID3 sin inventar datos, y sincronización determinista del catálogo (altas, ausencias y purga).
- **Exploración:** navegación taxonómica (género, artista, álbum, tipo de contenido) y agrupaciones personalizadas, con filtro textual y ordenamiento.
- **Reproducción:** reproducción por contexto, cola multimodal (secuencial, aleatorio reversible, repetición), controles de transporte y mutación de la cola en tiempo real.
- **Resiliencia:** supervivencia en segundo plano (Foreground Service + notificación persistente), gestión del foco de audio, corte de seguridad ante desconexión de la salida y restauración de sesión tras una terminación forzada del sistema.
- **Curación:** edición de metadatos ID3, gestión de playlists y eliminación física de archivos con confirmación explícita.

Principios inquebrantables: **sin conexión a red ni telemetría**, **sin datos de comportamiento del usuario**, **sin publicidad ni monetización**, y **nunca se inventan metadatos**.

## Stack tecnológico

Stack objetivo definido en los ADR de `docs/architecture/architecture_blueprint.md`:

- **Lenguaje / plataforma:** Kotlin sobre Android nativo (`minSdk 26`, `targetSdk`/`compileSdk 35`).
- **Arquitectura:** Arquitectura Limpia en 3 capas (presentación → dominio ← datos), patrón **MVVM** y enfoque **Single-Activity**.
- **UI:** Jetpack Compose + Material 3 + Navigation Compose; asincronía con Coroutines y `Flow`/`StateFlow`.
- **Persistencia:** Room sobre SQLite (ADR-001).
- **Reproducción:** AndroidX Media3 — ExoPlayer + MediaSession (ADR-002), como Foreground Service `mediaPlayback` (ADR-007).
- **Acceso a archivos:** Storage Access Framework (SAF) en lugar de MediaStore (ADR-003) + librería local de metadatos ID3 (ADR-004).
- **Otros:** WorkManager para el escaneo en segundo plano (ADR-006), Hilt para inyección de dependencias (ADR-008), Coil con caché solo en memoria para carátulas (ADR-009).
- **Autarquía verificable:** sin `android.permission.INTERNET` ni SDKs de red/analítica (ADR-010).

> El scaffold actual incluye únicamente las dependencias base de Compose/Material 3; el resto del stack se irá incorporando conforme avance la implementación de las historias.

Build: Gradle con Kotlin DSL y catálogo de versiones (`gradle/libs.versions.toml`).

## Requisitos previos

- **JDK 11** o superior.
- **Android Studio** (versión reciente) con el **Android SDK API 35**.
- Un **dispositivo o emulador con Android 8.0 (API 26) o superior**.
- **Git** para el control de versiones.

## Compilar y ejecutar

El proyecto Android vive en el subdirectorio `Sonus/`. Los comandos usan el Gradle Wrapper incluido (en Windows, reemplaza `./gradlew` por `gradlew.bat`).

```bash
# 1. Clonar el repositorio
git clone <url-del-repositorio>
cd Sonus/Sonus

# 2. Configurar el SDK (si no lo genera Android Studio)
#    Crear/ajustar local.properties con: sdk.dir=<ruta-al-Android-SDK>

# 3. Compilar el APK de depuración
./gradlew :app:assembleDebug

# 4. Instalar en un dispositivo/emulador conectado
./gradlew :app:installDebug

# 5. Ejecutar pruebas
./gradlew test                    # pruebas unitarias (JVM)
./gradlew connectedAndroidTest    # pruebas instrumentadas (requiere dispositivo)
```

También puedes abrir la carpeta `Sonus/` directamente en Android Studio y usar el botón **Run**.

## Estructura del proyecto

```
Sonus/
├── docs/                         # Documentación del producto (fuente de verdad)
│   ├── domain/
│   │   ├── definition/           # Definición del sistema (SDD) y requerimientos (RF/RNF)
│   │   └── stories/              # Mapa de historias + historias de usuario
│   └── architecture/             # Blueprint, modelo de dominio/estado, contrato de interfaces, estándares
├── Sonus/                        # Proyecto Android (Gradle)
│   ├── app/                      # Módulo de aplicación (:app)
│   ├── gradle/                   # Wrapper y catálogo de versiones (libs.versions.toml)
│   └── settings.gradle.kts
├── .ceiba-metodo/                # Activos del Método Ceiba (agentes y flujos de trabajo)
├── .github/ · .vscode/           # Configuración de CI/editor
└── README.md
```

## Documentación

- **Definición del Sistema (SDD)** — `docs/domain/definition/system_definition_document.md` · fuente de verdad del negocio.
- **Requerimientos (RF/RNF)** — `docs/domain/definition/requirements_specification.md`.
- **Blueprint de Arquitectura** — `docs/architecture/architecture_blueprint.md` · contexto, contenedores, componentes y ADR.
- **Modelo de Dominio y Estado** — `docs/architecture/domain_and_state_model.md`.
- **Contrato de Interfaces** — `docs/architecture/interfaces_contract.md`.
- **Estándares de Código** — `docs/architecture/coding-standards.md`.
- **Mapa de Historias** — `docs/domain/stories/story_mapping_index.md` · épicas, historias y plan de releases.

## Licencia

Proyecto personal. Todos los derechos reservados.
