package moe.rorita.kanaschule.ui.keyboard

import androidx.compose.runtime.Composable

/** Der Desktop hat eine echte Tastatur - hier gibt es nichts zu tun. */
@Composable
actual fun SystemKeyboardSink(
    active: Boolean,
    typed: String,
    onAnswer: (String) -> Unit,
    onSubmit: () -> Unit,
) {
}
