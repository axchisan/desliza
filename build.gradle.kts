// AGP 9 trae Kotlin integrado, pero con el compilador 2.2; las bibliotecas actuales de Compose y
// Coil ya publican metadatos de Kotlin 2.4, así que se fija aquí la versión del compilador.
buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    }
}

plugins {
    id("com.android.application") version "9.4.1" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.21" apply false
}
