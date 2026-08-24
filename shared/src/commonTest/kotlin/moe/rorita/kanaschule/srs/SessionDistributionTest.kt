package moe.rorita.kanaschule.srs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import moe.rorita.kanaschule.kana.KanaTable
import moe.rorita.kanaschule.kana.Row
import moe.rorita.kanaschule.kana.Script

/**
 * Eigenschaften, die sich erst über viele Runden zeigen. Der Lernende erlebt
 * genau diese Ebene - „warum kommt das dauernd und das nie“ - und sie ist
 * mit Einzelübergangs-Tests nicht erreichbar.
 */
class SessionDistributionTest {

    /** Fünf freigeschaltete Zeilen, unterschiedlich gut beherrscht. */
    private val fiveRows = setOf("H_A", "H_KA", "H_SA", "H_TA", "H_NA")

    private val unevenSkill = mapOf(
        Row.A to 0.98,
        Row.KA to 0.95,
        Row.SA to 0.90,
        Row.TA to 0.55,
        Row.NA to 0.70,
    )

    private fun simulation(
        targetSize: Int = SessionBuilder.TARGET_SIZE,
        groups: Set<String> = fiveRows,
    ) = LearningSimulation(
        unlockedGroups = groups,
        skill = unevenSkill,
        targetSize = targetSize,
    )

    @Test
    fun schwacheZeilenKommenOefterAlsSitzende() {
        val result = simulation().run(days = 10)
        println("Verteilung über ${result.rounds.size} Runden: ${result.rowReport()}")

        // Die zentrale Zusage des Systems. Ohne einen deutlichen Abstand ist
        // es kein Wiederholungssystem, sondern eine Zufallsabfrage.
        assertTrue(
            result.asks(Row.TA) > result.asks(Row.A) * 3 / 2,
            "Schwache Zeile muss klar öfter kommen: ${result.rowReport()}",
        )
        assertTrue(
            result.asks(Row.NA) > result.asks(Row.A),
            "Mittlere Zeile muss öfter kommen als die sitzende: ${result.rowReport()}",
        )
    }

    @Test
    fun keinZeichenVerhungert() {
        val result = simulation().run(days = 10)
        val unlocked = UnlockGroups.itemsOf(fiveRows)

        // Auch ein beherrschtes Zeichen darf nicht ganz aus der Rotation
        // fallen - sonst verfällt es unbemerkt.
        val never = unlocked.filter { (result.asksPerItem[it] ?: 0) == 0 }
        assertEquals(emptyList(), never, "Nie gefragte Zeichen: ${never.map { it.v }}")
    }

    @Test
    fun keineLeerenRundenSolangeEtwasOffenIst() {
        val result = simulation().run(days = 10)
        val empty = result.rounds.filter { it.empty }
        assertEquals(emptyList(), empty.map { "Tag ${it.day} ${it.hour}h" })
    }

    @Test
    fun rundenlaengeHaeltSichAnDieNachspielzeit() {
        val result = simulation().run(days = 10)
        val limit = SessionBuilder.TARGET_SIZE + DrillSession.DEFAULT_OVERTIME

        // Die Nachspielzeit darf offene Fehler festsetzen, aber die Runde
        // nicht unbegrenzt verlängern.
        val tooLong = result.rounds.filter { it.answered > limit }
        assertEquals(emptyList(), tooLong.map { "Tag ${it.day}: ${it.answered}" })
    }

    @Test
    fun niemalsZweimalDasselbeZeichenHintereinander() {
        val result = simulation().run(days = 10)
        for (round in result.rounds) {
            val repeat = round.sequence.zipWithNext().firstOrNull { it.first == it.second }
            assertEquals(null, repeat, "Doppel in Tag ${round.day} ${round.hour}h")
        }
    }

    @Test
    fun kurzeRundenLassenKeinZeichenLiegen() {
        // Wer die Runde auf 12 Fragen stellt, darf nicht die halbe Zeilenmenge
        // aus der Rotation verlieren.
        val result = simulation(targetSize = 12).run(days = 10)
        val unlocked = UnlockGroups.itemsOf(fiveRows)
        val never = unlocked.filter { (result.asksPerItem[it] ?: 0) == 0 }
        assertEquals(emptyList(), never, "Bei kurzen Runden nie gefragt: ${never.map { it.v }}")
    }

    @Test
    fun boxAchtBrauchtWochen() {
        // Box 8 heisst „nach zwei Monaten noch gewusst“. Die Boxfolge
        // 10min/1h/8h/1Tag/3Tage/1Woche/3Wochen ist erst nach gut fünf Wochen
        // durchlaufen - in fünf Tagen darf niemand dort ankommen, egal wie
        // fleissig geübt wird.
        val result = LearningSimulation(
            unlockedGroups = fiveRows,
            skill = Row.entries.associateWith { 1.0 },
            sessionHours = listOf(8L, 11L, 14L, 17L, 20L),
        ).run(days = 5)

        val tooHigh = KanaTable.of(Script.HIRAGANA, Row.A)
            .map { it.glyph to result.boxOf(it.glyph) }
            .filter { it.second >= Boxes.MAX }
        assertEquals(
            emptyList(),
            tooHigh,
            "Box ${Boxes.MAX} nach fünf Tagen erreicht: $tooHigh",
        )
    }
}
