package com.estebancoloradogonzalez.sonus.core.domain.error

/**
 * Typed failure contract of the domain (interfaces_contract §3.1). Travels inside
 * [com.estebancoloradogonzalez.sonus.core.domain.result.OperationResult.Failure].
 *
 * There is no `message` field: the human-readable text is resolved in the presentation layer from
 * [code] (i18n decoupled from data, §3.0 P5). Only the subtypes required so far are declared; the
 * hierarchy grows story by story.
 */
sealed class DomainError(
    val code: String,
    val severity: Severity,
    val recoverable: Boolean,
) {
    abstract val details: ErrorDetails?

    /** The OS did not grant the persistable permission over the `treeUri` (`TRG-LIB-01`, §3.2). */
    data object PermissionDenied : DomainError(
        code = "ERR_PERMISSION_DENIED",
        severity = Severity.ERROR,
        recoverable = true,
    ) {
        override val details: ErrorDetails? = null
    }

    /** The `treeUri` was already registered as a source folder (`TRG-LIB-01`, §3.2). */
    data object DuplicateSourceFolder : DomainError(
        code = "ERR_DUPLICATE_SOURCE_FOLDER",
        severity = Severity.WARNING,
        recoverable = true,
    ) {
        override val details: ErrorDetails? = null
    }

    /** The referenced entity id does not exist (`TRG-LIB-02`, §3.2). */
    data class EntityNotFound(
        val kind: String,
        val id: Long,
    ) : DomainError(
            code = "ERR_ENTITY_NOT_FOUND",
            severity = Severity.ERROR,
            recoverable = true,
        ) {
        override val details: ErrorDetails = ErrorDetails.Entity(kind, id)
    }
}

/** Determines the notification channel of a failure (interfaces_contract §3.1). */
enum class Severity {
    INFO,
    WARNING,
    ERROR,
}

/** Typed, bounded failure context. Never carries network or telemetry identifiers ([RNF-06]). */
sealed interface ErrorDetails {
    data class Entity(val kind: String, val id: Long) : ErrorDetails

    data class Field(val name: String, val constraint: String) : ErrorDetails

    data class Io(val cause: IoCauseCode) : ErrorDetails

    data class Cause(val error: DomainError) : ErrorDetails
}

/** Bounded local I/O cause code (never a remote stacktrace, §3.1). */
enum class IoCauseCode {
    NOT_FOUND,
    ACCESS_DENIED,
    WRITE_FAILED,
    DECODE_FAILED,
    UNKNOWN_LOCAL,
}
