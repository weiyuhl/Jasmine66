
package com.lhzkml.jasmine.core.network.demo

import JvmUnitTestDemoAssetManager
import android.os.Build.VERSION.SDK_INT
import android.os.Build.VERSION_CODES.M
import com.lhzkml.jasmine.core.common.network.Dispatcher
import com.lhzkml.jasmine.core.common.network.JasmineDispatchers.IO
import com.lhzkml.jasmine.core.network.JasmineNetworkDataSource
import com.lhzkml.jasmine.core.network.model.NetworkChangeList
import com.lhzkml.jasmine.core.network.model.NetworkTopic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.BufferedReader
import javax.inject.Inject

class DemoJasmineNetworkDataSource @Inject constructor(
) : JasmineNetworkDataSource {
}


