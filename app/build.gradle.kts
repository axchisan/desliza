import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

// La clave de firma no vive en el repositorio. Si `firma.properties` no existe, la compilación de
// publicación usa la clave de depuración para que una copia recién clonada compile igual.
val propiedadesFirma = Properties()
val archivoFirma = rootProject.file("firma.properties")
if (archivoFirma.exists()) {
    archivoFirma.inputStream().use { propiedadesFirma.load(it) }
}
val hayFirmaPropia = propiedadesFirma.containsKey("storeFile")

android {
    namespace = "com.axchisan.desliza"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.axchisan.desliza"
        // Android 11 es el primero con las peticiones de borrado por lotes de MediaStore.
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    androidResources {
        localeFilters += listOf("es")
    }

    signingConfigs {
        if (hayFirmaPropia) {
            create("publicacion") {
                storeFile = file(propiedadesFirma.getProperty("storeFile"))
                storePassword = propiedadesFirma.getProperty("storePassword")
                keyAlias = propiedadesFirma.getProperty("keyAlias")
                keyPassword = propiedadesFirma.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = if (hayFirmaPropia) {
                signingConfigs.getByName("publicacion")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    val bom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(bom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.core:core-ktx:1.19.1")

    implementation("io.coil-kt.coil3:coil-compose:3.6.3")
    implementation("io.coil-kt.coil3:coil-video:3.6.3")

    implementation("androidx.media3:media3-exoplayer:1.11.1")
    implementation("androidx.media3:media3-ui-compose:1.11.1")
}
