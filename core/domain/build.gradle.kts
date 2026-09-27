plugins {
    alias(libs.plugins.skycast.jvm.library)
}

dependencies {
    api(project(":core:model"))
    api(project(":core:common"))
    implementation(libs.javax.inject)

    testImplementation(project(":core:testing"))
}
