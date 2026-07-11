package com.estebancoloradogonzalez.sonus.core.data.saf

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.estebancoloradogonzalez.sonus.core.domain.port.SafPermissionGateway
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Android implementation of [SafPermissionGateway] over `ContentResolver` + `DocumentFile`
 * (ADR-003, canal C5). It is the single storage-access mechanism: no `MediaStore`, no media-runtime
 * permissions, no network (AC8). Infrastructure exceptions are caught here and surfaced as values
 * (principle P1); none crosses the domain boundary.
 */
class SafPermissionGatewayImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : SafPermissionGateway {
        override suspend fun takePersistablePermission(treeUri: String): Boolean =
            try {
                context.contentResolver.takePersistableUriPermission(Uri.parse(treeUri), PERSISTABLE_FLAGS)
                true
            } catch (securityException: SecurityException) {
                false
            }

        override suspend fun releasePersistablePermission(treeUri: String) {
            try {
                context.contentResolver.releasePersistableUriPermission(Uri.parse(treeUri), PERSISTABLE_FLAGS)
            } catch (securityException: SecurityException) {
                // The permission was already released or never held: nothing to undo (idempotent).
            }
        }

        override fun resolveDisplayPath(treeUri: String): String {
            val uri = Uri.parse(treeUri)
            return DocumentFile.fromTreeUri(context, uri)?.name ?: uri.lastPathSegment ?: treeUri
        }

        override suspend fun hasPersistedPermission(treeUri: String): Boolean {
            val uri = Uri.parse(treeUri)
            return context.contentResolver.persistedUriPermissions.any {
                it.uri == uri && it.isReadPermission
            }
        }

        private companion object {
            const val PERSISTABLE_FLAGS =
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        }
    }
