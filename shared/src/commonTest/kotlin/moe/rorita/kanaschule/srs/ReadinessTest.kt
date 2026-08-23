package moe.rorita.kanaschule.srs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import moe.rorita.kanaschule.kana.KanaId
import moe.rorita.kanaschule.kana.KanaTable
import moe.rorita.kanaschule.kana.Row
import moe.rorita.kanaschule.kana.Script

class ReadinessTest {

    private val now = 2_000_000_000L

    private fun state(box: Int, dueAtMs: Long = 0L, reps: Int = 1) =
        ItemState(box = box, dueAtMs = dueAtMs, reps = reps)

    private fun mastered(dueAtMs: Long = 0L) = ItemState(
        box = 6,
        dueAtMs = dueAtMs,
        reps = 20,
        streak = 3,
        latencyEwmaMs = 2000,
        recent = ItemState.RECENT_MASK,
        recentCount = ItemState.RECENT_BITS,
    )

    @Test
    fun stufenNachBox() {
        assertEquals(0.0, Readiness.of(ItemState(), now))
        assertEquals(0.0, Readiness.of(state(1), now))
        assertEquals(0.30, Readiness.of(state(2), now))
        assertEquals(0.30, Readiness.of(state(3), now))
        assertEquals(0.60, Readiness.of(state(4), now))
        assertEquals(0.60, Readiness.of(state(5), now))
        assertEquals(0.85, Readiness.of(state(6), now))
        assertEquals(0.85, Readiness.of(state(8), now))
    }

    @Test
    fun beherrschtBrauchtBoxStreakQuoteUndTempo() {
        assertEquals(1.0, Readiness.of(mastered(), now))
        assertEquals(0.85, Readiness.of(mastered().copy(streak = 2), now), "Streak zu kurz")
        assertEquals(0.85, Readiness.of(mastered().copy(latencyEwmaMs = 8000), now), "zu langsam")
        assertEquals(
            0.85,
            Readiness.of(mastered().copy(recent = 0b1010_1010_1010_1010_1010), now),
            "Quote zu schlecht",
        )
    }

    @Test
    fun ueberfaelligesZeichenVerliert() {
        val stale = state(4, dueAtMs = now - 3 * Boxes.baseIntervalMs(4))
        assertEquals(0.60 * 0.70, Readiness.of(stale, now), 1e-9)

        val fresh = state(4, dueAtMs = now - 1000)
        assertEquals(0.60, Readiness.of(fresh, now))
    }

    @Test
    fun beherrschtFaelltNichtUnterDenSockel() {
        val longGone = mastered(dueAtMs = now - 365 * 24 * 60 * 60 * 1000L)
        assertEquals(Readiness.MASTERY_FLOOR, Readiness.of(longGone, now))
    }

    @Test
    fun prozentZaehltGesperrteZeichenMit() {
        val scope = KanaTable.of(Script.HIRAGANA, Row.A)
        assertEquals(5, scope.size)

        val states = mapOf(scope[0].id to mastered(), scope[1].id to mastered())
        assertEquals(40, Readiness.percent(scope, states, now))
        assertEquals(0, Readiness.percent(scope, emptyMap(), now))
    }

    @Test
    fun prozentDerFreigeschaltetenIstDieNebenangabe() {
        val scope = KanaTable.of(Script.HIRAGANA, Row.A)
        val unlocked = setOf(scope[0].id, scope[1].id)
        val states = mapOf(scope[0].id to mastered(), scope[1].id to mastered())

        assertEquals(40, Readiness.percent(scope, states, now))
        assertEquals(100, Readiness.percentOfUnlocked(scope, states, unlocked, now))
    }

    @Test
    fun leererUmfangIstNull() {
        assertEquals(0, Readiness.percent(emptyList(), emptyMap(), now))
    }

    @Test
    fun stufenFuerDieAnzeige() {
        assertEquals(MasteryTier.NEW, Readiness.tier(ItemState()))
        assertEquals(MasteryTier.NEW, Readiness.tier(state(1)))
        assertEquals(MasteryTier.BUILDING, Readiness.tier(state(3)))
        assertEquals(MasteryTier.SAFE, Readiness.tier(state(5)))
        assertEquals(MasteryTier.SAFE, Readiness.tier(state(6)))
        assertEquals(MasteryTier.MASTERED, Readiness.tier(mastered()))
    }

    @Test
    fun vollstaendigBeherrschterUmfangGibtHundert() {
        val scope = KanaTable.byScript.getValue(Script.HIRAGANA)
        val states = scope.associate { it.id to mastered() }
        assertEquals(100, Readiness.percent(scope, states, now))
        assertTrue(scope.size == 112, "Hiragana-Umfang inklusive Wörter: ${scope.size}")
    }
}
