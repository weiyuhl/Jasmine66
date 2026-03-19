
plugins {
    alias(libs.plugins.jasmine.android.feature.impl)
    alias(libs.plugins.jasmine.android.library.compose)
}

android {
    namespace = "com.lhzkml.jasmine.feature.search.impl"
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.feature.interests.api)
    implementation(projects.feature.search.api)
    implementation(projects.feature.topic.api)
}
