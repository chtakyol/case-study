package com.cihatakyol.case_study.data.local.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.cihatakyol.case_study.data.local.database.entity.ContentItemEntity
import com.cihatakyol.case_study.data.local.database.entity.PageEntity
import com.cihatakyol.case_study.data.local.database.entity.ResponseEntity
import com.cihatakyol.case_study.data.local.database.entity.ResponseSetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentDao {
    @Query("SELECT * FROM pages")
    fun observePages(): Flow<List<PageEntity>>

    @Query("SELECT * FROM content_items")
    fun observeItems(): Flow<List<ContentItemEntity>>

    @Query("SELECT * FROM response_sets")
    fun observeResponseSets(): Flow<List<ResponseSetEntity>>

    @Query("SELECT * FROM responses")
    fun observeResponses(): Flow<List<ResponseEntity>>

    @Query("SELECT * FROM pages WHERE id = :pageId")
    fun observePage(pageId: Int): Flow<PageEntity?>

    @Query("SELECT * FROM content_items WHERE pageId = :pageId")
    fun observeItemsForPage(pageId: Int): Flow<List<ContentItemEntity>>

    @Query(
        "SELECT * FROM response_sets WHERE itemId IN " +
            "(SELECT id FROM content_items WHERE pageId = :pageId)",
    )
    fun observeResponseSetsForPage(pageId: Int): Flow<List<ResponseSetEntity>>

    @Query(
        "SELECT * FROM responses WHERE responseSetId IN " +
            "(SELECT id FROM response_sets WHERE itemId IN " +
            "(SELECT id FROM content_items WHERE pageId = :pageId))",
    )
    fun observeResponsesForPage(pageId: Int): Flow<List<ResponseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPages(pages: List<PageEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<ContentItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResponseSets(responseSets: List<ResponseSetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResponses(responses: List<ResponseEntity>)

    @Query("DELETE FROM responses")
    suspend fun clearResponses()

    @Query("DELETE FROM response_sets")
    suspend fun clearResponseSets()

    @Query("DELETE FROM content_items")
    suspend fun clearItems()

    @Query("DELETE FROM pages")
    suspend fun clearPages()

    @Transaction
    suspend fun replaceAll(
        pages: List<PageEntity>,
        items: List<ContentItemEntity>,
        responseSets: List<ResponseSetEntity>,
        responses: List<ResponseEntity>,
    ) {
        clearResponses()
        clearResponseSets()
        clearItems()
        clearPages()
        insertPages(pages)
        insertItems(items)
        insertResponseSets(responseSets)
        insertResponses(responses)
    }
}
