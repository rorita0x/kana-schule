package moe.rorita.kanaschule.ui.nav

import androidx.compose.runtime.Composable

/** Der Desktop hat keine Rückwärtstaste - Escape erledigt das an der Wurzel. */
@Composable
actual fun PlatformBackHandler(enabled: Boolean, onBack: () -> Unit) {
}
