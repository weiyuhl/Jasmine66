
plugins {
    alias(libs.plugins.jasmine.android.feature.impl)
    alias(libs.plugins.jasmine.android.library.compose)
}
android {
    namespace = "com.lhzkml.jasmine.feature.interests.impl"
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.feature.topic.api)
    implementation(projects.feature.interests.api)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.layout)
    implementation(libs.androidx.compose.material3.adaptive.navigation)
    implementation(libs.androidx.compose.material3.adaptive.navigation3)
}
