package moe.rorita.kanaschule.ui.drill

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.rorita.kanaschule.kana.Kana

/**
 * Das Zeichen selbst. Die Schriftgroesse kommt aus der verfuegbaren Flaeche,
 * nicht aus einer festen Zahl: dasselbe Kana soll auf dem Handy die Mitte
 * fuellen und auf dem Desktop nicht laecherlich klein wirken.
 */
@Composable
fun GlyphCard(
    kana: Kana,
    isNew: Boolean,
    modifier: Modifier = Modifier,
    scale: Float = 0.30f,
    minSize: Dp = 96.dp,
    maxSize: Dp = 160.dp,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = if (isNew) {
            androidx.compose.foundation.BorderStroke(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary,
            )
        } else {
            null
        },
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val shorter = if (maxWidth < maxHeight) maxWidth else maxHeight
            val target = (shorter.value * scale).dp.coerceIn(minSize, maxSize)

            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            ) {
                if (isNew) {
                    Text(
                        text = "neu",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = kana.glyph,
                        fontSize = target.value.sp,
                        lineHeight = (target.value * 1.15f).sp,
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}
