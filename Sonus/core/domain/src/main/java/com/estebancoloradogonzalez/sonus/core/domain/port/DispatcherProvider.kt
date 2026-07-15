package com.estebancoloradogonzalez.sonus.core.domain.port

import kotlinx.coroutines.CoroutineDispatcher

/**
 * Injectable coroutine dispatchers (coding-standards §5): I/O is never pinned to a fixed
 * `Dispatchers.IO` so tests can substitute a deterministic test dispatcher.
 */
interface DispatcherProvider {
    val io: CoroutineDispatcher

    val default: CoroutineDispatcher
}
