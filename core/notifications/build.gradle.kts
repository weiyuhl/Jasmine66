plugins {
    alias(libs.plugins.jasmine.android.library)
    alias(libs.plugins.jasmine.hilt)
}

android {
    namespace = "com.lhzkml.jasmine.core.notifications"
}

dependencies {
    api(projects.core.model)

    implementation(projects.core.common)

    compileOnly(platform(libs.androidx.compose.bom))
}
