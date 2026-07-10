package com.estebancoloradogonzalez.sonus.di

import com.estebancoloradogonzalez.sonus.core.data.permission.NotificationPermissionGatewayImpl
import com.estebancoloradogonzalez.sonus.core.domain.port.NotificationPermissionGateway
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Wires the domain port [NotificationPermissionGateway] to its Android implementation.
 *
 * The binding lives in `:app` because it is the only module that knows both the domain contract and
 * the data implementation (Clean Architecture: composition root).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PermissionModule {
    @Binds
    abstract fun bindNotificationPermissionGateway(
        impl: NotificationPermissionGatewayImpl,
    ): NotificationPermissionGateway
}
