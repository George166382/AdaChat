// kotlin
import java.util.Properties

val properties = Properties()
if (rootProject.file("local.properties").exists()) {
    properties.load(rootProject.file("local.properties").inputStream())
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    kotlin("plugin.serialization") version "2.0.0" // replaced id(...) with explicit kotlin(...) version
    id("com.google.devtools.ksp") version "2.0.0-1.0.21"
}

android {
    namespace = "com.example.andopsi"
    compileSdk = 35


    ndkVersion = "23.1.7779620"
    defaultConfig {
        applicationId = "com.example.andopsi"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "YOUTUBE_API_KEY",
            "\"${properties.getProperty("YOUTUBE_API_KEY", "")}\""
        )
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17" // Ensure C++17 is used
                arguments += "-DANDROID_ARM_NEON=ON" // Enable NEON for ARM
            }
        }
        ndk {
            abiFilters.add("arm64-v8a")
            abiFilters.add("armeabi-v7a")
        }
    }

    // 3. Link the CMake file
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    testOptions {
            unitTests {
                isReturnDefaultValues = true
            }
        }



    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
        checkDependencies = false
        quiet = true
        disable.add("NullSafeMutableLiveData")
        disable.add("RememberInComposition")
        disable.add("FrequentlyChangingValue")
        disable.add("AutoboxingStateCreation")
    }
}

tasks.configureEach {
    if (name.contains("lint", ignoreCase = true)) {
        enabled = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation(platform("androidx.compose:compose-bom:2024.09.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.4.3")
    // build.gradle.kts
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.1")
    implementation("androidx.room:room-runtime-android:2.8.4")
    implementation("androidx.compose.animation:animation-core-android:1.7.1")
    implementation("androidx.work:work-runtime-ktx:2.9.0")
    implementation("org.postgresql:postgresql:42.7.2")
    implementation("androidx.test:core-ktx:1.7.0")
    implementation("androidx.test.ext:junit-ktx:1.3.0")
    implementation("androidx.media3:media3-common-ktx:1.9.0")
    // Unit Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.10") // Powerful mocking library
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
    // Change this from androidTestImplementation to testImplementation
    testImplementation("androidx.work:work-testing:2.9.0")

    // Add Robolectric
    testImplementation("org.robolectric:robolectric:4.11.1")

    // Ensure these are also testImplementation
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("androidx.test:core:1.5.0")

    // Standard Android Testing tools
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    // --- SUPABASE & KTOR ---
    val supabaseVersion = "3.0.2"
    implementation(platform("io.github.jan-tennert.supabase:bom:$supabaseVersion"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")

    // Ktor Client (Engine)
    implementation("io.ktor:ktor-client-android:3.0.0")

    // Serialization (updated)
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    implementation("io.coil-kt:coil-compose:2.3.0")
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.pierfrancescosoffritti.androidyoutubeplayer:chromecast-sender:0.32")
    // Retrofit JSON converter for Kotlinx Serialization
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:0.8.0")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("androidx.fragment:fragment:1.8.9")

    implementation("io.github.maitrungduc1410:ffmpeg-kit-min:6.0.1")

    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.04.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}

tasks.withType<Test>().configureEach {
    enabled = false
}
