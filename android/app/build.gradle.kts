plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.magopato.voltanalytics"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.magopato.voltanalytics"
        minSdk = 24
        targetSdk = 35
        versionCode = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionName = "1.0." + (System.getenv("GITHUB_RUN_NUMBER") ?: "0")
    }

    // Firma: si el CI define las variables de entorno, se usa ese keystore;
    // si no, se firma con la clave de depuración para que siga siendo instalable.
    val ksPath = System.getenv("ANDROID_KEYSTORE_PATH")
    signingConfigs {
        if (ksPath != null) {
            create("release") {
                storeFile = file(ksPath)
                storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (ksPath != null) signingConfigs.getByName("release")
                            else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
}
