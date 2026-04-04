plugins {
    alias(libs.plugins.jasmine.android.feature.impl)
    alias(libs.plugins.jasmine.android.library.compose)
}

android {
    namespace = "com.lhzkml.jasmine.feature.sandbox.impl"
}

dependencies {
    implementation(projects.feature.sandbox.api)
    implementation(projects.linuxSandbox.sandbox)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.androidx.hilt.lifecycle.viewModelCompose)
}
