package moe.rorita.kanaschule.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import moe.rorita.kanaschule.store.ThemeMode

/**
 * Farben für Bewertungen. Bewusst neben dem Material-Schema, weil „richtig“
 * und „falsch“ hier eigene, über beide Modi stabile Rollen sind.
 */
@Immutable
data class FeedbackColors(
    val correct: Color,
    val wrong: Color,
    val neutral: Color,
)

val LocalFeedbackColors = staticCompositionLocalOf {
    FeedbackColors(CorrectLight, WrongLight, NeutralLight)
}

@Composable
fun KanaTheme(
    mode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val feedback = if (dark) {
        FeedbackColors(CorrectDark, WrongDark, NeutralDark)
    } else {
        FeedbackColors(CorrectLight, WrongLight, NeutralLight)
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalFeedbackColors provides feedback) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = KanaTypography,
            content = content,
        )
    }
}
