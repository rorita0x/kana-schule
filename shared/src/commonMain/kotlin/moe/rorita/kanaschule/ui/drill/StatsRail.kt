package moe.rorita.kanaschule.ui.drill

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import moe.rorita.kanaschule.ui.theme.LocalFeedbackColors
import moe.rorita.kanaschule.ui.theme.MetricTextStyle

/**
 * Die Seitenleiste auf breiten Fenstern. Sie ist der eigentliche Vorteil des
 * Desktops: was auf dem Handy einen zweiten Bildschirm braucht, steht hier
 * daneben und bleibt beim Ueben sichtbar.
 */
@Composable
fun StatsRail(
    state: DrillUiState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Session",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            LinearProgressIndicator(
                progress = { state.progress },
                modifier = Modifier.fillMaxWidth().height(6.dp),
            )
            Text(
                text = "${state.asked} von ${state.target}",
                style = MetricTextStyle,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        HorizontalDivider()

        Metric(label = "Trefferquote", value = "${state.accuracy} %")
        Metric(label = "Serie", value = state.streak.toString())
        Metric(
            label = "Pruefungsreif",
            value = "${state.readinessNow} %",
            delta = state.readinessNow - state.readinessAtStart,
        )

        if (state.weakest.isNotEmpty()) {
            HorizontalDivider()
            Text(
                text = "Schwaechste Zeichen",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                state.weakest.forEach { entry ->
                    WeakRow(entry)
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
private fun Metric(label: String, value: String, delta: Int? = null) {
    val colors = LocalFeedbackColors.current
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
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = value, style = MetricTextStyle)
            if (delta != null && delta != 0) {
                Text(
                    text = if (delta > 0) "+$delta" else "$delta",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (delta > 0) colors.correct else colors.wrong,
                )
            }
        }
    }
}

@Composable
private fun WeakRow(entry: WeakEntry) {
    val colors = LocalFeedbackColors.current
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = entry.kana.glyph,
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = entry.kana.canonical,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            Text(
                text = "${(entry.accuracy * 100).toInt()} %",
                style = MetricTextStyle,
                textAlign = TextAlign.End,
                color = if (entry.accuracy >= 0.7) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    colors.wrong
                },
            )
        }
    }
}
