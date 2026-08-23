package moe.rorita.kanaschule.ui.layout

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class WidthClass { COMPACT, MEDIUM, EXPANDED }

/**
 * Fensterklasse, aus der tatsächlichen Fläche abgeleitet - nicht aus der
 * Plattform. Ein Desktop-Fenster kann schmal sein, ein Tablet breit.
 *
 * [isShort] ist orthogonal zur Breite und fängt den einen Fall ab, der sonst
 * regelmäßig kaputt aussieht: Handy im Querformat.
 */
@Immutable
data class WindowClass(
    val width: Dp,
    val height: Dp,
) {
    val widthClass: WidthClass = when {
        width < MEDIUM_MIN -> WidthClass.COMPACT
        width < EXPANDED_MIN -> WidthClass.MEDIUM
        else -> WidthClass.EXPANDED
    }

    val isShort: Boolean = height < SHORT_MAX

    val isCompact: Boolean get() = widthClass == WidthClass.COMPACT
    val isExpanded: Boolean get() = widthClass == WidthClass.EXPANDED

    companion object {
        val MEDIUM_MIN = 600.dp
        val EXPANDED_MIN = 900.dp
        val SHORT_MAX = 480.dp
    }
}

val LocalWindowClass = staticCompositionLocalOf {
    WindowClass(width = 400.dp, height = 800.dp)
}
