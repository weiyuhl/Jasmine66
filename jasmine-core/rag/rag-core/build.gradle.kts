plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.lhzkml.jasmine.core.rag"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }
}

dependencies {
    api(project(":jasmine-core:prompt:prompt-llm"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}
