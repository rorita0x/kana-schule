package moe.rorita.kanaschule.ui.keyboard

import androidx.compose.runtime.Composable

/**
 * Unsichtbare Eingabesenke für die Tastatur des Systems.
 *
 * Die Antwort zeichnet die App selbst (siehe AnswerLine), ein Textfeld braucht
 * sie dafür nicht. Um die Tastatur des Systems überhaupt hervorzuholen,
 * verlangt Android aber ein fokussiertes Eingabefeld - also gibt es hier eines
 * ohne Ausdehnung, dessen Inhalt in den Zustand gespiegelt wird. Sichtbar wird
 * davon nichts.
 *
 * Auf dem Desktop ist das ein Nichts: dort tippt man auf der echten Tastatur,
 * und die läuft über onPreviewKeyEvent an der Wurzel.
 *
 * [typed] steuert das Feld vollständig, damit die Filterung im ViewModel das
 * letzte Wort hat und nicht die Autokorrektur.
 */
@Composable
expect fun SystemKeyboardSink(
    active: Boolean,
    typed: String,
    onAnswer: (String) -> Unit,
    onSubmit: () -> Unit,
)
