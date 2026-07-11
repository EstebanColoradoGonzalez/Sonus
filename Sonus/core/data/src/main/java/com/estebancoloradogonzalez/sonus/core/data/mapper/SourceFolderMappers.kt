package com.estebancoloradogonzalez.sonus.core.data.mapper

import com.estebancoloradogonzalez.sonus.core.data.local.room.entity.SourceFolder as SourceFolderEntity
import com.estebancoloradogonzalez.sonus.core.domain.model.SourceFolder as SourceFolderDomain

/** Pure mapping functions between the Room `@Entity` and the domain model (blueprint §3). */

fun SourceFolderEntity.toDomain(): SourceFolderDomain =
    SourceFolderDomain(
        id = id,
        treeUri = treeUri,
        displayPath = displayPath,
        dateAddedMs = dateAddedMs,
    )

fun SourceFolderDomain.toEntity(): SourceFolderEntity =
    SourceFolderEntity(
        id = id,
        treeUri = treeUri,
        displayPath = displayPath,
        dateAddedMs = dateAddedMs,
    )
