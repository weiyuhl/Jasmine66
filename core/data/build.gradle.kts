plugins {
    alias(libs.plugins.jasmine.android.library)
    alias(libs.plugins.jasmine.hilt)
    id("kotlinx-serialization")
}

android {
    namespace = "com.lhzkml.jasmine.core.data"
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    api(projects.core.common)
    api(projects.core.database)
    api(projects.core.datastore)
    api(projects.core.network)
    api(projects.websearch)

    implementation(libs.security.crypto)
    implementation(libs.kotlinx.serialization.json)

    implementation(projects.core.domain)
    implementation(projects.core.notifications)
    implementation(projects.jasmineCore.prompt.promptExecutor)
    implementation(projects.jasmineCore.agent.agentTools)
    implementation(projects.jasmineCore.agent.agentRuntime)
    implementation(projects.linuxSandbox.sandbox)

    testImplementation(libs.junit)
}
