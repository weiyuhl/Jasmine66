plugins {
    alias(libs.plugins.jasmine.android.feature.impl)
    alias(libs.plugins.jasmine.android.library.compose)
}

android {
    namespace = "com.lhzkml.jasmine.feature.chat.impl"
}

dependencies {
    implementation(projects.feature.chat.api)
    implementation(projects.core.data)
    implementation(projects.jasmineCore.prompt.promptUi)
    implementation(libs.compottie)
    implementation(libs.richtext.commonmark)
    implementation(libs.richtext.ui.material3)
}
