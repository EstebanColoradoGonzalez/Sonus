package com.estebancoloradogonzalez.sonus

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * Application entry point that hosts the Hilt dependency graph (ADR-008) and configures WorkManager
 * on-demand with the [HiltWorkerFactory], so `@HiltWorker` workers (the `LibraryScanWorker`, US-003)
 * receive their injected dependencies.
 */
@HiltAndroidApp
class SonusApplication :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() =
            Configuration.Builder()
                .setWorkerFactory(workerFactory)
                .build()
}
