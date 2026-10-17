package com.cihatakyol.case_study.data.local.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "pages")
data class PageEntity(@PrimaryKey val id: Int, val title: String, val position: Int)

@Entity(
    tableName = "content_items",
    foreignKeys = [
        ForeignKey(
            entity = PageEntity::class,
            parentColumns = ["id"],
            childColumns = ["pageId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = ContentItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("pageId"), Index("parentId"), Index("responseSetId")],
)
data class ContentItemEntity(
    @PrimaryKey val id: Int,
    val pageId: Int,
    val parentId: Int? = null,
    val position: Int,
    val type: String,
    val content: String? = null,
    val src: String? = null,
    val title: String? = null,
    val responseSetId: Int? = null,
)

@Entity(
    tableName = "response_sets",
    foreignKeys = [
        ForeignKey(
            entity = ContentItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["itemId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("itemId")],
)
data class ResponseSetEntity(@PrimaryKey val id: Int, val itemId: Int, val multipleSelection: Boolean)

@Entity(
    tableName = "responses",
    foreignKeys = [
        ForeignKey(
            entity = ResponseSetEntity::class,
            parentColumns = ["id"],
            childColumns = ["responseSetId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("responseSetId")],
)
data class ResponseEntity(
    @PrimaryKey val id: Int,
    val responseSetId: Int,
    val position: Int,
    val label: String,
    val score: Int?,
)
