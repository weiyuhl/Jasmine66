plugins {
    alias(libs.plugins.jasmine.android.library)
    alias(libs.plugins.jasmine.hilt)
}

android {
    namespace = "com.lhzkml.jasmine.core.websearch"
}

dependencies {
    api(projects.core.common)
    api(projects.core.model)

    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.coroutines.core)
}
