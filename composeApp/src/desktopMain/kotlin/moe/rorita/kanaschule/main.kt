package moe.rorita.kanaschule

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Kana-Schule") {
        App()
    }
}
