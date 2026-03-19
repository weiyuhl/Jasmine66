package com.lhzkml.jasmine.core.database.di

import com.lhzkml.jasmine.core.database.JasmineDatabase
import com.lhzkml.jasmine.core.database.dao.RecentSearchQueryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal object DaosModule {
    @Provides
    fun providesRecentSearchQueryDao(
        database: JasmineDatabase,
    ): RecentSearchQueryDao = database.recentSearchQueryDao()
}
