package moe.rorita.kanaschule.ui.drill

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import moe.rorita.kanaschule.ui.keyboard.RomajiKeyboard
import moe.rorita.kanaschule.ui.layout.LocalWindowClass
import moe.rorita.kanaschule.ui.theme.MetricTextStyle

/**
 * Ein Bildschirm, der sein Arrangement verzweigt - keine getrennten
 * Handy- und Desktop-Fassungen. Die divergieren binnen einer Woche.
 */
@Composable
fun DrillScreen(
    state: DrillUiState,
    showKeyboard: Boolean,
    onKey: (Char) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    onSkip: () -> Unit,
    onQuit: () -> Unit,
    onAdvance: () -> Unit,
) {
    val window = LocalWindowClass.current
    val kana = state.kana ?: return

    // Nach einer richtigen Antwort kurz stehen lassen, dann weiter.
    LaunchedEffect(state.answered, state.feedback, state.awaitingContinue) {
        val sat = state.feedback is Feedback.Correct ||
            (state.feedback is Feedback.Introduced && state.feedback.wasCorrect)
        if (sat && !state.awaitingContinue) {
            delay(CORRECT_FLASH_MS)
            onAdvance()
        }
    }

    when {
        window.isExpanded -> ExpandedLayout(
            state = state,
            showKeyboard = showKeyboard,
            onKey = onKey,
            onBackspace = onBackspace,
            onSubmit = onSubmit,
            onSkip = onSkip,
            onQuit = onQuit,
        )

        window.isShort -> ShortLayout(
            state = state,
            showKeyboard = showKeyboard,
            onKey = onKey,
            onBackspace = onBackspace,
            onSubmit = onSubmit,
            onSkip = onSkip,
            onQuit = onQuit,
        )

        else -> StackedLayout(
            state = state,
            showKeyboard = showKeyboard,
            onKey = onKey,
            onBackspace = onBackspace,
            onSubmit = onSubmit,
            onSkip = onSkip,
            onQuit = onQuit,
        )
    }
}

// ------------------------------------------------------------------ Compact

@Composable
private fun StackedLayout(
    state: DrillUiState,
    showKeyboard: Boolean,
    onKey: (Char) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    onSkip: () -> Unit,
    onQuit: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        DrillHeader(state = state, onQuit = onQuit)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            GlyphCard(
                kana = state.kana!!,
                isNew = state.isNewItem,
                // Nachgeben statt feste Hoehe: bei niedrigem Fenster schrumpft
                // die Karte, damit Antwortzeile und Tastatur sichtbar bleiben.
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .heightIn(max = GLYPH_CARD_COMPACT.dp),
            )
            AnswerLine(
                typed = state.typed,
                feedback = state.feedback,
                modifier = Modifier.padding(top = 18.dp),
            )
        }

        SkipRow(state = state, onSkip = onSkip, onSubmit = onSubmit)

        // Nie in einem Scroll-Container: der Glyphbereich schrumpft, die
        // Tastatur bleibt, wo sie ist.
        if (showKeyboard) {
            Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
                RomajiKeyboard(
                    onKey = onKey,
                    onBackspace = onBackspace,
                    onSubmit = onSubmit,
                    submitEnabled = state.typed.isNotEmpty() || state.awaitingContinue,
                )
            }
        }
    }
}

// -------------------------------------------------------------- Landscape

@Composable
private fun ShortLayout(
    state: DrillUiState,
    showKeyboard: Boolean,
    onKey: (Char) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    onSkip: () -> Unit,
    onQuit: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        DrillHeader(state = state, onQuit = onQuit)
        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(horizontal = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                GlyphCard(
                    kana = state.kana!!,
                    isNew = state.isNewItem,
                    scale = 0.35f,
                    minSize = 64.dp,
                    maxSize = 120.dp,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                )
                AnswerLine(
                    typed = state.typed,
                    feedback = state.feedback,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
            if (showKeyboard) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(SHORT_KEYBOARD_FRACTION).fillMaxHeight(),
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        RomajiKeyboard(
                            onKey = onKey,
                            onBackspace = onBackspace,
                            onSubmit = onSubmit,
                            submitEnabled = state.typed.isNotEmpty() || state.awaitingContinue,
                            keyHeight = 42.dp,
                        )
                    }
                }
            } else {
                SkipColumn(state = state, onSkip = onSkip, onSubmit = onSubmit)
            }
        }
    }
}

// ----------------------------------------------------------------- Expanded

@Composable
private fun ExpandedLayout(
    state: DrillUiState,
    showKeyboard: Boolean,
    onKey: (Char) -> Unit,
    onBackspace: () -> Unit,
    onSubmit: () -> Unit,
    onSkip: () -> Unit,
    onQuit: () -> Unit,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Der linke Bereich ist in der Breite gedeckelt und zentriert.
        // Uebriger Platz bleibt bewusst leer, statt Inhalt zu strecken.
        Box(
            modifier = Modifier.weight(1f).fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = EXPANDED_CONTENT_MAX.dp)
                    .fillMaxHeight()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                GlyphCard(
                    kana = state.kana!!,
                    isNew = state.isNewItem,
                    scale = 0.34f,
                    minSize = 140.dp,
                    maxSize = 240.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .heightIn(max = GLYPH_CARD_EXPANDED.dp),
                )
                AnswerLine(
                    typed = state.typed,
                    feedback = state.feedback,
                    modifier = Modifier.padding(top = 24.dp),
                )
                Text(
                    text = if (state.awaitingContinue) {
                        "Enter - weiter"
                    } else {
                        "Romaji tippen · Enter - abgeben · Leertaste - keine Ahnung"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (showKeyboard) {
                    RomajiKeyboard(
                        onKey = onKey,
                        onBackspace = onBackspace,
                        onSubmit = onSubmit,
                        submitEnabled = state.typed.isNotEmpty() || state.awaitingContinue,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
        }

        VerticalDivider()

        Surface(
            modifier = Modifier.width(RAIL_WIDTH.dp).fillMaxHeight(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onQuit) { Text("Beenden") }
                }
                StatsRail(state = state, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

// ------------------------------------------------------------------ Bausteine

@Composable
private fun DrillHeader(state: DrillUiState, onQuit: () -> Unit) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(start = 16.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = "${state.answered}/${state.target}",
                    style = MetricTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${state.accuracy} %",
                    style = MetricTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.streak >= STREAK_SHOW_FROM) {
                    Text(
                        text = "Serie ${state.streak}",
                        style = MetricTextStyle,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            TextButton(onClick = onQuit) { Text("Beenden") }
        }
        HorizontalDivider()
    }
}

@Composable
private fun SkipRow(state: DrillUiState, onSkip: () -> Unit, onSubmit: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        if (state.awaitingContinue) {
            TextButton(onClick = onSubmit) { Text("Weiter") }
        } else {
            TextButton(onClick = onSkip) { Text("Keine Ahnung") }
        }
    }
}

@Composable
private fun SkipColumn(state: DrillUiState, onSkip: () -> Unit, onSubmit: () -> Unit) {
    Column(
        modifier = Modifier.width(160.dp).fillMaxHeight(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.awaitingContinue) {
            TextButton(onClick = onSubmit) { Text("Weiter") }
        } else {
            TextButton(onClick = onSkip) { Text("Keine Ahnung") }
        }
    }
}

private const val CORRECT_FLASH_MS = 520L
private const val GLYPH_CARD_COMPACT = 280
private const val GLYPH_CARD_EXPANDED = 320
private const val EXPANDED_CONTENT_MAX = 720
private const val RAIL_WIDTH = 320
private const val SHORT_KEYBOARD_FRACTION = 0.45f
private const val STREAK_SHOW_FROM = 3
