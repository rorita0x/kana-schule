package moe.rorita.kanaschule.ui.learn

import androidx.compose.runtime.Immutable
import moe.rorita.kanaschule.kana.Kana
import moe.rorita.kanaschule.kana.PronunciationHint

/**
 * Eine Vorstellungskarte fuer ein neues Zeichen.
 *
 * Beide Schriften stehen zusammen auf der Karte, auch wenn erst eine von
 * beiden abgefragt wird: し und シ sind dieselbe Lesung, und die Verbindung
 * einmal gesehen zu haben kostet nichts und spart spaeter Arbeit.
 */
@Immutable
data class LearnCard(
    val hiragana: Kana?,
    val katakana: Kana?,
    val romaji: String,
    val hint: PronunciationHint,
    val audioName: String?,
    val noteDe: String?,
    val index: Int,
    val total: Int,
) {
    val isLast: Boolean get() = index == total - 1
}
