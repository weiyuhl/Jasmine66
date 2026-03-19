plugins {
    alias(libs.plugins.jasmine.android.library)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.lhzkml.jasmine.core.domain"
}

dependencies {
    api(projects.core.data)
    api(projects.core.model)

    implementation(libs.javax.inject)
}
