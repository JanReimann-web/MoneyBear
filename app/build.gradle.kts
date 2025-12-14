import org.gradle.api.GradleException
import org.gradle.api.Project

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.google.services)
}

fun Project.stringProp(name: String): String? = (findProperty(name) as String?) ?: System.getenv(name)

val isReleaseRequested =
    gradle.startParameter.taskNames.any { it.contains("release", ignoreCase = true) }

android {
    namespace = "com.jan.moneybear"

    compileSdk = 36

    defaultConfig {
        applicationId = "com.jan.moneybear"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val storeFilePath =
                if (isReleaseRequested) {
                    stringProp("MONEYBEAR_STORE_FILE")?.takeIf { it.isNotBlank() }
                        ?: throw GradleException(
                            "Missing release signing value MONEYBEAR_STORE_FILE. " +
                                "Set it in gradle.properties or as an environment variable."
                        )
                } else {
                    stringProp("MONEYBEAR_STORE_FILE")?.takeIf { it.isNotBlank() }
                }

            if (isReleaseRequested) {
                val keystoreFile = file(storeFilePath)
                if (!keystoreFile.exists()) {
                    throw GradleException("Release keystore not found at: $storeFilePath")
                }
            }

            val storePw =
                if (isReleaseRequested) {
                    stringProp("MONEYBEAR_STORE_PASSWORD")?.takeIf { it.isNotBlank() }
                        ?: throw GradleException(
                            "Missing release signing value MONEYBEAR_STORE_PASSWORD. " +
                                "Set it in gradle.properties or as an environment variable."
                        )
                } else {
                    stringProp("MONEYBEAR_STORE_PASSWORD")?.takeIf { it.isNotBlank() }
                }

            val alias =
                if (isReleaseRequested) {
                    stringProp("MONEYBEAR_KEY_ALIAS")?.takeIf { it.isNotBlank() }
                        ?: throw GradleException(
                            "Missing release signing value MONEYBEAR_KEY_ALIAS. " +
                                "Set it in gradle.properties or as an environment variable."
                        )
                } else {
                    stringProp("MONEYBEAR_KEY_ALIAS")?.takeIf { it.isNotBlank() }
                }

            val keyPw =
                if (isReleaseRequested) {
                    stringProp("MONEYBEAR_KEY_PASSWORD")?.takeIf { it.isNotBlank() }
                        ?: throw GradleException(
                            "Missing release signing value MONEYBEAR_KEY_PASSWORD. " +
                                "Set it in gradle.properties or as an environment variable."
                        )
                } else {
                    stringProp("MONEYBEAR_KEY_PASSWORD")?.takeIf { it.isNotBlank() }
                }

            storeFile = storeFilePath?.let { file(it) }
            storePassword = storePw
            keyAlias = alias
            keyPassword = keyPw
        }
    }

    buildTypes {
        debug { }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Compose BOM & UI
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.foundation:foundation-layout")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.navigation:navigation-compose:2.8.3")

    // Kotlin coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // AndroidX core
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Room
    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Firebase & Google Sign-In
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // Charts
    implementation("com.patrykandpatrick.vico:compose:2.0.0")
    implementation("com.patrykandpatrick.vico:compose-m3:2.0.0")
    implementation("com.patrykandpatrick.vico:core:2.0.0")

    // Billing
    implementation("com.android.billingclient:billing-ktx:7.0.0")

    // Images
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
