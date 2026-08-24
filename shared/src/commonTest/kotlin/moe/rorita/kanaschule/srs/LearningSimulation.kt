package moe.rorita.kanaschule.srs

import kotlin.random.Random
import moe.rorita.kanaschule.kana.KanaId
import moe.rorita.kanaschule.kana.KanaTable
import moe.rorita.kanaschule.kana.Row
import moe.rorita.kanaschule.kana.Script

/**
 * Ein simulierter Lernender über mehrere Tage.
 *
 * Einzeltests prüfen Zustandsübergänge; die interessanten Eigenschaften des
 * Systems entstehen aber erst aus dem Zusammenspiel von Zusammenstellung,
 * Reihenfolge und Boxen über Dutzende Runden - „kommt ein schwaches Zeichen
 * öfter dran als ein sitzendes“ ist in keinem Einzelübergang zu sehen.
 *
 * Deterministisch: gleicher [seed] ergibt denselben Verlauf, damit Aussagen
 * über Häufigkeiten überhaupt prüfbar sind.
 */
class LearningSimulation(
    private val unlockedGroups: Set<String>,
    /** Trefferwahrscheinlichkeit je Zeile - so gut kann der Simulierte das. */
    private val skill: Map<Row, Double>,
    /** Uhrzeiten der Runden je Tag. */
    private val sessionHours: List<Long> = listOf(9L, 13L, 20L),
    private val targetSize: Int = SessionBuilder.TARGET_SIZE,
    private val newBudget: Int = SessionBuilder.NEW_MAX,
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
        /** Die Zeichen in der Reihenfolge, in der sie gefragt wurden. */
        val sequence: List<KanaId>,
    ) {
        val empty: Boolean get() = planned == 0
    }

    data class Result(
        val states: Map<KanaId, ItemState>,
        val rounds: List<Round>,
    ) {
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

        /** Menschenlesbare Verteilung, für Fehlermeldungen. */
        fun rowReport(): String =
            asksPerRow.entries.sortedByDescending { it.value }
                .joinToString(", ") { "${it.key.name}=${it.value}" }
    }

    fun run(days: Int): Result {
        var states = mapOf<KanaId, ItemState>()
        val answers = Random(seed)
        val rounds = ArrayList<Round>()
        var index = 0

        for (day in 0 until days) {
            for (hour in sessionHours) {
                val now = (day * 24L + hour) * HOUR_MS
                val plan = SessionBuilder.review(
                    unlockedGroups = unlockedGroups,
                    states = states,
                    nowMs = now,
                    random = Random(day * 31 + index),
                    newBudget = newBudget,
                    targetSize = targetSize,
                )
                if (plan.items.isEmpty()) {
                    rounds += Round(day, hour, planned = 0, answered = 0, sequence = emptyList())
                    index++
                    continue
                }

                val drill = DrillSession(
                    plan = plan,
                    initialStates = states,
                    random = Random(day * 31 + index + 1),
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

                states = states + drill.changedStates
                rounds += Round(day, hour, plan.items.size, drill.answered, sequence)
                index++
            }
        }
        return Result(states, rounds)
    }

    private companion object {
        const val HOUR_MS = 3_600_000L

        /** Weit genug von jeder Lesung entfernt, um kein Tippfehler zu sein. */
        const val WRONG = "zzz"
    }
}
