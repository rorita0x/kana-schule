package moe.rorita.kanaschule.srs

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random
import moe.rorita.kanaschule.kana.KanaId
import moe.rorita.kanaschule.kana.Verdict

/**
 * Reine Zustandsuebergaenge. Kein I/O, keine Uhr, kein Zufall ausser dem, was
 * hereingegeben wird - damit ist die gesamte Lernlogik ohne Compose und ohne
 * Geraet testbar.
 */
object Scheduler {

    /** Schneller als das gilt als Wiedererkennen. */
    const val FAST_MS = 3000

    /** Langsamer als das ist Rekonstruktion, kein Wiedererkennen. */
    const val SLOW_MS = 6000

    const val LEECH_LAPSES = 6
    const val LEECH_ACCURACY = 0.70

    /** Wie viele Boxen ein Fehler kostet. */
    const val DEMOTION = 3

    /** Ab dieser Box schuetzt eine langsame Antwort vor dem Aufstieg. */
    private const val SLOW_GUARD_BOX = 4

    private const val EWMA_ALPHA = 0.3

    /** Obergrenze fuer den Partner einer Verwechslung. */
    private const val CONFUSION_PARTNER_BOX = 4

    fun apply(
        state: ItemState,
        verdict: Verdict,
        latencyMs: Int,
        nowMs: Long,
        mode: SessionMode,
        random: Random,
    ): ItemState = when (verdict) {
        // Tippfehler sind neutral: kein Boxwechsel, kein Streak-Verlust, und die
        // Antwortzeit des Wiederholungsversuchs verfaelscht den Mittelwert nicht.
        is Verdict.Typo -> state

        is Verdict.Correct -> correct(state, latencyMs, nowMs, mode, random)

        is Verdict.Confused -> lapse(state, nowMs, mode, confusedWith = verdict.with)

        Verdict.Wrong, Verdict.Skipped -> lapse(state, nowMs, mode, confusedWith = null)
    }

    private fun correct(
        state: ItemState,
        latencyMs: Int,
        nowMs: Long,
        mode: SessionMode,
        random: Random,
    ): ItemState {
        val slow = latencyMs > SLOW_MS
        val holdBack = slow && state.box >= SLOW_GUARD_BOX
        val box = if (mode == SessionMode.SPEED || holdBack) {
            state.box
        } else {
            min(state.box + 1, Boxes.MAX)
        }
        val streak = state.streak + 1

        return state.copy(
            box = box,
            dueAtMs = if (mode == SessionMode.SPEED) state.dueAtMs else nowMs + Boxes.intervalMs(box, random),
            firstSeenMs = state.firstSeenMs ?: nowMs,
            lastSeenMs = nowMs,
            reps = state.reps + 1,
            streak = streak,
            fastStreak = if (latencyMs <= FAST_MS) state.fastStreak + 1 else 0,
            latencyEwmaMs = blend(state.latencyEwmaMs, latencyMs),
            recent = push(state.recent, correct = true),
            recentCount = min(state.recentCount + 1, ItemState.RECENT_BITS),
            relearning = state.relearning && streak < RELEARN_STREAK,
        )
    }

    /**
     * Bewusst kein Reset auf Box 0: ein Totalabsturz nach einem einzigen
     * Ausrutscher drillt Bekanntes neu und ist der Hauptgrund, aus dem Leute
     * Wiederholungs-Apps aufgeben. Drei Boxen sind ein spuerbarer, aber
     * ueberlebbarer Rueckschlag.
     *
     * Im Speed-Drill bleibt die Box unberuehrt, sonst wuerde man den Modus
     * meiden, statt ihn zu nutzen.
     */
    private fun lapse(
        state: ItemState,
        nowMs: Long,
        mode: SessionMode,
        confusedWith: KanaId?,
    ): ItemState {
        val speed = mode == SessionMode.SPEED
        val box = if (speed) state.box else max(1, state.box - DEMOTION)
        val lapses = if (speed) state.lapses else state.lapses + 1
        val recent = push(state.recent, correct = false)
        val recentCount = min(state.recentCount + 1, ItemState.RECENT_BITS)
        val accuracy = recent.countOneBits() / recentCount.toDouble()

        return state.copy(
            box = box,
            dueAtMs = if (speed) state.dueAtMs else nowMs,
            firstSeenMs = state.firstSeenMs ?: nowMs,
            lastSeenMs = nowMs,
            reps = state.reps + 1,
            lapses = lapses,
            streak = 0,
            fastStreak = 0,
            recent = recent,
            recentCount = recentCount,
            leech = lapses >= LEECH_LAPSES && accuracy < LEECH_ACCURACY,
            relearning = !speed,
            confusions = confusedWith?.let { state.confusions.increment(it.v) } ?: state.confusions,
        )
    }

    /**
     * Wer auf シ mit „tsu“ antwortet, hat nicht nur シ unsicher, sondern auch
     * ツ - beides ist derselbe undifferenzierte Klumpen. Deshalb wird der
     * Partner mitgezogen, aber nur bis Box 4 gedeckelt statt um drei Boxen
     * heruntergestuft.
     */
    fun applyConfusionPartner(state: ItemState, target: KanaId, nowMs: Long): ItemState =
        state.copy(
            box = min(state.box, CONFUSION_PARTNER_BOX),
            dueAtMs = nowMs,
            streak = 0,
            confusions = state.confusions.increment(target.v),
        )

    /** Am Sessionende: alte Verwechslungen verblassen. */
    fun decayConfusions(state: ItemState): ItemState =
        state.copy(confusions = state.confusions.mapValues { it.value - 1 }.filterValues { it > 0 })

    private fun Map<String, Int>.increment(key: String): Map<String, Int> =
        toMutableMap().apply { this[key] = (this[key] ?: 0) + 1 }

    private fun push(recent: Int, correct: Boolean): Int =
        ((recent shl 1) or if (correct) 1 else 0) and ItemState.RECENT_MASK

    private fun blend(current: Int, latencyMs: Int): Int =
        if (current == 0) latencyMs else (current * (1 - EWMA_ALPHA) + latencyMs * EWMA_ALPHA).roundToInt()

    /** So viele richtige Antworten in Folge loesen den Relearning-Zustand. */
    const val RELEARN_STREAK = 2
}
