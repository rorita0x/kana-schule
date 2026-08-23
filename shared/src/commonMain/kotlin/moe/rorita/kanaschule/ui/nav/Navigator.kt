package moe.rorita.kanaschule.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * Vier Bildschirme, keine Deeplinks, keine Argument-Serialisierung. Dafür
 * braucht es kein Navigations-Framework - eine sealed interface und ein
 * beobachteter Stack genügen.
 */
sealed interface Screen {
    data object Home : Screen
    data object Drill : Screen
    data object Summary : Screen
    data object Settings : Screen
}

@Stable
class Navigator(start: Screen = Screen.Home) {

    private val stack: SnapshotStateList<Screen> = mutableStateListOf(start)

    val current: Screen get() = stack.last()

    val canGoBack: Boolean get() = stack.size > 1

    fun go(screen: Screen) {
        if (stack.last() != screen) stack.add(screen)
    }

    /** Ersetzt den obersten Eintrag - für Übergänge ohne Rückweg. */
    fun replace(screen: Screen) {
        stack[stack.lastIndex] = screen
    }

    fun back(): Boolean {
        if (!canGoBack) return false
        stack.removeAt(stack.lastIndex)
        return true
    }

    fun home() {
        while (stack.size > 1) stack.removeAt(stack.lastIndex)
    }
}

@Composable
fun rememberNavigator(start: Screen = Screen.Home): Navigator = remember { Navigator(start) }
