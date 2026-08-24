package moe.rorita.kanaschule.srs

import kotlin.random.Random
import moe.rorita.kanaschule.kana.KanaId
import moe.rorita.kanaschule.kana.KanaTable
import moe.rorita.kanaschule.kana.Row
import moe.rorita.kanaschule.kana.Script
import moe.rorita.kanaschule.store.AppState
import moe.rorita.kanaschule.store.UnlockState
import moe.rorita.kanaschule.ui.drill.KanaViewModel

/**
 * Ein simulierter Lernender über mehrere Tage.
 *
 * Einzeltests prüfen Zustandsübergänge; die interessanten Eigenschaften des
 * Systems entstehen aber erst aus dem Zusammenspiel von Zusammenstellung,
 * Reihenfolge, Boxen und Freischaltleiter über Dutzende Runden - „kommt ein
 * schwaches Zeichen öfter dran als ein sitzendes“ und „wie lange dauert die
 * Leiter“ sind in keinem einzelnen Übergang zu sehen.
 *
 * Gefahren wird mit einem echten [AppState] und dessen eigenen Hilfsfunktionen,
 * damit Tagesbudget und Freischaltung dieselbe Rechnung durchlaufen wie in der
 * App und nicht eine zweite, die davon abdriftet.
 *
 * Deterministisch: gleicher [seed] ergibt denselben Verlauf, damit Aussagen
 * über Häufigkeiten und Tempo überhaupt prüfbar sind.
 */
class LearningSimulation(
    /**
     * Fest offene Gruppen. `null` heisst: der Leiter folgen, so wie die App es
     * tut - beginnend bei der ersten Gruppe, höchstens eine pro Tag.
     */
    private val unlockedGroups: Set<String>? = null,
    /** Trefferwahrscheinlichkeit je Zeile - so gut kann der Simulierte das. */
    private val skill: Map<Row, Double>,
    /** Uhrzeiten der Runden je Tag. */
    private val sessionHours: List<Long> = listOf(9L, 13L, 20L),
    private val targetSize: Int = SessionBuilder.TARGET_SIZE,
    private val defaultSkill: Double = 0.8,
    private val latencyMs: Int = 1500,
    private val seed: Int = 7,
) {

    /** Eine gelaufene Runde. */
    data class Round(
        val day: Int,
        val hour: Long,
        val planned: Int,
        val answered: Int,
        /** Offene Gruppen nach dieser Runde. */
        val unlockedGroups: Int,
        /** Die Zeichen in der Reihenfolge, in der sie gefragt wurden. */
        val sequence: List<KanaId>,
    ) {
        val empty: Boolean get() = planned == 0
    }

    data class Result(
        val app: AppState,
        val rounds: List<Round>,
    ) {
        val states: Map<KanaId, ItemState> get() = app.states

        /** Wie oft ein Zeichen insgesamt gefragt wurde. */
        val asksPerItem: Map<KanaId, Int> =
            rounds.flatMap { it.sequence }.groupingBy { it }.eachCount()

        val asksPerRow: Map<Row, Int> =
            rounds.flatMap { it.sequence }
                .groupingBy { KanaTable.require(it).row }
                .eachCount()

        fun asks(row: Row): Int = asksPerRow[row] ?: 0

        fun state(glyph: String): ItemState {
            val kana = KanaTable.singles.first { it.glyph == glyph }
            return states[kana.id] ?: ItemState()
        }

        fun boxOf(glyph: String): Int = state(glyph).box

        /** Höchste erreichte Box unter den Zeichen einer Zeile. */
        fun maxBox(row: Row, script: Script = Script.HIRAGANA): Int =
            KanaTable.of(script, row).maxOf { (states[it.id] ?: ItemState()).box }

        /** Erster Tag, an dem so viele Gruppen offen waren, oder null. */
        fun dayGroupsReached(count: Int): Int? =
            rounds.firstOrNull { it.unlockedGroups >= count }?.day

        /** Menschenlesbare Verteilung, für Fehlermeldungen. */
        fun rowReport(): String =
            asksPerRow.entries.sortedByDescending { it.value }
                .joinToString(", ") { "${it.key.name}=${it.value}" }
    }

    fun run(days: Int): Result {
        var app = start()
        val answers = Random(seed)
        val rounds = ArrayList<Round>()
        var index = 0

        for (day in 0 until days) {
            for (hour in sessionHours) {
                val now = (day * 24L + hour) * HOUR_MS
                val dayNumber = day.toLong()

                if (unlockedGroups == null) app = app.maybeUnlock(dayNumber, now)

                val plan = SessionBuilder.review(
                    unlockedGroups = app.unlockedGroups.toSet(),
                    states = app.states,
                    nowMs = now,
                    random = Random(day * 31 + index),
                    newBudget = minOf(KanaViewModel.NEW_PER_SESSION_MAX, app.newItemBudget(dayNumber)),
                    targetSize = targetSize,
                )
                if (plan.items.isEmpty()) {
                    rounds += Round(day, hour, 0, 0, app.unlockedGroups.size, emptyList())
                    index++
                    continue
                }

                val drill = DrillSession(
                    plan = plan,
                    initialStates = app.states,
                    targetAnswers = targetSize,
                )
                val sequence = ArrayList<KanaId>()
                var kana = drill.start()
                while (kana != null) {
                    sequence += kana.id
                    val hit = answers.nextDouble() < (skill[kana.row] ?: defaultSkill)
                    drill.submit(if (hit) kana.canonical else WRONG, latencyMs, now)
                    kana = drill.advance()
                }

                app = app.withStates(drill.changedStates)
                    .withNewItemsToday(drill.introduced, dayNumber)
                rounds += Round(day, hour, plan.items.size, drill.answered, app.unlockedGroups.size, sequence)
                index++
            }
        }
        return Result(app, rounds)
    }

    private fun start(): AppState {
        val fresh = AppState.fresh(0L)
        return if (unlockedGroups == null) fresh
        else fresh.copy(unlock = UnlockState(unlockedGroups = unlockedGroups.toList()))
    }

    /** Dieselbe Regel wie `KanaViewModel.maybeUnlock`. */
    private fun AppState.maybeUnlock(day: Long, nowMs: Long): AppState {
        if (!canUnlockToday(day)) return this
        val group = Unlock.nextGroup(unlockedGroups, states, nowMs) ?: return this
        return withUnlockedGroup(group.id, day)
    }

    private companion object {
        const val HOUR_MS = 3_600_000L

        /** Weit genug von jeder Lesung entfernt, um kein Tippfehler zu sein. */
        const val WRONG = "zzz"
    }
}
