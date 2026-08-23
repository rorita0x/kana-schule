package moe.rorita.kanaschule.ui.keyboard

import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
actual fun SystemKeyboardSink(
    active: Boolean,
    typed: String,
    onAnswer: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    if (!active) return

    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    BasicTextField(
        // Der Cursor steht immer am Ende: das Feld ist ein Kanal, keine
        // Bearbeitungsfläche.
        value = TextFieldValue(typed, TextRange(typed.length)),
        onValueChange = { onAnswer(it.text) },
        modifier = Modifier
            .size(1.dp)
            .alpha(0f)
            .focusRequester(focusRequester),
        // Autokorrektur und Grossschreibung müssen aus: sonst macht das System
        // aus "tu" ein "to" und aus "shi" ein "Shi".
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Ascii,
            imeAction = ImeAction.Done,
            autoCorrectEnabled = false,
            capitalization = KeyboardCapitalization.None,
        ),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        singleLine = true,
    )

    // Solange die Senke steht, gehört ihr der Fokus. Ohne das erscheint die
    // Tastatur erst, wenn der Nutzer irgendwohin tippt - und tippen kann er
    // nirgendwo, weil das Feld unsichtbar ist.
    LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
        keyboard?.show()
    }
}
