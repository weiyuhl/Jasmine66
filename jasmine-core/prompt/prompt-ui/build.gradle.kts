plugins {
    alias(libs.plugins.jasmine.android.library)
    alias(libs.plugins.jasmine.android.library.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.lhzkml.jasmine.core.prompt.ui"
}

dependencies {
    api(project(":jasmine-core:prompt:prompt-model"))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.coil.kt.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
}

android {
    namespace = "com.lhzkml.jasmine.core.prompt.ui"
}

dependencies {
    api(project(":jasmine-core:prompt:prompt-model"))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.coil.kt.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
}

android {
    namespace = "com.lhzkml.jasmine.core.prompt.ui"
}

dependencies {
    api(project(":jasmine-core:prompt:prompt-model"))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.coil.kt.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
}

android {
    namespace = "com.lhzkml.jasmine.core.prompt.ui"
}

dependencies {
    api(project(":jasmine-core:prompt:prompt-model"))
    implementation(libs.coil.kt.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
}
