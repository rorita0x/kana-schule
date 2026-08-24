package moe.rorita.kanaschule.srs

/**
 * Neun Leitner-Boxen mit festen Intervallen. Bewusst kein SM-2 und kein FSRS:
 * das Deck ist bei 230 Items geschlossen, das Ziel ist Vollbeherrschung in
 * wenigen Wochen, und ein Intervall, das man dem Lernenden hinschreiben kann
 * („nächste Wiederholung in 3 Tagen“), ist Voraussetzung für eine
 * glaubwürdige Prüfungsreif-Anzeige.
 *
 * Box 0 ist reine Session-Arbeit und wird nie hinausgeplant.
 */
object Boxes {

    const val MAX = 8

    private val minutes = intArrayOf(
        0,               // 0: nur innerhalb der Session
        10,              // 1: 10 Minuten
        60,              // 2: 1 Stunde
        8 * 60,          // 3: 8 Stunden
        24 * 60,         // 4: 1 Tag
        3 * 24 * 60,     // 5: 3 Tage
        7 * 24 * 60,     // 6: 1 Woche
        21 * 24 * 60,    // 7: 3 Wochen
        60 * 24 * 60,    // 8: 2 Monate
    )

    fun baseIntervalMs(box: Int): Long = minutes[box.coerceIn(0, MAX)] * 60_000L

    /**
     * Mit +/-10 Prozent Streuung, damit keine Wiederholungslawinen entstehen.
     *
     * Die Streuung kommt aus [seedMs], dem Zeitpunkt der Antwort, und nicht aus
     * einem Generator: dieselbe Antwortfolge muss dieselben Fälligkeiten
     * ergeben. Sonst wäre keine Aussage über einen Lernverlauf im Test
     * wiederholbar, und zwei Geräte könnten sich später nicht auf denselben
     * Stand einigen.
     */
    fun intervalMs(box: Int, seedMs: Long): Long {
        val base = baseIntervalMs(box)
        if (base == 0L) return 0L
        return (base * (0.90 + 0.20 * spread(seedMs))).toLong()
    }

    /**
     * Gut verteilter Wert in [0, 1) aus einem Zeitstempel. Benachbarte
     * Millisekunden müssen weit auseinanderliegende Ergebnisse geben, sonst
     * bekommen alle Antworten einer Runde fast dieselbe Streuung.
     */
    private fun spread(seedMs: Long): Double {
        var x = seedMs * 0x2545F4914F6CDD1DL + 0x9E3779B9L
        x = x xor (x ushr 33)
        x *= 0x5851F42D4C957F2DL
        x = x xor (x ushr 29)
        return (x ushr 11).toDouble() / (1L shl 53).toDouble()
    }
}
