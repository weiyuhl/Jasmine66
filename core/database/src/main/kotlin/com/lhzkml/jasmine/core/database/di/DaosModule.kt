
package com.lhzkml.jasmine.core.database.di

import com.lhzkml.jasmine.core.database.JasmineDatabase
import com.lhzkml.jasmine.core.database.dao.RecentSearchQueryDao
import com.lhzkml.jasmine.core.database.dao.TopicDao
import com.lhzkml.jasmine.core.database.dao.TopicFtsDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object DaosModule {
    @Provides
    fun providesTopicsDao(
        database: JasmineDatabase,
    ): TopicDao = database.topicDao()


    @Provides
    fun providesTopicFtsDao(
        database: JasmineDatabase,
    ): TopicFtsDao = database.topicFtsDao()


    @Provides
    fun providesRecentSearchQueryDao(
        database: JasmineDatabase,
    ): RecentSearchQueryDao = database.recentSearchQueryDao()
}

