package com.cihatakyol.case_study.domain.model

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class ResultTest {

    @Test
    fun `success holds the produced data`() {
        val result: Result<Int> = Result.Success(42)

        assertEquals(Result.Success(42), result)
        assertEquals(42, (result as Result.Success).data)
    }

    @Test
    fun `error carries the cause`() {
        val cause = IOException("boom")

        val result: Result<Int> = Result.Error(cause, "Something went wrong")

        assertSame(cause, (result as Result.Error).cause)
        assertEquals("Something went wrong", result.message)
    }
}
