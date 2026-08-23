package moe.rorita.kanaschule.srs

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import moe.rorita.kanaschule.kana.ConfusionKind
import moe.rorita.kanaschule.kana.KanaId
import moe.rorita.kanaschule.kana.RomajiSystem
import moe.rorita.kanaschule.kana.Verdict

class SchedulerTest {

    private val now = 1_000_000_000L
    private val random = Random(42)
    private val correct = Verdict.Correct(RomajiSystem.HEPBURN, hint = null)
    private val confused = Verdict.Confused(KanaId("k.tsu"), ConfusionKind.VISUAL)

    private fun apply(
        state: ItemState,
        verdict: Verdict,
        latencyMs: Int = 1500,
        mode: SessionMode = SessionMode.REVIEW,
        nowMs: Long = now,
    ) = Scheduler.apply(state, verdict, latencyMs, nowMs, mode, random)

    @Test
    fun richtigeAntwortSteigtEineBoxUndPlantVoraus() {
        val next = apply(ItemState(), correct)
        assertEquals(1, next.box)
        assertEquals(1, next.streak)
        assertEquals(1, next.reps)
        assertEquals(1, next.fastStreak)
        assertTrue(next.dueAtMs > now, "muss in die Zukunft geplant sein")
        assertEquals(now, next.firstSeenMs)
    }

    @Test
    fun achtRichtigeAntwortenErreichenDieHoechsteBox() {
        var state = ItemState()
        repeat(8) { state = apply(state, correct) }
        assertEquals(Boxes.MAX, state.box)
        state = apply(state, correct)
        assertEquals(Boxes.MAX, state.box, "Box 8 ist die Obergrenze")
    }

    @Test
    fun langsameAntwortHaeltHoheBoxenAuf() {
        val high = ItemState(box = 5)
        assertEquals(5, apply(high, correct, latencyMs = Scheduler.SLOW_MS + 1).box)
        assertEquals(6, apply(high, correct, latencyMs = Scheduler.SLOW_MS).box)
    }

    @Test
    fun langsameAntwortInNiedrigenBoxenSteigtTrotzdem() {
        assertEquals(3, apply(ItemState(box = 2), correct, latencyMs = 9000).box)
    }

    @Test
    fun schnellStreakBrichtBeiLangsamerAntwort() {
        val state = ItemState(box = 1, fastStreak = 4)
        assertEquals(0, apply(state, correct, latencyMs = Scheduler.FAST_MS + 1).fastStreak)
        assertEquals(5, apply(state, correct, latencyMs = Scheduler.FAST_MS).fastStreak)
    }

    @Test
    fun fehlerKostetDreiBoxenUndNieMehr() {
        assertEquals(5, apply(ItemState(box = 8), Verdict.Wrong).box)
        assertEquals(3, apply(ItemState(box = 6), Verdict.Wrong).box)
        assertEquals(1, apply(ItemState(box = 4), Verdict.Wrong).box)
        assertEquals(1, apply(ItemState(box = 2), Verdict.Wrong).box)
        assertEquals(1, apply(ItemState(box = 0), Verdict.Wrong).box, "nie unter Box 1")
    }

    @Test
    fun fehlerIstSofortWiederFaellig() {
        val next = apply(ItemState(box = 6, dueAtMs = now + 999_999), Verdict.Wrong)
        assertEquals(now, next.dueAtMs)
        assertTrue(next.relearning)
        assertEquals(0, next.streak)
        assertEquals(1, next.lapses)
    }

    @Test
    fun uebersprungenZaehltWieFalsch() {
        val skipped = apply(ItemState(box = 4), Verdict.Skipped)
        assertEquals(1, skipped.box)
        assertEquals(1, skipped.lapses)
    }

    @Test
    fun tippfehlerAendertNichts() {
        val state = ItemState(box = 5, streak = 3, reps = 9, latencyEwmaMs = 2000)
        assertEquals(state, apply(state, Verdict.Typo("kitte"), latencyMs = 12_000))
    }

    @Test
    fun relearningEndetErstNachZweiRichtigen() {
        var state = apply(ItemState(box = 6), Verdict.Wrong)
        assertTrue(state.relearning)
        state = apply(state, correct)
        assertTrue(state.relearning, "einmal richtig genügt nicht")
        state = apply(state, correct)
        assertFalse(state.relearning)
    }

    @Test
    fun leechAbSechsFehlernUndSchlechterQuote() {
        var state = ItemState()
        repeat(5) { state = apply(state, Verdict.Wrong) }
        assertFalse(state.leech, "fünf Fehler sind noch kein Leech")
        state = apply(state, Verdict.Wrong)
        assertTrue(state.leech)
    }

    @Test
    fun guteQuoteVerhindertLeechTrotzVielerFehler() {
        var state = ItemState()
        repeat(6) { state = apply(state, Verdict.Wrong) }
        repeat(14) { state = apply(state, correct) }
        assertTrue(state.accuracy20 >= Scheduler.LEECH_ACCURACY)
        state = apply(state, Verdict.Wrong)
        assertFalse(state.leech, "bei guter Quote kein Leech")
    }

    @Test
    fun ringpufferHaeltZwanzigAntworten() {
        var state = ItemState()
        repeat(25) { state = apply(state, correct) }
        assertEquals(ItemState.RECENT_BITS, state.recentCount)
        assertEquals(1.0, state.accuracy20)

        repeat(10) { state = apply(state, Verdict.Wrong) }
        assertEquals(0.5, state.accuracy20)
    }

    @Test
    fun speedDrillLaesstDenPlanUnberuehrt() {
        val state = ItemState(box = 6, dueAtMs = now + 500_000, lapses = 1)
        val missed = apply(state, Verdict.Wrong, mode = SessionMode.SPEED)
        assertEquals(6, missed.box, "Speed-Drill darf die Box nicht senken")
        assertEquals(state.dueAtMs, missed.dueAtMs)
        assertEquals(1, missed.lapses)
        assertEquals(0, missed.streak, "der Streak bricht aber")
        assertFalse(missed.relearning)

        val hit = apply(state, correct, mode = SessionMode.SPEED)
        assertEquals(6, hit.box, "und sie auch nicht heben")
        assertEquals(state.dueAtMs, hit.dueAtMs)
    }

    @Test
    fun verwechslungWirdAmZeichenVermerkt() {
        val next = apply(ItemState(box = 5), confused)
        assertEquals(mapOf("k.tsu" to 1), next.confusions)
        assertEquals(2, next.box)
    }

    @Test
    fun partnerEinerVerwechslungWirdNurGedeckelt() {
        val partner = ItemState(box = 8, streak = 5, dueAtMs = now + 999)
        val next = Scheduler.applyConfusionPartner(partner, KanaId("k.shi"), now)
        assertEquals(4, next.box, "nur gedeckelt, nicht um drei Boxen gesenkt")
        assertEquals(now, next.dueAtMs)
        assertEquals(0, next.streak)
        assertEquals(mapOf("k.shi" to 1), next.confusions)
    }

    @Test
    fun partnerInNiedrigerBoxBleibtWoErIst() {
        val partner = ItemState(box = 2)
        assertEquals(2, Scheduler.applyConfusionPartner(partner, KanaId("k.shi"), now).box)
    }

    @Test
    fun verwechslungenVerblassen() {
        val state = ItemState(confusions = mapOf("k.tsu" to 2, "k.n" to 1))
        val decayed = Scheduler.decayConfusions(state)
        assertEquals(mapOf("k.tsu" to 1), decayed.confusions)
    }

    @Test
    fun antwortzeitWirdGeglaettet() {
        val first = apply(ItemState(), correct, latencyMs = 2000)
        assertEquals(2000, first.latencyEwmaMs, "der erste Wert setzt den Mittelwert")
        val second = apply(first, correct, latencyMs = 4000)
        assertEquals(2600, second.latencyEwmaMs)
    }
}
