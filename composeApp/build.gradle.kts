plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.composeMultiplatform)
}

kotlin {
    jvmToolchain(17)

    jvm("desktop")

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.material3)
            implementation(libs.kotlinx.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        getByName("desktopMain").dependencies {
            implementation(compose.desktop.currentOs)
        }
    }
}

compose.desktop.application {
    mainClass = "moe.rorita.kanaschule.MainKt"

    nativeDistributions {
        targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb)
        packageName = "kana-schule"
        // jpackage lehnt Versionen unter 1.0.0 ab
        packageVersion = "1.0.0"
    }
}
