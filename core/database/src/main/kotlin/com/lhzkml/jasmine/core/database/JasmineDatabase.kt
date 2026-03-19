package com.lhzkml.jasmine.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.lhzkml.jasmine.core.database.dao.RecentSearchQueryDao
import com.lhzkml.jasmine.core.database.model.RecentSearchQueryEntity
import com.lhzkml.jasmine.core.database.util.InstantConverter

@Database(
    entities = [
        RecentSearchQueryEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(
    InstantConverter::class,
)
internal abstract class JasmineDatabase : RoomDatabase() {
    abstract fun recentSearchQueryDao(): RecentSearchQueryDao
}
