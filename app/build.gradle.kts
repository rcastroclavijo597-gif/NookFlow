plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.myapplication"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 33
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.database)
    implementation(libs.material)
    implementation("org.osmdroid:osmdroid-android:6.1.20")
    // Mapa vectorial gratuito (MapLibre). Se usa la versión OpenGL para que funcione en el emulador.
    implementation("org.maplibre.gl:android-sdk-opengl:13.6.1")
    implementation("com.google.android.gms:play-services-maps:20.1.0")
    implementation("com.google.android.gms:play-services-location:21.4.0")
    implementation("com.google.android.libraries.places:places:3.5.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}