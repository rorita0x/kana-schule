package moe.rorita.kanaschule.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Tinte auf warmem Papier mit einem Zinnober-Akzent. Kein Material-Standard-
 * Violett: das Zeichen soll das Auffälligste auf dem Bildschirm sein, alles
 * andere tritt zurück.
 */
private val Paper = Color(0xFFFBF8F3)
private val PaperRaised = Color(0xFFFFFFFF)
private val PaperSunken = Color(0xFFF1ECE3)
private val Ink = Color(0xFF1C1917)
private val InkSoft = Color(0xFF57534E)

private val Vermilion = Color(0xFFC8442A)
private val VermilionSoft = Color(0xFFFBE6E1)
private val IndigoInk = Color(0xFF2F4858)
private val IndigoSoft = Color(0xFFE3EAEE)

private val Sumi = Color(0xFF14110F)
private val SumiRaised = Color(0xFF1E1B19)
private val SumiSunken = Color(0xFF0D0B0A)
private val Chalk = Color(0xFFF0EBE4)
private val ChalkSoft = Color(0xFFA8A29E)

private val VermilionBright = Color(0xFFE06A4E)
private val VermilionDeep = Color(0xFF4A1A11)
private val IndigoBright = Color(0xFF8FB2C4)
private val IndigoDeep = Color(0xFF1B2A33)

/** Richtig und falsch sind eigene Rollen, keine Material-Farben. */
val CorrectLight = Color(0xFF2E7D5B)
val CorrectDark = Color(0xFF6FCFA3)
val WrongLight = Color(0xFFB3261E)
val WrongDark = Color(0xFFF2837B)
val NeutralLight = InkSoft
val NeutralDark = ChalkSoft

val LightColors = lightColorScheme(
    primary = Vermilion,
    onPrimary = Color.White,
    primaryContainer = VermilionSoft,
    onPrimaryContainer = Color(0xFF5C1B10),
    secondary = IndigoInk,
    onSecondary = Color.White,
    secondaryContainer = IndigoSoft,
    onSecondaryContainer = Color(0xFF17242C),
    background = Paper,
    onBackground = Ink,
    surface = PaperRaised,
    onSurface = Ink,
    surfaceVariant = PaperSunken,
    onSurfaceVariant = InkSoft,
    outline = Color(0xFFD6CFC4),
    outlineVariant = Color(0xFFE8E2D8),
    error = WrongLight,
    onError = Color.White,
)

val DarkColors = darkColorScheme(
    primary = VermilionBright,
    onPrimary = Color(0xFF2A0C06),
    primaryContainer = VermilionDeep,
    onPrimaryContainer = Color(0xFFFFD9D0),
    secondary = IndigoBright,
    onSecondary = Color(0xFF0F1A20),
    secondaryContainer = IndigoDeep,
    onSecondaryContainer = Color(0xFFCFE2EB),
    background = Sumi,
    onBackground = Chalk,
    surface = SumiRaised,
    onSurface = Chalk,
    surfaceVariant = SumiSunken,
    onSurfaceVariant = ChalkSoft,
    outline = Color(0xFF3B3634),
    outlineVariant = Color(0xFF292523),
    error = WrongDark,
    onError = Color(0xFF2A0906),
)
