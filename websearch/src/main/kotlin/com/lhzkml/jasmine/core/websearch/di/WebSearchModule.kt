package com.lhzkml.jasmine.core.websearch.di

import com.lhzkml.jasmine.core.websearch.DuckDuckGoSearchService
import com.lhzkml.jasmine.core.websearch.WebSearchService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WebSearchModule {

    @Provides
    @Singleton
    fun provideWebSearchService(): WebSearchService {
        return DuckDuckGoSearchService()
    }
}
