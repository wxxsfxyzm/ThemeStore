plugins {
    alias(libs.plugins.agp.lib)
}

android {
    namespace = "com.com.merak.hidden_api"
    compileSdk = 37

    defaultConfig {
        minSdk = 35
    }

    compileOptions {
        targetCompatibility = JavaVersion.VERSION_25
        sourceCompatibility = JavaVersion.VERSION_25
    }
}

dependencies {
}
