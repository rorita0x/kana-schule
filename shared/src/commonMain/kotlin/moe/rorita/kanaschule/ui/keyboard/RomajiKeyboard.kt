package moe.rorita.kanaschule.ui.keyboard

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Die eigene Tastatur. Bewusst kein Systemkeyboard: das würde die Eingabe
 * hilfreich groß schreiben, „tu“ zu „to“ verbessern und mit seiner
 * Vorschlagsleiste die Zeitmessung verfälschen.
 */
@Composable
fun RomajiKeyboard(
    onKey: (Char) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    submitEnabled: Boolean,
    modifier: Modifier = Modifier,
    keyHeight: Dp = 52.dp,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 5.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        RomajiKeyLayout.rows.forEachIndexed { index, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                if (index == LAST_ROW) {
                    Key(
                        label = "⌫",
                        weight = 1.7f,
                        keyHeight = keyHeight,
                        fontSize = 18.sp,
                        background = MaterialTheme.colorScheme.surfaceVariant,
                        foreground = MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = onBackspace,
                    )
                }
                row.forEach { char ->
                    Key(
                        label = char.toString(),
                        weight = 1f,
                        keyHeight = keyHeight,
                        fontSize = 19.sp,
                        background = MaterialTheme.colorScheme.surface,
                        foreground = MaterialTheme.colorScheme.onSurface,
                        onClick = { onKey(char) },
                    )
                }
                if (index == LAST_ROW) {
                    Key(
                        label = "⏎",
                        weight = 1.7f,
                        keyHeight = keyHeight,
                        fontSize = 18.sp,
                        background = if (submitEnabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        },
                        foreground = if (submitEnabled) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        onClick = onSubmit,
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.Key(
    label: String,
    weight: Float,
    keyHeight: Dp,
    fontSize: androidx.compose.ui.unit.TextUnit,
    background: Color,
    foreground: Color,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .weight(weight)
            .height(keyHeight),
        shape = RoundedCornerShape(8.dp),
        color = background,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                fontSize = fontSize,
                fontWeight = FontWeight.Medium,
                color = foreground,
            )
        }
    }
}

private const val LAST_ROW = 2
