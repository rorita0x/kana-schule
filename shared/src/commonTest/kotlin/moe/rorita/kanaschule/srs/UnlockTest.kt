package moe.rorita.kanaschule.srs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import moe.rorita.kanaschule.kana.KanaId
import moe.rorita.kanaschule.kana.KanaTable
import moe.rorita.kanaschule.kana.Script

class UnlockTest {

    private val now = 2_000_000_000L

    private fun mastered(box: Int = 6) = ItemState(
        box = box,
        reps = 20,
        streak = 3,
        latencyEwmaMs = 2000,
        recent = ItemState.RECENT_MASK,
        recentCount = ItemState.RECENT_BITS,
    )

    private fun statesFor(vararg groups: UnlockGroup, box: Int = 6): Map<KanaId, ItemState> =
        groups.flatMap { it.itemIds }.associateWith { mastered(box) }

    // ------------------------------------------------------------- Leiter

    @Test
    fun leiterDecktAlleZeichenGenauEinmalAb() {
        val all = UnlockGroups.ordered.flatMap { it.itemIds }
        assertEquals(KanaTable.all.size, all.size, "jedes Zeichen genau einmal")
        assertEquals(all.size, all.toSet().size, "keine Dopplungen")
        assertEquals(KanaTable.all.map { it.id }.toSet(), all.toSet())
    }

    @Test
    fun hiraganaKommtVorKatakana() {
        val scripts = UnlockGroups.ordered.map { it.script }
        assertEquals(
            listOf(Script.HIRAGANA, Script.KATAKANA),
            scripts.distinct(),
            "erst alle Hiragana-Gruppen, dann Katakana",
        )
    }

    @Test
    fun ersteGruppeSindDieVokale() {
        assertEquals("H_A", UnlockGroups.first.id)
        assertEquals(5, UnlockGroups.first.itemIds.size)
        assertEquals("あ-Reihe", UnlockGroups.first.labelDe)
    }

    @Test
    fun gruppengroessenBleibenHandlich() {
        for (group in UnlockGroups.ordered) {
            assertTrue(
                group.itemIds.size in 3..9,
                "${group.id} hat ${group.itemIds.size} Zeichen",
            )
        }
    }

    @Test
    fun jedesZeichenKenntSeineGruppe() {
        for (kana in KanaTable.all) {
            assertNotNull(UnlockGroups.groupOf(kana.id), kana.id.v)
        }
    }

    @Test
    fun woerterHabenEigeneGruppen() {
        assertNotNull(UnlockGroups.byId["H_SOKUON"])
        assertNotNull(UnlockGroups.byId["K_SOKUON"])
        assertNotNull(UnlockGroups.byId["K_CHOUONPU"])
        assertNull(UnlockGroups.byId["H_CHOUONPU"], "ー gibt es nur in Katakana")
    }

    // ---------------------------------------------------------- Meisterung

    @Test
    fun gruppeGiltErstAbBoxDreiAlsGeschafft() {
        val group = UnlockGroups.first
        assertFalse(Unlock.isMastered(group, emptyMap()))
        assertFalse(Unlock.isMastered(group, statesFor(group, box = 2)))
        assertTrue(Unlock.isMastered(group, statesFor(group, box = 3)))
    }

    @Test
    fun einSchwachesZeichenBlockiertDieGruppe() {
        val group = UnlockGroups.first
        val states = statesFor(group).toMutableMap()
        states[group.itemIds.last()] = ItemState(box = 1, reps = 3)
        assertFalse(Unlock.isMastered(group, states))
    }

    @Test
    fun schlechteQuoteBlockiertTrotzHoherBox() {
        val group = UnlockGroups.first
        val weak = mastered().copy(recent = 0b1000_1000_1000_1000_1000)
        val states = group.itemIds.associateWith { weak }
        assertTrue(states.values.all { it.box >= Unlock.MASTER_BOX })
        assertFalse(Unlock.isMastered(group, states))
    }

    // ------------------------------------------------------- Freischaltung

    @Test
    fun ohneFortschrittIstNurDieErsteGruppeDran() {
        val next = assertNotNull(Unlock.nextGroup(emptySet(), emptyMap(), now))
        assertEquals("H_A", next.id)
    }

    @Test
    fun naechsteGruppeErstNachGeschaffterVorgaengerin() {
        val first = UnlockGroups.first
        val unlocked = setOf(first.id)
        assertNull(Unlock.nextGroup(unlocked, emptyMap(), now), "noch nichts gelernt")

        val next = assertNotNull(Unlock.nextGroup(unlocked, statesFor(first), now))
        assertEquals("H_KA", next.id)
    }

    @Test
    fun katakanaBleibtGeschlossenBisHiraganaSitzt() {
        val hiragana = UnlockGroups.of(Script.HIRAGANA)
        val unlocked = hiragana.map { it.id }.toSet()

        // Alle Hiragana-Gruppen offen, aber nur die halbe Menge geübt:
        // die Katakana-Schwelle von 70 Prozent ist damit nicht erreicht.
        val half = hiragana.take(hiragana.size / 2)
        val states = statesFor(*half.toTypedArray())
        assertNull(Unlock.nextGroup(unlocked, states, now))

        val allStates = statesFor(*hiragana.toTypedArray())
        val next = assertNotNull(Unlock.nextGroup(unlocked, allStates, now))
        assertEquals(Script.KATAKANA, next.script)
        assertEquals("K_A", next.id)
    }
}
