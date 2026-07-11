package com.estebancoloradogonzalez.sonus.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.verify.assertFalse
import org.junit.jupiter.api.Test

/**
 * US-002 · AC8 — Autarquía verificable: el acceso al almacenamiento es exclusivamente por SAF.
 * Ningún archivo importa `MediaStore` ni permisos de media runtime (ADR-003 / [CT-05] / Invariante 1).
 */
class NoMediaStoreTest {
    @Test
    fun `no file imports MediaStore or media runtime permissions`() {
        Konsist.scopeFromProject()
            .files
            .assertFalse { file ->
                file.imports.any { import ->
                    import.name.startsWith("android.provider.MediaStore") ||
                        import.name.contains("READ_MEDIA_AUDIO") ||
                        import.name.contains("READ_EXTERNAL_STORAGE")
                }
            }
    }
}
