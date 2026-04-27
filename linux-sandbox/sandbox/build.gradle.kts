plugins {
    alias(libs.plugins.jasmine.android.library)
    alias(libs.plugins.jasmine.android.library.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.android.sandbox"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("src/main/jniLibs")
        }
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")

    implementation("io.ktor:ktor-client-core:3.4.2")
    implementation("io.ktor:ktor-client-android:3.4.2")

    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")

    api(projects.jasmineCore.agent.agentTools)
    api(projects.jasmineCore.prompt.promptModel)

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
}
