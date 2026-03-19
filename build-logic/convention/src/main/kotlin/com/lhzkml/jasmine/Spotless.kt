
package com.lhzkml.jasmine

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

internal fun Project.configureSpotlessForAndroid() {
    configureSpotlessCommon()
    extensions.configure<SpotlessExtension> {
        format("xml") {
            target("src/**/*.xml")
            endWithNewline()
        }
    }
}

internal fun Project.configureSpotlessForJvm() {
    configureSpotlessCommon()
}

internal fun Project.configureSpotlessForRootProject() {
    apply(plugin = "com.diffplug.spotless")
    extensions.configure<SpotlessExtension> {
        kotlin {
            target("build-logic/convention/src/**/*.kt")
            ktlint(libs.findVersion("ktlint").get().requiredVersion).editorConfigOverride(
                mapOf("android" to "true"),
            )
            endWithNewline()
        }
        format("kts") {
            target("*.kts")
            target("build-logic/*.kts")
            target("build-logic/convention/*.kts")
            endWithNewline()
        }
    }
}

private fun Project.configureSpotlessCommon() {
    apply(plugin = "com.diffplug.spotless")
    extensions.configure<SpotlessExtension> {
        kotlin {
            target("src/**/*.kt")
            ktlint(libs.findVersion("ktlint").get().requiredVersion).editorConfigOverride(
                mapOf("android" to "true"),
            )
            endWithNewline()
        }
        format("kts") {
            target("*.kts")
            endWithNewline()
        }
    }
}
