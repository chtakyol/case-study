@file:OptIn(ExperimentalCoroutinesApi::class)

package com.cihatakyol.case_study

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.cihatakyol.case_study.data.local.database.AppDatabase
import com.cihatakyol.case_study.data.remote.ContentApi
import com.cihatakyol.case_study.data.remote.FakeContentInterceptor
import com.cihatakyol.case_study.data.repository.ContentRepositoryImpl
import com.cihatakyol.case_study.domain.model.Result
import com.cihatakyol.case_study.domain.model.Section
import com.cihatakyol.case_study.domain.model.Text
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

@RunWith(RobolectricTestRunner::class)
class DebugFixtureEndToEndTest {

    private lateinit var database: AppDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `debug fixture interceptor feeds the document through to the domain tree`() = runTest {
        val payload = debugContentFixtureJson()
        val client = OkHttpClient.Builder()
            .addInterceptor(FakeContentInterceptor(isEnabled = true) { payload })
            .build()
        val api = Retrofit.Builder()
            .baseUrl("https://api.example.com/")
            .client(client)
            .addConverterFactory(testJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ContentApi::class.java)
        val repository = ContentRepositoryImpl(database.contentDao(), api, UnconfinedTestDispatcher())

        assertEquals(Result.Success(Unit), repository.refreshPages())

        val pages = repository.observePages().first()
        assertEquals(listOf("Main Page", "Second Page"), pages.map { it.title })

        val chapter1 = pages[0].items.filterIsInstance<Section>().first { it.title == "Chapter 1" }
        assertTrue(chapter1.items.filterIsInstance<Section>().single().items.any { it is Text })
    }
}
