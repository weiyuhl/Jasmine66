
plugins {
    alias(libs.plugins.jasmine.android.feature.api)
}

android {
    namespace = "com.lhzkml.jasmine.feature.foryou.api"
}

dependencies {
    api(projects.core.navigation)
}
