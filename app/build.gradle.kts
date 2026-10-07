plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.bijao.clientes"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.bijao.clientes"
        // DA5 del plan: API 26 cubre ~98%+ de dispositivos activos y ya
        // trae notification channels / adaptive icons nativos.
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // Sin bloque `kotlinOptions {}` -- esa extensión la aportaba el plugin
    // "kotlin-android", que ya no se aplica (Kotlin viene integrado en AGP
    // 9). El jvmTarget de Kotlin se deriva de `compileOptions` de arriba.

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.kotlinx.serialization.json)

    // Auth nativa contra el proyecto Supabase AISLADO de Clientes (DA2) --
    // mismo JWT, mismo flujo OTP por SMS vía Bird que ya usa iOS.
    implementation(libs.supabase.auth)
    implementation(libs.ktor.client.android)

    // Red contra el backend FastAPI compartido (DA3) -- /api/app/*, sin
    // ningún endpoint nuevo para arrancar.
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)

    // Ubicación actual para el selector de ciudad del Onboarding (Fase A2) --
    // junto con `Geocoder` (del SDK de Android, sin API key) resuelve nombre
    // de ciudad sin depender todavía de Places/Maps (eso sigue bloqueado por
    // la API key de Google Cloud, ver §1.1 del plan).
    implementation(libs.play.services.location)
}
