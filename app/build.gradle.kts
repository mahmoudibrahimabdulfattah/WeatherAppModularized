plugins {
    alias(libs.plugins.skycast.android.application)
    alias(libs.plugins.skycast.android.application.compose)
    alias(libs.plugins.skycast.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.mk.skycast"

    defaultConfig {
        applicationId = "com.mk.skycast"
        versionCode = 1
        versionName = "2.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    androidResources {
        localeFilters += listOf("en", "ar")
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:common"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:location"))
    implementation(project(":core:mvi"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":sync:work"))

    implementation(project(":feature:home"))
    implementation(project(":feature:places"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:routine"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(project(":core:testing"))
}
