plugins {
    alias(libs.plugins.skycast.android.library.compose)
    alias(libs.plugins.skycast.hilt)
}

dependencies {
    implementation(project(":core:domain"))
    implementation(project(":core:ui"))
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
}
