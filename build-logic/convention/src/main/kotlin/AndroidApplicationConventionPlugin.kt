import com.android.build.api.dsl.ApplicationExtension
import com.mk.skycast.buildlogic.SkycastConfig
import com.mk.skycast.buildlogic.configureKotlinAndroid
import com.mk.skycast.buildlogic.libs
import com.mk.skycast.buildlogic.pluginId
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply(libs.pluginId("android-application"))
        pluginManager.apply(libs.pluginId("kotlin-android"))
        extensions.configure<ApplicationExtension> {
            configureKotlinAndroid(this)
            defaultConfig.targetSdk = SkycastConfig.TARGET_SDK
        }
    }
}
