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
        assertEquals("/lumiform-android-test.json", server.takeRequest().path)
    }

    @Test
    fun `deserializes a payload served as plain text`() = runTest {
        // GitHub serves gist payloads as text/plain, so the converter must not depend on the media type.
        server.enqueue(
            MockResponse()
                .setHeader("Content-Type", "text/plain; charset=utf-8")
                .setBody(sampleContentJson()),
        )

        val pages = api.getPages()

        assertEquals(2, pages.size)
    }

    @Test
    fun `propagates an HTTP error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))

        val failure = runCatching { api.getPages() }

        assertEquals(true, failure.isFailure)
    }
}
