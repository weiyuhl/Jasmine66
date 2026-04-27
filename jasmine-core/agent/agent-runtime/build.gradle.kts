plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.lhzkml.jasmine.core.agent.runtime"
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
    api(project(":jasmine-core:agent:agent-tools"))
    api(project(":jasmine-core:agent:agent-observe"))
    api(project(":jasmine-core:agent:agent-graph"))
    api(project(":jasmine-core:agent:agent-planner"))
    api(project(":jasmine-core:agent:agent-mcp"))
    api(project(":jasmine-core:config:config-manager"))
    api(project(":jasmine-core:conversation:conversation-storage"))
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.vintage)
    testImplementation(libs.kotlinx.coroutines.test)
}
