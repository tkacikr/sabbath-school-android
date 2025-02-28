/*
 * Copyright (c) 2020. Adventech <info@adventech.io>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import extensions.readPropertyValue
import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
}

val useReleaseKeystore = file(BuildAndroidConfig.KEYSTORE_PROPS_FILE).exists()
val appVersionCode = readPropertyValue(
    filePath = "build_number.properties",
    key = "BUILD_NUMBER",
    defaultValue = "1"
).toInt() + 1490

val webClientId = readPropertyValue(
    filePath = "$rootDir/${BuildAndroidConfig.API_KEYS_PROPS_FILE}",
    key = "WEB_CLIENT_ID",
    defaultValue = ""
)

android {
    compileSdk = BuildAndroidConfig.COMPILE_SDK_VERSION

    defaultConfig {
        applicationId = BuildAndroidConfig.APP_ID
        minSdk = BuildAndroidConfig.MIN_SDK_VERSION
        targetSdk = BuildAndroidConfig.TARGET_SDK_VERSION

        versionCode = appVersionCode
        versionName = "${BuildAndroidConfig.Version.name} ($appVersionCode)"

        testInstrumentationRunner = BuildAndroidConfig.TEST_INSTRUMENTATION_RUNNER

        vectorDrawables.useSupportLibrary = true

        ndk {
            abiFilters.addAll(
                // keep all binaries supported by PSPDFKit
                listOf("x86", "x86_64", "arm64-v8a", "armeabi-v7a")
            )
        }

        buildConfigField("String", "WEB_CLIENT_ID", "\"$webClientId\"")
    }

    namespace = BuildAndroidConfig.APP_ID

    signingConfigs {
        if (useReleaseKeystore) {
            val keyProps = Properties().apply {
                load(FileInputStream(file(BuildAndroidConfig.KEYSTORE_PROPS_FILE)))
            }

            create("release") {
                storeFile = file(keyProps.getProperty("release.keystore"))
                storePassword = keyProps.getProperty("release.keystore.password")
                keyAlias = keyProps.getProperty("key.alias")
                keyPassword = keyProps.getProperty("key.password")
            }
        }
    }

    buildTypes {
        val release by getting {
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles("proguard-rules.pro")
            if (useReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }

            ndk { debugSymbolLevel = "FULL" }

            // Temporary for Glance
            optimization {
                keepRules {
                    ignoreExternalDependencies("androidx.glance:glance-appwidget")
                }
            }
        }

        val benchmark by creating {
            // Enable all the optimizations from release build through initWith(release).
            initWith(release)
            matchingFallbacks.add("release")
            // Debug key signing is available to everyone.
            signingConfig = signingConfigs.getByName("debug")
            // Only use benchmark proguard rules
            proguardFiles("benchmark-proguard-rules.pro")
            isMinifyEnabled = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = libs.versions.jvmTarget.get()
        freeCompilerArgs = freeCompilerArgs + "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
        unitTests.isReturnDefaultValues = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.androidx.compose.compiler.get()
    }

    buildFeatures {
        viewBinding = true
        dataBinding = true
        compose = true
    }

    packagingOptions {
        // Multiple dependency bring these files in. Exclude them to enable
        // our test APK to build (has no effect on our AARs)
        resources.excludes += "/META-INF/AL2.0"
        resources.excludes += "/META-INF/LGPL2.1"
    }
}

dependencies {
    implementation(projects.common.auth)
    implementation(projects.common.core)
    implementation(projects.common.design)
    implementation(projects.common.designCompose)
    implementation(projects.common.lessonsData)
    implementation(projects.common.network)
    implementation(projects.common.runtimePermissions)
    implementation(projects.common.storage)
    implementation(projects.common.translations)
    implementation(projects.features.appWidgets)
    implementation(projects.features.account)
    implementation(projects.features.bible)
    implementation(projects.features.lessons)
    implementation(projects.features.media)
    implementation(projects.features.pdf)
    implementation(projects.features.settings)

    implementation(libs.kotlin.coroutines)
    implementation(libs.kotlin.coroutines.android)

    implementation(libs.google.material)
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.extensions)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.startup)
    implementation(libs.androidx.work)
    implementation(libs.androidx.hilt.work)
    kapt(libs.androidx.hilt.compiler)

    implementation(libs.google.hilt.android)
    kapt(libs.google.hilt.compiler)

    implementation(libs.google.play.auth)

    implementation(libs.timber)

    implementation(libs.joda.android)

    testImplementation(libs.bundles.testing.common)
    testImplementation(projects.libraries.testUtils)
    kaptTest(libs.google.hilt.compiler)
    androidTestImplementation(libs.bundles.testing.android.common)
    kaptAndroidTest(libs.google.hilt.compiler)
    androidTestImplementation(projects.libraries.testUtils)
    androidTestImplementation(libs.test.androidx.espresso.contrib) {
        exclude(group = "org.checkerframework", module = "checker")
    }
}
