package moe.rorita.kanaschule.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Systemschrift, aber mit engeren Zeilenabständen und kräftigeren Titeln als
 * der Material-Standard. Die Glyphengröße selbst wird nicht hier festgelegt,
 * sondern vom Layout aus der verfügbaren Fläche berechnet.
 */
val KanaTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontWeight = FontWeight.Normal),
        headlineLarge = base.headlineLarge.copy(fontWeight = FontWeight.SemiBold),
        headlineMedium = base.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = base.titleMedium.copy(fontWeight = FontWeight.Medium),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp),
    )
}

/** Die getippte Antwort: gleiche Zeichenbreite, damit der Cursor nicht wandert. */
val AnswerTextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Medium,
    fontSize = 34.sp,
    letterSpacing = 2.sp,
)

/** Kleine Kennzahlen in der Kopfzeile und der Seitenleiste. */
val MetricTextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Medium,
    fontSize = 15.sp,
)
