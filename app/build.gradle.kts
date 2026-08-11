plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
}

val app_name = "Bluetooth LE Spam"

android {
    namespace = "de.simon.dankelmann.bluetoothlespam"
    compileSdk = 37

    defaultConfig {
        applicationId = "de.simon.dankelmann.bluetoothlespam"
        minSdk = 26
        targetSdk = compileSdk
        versionCode = 3
        versionName = "1.0.9"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file("release.jks")
            storePassword = System.getenv("STORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }

    buildTypes {
        configureEach {
            val variant = if (File("release.jks").exists()) "release" else "debug"
            signingConfig = signingConfigs[variant]
        }
        release {
            resValue("string", "app_name", app_name)
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            resValue("string", "app_name", "$app_name Debug")
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        resValues = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        // Room schema JSON (ksp room.schemaLocation above) — MigrationTestHelper loads
        // expected/target schemas from here to build + validate migration test databases.
        // Robolectric's unit-test config points at the "debug" variant's merged assets (not a
        // test-specific assets dir), so this has to live on the debug build type, not
        // sourceSets.test — scoped to debug only so schemas/ never ships in a release APK.
        getByName("debug").assets.srcDirs("$projectDir/schemas")
        getByName("androidTest").assets.srcDirs("$projectDir/schemas")
    }
}


ksp {
    // Ground truth for hand-written Migration SQL + lets MigrationTestHelper validate against
    // the real expected schema instead of a hand-rolled one.
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(libs.airbnb.lottie)
    implementation(libs.airbnb.lottie.compose)

    implementation(libs.core.ktx)
    implementation(libs.preference.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.androidx.appcompat)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.legacy.support)
    implementation(libs.google.material)

    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // To use Kotlin Symbol Processing (KSP)
    ksp(libs.room.compiler)

    // optional - Kotlin Extensions and Coroutines support for Room
    //implementation(libs.room.ktx)

    // optional - RxJava2 support for Room
    //implementation(libs.room.rxjava2)

    // optional - RxJava3 support for Room
    implementation(libs.room.rxjava3)

    // optional - Guava support for Room, including Optional and ListenableFuture
    //implementation(libs.room.guava)

    // optional - Paging 3 Integration
    //implementation(libs.room.paging)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Blur (floating nav bar backdrop only, see plan §4) + dynamic-color-consistent static palette
    implementation(libs.haze)
    implementation(libs.haze.materials)
    implementation(libs.material.kolor)

    // Test infra (T0)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.room.testing)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.room.testing)
}
