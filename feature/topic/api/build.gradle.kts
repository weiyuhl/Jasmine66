
plugins {
    alias(libs.plugins.jasmine.android.feature.api)
    alias(libs.plugins.jasmine.android.feature.impl)
    alias(libs.plugins.jasmine.android.library.compose)
}

android {
    namespace = "com.lhzkml.jasmine.feature.topic.api"
}
