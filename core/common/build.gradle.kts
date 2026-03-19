plugins {
    alias(libs.plugins.jasmine.jvm.library)
    alias(libs.plugins.jasmine.hilt)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}
