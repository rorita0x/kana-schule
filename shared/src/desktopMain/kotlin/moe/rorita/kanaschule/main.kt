package moe.rorita.kanaschule

import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Dimension

fun main() = application {
    // Ohne eigene Angabe öffnet Compose Desktop mit 800x600. Das liegt unter
    // der Schwelle von 900 dp, also käme selbst auf einem großen Bildschirm
    // das einspaltige Handy-Layout.
    val windowState = rememberWindowState(
        size = DpSize(1120.dp, 840.dp),
        position = WindowPosition(Alignment.Center),
    )

    Window(
        onCloseRequest = ::exitApplication,
        title = "Kana Führerschein",
        state = windowState,
    ) {
        // Verhindert, dass das Fenster kleiner gezogen wird als die Oberfläche
        // sinnvoll darstellbar ist.
        window.minimumSize = Dimension(MIN_WIDTH, MIN_HEIGHT)
        App()
    }
}

private const val MIN_WIDTH = 480
private const val MIN_HEIGHT = 640
