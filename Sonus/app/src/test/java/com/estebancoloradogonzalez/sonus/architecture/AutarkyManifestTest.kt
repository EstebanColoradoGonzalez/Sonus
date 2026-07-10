package com.estebancoloradogonzalez.sonus.architecture

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import java.io.File

/**
 * US-001 · AC6 — Autarquía verificable ([RNF-06] / Invariante 1 / ADR-010).
 *
 * Guards the app manifest against network and media-runtime permissions, and confirms
 * POST_NOTIFICATIONS is declared as the single runtime permission.
 */
class AutarkyManifestTest {
    private val manifest: String by lazy { readAppManifest() }

    @Test
    fun `does not declare the INTERNET permission`() {
        assertThat(manifest).doesNotContain("android.permission.INTERNET")
    }

    @Test
    fun `does not declare media runtime permissions`() {
        assertThat(manifest).doesNotContain("READ_MEDIA_AUDIO")
        assertThat(manifest).doesNotContain("READ_EXTERNAL_STORAGE")
    }

    @Test
    fun `declares POST_NOTIFICATIONS as the runtime permission`() {
        assertThat(manifest).contains("android.permission.POST_NOTIFICATIONS")
    }

    private fun readAppManifest(): String {
        val candidates =
            listOf(
                File("src/main/AndroidManifest.xml"),
                File("app/src/main/AndroidManifest.xml"),
            )
        val file =
            candidates.firstOrNull { it.exists() }
                ?: error("AndroidManifest.xml not found from ${File("").absolutePath}")
        // Strip XML comments so the assertions target actual declarations, not documentation
        // (the manifest documents which permissions are deliberately absent).
        return file.readText().replace(COMMENT_REGEX, "")
    }

    private companion object {
        val COMMENT_REGEX = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
    }
}
