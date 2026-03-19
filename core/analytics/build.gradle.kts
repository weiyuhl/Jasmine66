plugins {
    alias(libs.plugins.jasmine.android.library)
    alias(libs.plugins.jasmine.android.library.compose)
    alias(libs.plugins.jasmine.hilt)
}

android {
    namespace = "com.lhzkml.jasmine.core.analytics"
}

dependencies {
    implementation(libs.androidx.compose.runtime)

}
