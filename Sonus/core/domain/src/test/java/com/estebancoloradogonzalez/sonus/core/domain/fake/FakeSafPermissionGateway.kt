package com.estebancoloradogonzalez.sonus.core.domain.fake

import com.estebancoloradogonzalez.sonus.core.domain.port.SafPermissionGateway

/**
 * In-memory fake of [SafPermissionGateway] with configurable outcomes and inspectable state (no
 * business rule; coding-standards §5.3).
 */
class FakeSafPermissionGateway(
    private val grantPermission: Boolean = true,
    private val displayPath: String = "Music",
    private val persisted: Boolean = true,
) : SafPermissionGateway {
    val releasedUris = mutableListOf<String>()

    override suspend fun takePersistablePermission(treeUri: String): Boolean = grantPermission

    override suspend fun releasePersistablePermission(treeUri: String) {
        releasedUris += treeUri
    }

    override fun resolveDisplayPath(treeUri: String): String = displayPath

    override suspend fun hasPersistedPermission(treeUri: String): Boolean = persisted
}
