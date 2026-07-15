package com.estebancoloradogonzalez.sonus.core.data.time

import com.estebancoloradogonzalez.sonus.core.domain.port.DispatcherProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

/** Production [DispatcherProvider] backed by the real coroutine dispatchers. */
class DefaultDispatcherProvider
    @Inject
    constructor() : DispatcherProvider {
        override val io: CoroutineDispatcher = Dispatchers.IO
        override val default: CoroutineDispatcher = Dispatchers.Default
    }
