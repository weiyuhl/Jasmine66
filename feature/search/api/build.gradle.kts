
plugins {
    alias(libs.plugins.jasmine.android.feature.api)
}

android {
    namespace = "com.lhzkml.jasmine.feature.search.api"
}

dependencies {
    implementation(projects.core.domain)
}
