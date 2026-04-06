plugins {
    alias(libs.plugins.jasmine.android.feature.api)
}

android {
    namespace = "com.lhzkml.jasmine.feature.skills.api"
}

dependencies {
    implementation(projects.core.model)
}
