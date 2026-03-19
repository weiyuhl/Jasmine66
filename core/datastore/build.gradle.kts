
plugins {
    alias(libs.plugins.jasmine.android.library)
    alias(libs.plugins.jasmine.hilt)
}

android {
    defaultConfig {
        consumerProguardFiles("consumer-proguard-rules.pro")
    }
    namespace = "com.lhzkml.jasmine.core.datastore"
}

dependencies {
    api(libs.androidx.dataStore)
    api(projects.core.datastoreProto)
    api(projects.core.model)

    implementation(projects.core.common)
}
