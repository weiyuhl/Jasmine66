
plugins {
    alias(libs.plugins.jasmine.android.feature.impl)
    alias(libs.plugins.jasmine.android.library.compose)
}

android {
    namespace = "com.lhzkml.jasmine.feature.bookmarks.impl"
}

dependencies {
    implementation(projects.core.data)
    implementation(projects.feature.bookmarks.api)
    implementation(projects.feature.topic.api)
}
