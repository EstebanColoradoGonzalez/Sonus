package com.estebancoloradogonzalez.sonus

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** Application entry point that hosts the Hilt dependency graph (ADR-008). */
@HiltAndroidApp
class SonusApplication : Application()
