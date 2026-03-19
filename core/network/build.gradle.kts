
import com.android.build.api.variant.BuildConfigField
import java.io.StringReader
import java.util.Properties

plugins {
    alias(libs.plugins.jasmine.android.library)
    alias(libs.plugins.jasmine.hilt)
    id("kotlinx-serialization")
}

android {
    buildFeatures {
        buildConfig = true
    }
    namespace = "com.lhzkml.jasmine.core.network"
}

dependencies {
    api(libs.kotlinx.datetime)
    api(projects.core.common)
    api(projects.core.model)

    implementation(libs.coil.kt)
    implementation(libs.coil.kt.svg)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlin.serialization)
}

val backendUrl = providers.fileContents(
    isolated.rootProject.projectDirectory.file("local.properties")
).asText.map { text ->
    val properties = Properties()
    properties.load(StringReader(text))
    properties["BACKEND_URL"]
}.orElse("http://example.com")

androidComponents {
    onVariants {
        it.buildConfigFields!!.put("BACKEND_URL", backendUrl.map { value ->
            BuildConfigField(type = "String", value = """"$value"""", comment = null)
        })
    }
}
