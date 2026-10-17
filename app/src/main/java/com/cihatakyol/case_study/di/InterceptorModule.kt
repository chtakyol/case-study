package com.cihatakyol.case_study.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import okhttp3.Interceptor

/**
 * Declares the OkHttp interceptor set so any module (for example a debug-only one) can contribute to
 * it without editing [NetworkModule].
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class InterceptorModule {
    @Multibinds
    abstract fun interceptors(): Set<Interceptor>
}
