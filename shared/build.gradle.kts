plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
}

kotlin {
    jvmToolchain(17)

    // AGP 9 erlaubt com.android.application nicht mehr im KMP-Modul, deshalb ist
    // das Android-Target hier eine Bibliothek; das APK baut :androidApp.
    android {
        namespace = "moe.rorita.kanaschule.shared"
        compileSdk = libs.versions.androidCompileSdk.get().toInt()
        minSdk = libs.versions.androidMinSdk.get().toInt()
        withHostTest {}
    }

    jvm("desktop")

    applyDefaultHierarchyTemplate()

    sourceSets {
        // Android und Desktop sind beides JVM-Ziele: alles Datei-I/O liegt einmal
        // in jvmShared, expect/actual bleibt auf das Verzeichnis beschraenkt.
        val jvmSharedMain = create("jvmSharedMain") { dependsOn(commonMain.get()) }
        androidMain.get().dependsOn(jvmSharedMain)
        getByName("desktopMain").dependsOn(jvmSharedMain)

        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        // Die Audiodateien liegen genau einmal unter shared/media und werden
        // von beiden Modulen als Ressourcenverzeichnis eingebunden. Der
        // KMP-Android-Library-Plugin verdrahtet androidMain/resources nicht.
        getByName("desktopMain").resources.srcDir("media")

        getByName("desktopMain").dependencies {
            implementation(compose.desktop.currentOs)
        }

        // Dasselbe Artefakt, das :androidApp schon benutzt. Gebraucht fuer den
        // BackHandler: der plattformuebergreifende von Compose liegt in einem
        // eigenen Artefakt, und dafuer lohnt keine weitere Abhaengigkeit.
        androidMain.get().dependencies {
            implementation(libs.androidx.activity.compose)
        }
    }
}

compose.desktop.application {
    mainClass = "moe.rorita.kanaschule.MainKt"

    // Die ProGuard-Task des Compose-Plugins ist mit Gradle 9.7 unvertraeglich
    // ("getStandardOutput(...) must not be null"). Verkleinern waere ohnehin
    // Keep-Regel-Arbeit fuer Compose und kotlinx-serialization - ein kaputtes
    // Release ist schlimmer als ein grosses.
    buildTypes.release.proguard {
        isEnabled.set(false)
    }

    nativeDistributions {
        targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb)
        packageName = "kana-schule"
        // jpackage lehnt Versionen unter 1.0.0 ab
        packageVersion = "1.0.0"
    }
}
