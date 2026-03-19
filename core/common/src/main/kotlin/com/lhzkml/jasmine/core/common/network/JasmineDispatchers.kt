
package com.lhzkml.jasmine.core.common.network

import javax.inject.Qualifier
import kotlin.annotation.AnnotationRetention.RUNTIME

@Qualifier
@Retention(RUNTIME)
annotation class Dispatcher(val jasmineDispatcher: JasmineDispatchers)

enum class JasmineDispatchers {
    Default,
    IO,
}
