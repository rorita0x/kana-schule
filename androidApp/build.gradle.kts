import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

/**
 * Signierdaten aus einer Datei ausserhalb der Versionskontrolle. Fehlt sie,
 * bleibt der Release-Build unsigniert statt fehlzuschlagen - dann steht in
 * der Ausgabe, was zu tun ist.
 */
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
}

android {
    namespace = "moe.rorita.kanaschule"
    compileSdk = libs.versions.androidCompileSdk.get().toInt()

    defaultConfig {
        applicationId = "moe.rorita.kanaschule"
        minSdk = libs.versions.androidMinSdk.get().toInt()
        targetSdk = libs.versions.androidTargetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
    }

    // Dieselben Dateien wie beim Desktop-Ziel, nur als Assets: AGP 9 packt
    // Java-Ressourcen nicht mehr ins APK, Assets schon.
    sourceSets.getByName("main").assets.directories.add("../shared/media")

    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.findByName("release")
            // R8 bleibt vorerst aus: Compose und kotlinx-serialization
            // brauchen dafuer Keep-Regeln, und ein kaputtes Release ist
            // schlimmer als ein grosses.
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
}
