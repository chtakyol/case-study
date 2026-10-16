package com.cihatakyol.case_study.data.remote

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * Debug-only interceptor that answers `GET pages` from a bundled fixture, so a debug build renders
 * content even though the configured base URL is a placeholder host. When [isEnabled] is false it
 * passes requests through untouched, letting a developer point at a real server.
 */
class FakeContentInterceptor(private val isEnabled: Boolean, private val loadPayload: () -> String) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val servesContentDocument = request.method == "GET" &&
            request.url.encodedPath.trimEnd('/').endsWith(CONTENT_PATH)
        if (!isEnabled || !servesContentDocument) {
            return chain.proceed(request)
        }
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(loadPayload().toResponseBody(JSON_MEDIA_TYPE))
            .build()
    }

    companion object {
        const val FIXTURE_ASSET = "content_pages.json"

        private const val CONTENT_PATH = "pages"
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
