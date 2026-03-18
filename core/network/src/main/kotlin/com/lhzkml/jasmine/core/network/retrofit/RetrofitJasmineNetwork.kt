/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.lhzkml.jasmine.core.network.retrofit

import androidx.tracing.trace
import com.lhzkml.jasmine.core.network.BuildConfig
import com.lhzkml.jasmine.core.network.JasmineNetworkDataSource
import com.lhzkml.jasmine.core.network.model.NetworkChangeList
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Retrofit API declaration for Jasmine Network API
 */
private interface RetrofitJasmineNetworkApi {
}

private const val JASMINE_BASE_URL = BuildConfig.BACKEND_URL

/**
 * Wrapper for data provided from the [JASMINE_BASE_URL]
 */
@Serializable
private data class NetworkResponse<T>(
    val data: T,
)

/**
 * [Retrofit] backed [JasmineNetworkDataSource]
 */
@Singleton
internal class RetrofitJasmineNetwork @Inject constructor(
    networkJson: Json,
    okhttpCallFactory: dagger.Lazy<Call.Factory>,
) : JasmineNetworkDataSource {

    private val networkApi = trace("RetrofitJasmineNetwork") {
        Retrofit.Builder()
            .baseUrl(JASMINE_BASE_URL)
            // We use callFactory lambda here with dagger.Lazy<Call.Factory>
            // to prevent initializing OkHttp on the main thread.
            .callFactory { okhttpCallFactory.get().newCall(it) }
            .addConverterFactory(
                networkJson.asConverterFactory("application/json".toMediaType()),
            )
            .build()
            .create(RetrofitJasmineNetworkApi::class.java)
    }
}
