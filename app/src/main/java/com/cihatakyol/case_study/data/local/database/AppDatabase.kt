package com.cihatakyol.case_study.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.cihatakyol.case_study.data.local.database.entity.ContentItemEntity
import com.cihatakyol.case_study.data.local.database.entity.PageEntity
import com.cihatakyol.case_study.data.local.database.entity.ResponseEntity
import com.cihatakyol.case_study.data.local.database.entity.ResponseSetEntity

@Database(
    entities = [
        PageEntity::class,
        ContentItemEntity::class,
        ResponseSetEntity::class,
        ResponseEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contentDao(): ContentDao
}
