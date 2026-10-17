package com.cihatakyol.case_study

import java.io.File
import kotlinx.serialization.json.Json

/** Shared [Json] matching the production configuration used by the network layer. */
internal val testJson = Json {
    ignoreUnknownKeys = true
}

/** Reads the sample content document from the test resources. */
internal fun sampleContentJson(): String =
    checkNotNull(TestFixtures::class.java.classLoader?.getResourceAsStream("content_sample.json")) {
        "content_sample.json not found on the test classpath"
    }.bufferedReader().use { it.readText() }

/**
 * Reads the debug source-set fixture served by the debug content interceptor. Read from disk (not the
 * classpath) so the shared test source set works for every variant.
 */
internal fun debugContentFixtureJson(): String {
    val candidates = listOf(
        File("src/debug/assets/content_pages.json"),
        File("app/src/debug/assets/content_pages.json"),
    )
    return candidates.firstOrNull(File::isFile)?.readText()
        ?: error("content_pages.json not found; looked in ${candidates.joinToString { it.absolutePath }}")
}

internal object TestFixtures
