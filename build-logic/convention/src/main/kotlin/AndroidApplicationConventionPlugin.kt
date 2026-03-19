
import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.lhzkml.jasmine.configureBadgingTasks
import com.lhzkml.jasmine.configureKotlinAndroid
import com.lhzkml.jasmine.configurePrintApksTask
import com.lhzkml.jasmine.configureSpotlessForAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure

abstract class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.application")
            apply(plugin = "jasmine.android.lint")
            apply(plugin = "com.dropbox.dependency-guard")

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)
                defaultConfig.targetSdk = 36
            }
            extensions.configure<ApplicationAndroidComponentsExtension> {
                configurePrintApksTask(this)
                configureBadgingTasks(this)
            }
            configureSpotlessForAndroid()
        }
    }
}

