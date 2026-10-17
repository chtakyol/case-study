package com.cihatakyol.case_study.data.repository

import com.cihatakyol.case_study.data.local.database.ContentDao
import com.cihatakyol.case_study.data.local.database.mapper.buildPage
import com.cihatakyol.case_study.data.local.database.mapper.buildPages
import com.cihatakyol.case_study.data.remote.ContentApi
import com.cihatakyol.case_study.data.remote.mapper.toDocumentEntities
import com.cihatakyol.case_study.di.IoDispatcher
import com.cihatakyol.case_study.domain.model.Page
import com.cihatakyol.case_study.domain.model.Result
import com.cihatakyol.case_study.domain.repository.ContentRepository
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

class ContentRepositoryImpl @Inject constructor(
    private val dao: ContentDao,
    private val api: ContentApi,
    @param:IoDispatcher
    private val ioDispatcher: CoroutineDispatcher,
) : ContentRepository {

    override fun observePages(): Flow<List<Page>> = combine(
        dao.observePages(),
        dao.observeItems(),
        dao.observeResponseSets(),
        dao.observeResponses(),
    ) { pages, items, responseSets, responses ->
        buildPages(pages, items, responseSets, responses)
    }

    override fun observePage(pageId: Int): Flow<Page?> = combine(
        dao.observePage(pageId),
        dao.observeItemsForPage(pageId),
        dao.observeResponseSetsForPage(pageId),
        dao.observeResponsesForPage(pageId),
    ) { page, items, responseSets, responses ->
        buildPage(page, items, responseSets, responses)
    }

    override suspend fun refreshPages(): Result<Unit> = withContext(ioDispatcher) {
        runCatching { api.getPages() }
            .mapCatching { dtos ->
                val document = dtos.toDocumentEntities()
                dao.replaceAll(
                    pages = document.pages,
                    items = document.items,
                    responseSets = document.responseSets,
                    responses = document.responses,
                )
            }
            .fold(
                onSuccess = { Result.Success(Unit) },
                onFailure = { cause -> Result.Error(cause) },
            )
    }
}
