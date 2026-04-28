package com.lhzkml.jasmine.core.config.di

import com.lhzkml.jasmine.core.config.ConfigRepository
import com.lhzkml.jasmine.core.config.InMemoryConfigRepository
import com.lhzkml.jasmine.core.config.ProviderRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ConfigModule {

    @Provides
    @Singleton
    fun provideConfigRepository(): ConfigRepository = InMemoryConfigRepository()

    @Provides
    @Singleton
    fun provideProviderRegistry(configRepo: ConfigRepository): ProviderRegistry {
        return ProviderRegistry(configRepo).also { it.initialize() }
    }
}
