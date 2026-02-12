package com.ltquiz.test.di

import com.ltquiz.test.managers.IdentityManager
import com.ltquiz.test.managers.IdentityManagerImpl
import com.ltquiz.test.managers.QRCodeManager
import com.ltquiz.test.managers.QRCodeManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module for application-level dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {
    
    @Binds
    abstract fun bindIdentityManager(
        identityManagerImpl: IdentityManagerImpl
    ): IdentityManager
    
    @Binds
    abstract fun bindQRCodeManager(
        qrCodeManagerImpl: QRCodeManagerImpl
    ): QRCodeManager
}