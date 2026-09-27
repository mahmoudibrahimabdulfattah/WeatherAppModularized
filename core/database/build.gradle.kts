plugins {
    alias(libs.plugins.skycast.android.library)
    alias(libs.plugins.skycast.android.room)
    alias(libs.plugins.skycast.hilt)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)
}
