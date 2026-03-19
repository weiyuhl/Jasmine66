
package com.lhzkml.jasmine.core.network.di

import com.lhzkml.jasmine.core.network.JasmineNetworkDataSource
import com.lhzkml.jasmine.core.network.retrofit.RetrofitJasmineNetwork
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface FlavoredNetworkModule {

    @Binds
    fun binds(impl: RetrofitJasmineNetwork): JasmineNetworkDataSource
}

