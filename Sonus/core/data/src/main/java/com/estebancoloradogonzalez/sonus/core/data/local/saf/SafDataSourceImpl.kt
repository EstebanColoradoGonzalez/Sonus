package com.estebancoloradogonzalez.sonus.core.data.local.saf

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * SAF implementation of [SafDataSource] over `DocumentsContract` (ADR-003). Traverses the tree
 * iteratively (no recursion depth limit) collecting documents whose MIME type starts with `audio`.
 * Never touches `MediaStore` (AC7). A lost permission throws `SecurityException`, propagated to the
 * border.
 */
class SafDataSourceImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : SafDataSource {
        override suspend fun listAudioFiles(treeUri: String): List<DiscoveredFile> {
            val treeRoot = Uri.parse(treeUri)
            val result = mutableListOf<DiscoveredFile>()
            val pending = ArrayDeque<String>()
            pending.addLast(DocumentsContract.getTreeDocumentId(treeRoot))
            while (pending.isNotEmpty()) {
                val parentDocumentId = pending.removeLast()
                collectChildren(treeRoot, parentDocumentId, pending, result)
            }
            return result
        }

        private fun collectChildren(
            treeRoot: Uri,
            parentDocumentId: String,
            pending: ArrayDeque<String>,
            result: MutableList<DiscoveredFile>,
        ) {
            val childrenUri =
                DocumentsContract.buildChildDocumentsUriUsingTree(treeRoot, parentDocumentId)
            context.contentResolver
                .query(childrenUri, PROJECTION, null, null, null)
                ?.use { cursor ->
                    while (cursor.moveToNext()) {
                        val documentId = cursor.getString(INDEX_DOCUMENT_ID)
                        val mimeType = cursor.getString(INDEX_MIME_TYPE).orEmpty()
                        val lastModified = cursor.getLong(INDEX_LAST_MODIFIED)
                        if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                            pending.addLast(documentId)
                        } else if (mimeType.startsWith(AUDIO_MIME_PREFIX)) {
                            val fileUri =
                                DocumentsContract.buildDocumentUriUsingTree(treeRoot, documentId)
                            result.add(DiscoveredFile(fileUri.toString(), mimeType, lastModified))
                        }
                    }
                }
        }

        private companion object {
            const val AUDIO_MIME_PREFIX = "audio/"
            const val INDEX_DOCUMENT_ID = 0
            const val INDEX_MIME_TYPE = 1
            const val INDEX_LAST_MODIFIED = 2
            val PROJECTION =
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                    DocumentsContract.Document.COLUMN_LAST_MODIFIED,
                )
        }
    }
