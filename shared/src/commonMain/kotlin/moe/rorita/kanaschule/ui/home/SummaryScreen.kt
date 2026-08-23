package moe.rorita.kanaschule.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.rorita.kanaschule.ui.drill.SessionResult
import moe.rorita.kanaschule.ui.theme.LocalFeedbackColors
import moe.rorita.kanaschule.ui.theme.MetricTextStyle

/**
 * Der Abschluss. Ganz oben steht die Veraenderung der Pruefungsreife mit
 * Pfeil - nicht die Trefferquote dieser Session, denn die haengt vor allem
 * daran, wie viel Neues dabei war, und wuerde damit genau das richtige
 * Verhalten bestrafen.
 */
@Composable
fun SummaryScreen(
    result: SessionResult,
    onDone: () -> Unit,
    onAgain: () -> Unit,
) {
    val colors = LocalFeedbackColors.current
    val delta = result.readinessAfter - result.readinessBefore

    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = CONTENT_MAX.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Pruefungsreif",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "${result.readinessBefore} %",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(text = "→", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "${result.readinessAfter} %",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (delta != 0) {
                        Text(
                            text = if (delta > 0) "+$delta" else "$delta",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (delta > 0) colors.correct else colors.wrong,
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp),
                ) {
                    val accuracy =
                        if (result.asked == 0) 0 else 100 * result.correct / result.asked
                    Line("Richtig", "${result.correct} von ${result.asked}  ($accuracy %)")
                    Line("Aufgestiegen", "${result.promoted}")
                    Line("Abgestiegen", "${result.demoted}")
                    Line("Mittlere Zeit", "${result.medianMs} ms")
                    if (result.newItems > 0) {
                        Line("Neue Zeichen", "${result.newItems}")
                    }
                    result.unlockedGroupLabel?.let {
                        HorizontalDivider()
                        Line("Freigeschaltet", it)
                    }
                }
            }

            if (result.missed.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Danebengegangen",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        result.missed.forEach { entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    text = entry.kana.glyph,
                                    style = MaterialTheme.typography.headlineSmall,
                                )
                                Text(
                                    text = entry.expected,
                                    style = MetricTextStyle,
                                    color = colors.correct,
                                )
                                Text(
                                    text = entry.typed.ifEmpty { "(leer)" },
                                    style = MetricTextStyle,
                                    color = colors.wrong,
                                )
                            }
                        }
                    }
                }
            }

            result.topConfusion?.let { (a, b) ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "Verwechslung dieser Session",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Text(
                            text = "${a.glyph} ↔ ${b.glyph}",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            Button(
                onClick = onAgain,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text("Noch eine Runde")
            }
            Button(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth().height(48.dp),
            ) {
                Text("Fertig")
            }
        }
    }
}

@Composable
private fun Line(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = value, style = MetricTextStyle)
    }
}

private const val CONTENT_MAX = 460
