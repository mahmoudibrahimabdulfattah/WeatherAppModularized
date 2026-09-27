plugins {
    alias(libs.plugins.skycast.android.library.compose)
}

dependencies {
    api(libs.androidx.lifecycle.viewmodel.compose)
    api(libs.androidx.lifecycle.runtime.compose)
    api(libs.kotlinx.coroutines.android)
}
