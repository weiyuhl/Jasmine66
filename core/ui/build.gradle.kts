plugins {
    alias(libs.plugins.jasmine.android.library)
    alias(libs.plugins.jasmine.android.library.compose)
}

android {
    namespace = "com.lhzkml.jasmine.core.ui"
}

dependencies {
    api(libs.androidx.metrics)
    api(projects.core.analytics)
    api(projects.core.designsystem)
    api(projects.core.model)

    implementation(libs.androidx.browser)
    implementation(libs.coil.kt)
    implementation(libs.coil.kt.compose)
}
