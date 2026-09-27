import com.android.build.api.dsl.LibraryExtension
import com.mk.skycast.buildlogic.configureKotlinAndroid
import com.mk.skycast.buildlogic.library
import com.mk.skycast.buildlogic.libs
import com.mk.skycast.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("android-library"))
        pluginManager.apply(libs.pluginId("kotlin-android"))
        extensions.configure<LibraryExtension> {
            configureKotlinAndroid(this)
            defaultConfig.testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            // Namespace derived from module path, e.g. :core:data -> com.mk.skycast.core.data
            namespace = "com.mk.skycast" + path.replace(':', '.').replace('-', '_')
        }
        dependencies {
            add("testImplementation", libs.library("junit"))
            add("testImplementation", libs.library("truth"))
            add("testImplementation", libs.library("turbine"))
            add("testImplementation", libs.library("kotlinx-coroutines-test"))
        }
    }
}
