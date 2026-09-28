plugins {
    alias(libs.plugins.skycast.android.library.compose)
}

dependencies {
    api(project(":core:model"))
    api(project(":core:designsystem"))
    implementation(project(":core:common"))
    implementation(libs.androidx.appcompat)
    implementation(project(":core:domain"))
}
