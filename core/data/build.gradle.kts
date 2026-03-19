plugins {
    alias(libs.plugins.jasmine.android.library)
    alias(libs.plugins.jasmine.hilt)
    id("kotlinx-serialization")
}

android {
    namespace = "com.lhzkml.jasmine.core.data"
}

dependencies {
    api(projects.core.common)
    api(projects.core.database)
    api(projects.core.datastore)
    api(projects.core.network)

    implementation(projects.core.analytics)
    implementation(projects.core.notifications)
}
