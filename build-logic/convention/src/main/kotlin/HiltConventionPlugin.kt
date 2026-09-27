import com.mk.skycast.buildlogic.library
import com.mk.skycast.buildlogic.libs
import com.mk.skycast.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("ksp"))
        dependencies {
            add("ksp", libs.library("hilt-compiler"))
        }
        // Pure JVM modules only need Dagger/Hilt annotations.
        pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
            dependencies { add("implementation", libs.library("hilt-core")) }
        }
        // Android modules get the full Hilt Gradle plugin.
        pluginManager.withPlugin("com.android.base") {
            pluginManager.apply(libs.pluginId("hilt"))
            dependencies { add("implementation", libs.library("hilt-android")) }
        }
    }
}
