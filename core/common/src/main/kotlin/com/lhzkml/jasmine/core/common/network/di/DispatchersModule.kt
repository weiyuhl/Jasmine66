
package com.lhzkml.jasmine.core.common.network.di

import com.lhzkml.jasmine.core.common.network.Dispatcher
import com.lhzkml.jasmine.core.common.network.JasmineDispatchers
import com.lhzkml.jasmine.core.common.network.JasmineDispatchers.Default
import com.lhzkml.jasmine.core.common.network.JasmineDispatchers.IO
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
@InstallIn(SingletonComponent::class)
object JasmineDispatchersModule {
    @Provides
    @Dispatcher(IO)
    fun providesIODispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Dispatcher(Default)
    fun providesDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default
}


