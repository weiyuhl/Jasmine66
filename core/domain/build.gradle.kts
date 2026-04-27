plugins {
    alias(libs.plugins.jasmine.android.library)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.lhzkml.jasmine.core.domain"
}

dependencies {
    api(projects.core.model)
    api(projects.core.common)

    implementation(libs.javax.inject)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.hilt.android)
    implementation(libs.security.crypto)
}
