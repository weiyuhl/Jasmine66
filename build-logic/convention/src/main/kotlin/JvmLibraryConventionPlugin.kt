
import com.lhzkml.jasmine.configureKotlinJvm
import com.lhzkml.jasmine.configureSpotlessForJvm
import com.lhzkml.jasmine.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

abstract class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.jvm")
            apply(plugin = "jasmine.android.lint")

            configureKotlinJvm()
            configureSpotlessForJvm()
        }
    }
}

