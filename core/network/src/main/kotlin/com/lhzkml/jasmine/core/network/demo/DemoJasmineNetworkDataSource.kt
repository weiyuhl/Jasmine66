/*
 * Copyright 2024 The Android Open Source Project
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

package com.lhzkml.jasmine.core.network.demo

import JvmUnitTestDemoAssetManager
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.M
import com.lhzkml.jasmine.core.common.network.Dispatcher
import com.lhzkml.jasmine.core.common.network.Dispatchers.IO
import com.lhzkml.jasmine.core.network.NetworkDataSource
import com.lhzkml.jasmine.core.network.model.NetworkChangeList
import com.lhzkml.jasmine.core.network.model.NetworkTopic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.BufferedReader
import javax.inject.Inject

class DemoNetworkDataSource @Inject constructor(
) : NetworkDataSource {
}
