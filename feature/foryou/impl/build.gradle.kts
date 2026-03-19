
plugins {
    alias(libs.plugins.jasmine.android.feature.impl)
    alias(libs.plugins.jasmine.android.library.compose)
}

android {
    namespace = "com.lhzkml.jasmine.feature.foryou.impl"
}

dependencies {
    implementation(libs.accompanist.permissions)
    implementation(projects.core.domain)
    implementation(projects.core.notifications)
    implementation(projects.feature.foryou.api)
    implementation(projects.feature.topic.api)
    implementation(libs.androidx.activity.compose)
}
