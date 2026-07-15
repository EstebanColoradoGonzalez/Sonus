package com.estebancoloradogonzalez.sonus.core.domain.model

/**
 * Scan strategy (interfaces_contract §2.1, `TRG-LIB-03`).
 *
 * - [FULL]: total reconstruction — used by the foundational first-run scan (US-003), where the
 *   catalog is empty and there is no prior state to diff against.
 * - [INCREMENTAL]: diff by `fileLastModifiedMs` against the existing catalog (US-008).
 */
enum class ScanMode {
    FULL,
    INCREMENTAL,
}
