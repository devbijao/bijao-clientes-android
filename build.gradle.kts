// AGP 9 trae Kotlin integrado (ver app/build.gradle.kts) pero por defecto
// usa SU PROPIA versión interna de KGP -- esto la fija a la misma versión
// del catálogo, para que no conviva una KGP distinta a la que usan los
// plugins compañeros (compose, serialization) declarados abajo.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    }
}

// Nivel raíz: solo declara los plugins para que las versiones queden
// resueltas una vez (el catálogo en gradle/libs.versions.toml es la fuente
// de verdad) -- cada módulo los aplica sin volver a fijar versión.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}
