plugins {
    alias(libs.plugins.jasmine.android.feature.api)
}

android {
    namespace = "com.lhzkml.jasmine.feature.knowledgebase.api"
}

dependencies {
    implementation(projects.core.navigation)
}
