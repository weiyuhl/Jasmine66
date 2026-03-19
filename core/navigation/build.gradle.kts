
plugins {
    alias(libs.plugins.jasmine.android.library)
    alias(libs.plugins.jasmine.hilt)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose)
}

android {
    namespace = "com.lhzkml.jasmine.core.navigation"
}

dependencies {
    api(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.savedstate.compose)
    implementation(libs.androidx.lifecycle.viewModel.navigation3)
}
