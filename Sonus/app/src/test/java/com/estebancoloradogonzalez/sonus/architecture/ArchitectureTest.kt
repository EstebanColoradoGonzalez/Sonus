package com.estebancoloradogonzalez.sonus.architecture

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test

/**
 * Verifiable architecture rules (coding-standards §6, Konsist): the dependency direction points
 * inward and the layer is declared by the type suffix.
 */
class ArchitectureTest {
    @Test
    fun `domain layer does not depend on Android`() {
        Konsist.scopeFromProject()
            .files
            .filter { it.packagee?.name?.startsWith(DOMAIN_PACKAGE) == true }
            .assertFalse { file ->
                file.imports.any { import ->
                    import.name.startsWith("android.") || import.name.startsWith("androidx.")
                }
            }
    }

    @Test
    fun `use cases reside in the domain usecase package`() {
        Konsist.scopeFromProject()
            .classes()
            .withNameEndingWith("UseCase")
            .assertTrue { it.resideInPackage("..core.domain.usecase..") }
    }

    @Test
    fun `impl classes reside in the data layer`() {
        Konsist.scopeFromProject()
            .classes()
            .withNameEndingWith("Impl")
            .assertTrue { it.resideInPackage("..core.data..") }
    }

    private companion object {
        const val DOMAIN_PACKAGE = "com.estebancoloradogonzalez.sonus.core.domain"
    }
}
