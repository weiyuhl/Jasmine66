
plugins {
    alias(libs.plugins.jasmine.android.library)
    alias(libs.plugins.jasmine.android.room)
    alias(libs.plugins.jasmine.hilt)
}

android {
    namespace = "com.lhzkml.jasmine.core.database"
}

dependencies {
    api(projects.core.model)

    implementation(libs.kotlinx.datetime)
}
