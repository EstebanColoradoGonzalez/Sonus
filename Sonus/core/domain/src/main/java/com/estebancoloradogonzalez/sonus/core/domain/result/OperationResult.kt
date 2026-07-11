package com.estebancoloradogonzalez.sonus.core.domain.result

import com.estebancoloradogonzalez.sonus.core.domain.error.DomainError

/**
 * Punctual outcome of an operation that can fail meaningfully (interfaces_contract §2.0).
 *
 * Errors travel as values, never as thrown exceptions across the domain boundary (principle P1):
 * a [Failure] always wraps a typed [DomainError]. Consumed with an exhaustive `when` (no `else`).
 */
sealed interface OperationResult<out T> {
    data class Success<out T>(val data: T) : OperationResult<T>

    data class Failure(val error: DomainError) : OperationResult<Nothing>
}
