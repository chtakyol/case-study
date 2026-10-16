package com.cihatakyol.case_study.di

import android.content.Context
import androidx.room.Room
import com.cihatakyol.case_study.data.local.database.AppDatabase
import com.cihatakyol.case_study.data.local.database.ContentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "content.db").build()

    @Provides
    fun provideContentDao(database: AppDatabase): ContentDao = database.contentDao()
}
