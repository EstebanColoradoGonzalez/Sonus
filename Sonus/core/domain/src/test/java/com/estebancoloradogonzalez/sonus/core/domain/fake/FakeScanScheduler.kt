package com.estebancoloradogonzalez.sonus.core.domain.fake

import com.estebancoloradogonzalez.sonus.core.domain.model.ScanMode
import com.estebancoloradogonzalez.sonus.core.domain.port.ScanScheduler

/**
 * In-memory fake of [ScanScheduler] recording how many times each action was requested and the
 * [ScanMode] of every enqueued scan (so tests can assert mode propagation, US-007).
 */
class FakeScanScheduler : ScanScheduler {
    val enqueuedModes = mutableListOf<ScanMode>()
    var cancelCount = 0

    val enqueueCount: Int get() = enqueuedModes.size

    override fun enqueueScan(mode: ScanMode) {
        enqueuedModes.add(mode)
    }

    override fun cancel() {
        cancelCount++
    }
}
