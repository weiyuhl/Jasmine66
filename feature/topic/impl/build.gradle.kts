
plugins {
    alias(libs.plugins.jasmine.android.feature.impl)
    alias(libs.plugins.jasmine.android.library.compose)
}

android {
    namespace = "com.lhzkml.jasmine.feature.topic.impl"
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.feature.topic.api)

    implementation(libs.androidx.compose.material3.adaptive.navigation3)
}
