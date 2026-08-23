package moe.rorita.kanaschule.ui.learn

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import moe.rorita.kanaschule.kana.Kana
import moe.rorita.kanaschule.ui.layout.LocalWindowClass
import moe.rorita.kanaschule.ui.theme.LocalFeedbackColors
import moe.rorita.kanaschule.ui.theme.MetricTextStyle

/**
 * Zeigt ein Zeichen zum Lernen: beide Schriften, die Romaji-Lesung, die
 * Aussprache zum Nachlesen und zum Anhoeren.
 */
@Composable
fun LearnScreen(
    state: LearnState,
    onPlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleMute: () -> Unit,
    onToggleShowAll: () -> Unit,
    onQuit: () -> Unit,
) {
    val window = LocalWindowClass.current
    val colors = LocalFeedbackColors.current
    val card = state.card

    // Automatisch vorspielen, aber nur wenn der Ton nicht abgeschaltet ist.
    LaunchedEffect(card.index, state.muted) {
        if (card.audioName != null && !state.muted) onPlay()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${card.index + 1} von ${card.total}",
                style = MetricTextStyle,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                MuteToggle(muted = state.muted, onToggle = onToggleMute)
                TextButton(onClick = onQuit) {
                    Text(if (state.standalone) "Zurueck" else "Beenden")
                }
            }
        }
        HorizontalDivider()

        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = CONTENT_MAX.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    card.hiragana?.let {
                        ScriptCard(
                            label = "Hiragana",
                            kana = it,
                            glyphSize = if (window.isCompact) 88 else 112,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    card.katakana?.let {
                        ScriptCard(
                            label = "Katakana",
                            kana = it,
                            glyphSize = if (window.isCompact) 88 else 112,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = card.romaji,
                            fontSize = 46.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        if (card.audioName != null) {
                            PlayButton(onPlay = onPlay)
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
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            text = "Aussprache",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        card.hint.consonant?.let { HintLine("Konsonant", it) }
                        card.hint.vowel?.let { HintLine("Vokal", it) }
                        card.hint.warning?.let { HintLine("Achtung", it, accent = colors.wrong) }
                        card.noteDe?.let { HintLine("Gebrauch", it, accent = colors.neutral) }
                    }
                }

                if (state.standalone) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        OutlinedButton(
                            onClick = onPrevious,
                            enabled = card.hasPrevious,
                            modifier = Modifier.weight(1f).height(50.dp),
                        ) {
                            Text("Zurueck")
                        }
                        Button(
                            onClick = onNext,
                            enabled = !card.isLast,
                            modifier = Modifier.weight(1f).height(50.dp),
                        ) {
                            Text("Weiter")
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onToggleShowAll)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Checkbox(checked = state.showAll, onCheckedChange = { onToggleShowAll() })
                        Column {
                            Text(
                                text = "Alle Zeichen zeigen",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = if (state.showAll) {
                                    "Alle Lesungen, unabhaengig vom Fortschritt"
                                } else {
                                    "Nur die noch nicht gelernten"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Text(
                        text = "Pfeiltasten - blaettern · Leertaste - anhoeren · Esc - zurueck",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Button(
                        onClick = onNext,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                    ) {
                        Text(if (card.isLast) "Los geht's" else "Weiter")
                    }
                    Text(
                        text = "Enter - weiter · Leertaste - nochmal hoeren",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MuteToggle(muted: Boolean, onToggle: () -> Unit) {
    TextButton(onClick = onToggle) {
        Text(
            text = if (muted) "Ton aus" else "Ton an",
            style = MaterialTheme.typography.labelLarge,
            color = if (muted) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
    }
}

@Composable
private fun ScriptCard(
    label: String,
    kana: Kana,
    glyphSize: Int,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = kana.glyph,
                fontSize = glyphSize.sp,
                lineHeight = (glyphSize * 1.2f).sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun PlayButton(onPlay: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primary,
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onPlay)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "▶",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Text(
                text = "Aussprache",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun HintLine(label: String, text: String, accent: Color? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = accent ?: MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private const val CONTENT_MAX = 520
