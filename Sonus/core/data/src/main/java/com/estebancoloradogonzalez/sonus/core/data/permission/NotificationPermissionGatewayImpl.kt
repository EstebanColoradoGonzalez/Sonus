package com.estebancoloradogonzalez.sonus.core.data.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.estebancoloradogonzalez.sonus.core.domain.port.NotificationPermissionGateway
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Android implementation of [NotificationPermissionGateway].
 *
 * Reads the runtime permission state from the OS. Only the application [Context] is needed here:
 * the rationale dialog, the launch of the system dialog and the permanent-denial detection live in
 * the presentation edge (they require the hosting `Activity`).
 */
class NotificationPermissionGatewayImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : NotificationPermissionGateway {
        override fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

        override fun isGranted(): Boolean {
            if (!isSupported()) return true
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
