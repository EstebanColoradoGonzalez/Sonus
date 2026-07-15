package com.estebancoloradogonzalez.sonus.core.domain.fake

import com.estebancoloradogonzalez.sonus.core.domain.port.ScanScheduler

/** In-memory fake of [ScanScheduler] counting how many times each action was requested. */
class FakeScanScheduler : ScanScheduler {
    var enqueueCount = 0
    var cancelCount = 0

    override fun enqueueFullScan() {
        enqueueCount++
    }

    override fun cancel() {
        cancelCount++
    }
}
