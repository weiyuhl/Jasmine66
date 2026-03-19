
package com.lhzkml.jasmine

/**
 * This is shared between :app and :benchmarks module to provide configurations type safety.
 */
enum class JasmineBuildType(val applicationIdSuffix: String? = null) {
    DEBUG(".debug"),
    RELEASE,
}
