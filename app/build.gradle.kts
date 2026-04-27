import com.lhzkml.jasmine.JasmineBuildType
import java.util.Properties

plugins {
    alias(libs.plugins.jasmine.android.application)
    alias(libs.plugins.jasmine.android.application.compose)
    alias(libs.plugins.jasmine.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.osslicenses)
}

// Release signing config
val keystorePropertiesFile = rootProject.file("keystore.properties")
val releaseSigningConfig = if (keystorePropertiesFile.exists()) {
    val props = Properties()
    props.load(keystorePropertiesFile.inputStream())
    android.signingConfigs.create("release") {
        storeFile = rootProject.file(props.getProperty("storeFile"))
        storePassword = props.getProperty("storePassword")
        keyAlias = props.getProperty("keyAlias")
        keyPassword = props.getProperty("keyPassword")
    }
} else {
    null
}

android {
    defaultConfig {
        applicationId = "com.lhzkml.jasmine"
        versionCode = 8
        versionName = "0.1.2" // X.Y.Z; X = Major, Y = minor, Z = Patch level
    }

    buildTypes {
        debug {
            applicationIdSuffix = JasmineBuildType.DEBUG.applicationIdSuffix
        }
        release {
            isMinifyEnabled = providers.gradleProperty("minifyWithR8")
                .map(String::toBooleanStrict).getOrElse(true)
            applicationIdSuffix = JasmineBuildType.RELEASE.applicationIdSuffix
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"),
                          "proguard-rules.pro")

            signingConfig = releaseSigningConfig ?: signingConfigs.getByName("debug")
        }
    }

    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
        jniLibs {
            useLegacyPackaging = true
        }
    }
    namespace = "com.lhzkml.jasmine"
}

dependencies {
    implementation(projects.feature.chat.api)
    implementation(projects.feature.chat.impl)
    implementation(projects.feature.tools.api)
    implementation(projects.feature.tools.impl)
    implementation(projects.feature.knowledgebase.api)
    implementation(projects.feature.knowledgebase.impl)

    implementation(projects.feature.search.api)
    implementation(projects.feature.search.impl)
    implementation(projects.feature.settings.impl)

    implementation(projects.feature.sandbox.api)
    implementation(projects.feature.sandbox.impl)
    implementation(projects.feature.skills.api)
    implementation(projects.feature.skills.impl)

    implementation(projects.core.common)
    implementation(projects.core.ui)
    implementation(projects.core.designsystem)
    implementation(projects.core.data)
    implementation(projects.core.model)
    implementation(projects.core.analytics)
    implementation(projects.websearch)

    // Linux Sandbox module
    implementation(projects.linuxSandbox.sandbox)

    // Agent modules
    implementation(projects.jasmineCore.agent.agentRuntime)
    implementation(projects.jasmineCore.config.configManager)
    implementation(projects.jasmineCore.conversation.conversationStorage)

    implementation(libs.androidx.activity.compose)

    /* Duplicate removed */
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.compose.material3.adaptive.layout)
    implementation(libs.androidx.compose.material3.adaptive.navigation)
    implementation(libs.androidx.compose.material3.adaptive.navigation3)
    implementation(libs.androidx.compose.material3.windowSizeClass)
    implementation(libs.androidx.compose.runtime.tracing)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewModel.navigation3)
    implementation(libs.androidx.tracing.ktx)
    implementation(libs.androidx.window.core)
    implementation(libs.kotlinx.coroutines.guava)
    implementation(libs.coil.kt)
    implementation(libs.kotlinx.serialization.json)
    
    // Compottie - Lottie 动画支持
    implementation(libs.compottie)
    implementation(libs.compottie.dot)
    implementation(libs.compottie.resources)

    ksp(libs.hilt.compiler)
}

dependencyGuard {
    configuration("releaseRuntimeClasspath")
}
