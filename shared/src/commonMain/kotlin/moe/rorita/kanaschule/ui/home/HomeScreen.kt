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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.rorita.kanaschule.ui.drill.HomeInfo
import moe.rorita.kanaschule.ui.theme.MetricTextStyle

/**
 * Die Startseite. Die Prüfungsreif-Zahl ist die Schlagzeile, alles andere
 * ordnet sich darunter - das ist die Mechanik, die zum Wiederkommen bewegt.
 */
@Composable
fun HomeScreen(
    info: HomeInfo,
    muted: Boolean,
    onDrill: () -> Unit,
    onLearn: () -> Unit,
    onToggleMute: () -> Unit,
) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.widthIn(max = CONTENT_MAX.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = "Kana Führerschein",
                style = MaterialTheme.typography.headlineMedium,
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = "Prüfungsreif",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = "${info.readinessAll} %",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "${info.readinessUnlocked} % der freigeschalteten Zeichen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LinearProgressIndicator(
                        progress = { info.readinessAll / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp).padding(top = 14.dp),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        ScriptStat("Hiragana", info.readinessHiragana)
                        ScriptStat("Katakana", info.readinessKatakana)
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
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    InfoRow("Jetzt fällig", "${info.dueNow}")
                    HorizontalDivider()
                    InfoRow("Freigeschaltet", "${info.unlockedItems} von ${info.totalItems}")
                    InfoRow("Aktuelle Gruppe", info.currentGroupLabel.ifEmpty { "-" })
                    if (info.newItemsAvailable > 0) {
                        InfoRow("Neu zu lernen", "${info.newItemsAvailable}")
                    }
                    if (info.dayStreak > 0) {
                        InfoRow("Tage in Folge", "${info.dayStreak}")
                    }
                }
            }

            Button(
                onClick = onDrill,
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text(text = "Üben", style = MaterialTheme.typography.titleMedium)
            }

            OutlinedButton(
                onClick = onLearn,
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Text(text = "Lernen", style = MaterialTheme.typography.titleMedium)
            }

            // Der Ton-Schalter gehört hierher: im Lernmodus spielt die
            // Aussprache automatisch, und das muss man vorher abstellen
            // können, nicht erst danach.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleMute)
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Aussprache automatisch",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = if (muted) {
                            "Aus - nur auf Knopfdruck"
                        } else {
                            "An - spielt beim Anzeigen"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = !muted, onCheckedChange = { onToggleMute() })
            }

            info.loadProblem?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ScriptStat(label: String, percent: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(text = "$percent %", style = MetricTextStyle)
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
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
