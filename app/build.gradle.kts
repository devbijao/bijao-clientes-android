import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// Maps API key: `secrets.properties` (gitignorado, con la key real) pisa a
// `secrets.defaults.properties` (versionado, vacío) si existe -- mismo patrón
// que usa el sample oficial de maps-compose, sin necesitar su plugin de
// Gradle aparte. La key es la MISMA que ya usan el frontend y el backend del
// monorepo (`GOOGLE_MAPS_API_KEY` en `backend/.env`); falta restringirla por
// paquete+SHA-1 de Android en Google Cloud Console (paso manual pendiente).
val secretsProperties = Properties().apply {
    val defaults = rootProject.file("secrets.defaults.properties")
    val local = rootProject.file("secrets.properties")
    if (defaults.exists()) load(defaults.inputStream())
    if (local.exists()) load(local.inputStream())
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
        manifestPlaceholders["MAPS_API_KEY"] = secretsProperties.getProperty("MAPS_API_KEY", "")
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

    // Ubicación actual, compartida entre el selector de ciudad del Onboarding
    // (Fase A2) y "cerca de mí" en Explorar (Fase A3).
    implementation(libs.play.services.location)

    // Mapa del directorio (Fase A3, §1.1 del plan) -- Maps SDK vía Compose.
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)

    // Logos/portadas de negocios (Fase A3+) -- equivalente de `AsyncImage` de SwiftUI.
    implementation(libs.coil.compose)
}
