import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.agp.app)
    /*    alias(libs.plugins.ksp)
        alias(libs.plugins.room)*/
    //alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.merak.x"
    compileSdk = 36
    val keystorePropertiesFile = rootProject.file("keystore.properties")
    val keystoreProps = Properties().apply {
        load(FileInputStream(keystorePropertiesFile))
    }
    defaultConfig {
        applicationId = "com.merak.x"
        minSdk = 35
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        getByName("debug") {
            keyAlias = keystoreProps.getProperty("keyAlias")
            keyPassword = keystoreProps.getProperty("keyPassword")
            storeFile = rootProject.file(keystoreProps["storeFile"] as String)
            storePassword = keystoreProps.getProperty("storePassword")
            enableV1Signing = true
            enableV2Signing = true
        }

        create("release") {
            keyAlias = keystoreProps.getProperty("keyAlias")
            keyPassword = keystoreProps.getProperty("keyPassword")
            storeFile = rootProject.file(keystoreProps["storeFile"] as String)
            storePassword = keystoreProps.getProperty("storePassword")
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        getByName("debug") {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isShrinkResources = true
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }

    buildFeatures {
        buildConfig = true
        compose = true
        aidl = true
    }
    packaging {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
}

kotlin {
    jvmToolchain(25)
}

/*room {
    // Specify the schema directory
    schemaDirectory("$projectDir/schemas")
}*/

dependencies {
    implementation(libs.androidx.core)
    implementation(libs.androidx.lifecycle)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androix.splashscreen)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.navigation)
    implementation(libs.compose.materialIcons)
    // Preview support only for debug builds
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    debugImplementation(libs.compose.runtime.tracing)

    //implementation(libs.room.runtime)
    // ksp(libs.room.compiler)
    //implementation(libs.room.ktx)
    // implementation(libs.ktx.serializationJson)

    implementation(libs.hiddenapibypass)

    implementation(platform(libs.koin.bom))
    implementation(libs.koin.core)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.koin.compose.viewmodel)

    implementation(libs.rikka.shizuku.api)
    implementation(libs.rikka.shizuku.provider)

    // implementation(libs.iamr0s.androidAppProcess)

    // log
    implementation(libs.timber)

    // miuix
    implementation(libs.miuix)
    implementation(libs.miuix.icons)

    // haze
    implementation(libs.haze)
    implementation(libs.haze.materials)

    // m3color
    implementation(libs.m3color)
}