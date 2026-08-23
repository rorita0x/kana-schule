package moe.rorita.kanaschule.kana

/**
 * Wie eine getippte Antwort bewertet wird.
 *
 * Die Reihenfolge der Pruefung in [evaluate] ist tragend: eine Verwechslung
 * muss vor einem Tippfehler erkannt werden, sonst wird „hi“ fuer し als
 * Tippfehler verbucht statt als das, was es ist.
 */
sealed interface Verdict {
    /** Richtig. [hint] ist gesetzt, wenn die Antwort nicht Hepburn war. */
    data class Correct(val system: RomajiSystem, val hint: String?) : Verdict

    /** Neutral: kein Boxwechsel, kein Streak-Verlust, sofort nochmal fragen. */
    data class Typo(val nearest: String) : Verdict

    /** Die Antwort benennt ein anderes Zeichen. */
    data class Confused(val with: KanaId, val kind: ConfusionKind) : Verdict

    data object Wrong : Verdict

    /** Leere Eingabe: „keine Ahnung“, nicht „falsche Ahnung“. */
    data object Skipped : Verdict
}

enum class ConfusionKind {
    /** Optisch aehnliches Zeichen: シ mit „tsu“ beantwortet. */
    VISUAL,

    /** Stimmhaftigkeit vergessen oder erfunden: が mit „ka“. */
    DAKUTEN,

    /** Kleines ya/yu/yo uebersehen: きゃ mit „ki“. */
    YOON_BASE,

    /** Richtiger Konsonant, falscher Vokal: か mit „ko“. */
    VOWEL_ROW,

    /** Richtiger Vokal, falscher Konsonant: か mit „sa“. */
    CONSONANT_COL,

    UNRELATED,
}

object Romaji {

    private const val TYPO_MIN_LENGTH = 3

    /**
     * Vereinheitlicht die Eingabe: NFKC, Kleinschreibung, und alles ausser
     * Buchstaben, Apostroph und Bindestrich fliegt raus. Damit wird auch
     * „ki tte“ zu „kitte“.
     *
     * Makron-Vokale werden absichtlich nicht auf den Grundvokal reduziert:
     * sonst waeren sākuru und sakuru nicht mehr zu unterscheiden.
     */
    fun normalize(raw: String): String =
        nfkc(raw).lowercase().filter { it.isLetter() || it == '\'' || it == '-' }

    fun evaluate(target: Kana, raw: String): Verdict {
        val input = normalize(raw)
        if (input.isEmpty()) return Verdict.Skipped

        target.systemOf(input)?.let { system ->
            return Verdict.Correct(system, hintFor(target, system, input))
        }

        confusedWith(target, input)?.let { other ->
            return Verdict.Confused(other.id, kindOf(target, other))
        }

        target.accepted
            .filter { it.length >= TYPO_MIN_LENGTH && levenshtein(input, it) <= 1 }
            .minByOrNull { it.length }
            ?.let { return Verdict.Typo(it) }

        return Verdict.Wrong
    }

    /**
     * Hinweis auf die Hepburn-Schreibweise. Volle Punkte gibt es trotzdem: wer
     * „si“ tippt, kennt し. Aber wer sechs Wochen „sya“ tippt, ohne dass es
     * jemand sagt, lernt nie, dass „sha“ existiert.
     */
    private fun hintFor(target: Kana, system: RomajiSystem, input: String): String? =
        when (system) {
            RomajiSystem.HEPBURN -> null
            RomajiSystem.KUNREI ->
                "Hepburn schreibt „${target.canonical}“ - „$input“ ist die Kunrei-Form."
            RomajiSystem.WAPURO ->
                "Hepburn schreibt „${target.canonical}“ - „$input“ ist die Eingabe-Schreibweise."
            RomajiSystem.TOLERATED ->
                "Hepburn schreibt „${target.canonical}“ - „$input“ gilt als ungewoehnlich."
        }

    /**
     * Das Zeichen, das die Eingabe tatsaechlich benennt. Zeichen derselben
     * Schrift haben Vorrang, optische Nachbarn davon noch einmal.
     */
    private fun confusedWith(target: Kana, input: String): Kana? =
        KanaTable.reverseIndex[input]
            ?.asSequence()
            ?.filter { it != target.id }
            ?.map { KanaTable.require(it) }
            ?.minByOrNull { rank(target, it) }

    private fun rank(target: Kana, other: Kana): Int = when {
        other.script != target.script -> 2
        other.id in target.visualNeighbors -> 0
        else -> 1
    }

    private fun kindOf(target: Kana, other: Kana): ConfusionKind = when {
        other.id in target.visualNeighbors -> ConfusionKind.VISUAL

        other.id == target.baseId ->
            if (target.kClass == KanaClass.YOON) ConfusionKind.YOON_BASE else ConfusionKind.DAKUTEN

        target.id == other.baseId ->
            if (other.kClass == KanaClass.YOON) ConfusionKind.YOON_BASE else ConfusionKind.DAKUTEN

        target.baseId != null && target.baseId == other.baseId -> ConfusionKind.DAKUTEN

        other.script == target.script && other.row == target.row -> ConfusionKind.VOWEL_ROW

        other.script == target.script && vowel(other) != null && vowel(other) == vowel(target) ->
            ConfusionKind.CONSONANT_COL

        else -> ConfusionKind.UNRELATED
    }

    private fun vowel(kana: Kana): Char? = kana.canonical.lastOrNull { it in "aiueo" }

    internal fun levenshtein(a: String, b: String): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)

        for (i in 1..a.length) {
            current[0] = i
            for (j in 1..b.length) {
                val substitution = previous[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(current[j - 1] + 1, previous[j] + 1, substitution)
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[b.length]
    }
}
