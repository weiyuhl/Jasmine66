plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.lhzkml.jasmine.core.config"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
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
    api(project(":jasmine-core:prompt:prompt-executor"))
    api(project(":jasmine-core:agent:agent-tools"))
    api(project(":jasmine-core:agent:agent-observe"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}
