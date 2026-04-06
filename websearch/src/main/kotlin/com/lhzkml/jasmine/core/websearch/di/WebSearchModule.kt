package com.lhzkml.jasmine.core.websearch.di

import com.lhzkml.jasmine.core.websearch.DuckDuckGoSearchService
import com.lhzkml.jasmine.core.websearch.WebSearchService
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WebSearchModule {

    @Binds
    @Singleton
    abstract fun bindWebSearchService(
        service: DuckDuckGoSearchService,
    ): WebSearchService
}
