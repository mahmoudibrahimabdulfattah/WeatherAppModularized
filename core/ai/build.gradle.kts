plugins {
    alias(libs.plugins.skycast.android.library)
    alias(libs.plugins.skycast.hilt)
}

dependencies {
    implementation(project(":core:domain"))
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.ai)
}
