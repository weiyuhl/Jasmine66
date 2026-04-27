plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.lhzkml.jasmine.core.assistant"
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
    api(project(":jasmine-core:assistant:assistant-tools"))
    api(project(":jasmine-core:assistant:assistant-memory"))
    api(project(":jasmine-core:assistant:assistant-scheduler"))
    api(project(":jasmine-core:assistant:assistant-email"))
    
    // 基础依赖保持
    implementation(libs.kotlinx.serialization.json)
    
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.vintage)
    testImplementation(libs.kotlinx.coroutines.test)
}
