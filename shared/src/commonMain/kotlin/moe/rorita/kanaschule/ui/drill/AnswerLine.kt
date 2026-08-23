package moe.rorita.kanaschule.ui.drill

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import moe.rorita.kanaschule.ui.theme.AnswerTextStyle
import moe.rorita.kanaschule.ui.theme.LocalFeedbackColors

/**
 * Die getippte Antwort mit blinkendem Cursor - kein TextField.
 *
 * Damit entfällt die gesamte Klasse von IME-Problemen: kein Systemkeyboard,
 * das über der eigenen Tastatur aufgeht, keine Autokorrektur, die „tu“ zu
 * „to“ macht, und keine Vorschlagsleiste, die die Zeitmessung verfälscht.
 */
@Composable
fun AnswerLine(
    typed: String,
    feedback: Feedback?,
    modifier: Modifier = Modifier,
) {
    val colors = LocalFeedbackColors.current
    val tint = when (feedback) {
        is Feedback.Correct -> colors.correct
        is Feedback.Introduced -> colors.neutral
        is Feedback.Wrong, is Feedback.Skipped -> colors.wrong
        is Feedback.Typo -> colors.neutral
        null -> MaterialTheme.colorScheme.onBackground
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.height(52.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = typed,
                style = AnswerTextStyle,
                color = tint,
            )
            Caret(visible = feedback == null)
        }

        Box(
            modifier = Modifier
                .width(220.dp)
                .height(2.dp)
                .background(
                    color = if (feedback == null) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        tint
                    },
                    shape = RoundedCornerShape(1.dp),
                ),
        )

        // Fester Platz für die Rückmeldung: sonst springt die Glyphenkarte,
        // sobald „richtig“ oder „falsch“ erscheint.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = FEEDBACK_SLOT_HEIGHT.dp)
                .padding(top = 10.dp, start = 16.dp, end = 16.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            FeedbackText(feedback)
        }
    }
}

@Composable
private fun FeedbackText(feedback: Feedback?) {
    val colors = LocalFeedbackColors.current
    when (feedback) {
        null -> Unit

        is Feedback.Correct -> Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = "richtig",
                style = MaterialTheme.typography.titleMedium,
                color = colors.correct,
            )
            feedback.hint?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }

        is Feedback.Introduced -> Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = feedback.expected,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Erstkontakt - zählt nicht in die Quote",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        is Feedback.Typo -> Text(
            text = "Fast - nochmal tippen.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.neutral,
            textAlign = TextAlign.Center,
        )

        is Feedback.Skipped -> Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = feedback.expected,
                style = MaterialTheme.typography.titleLarge,
                color = colors.wrong,
            )
            Text(
                text = "Weiter mit Enter",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        is Feedback.Wrong -> Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = feedback.typed.ifEmpty { "-" },
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.wrong,
                )
                Text(
                    text = "→",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = feedback.expected,
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.correct,
                )
            }
            feedback.confusedWith?.let { other ->
                Text(
                    text = "„${feedback.typed}“ ist ${other.glyph}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            (feedback.discriminator ?: feedback.explanation)?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun Caret(visible: Boolean) {
    var on by remember { mutableStateOf(true) }
    LaunchedEffect(visible) {
        if (!visible) {
            on = false
            return@LaunchedEffect
        }
        while (true) {
            on = true
            delay(BLINK_ON_MS)
            on = false
            delay(BLINK_OFF_MS)
        }
    }
    val alpha by animateFloatAsState(if (on) 1f else 0f)

    Box(
        modifier = Modifier
            .padding(start = 2.dp)
            .size(width = 3.dp, height = 34.dp)
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                shape = RoundedCornerShape(2.dp),
            ),
    )
}

private const val FEEDBACK_SLOT_HEIGHT = 78
private const val BLINK_ON_MS = 520L
private const val BLINK_OFF_MS = 420L
