plugins {
    alias(libs.plugins.agp.app)
    alias(libs.plugins.kotlin)
    alias(libs.plugins.safeargs)
    id("kotlin-kapt")
}

val app_name = "Bluetooth LE Spam"

android {
    namespace = "de.simon.dankelmann.bluetoothlespam"
    compileSdk = 35

    defaultConfig {
        applicationId = "de.simon.dankelmann.bluetoothlespam"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "1.0.9"
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

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        viewBinding = true
    }
}


dependencies {
    implementation(libs.airbnb.lottie)

    implementation(libs.core.ktx)
    implementation(libs.preference.ktx)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.androidx.appcompat)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.legacy.support)
    implementation(libs.android.constraintlayout)
    implementation(libs.google.material)

    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)

    // To use Kotlin annotation processing tool (kapt)
    kapt(libs.room.compiler)

    // To use Kotlin Symbol Processing (KSP)
    //ksp(libs.room.compiler)

    // optional - Kotlin Extensions and Coroutines support for Room
    //implementation(libs.room.ktx)

    // optional - RxJava2 support for Room
    //implementation(libs.room.rxjava2)

    // optional - RxJava3 support for Room
    implementation(libs.room.rxjava3)

    // optional - Guava support for Room, including Optional and ListenableFuture
    //implementation(libs.room.guava)

    // optional - Test helpers
    //testImplementation(libs.room.testing)

    // optional - Paging 3 Integration
    //implementation(libs.room.paging)
}
