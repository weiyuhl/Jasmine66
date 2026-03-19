
package com.lhzkml.jasmine.core.network.di

import com.lhzkml.jasmine.core.network.JasmineNetworkDataSource
import com.lhzkml.jasmine.core.network.demo.DemoJasmineNetworkDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface FlavoredNetworkModule {

    @Binds
    fun binds(impl: DemoJasmineNetworkDataSource): JasmineNetworkDataSource
}

