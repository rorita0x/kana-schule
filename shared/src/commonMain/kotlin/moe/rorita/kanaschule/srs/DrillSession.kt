package moe.rorita.kanaschule.srs

import moe.rorita.kanaschule.kana.Kana
import moe.rorita.kanaschule.kana.KanaId
import moe.rorita.kanaschule.kana.KanaTable
import moe.rorita.kanaschule.kana.Romaji
import moe.rorita.kanaschule.kana.Verdict
import moe.rorita.kanaschule.store.Outcome

/** Was eine einzelne Antwort bewirkt hat. */
data class AnswerOutcome(
    val kana: Kana,
    val verdict: Verdict,
    val outcome: Outcome,
    val boxBefore: Int,
    val boxAfter: Int,
    val confusedWith: KanaId?,
    /** Erste Antwort nach der Vorstellungskarte: zählt nicht als Prüfung. */
    val introduction: Boolean,
    /** Der Hinweis auf die Hepburn-Schreibweise, falls einer fällig ist. */
    val hint: String?,
    /** Bei Tippfehlern und Fehlern: die kanonische Antwort zum Anzeigen. */
    val expected: String,
)

/**
 * Der Ablauf einer Session: welches Zeichen kommt, was eine Antwort bewirkt,
 * wann Schluss ist.
 *
 * Rein und ohne I/O, und ohne jeden Zufall: die Uhr kommt von aussen, die
 * Streuung der Intervalle hängt am Zeitpunkt der Antwort. Dieselbe Session
 * lässt sich damit im Test Zug für Zug nachspielen.
 */
class DrillSession(
    plan: SessionPlan,
    initialStates: Map<KanaId, ItemState>,
    private val targetAnswers: Int = SessionBuilder.TARGET_SIZE,
    /** Nachspielzeit, um offene Fehler noch festzusetzen. */
    private val overtimeLimit: Int = DEFAULT_OVERTIME,
    /** Nur Hepburn als volle Antwort zählen. */
    private val strictHepburn: Boolean = false,
) {

    private val queue = SessionQueue(plan.items)
    private val mutableStates = initialStates.toMutableMap()
    private val touched = LinkedHashSet<KanaId>()

    /** Alles, was eine Frage verbraucht hat. */
    val answered: Int get() = asked + introduced

    val mode: SessionMode = plan.mode
    val plannedSize: Int = plan.items.size
    val newItems: Set<KanaId> = plan.newItems.toSet()

    /** Wie viele Fragen der Runde wirklich fällig waren. */
    val dueSize: Int = plan.dueItems.size

    /** Der Rest, abzüglich der Erstkontakte: Auffrischung ohne Boxgewinn. */
    val refreshSize: Int = plan.items.size - plan.dueItems.size - plan.newItems.size

    var current: Kana? = null
        private set

    var asked: Int = 0
        private set

    /** Erstkontakte. Zählen für die Sessionlänge, nicht für die Quote. */
    var introduced: Int = 0
        private set

    var correct: Int = 0
        private set

    var promoted: Int = 0
        private set

    var demoted: Int = 0
        private set

    /** Zeichen, deren Zustand sich geändert hat - nur die müssen gespeichert werden. */
    val changedStates: Map<KanaId, ItemState>
        get() = touched.associateWith { mutableStates.getValue(it) }

    val states: Map<KanaId, ItemState> get() = mutableStates

    val latencies: MutableList<Int> = ArrayList()

    private var typoRetries = 0
    private var overtimeUsed = 0

    /**
     * Zeichen, die nach einem Fehler noch nicht wieder sassen. Solange hier
     * etwas offen ist, endet die Session nicht freiwillig.
     */
    private val unsettled = LinkedHashSet<KanaId>()

    fun start(): Kana? = advance()

    /**
     * Bewertet die Eingabe, schreibt den Lernstand fort und reiht das Zeichen
     * bei Bedarf wieder ein. Das nächste Zeichen holt danach [advance].
     */
    fun submit(typed: String, latencyMs: Int, nowMs: Long): AnswerOutcome {
        val kana = current ?: error("Keine offene Frage")
        val before = stateOf(kana.id)
        val verdict = Romaji.evaluate(kana, typed, strictHepburn)

        if (verdict is Verdict.Typo && typoRetries < MAX_TYPO_RETRIES) {
            typoRetries++
            queue.requeueTypo(kana.id)
            return AnswerOutcome(
                kana = kana,
                verdict = verdict,
                outcome = Outcome.TYPO,
                boxBefore = before.box,
                boxAfter = before.box,
                confusedWith = null,
                hint = null,
                expected = kana.canonical,
                introduction = false,
            )
        }

        // Nach zwei Tippfehlern ist es kein Tippfehler mehr.
        val effective = if (verdict is Verdict.Typo) Verdict.Wrong else verdict

        if (isIntroduction(kana.id, before)) {
            return introduce(kana, effective, before, nowMs)
        }

        val after = Scheduler.apply(before, effective, latencyMs, nowMs, mode)
        put(kana.id, after)

        asked++
        latencies += latencyMs
        typoRetries = 0
        when {
            after.box > before.box -> promoted++
            after.box < before.box -> demoted++
        }

        when (effective) {
            is Verdict.Correct -> {
                correct++
                if (after.relearning) unsettled += kana.id else unsettled -= kana.id
            }

            is Verdict.Confused -> {
                unsettled += kana.id
                queue.requeueAfterMiss(kana.id)
                demotePartner(effective.with, kana.id, nowMs)
                queue.requeueConfusionPair(kana.id, effective.with)
            }

            Verdict.Wrong, Verdict.Skipped -> {
                unsettled += kana.id
                queue.requeueAfterMiss(kana.id)
            }

            is Verdict.Typo -> Unit
        }

        return AnswerOutcome(
            kana = kana,
            verdict = effective,
            outcome = effective.toOutcome(),
            boxBefore = before.box,
            boxAfter = after.box,
            confusedWith = (effective as? Verdict.Confused)?.with,
            hint = (effective as? Verdict.Correct)?.hint,
            expected = kana.canonical,
            introduction = false,
        )
    }

    private fun isIntroduction(id: KanaId, before: ItemState): Boolean =
        id in newItems && before.reps == 0

    /**
     * Der Erstkontakt landet in Box 1 und lässt Trefferquote, Ringpuffer und
     * Fehlerzähler unberührt. Wer ein Zeichen abschreibt, das gerade noch auf
     * dem Bildschirm stand, hat es nicht gewusst - und hat es auch nicht
     * falsch gewusst.
     */
    private fun introduce(
        kana: Kana,
        verdict: Verdict,
        before: ItemState,
        nowMs: Long,
    ): AnswerOutcome {
        put(
            kana.id,
            before.copy(
                box = 1,
                dueAtMs = nowMs + Boxes.intervalMs(1, nowMs),
                firstSeenMs = before.firstSeenMs ?: nowMs,
                lastSeenMs = nowMs,
                reps = 1,
            ),
        )
        introduced++
        typoRetries = 0

        return AnswerOutcome(
            kana = kana,
            verdict = verdict,
            outcome = Outcome.INTRODUCED,
            boxBefore = before.box,
            boxAfter = 1,
            confusedWith = null,
            hint = (verdict as? Verdict.Correct)?.hint,
            expected = kana.canonical,
            introduction = true,
        )
    }

    /** Nächstes Zeichen, oder null wenn die Session zu Ende ist. */
    fun advance(): Kana? {
        if (answered >= targetAnswers) {
            if (unsettled.isEmpty() || overtimeUsed >= overtimeLimit) {
                current = null
                return null
            }
            overtimeUsed++
        }
        val next = queue.next()
        current = next?.let(KanaTable::require)
        return current
    }

    val finished: Boolean get() = current == null && answered > 0

    val remaining: Int get() = (targetAnswers - answered).coerceAtLeast(0)

    val medianLatencyMs: Int
        get() = if (latencies.isEmpty()) 0 else latencies.sorted()[latencies.size / 2]

    /** Die schwächsten Zeichen dieser Session, für die Seitenleiste. */
    fun weakest(limit: Int): List<Pair<KanaId, ItemState>> =
        touched.map { it to mutableStates.getValue(it) }
            .filter { it.second.reps > 0 }
            .sortedWith(compareBy({ it.second.accuracy20 }, { -it.second.lapses }))
            .take(limit)

    private fun demotePartner(partner: KanaId, target: KanaId, nowMs: Long) {
        put(partner, Scheduler.applyConfusionPartner(stateOf(partner), target, nowMs))
    }

    private fun stateOf(id: KanaId): ItemState = mutableStates[id] ?: ItemState()

    private fun put(id: KanaId, state: ItemState) {
        mutableStates[id] = state
        touched += id
    }

    private fun Verdict.toOutcome(): Outcome = when (this) {
        is Verdict.Correct -> Outcome.CORRECT
        is Verdict.Typo -> Outcome.TYPO
        is Verdict.Confused -> Outcome.CONFUSED
        Verdict.Wrong -> Outcome.WRONG
        Verdict.Skipped -> Outcome.SKIPPED
    }

    companion object {
        const val MAX_TYPO_RETRIES = 2
        const val DEFAULT_OVERTIME = 8
    }
}
