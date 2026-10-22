package com.cihatakyol.case_study.data.remote

import com.cihatakyol.case_study.data.remote.dto.PageDto
import retrofit2.http.GET

interface ContentApi {
    @GET("lumiform-android-test.json")
    suspend fun getPages(): List<PageDto>
}
