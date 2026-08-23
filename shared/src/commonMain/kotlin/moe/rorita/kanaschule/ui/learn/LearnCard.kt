package moe.rorita.kanaschule.ui.learn

import androidx.compose.runtime.Immutable
import moe.rorita.kanaschule.kana.Kana
import moe.rorita.kanaschule.kana.PronunciationHint

/**
 * Eine Vorstellungskarte fuer ein Zeichen.
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
    val hasPrevious: Boolean get() = index > 0
}

/** Der Lernmodus als Ganzes: Karte plus die Schalter darum. */
@Immutable
data class LearnState(
    val card: LearnCard,
    /** true: alle Zeichen durchblaettern, nicht nur die noch nicht gelernten. */
    val showAll: Boolean,
    val muted: Boolean,
    /**
     * true, wenn der Lernmodus aus dem Hauptmenue kommt. Sonst ist es die
     * Vorstellung der neuen Zeichen vor einer Uebungsrunde, und danach geht
     * es direkt ins Abfragen.
     */
    val standalone: Boolean,
)
