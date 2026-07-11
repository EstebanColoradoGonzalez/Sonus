package com.estebancoloradogonzalez.sonus.core.data.time

import com.estebancoloradogonzalez.sonus.core.domain.port.TimeProvider
import javax.inject.Inject

/** Implements [TimeProvider] with the real system clock (epoch ms UTC). */
class SystemTimeProvider
    @Inject
    constructor() : TimeProvider {
        override fun nowMs(): Long = System.currentTimeMillis()
    }
