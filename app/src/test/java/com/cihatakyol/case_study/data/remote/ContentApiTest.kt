package com.cihatakyol.case_study.data.remote

import com.cihatakyol.case_study.sampleContentJson
import com.cihatakyol.case_study.testJson
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class ContentApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: ContentApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .addConverterFactory(testJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(ContentApi::class.java)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `fetches and deserializes the served payload`() = runTest {
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(sampleContentJson()),
        )

        val pages = api.getPages()

        assertEquals(2, pages.size)
        assertEquals("Main Page", pages.first().title)
        assertEquals("/pages", server.takeRequest().path)
    }

    @Test
    fun `propagates an HTTP error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        val failure = runCatching { api.getPages() }

        assertEquals(true, failure.isFailure)
    }
}
