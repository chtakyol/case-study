package com.cihatakyol.case_study.di

import android.content.Context
import com.cihatakyol.case_study.BuildConfig
import com.cihatakyol.case_study.data.remote.FakeContentInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Singleton
import okhttp3.Interceptor

/**
 * Debug-only network wiring. Contributes a fixture-serving interceptor so debug builds show content
 * without a reachable backend; release builds have no such binding.
 */
@Module
@InstallIn(SingletonComponent::class)
object DebugNetworkModule {

    @Provides
    @IntoSet
    @Singleton
    fun provideFakeContentInterceptor(@ApplicationContext context: Context): Interceptor = FakeContentInterceptor(
        isEnabled = BuildConfig.USE_FAKE_CONTENT,
        loadPayload = {
            context.assets.open(FakeContentInterceptor.FIXTURE_ASSET)
                .use { it.readBytes().decodeToString() }
        },
    )
}
