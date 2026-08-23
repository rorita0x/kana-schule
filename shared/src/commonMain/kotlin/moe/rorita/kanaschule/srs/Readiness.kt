package moe.rorita.kanaschule.srs

import kotlin.math.max
import kotlin.math.roundToInt
import moe.rorita.kanaschule.kana.Kana
import moe.rorita.kanaschule.kana.KanaId

enum class MasteryTier { LOCKED, NEW, BUILDING, SAFE, MASTERED }

/**
 * Die Pruefungsreif-Zahl. Sie ist ein Versprechen an den Lernenden, deshalb
 * gehen Antwortzeit und Trefferquote in die höchste Stufe ein: Kana in acht
 * Sekunden pro Zeichen zu lesen ist kein Lesen.
 */
object Readiness {

    /** Antwortzeit, unter der ein Zeichen als beherrscht gelten kann. */
    const val MASTERED_LATENCY_MS = 4000
    const val MASTERED_ACCURACY = 0.90
    const val MASTERED_STREAK = 3
    private const val MASTERED_BOX = 6
    private const val SAFE_BOX = 4
    private const val BUILDING_BOX = 2

    /**
     * Ein beherrschtes Zeichen faellt nach langer Pause nicht unter diesen
     * Wert. Der Totaleinbruch nach zwei Wochen Urlaub ist das
     * demotivierendste Verhalten, das eine Wiederholungs-App zeigen kann.
     */
    const val MASTERY_FLOOR = 0.70

    private const val STALE_FACTOR = 0.70

    /** Stufe fuer die Anzeige. LOCKED entscheidet die Unlock-Leiter, nicht diese Funktion. */
    fun tier(state: ItemState): MasteryTier = when {
        !state.seen -> MasteryTier.NEW
        isMastered(state) -> MasteryTier.MASTERED
        state.box >= SAFE_BOX -> MasteryTier.SAFE
        state.box >= BUILDING_BOX -> MasteryTier.BUILDING
        else -> MasteryTier.NEW
    }

    fun of(state: ItemState, nowMs: Long): Double {
        val base = when {
            isMastered(state) -> 1.0
            state.box >= MASTERED_BOX -> 0.85
            state.box >= SAFE_BOX -> 0.60
            state.box >= BUILDING_BOX -> 0.30
            else -> 0.0
        }
        if (base == 0.0) return 0.0

        val overdue = state.dueAtMs != 0L &&
            nowMs > state.dueAtMs + 2 * Boxes.baseIntervalMs(state.box)
        val value = if (overdue) base * STALE_FACTOR else base

        return if (base == 1.0) max(value, MASTERY_FLOOR) else value
    }

    /**
     * Der Nenner ist der volle Umfang, gesperrte Zeichen eingeschlossen. Man
     * kann nicht zu 80 Prozent hiragana-pruefungsreif sein, wenn drei Zeilen
     * freigeschaltet sind.
     */
    fun percent(scope: List<Kana>, states: Map<KanaId, ItemState>, nowMs: Long): Int {
        if (scope.isEmpty()) return 0
        val sum = scope.sumOf { of(states[it.id] ?: ItemState(), nowMs) }
        return (100.0 * sum / scope.size).roundToInt()
    }

    /** Nur die freigeschalteten Zeichen - als graue Nebenangabe. */
    fun percentOfUnlocked(
        scope: List<Kana>,
        states: Map<KanaId, ItemState>,
        unlocked: Set<KanaId>,
        nowMs: Long,
    ): Int = percent(scope.filter { it.id in unlocked }, states, nowMs)

    private fun isMastered(state: ItemState): Boolean =
        state.box >= MASTERED_BOX &&
            state.streak >= MASTERED_STREAK &&
            state.accuracy20 >= MASTERED_ACCURACY &&
            state.latencyEwmaMs in 1..MASTERED_LATENCY_MS
}
