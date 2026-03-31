plugins {
    alias(libs.plugins.jasmine.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.lhzkml.jasmine.core.prompt.llm"
}

dependencies {
    api(project(":jasmine-core:prompt:prompt-model"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}
