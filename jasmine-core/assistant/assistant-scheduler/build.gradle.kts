plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.lhzkml.jasmine.core.assistant.scheduler"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
    }
}

dependencies {
    implementation(project(":jasmine-core:assistant:assistant-email"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.core.ktx)
}
