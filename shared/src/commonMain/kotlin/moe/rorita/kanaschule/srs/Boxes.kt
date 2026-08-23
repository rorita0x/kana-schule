package moe.rorita.kanaschule.srs

import kotlin.random.Random

/**
 * Neun Leitner-Boxen mit festen Intervallen. Bewusst kein SM-2 und kein FSRS:
 * das Deck ist bei 230 Items geschlossen, das Ziel ist Vollbeherrschung in
 * wenigen Wochen, und ein Intervall, das man dem Lernenden hinschreiben kann
 * („naechste Wiederholung in 3 Tagen“), ist Voraussetzung fuer eine
 * glaubwuerdige Pruefungsreif-Anzeige.
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

    /** Mit +/-10 Prozent Streuung, damit keine Wiederholungslawinen entstehen. */
    fun intervalMs(box: Int, random: Random): Long {
        val base = baseIntervalMs(box)
        if (base == 0L) return 0L
        return (base * (0.90 + random.nextDouble() * 0.20)).toLong()
    }
}
