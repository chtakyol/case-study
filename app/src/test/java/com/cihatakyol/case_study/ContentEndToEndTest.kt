@file:OptIn(ExperimentalCoroutinesApi::class)

package com.cihatakyol.case_study

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.cihatakyol.case_study.data.local.database.AppDatabase
import com.cihatakyol.case_study.data.remote.ContentApi
import com.cihatakyol.case_study.data.repository.ContentRepositoryImpl
import com.cihatakyol.case_study.domain.model.Choice
import com.cihatakyol.case_study.domain.model.Result
import com.cihatakyol.case_study.domain.model.Section
import com.cihatakyol.case_study.domain.model.Text
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
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
class ContentEndToEndTest {

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
    fun `fake interceptor feeds the payload through to the domain tree`() = runTest {
        val client = OkHttpClient.Builder()
            .addInterceptor(FakeContentInterceptor(sampleContentJson()))
            .build()
        val api = Retrofit.Builder()
            .baseUrl("https://content.test/")
            .client(client)
            .addConverterFactory(testJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ContentApi::class.java)
        val repository = ContentRepositoryImpl(database.contentDao(), api, UnconfinedTestDispatcher())

        assertEquals(Result.Success(Unit), repository.refreshPages())

        val pages = repository.observePages().first()
        assertEquals(listOf("Main Page", "Second Page"), pages.map { it.title })

        val main = pages[0]
        val chapter1 = main.items.filterIsInstance<Section>().first { it.title == "Chapter 1" }
        val subsection = chapter1.items.filterIsInstance<Section>().single()
        assertEquals("Subsection 1.1", subsection.title)
        assertTrue(subsection.items.any { it is Text })

        val chapter2 = pages[1].items.filterIsInstance<Section>().single()
        val choices = chapter2.items.filterIsInstance<Choice>()
        assertEquals(2, choices.size)
        assertEquals(3, choices[0].responseSet.responses.size)
        assertTrue(choices[1].responseSet.multipleSelection)
    }
}

private class FakeContentInterceptor(private val body: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response = Response.Builder()
        .request(chain.request())
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(body.toResponseBody("application/json".toMediaType()))
        .build()
}
