package moe.rorita.kanaschule.srs

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import moe.rorita.kanaschule.kana.KanaId
import moe.rorita.kanaschule.kana.KanaTable
import moe.rorita.kanaschule.kana.Verdict
import moe.rorita.kanaschule.store.Outcome

class DrillSessionTest {

    private val now = 1_700_000_000_000L

    private fun plan(vararg ids: String, new: List<String> = emptyList()) = SessionPlan(
        mode = SessionMode.REVIEW,
        items = ids.map { KanaId(it) },
        newItems = new.map { KanaId(it) },
    )

    private fun session(
        plan: SessionPlan,
        states: Map<KanaId, ItemState> = emptyMap(),
        target: Int = 30,
        overtime: Int = DrillSession.DEFAULT_OVERTIME,
    ) = DrillSession(plan, states, Random(11), target, overtime)

    /** Antwortet auf alles richtig, bis die Session zu Ende ist. */
    private fun playThrough(session: DrillSession, answer: (String) -> String): Int {
        var guard = 0
        var kana = session.start()
        while (kana != null) {
            session.submit(answer(kana.id.v), 1200, now)
            kana = session.advance()
            if (++guard > 500) error("Session endet nicht")
        }
        return guard
    }

    @Test
    fun startLiefertDasErsteZeichen() {
        val session = session(plan("h.a", "h.i", "h.u"))
        val first = assertNotNull(session.start())
        assertEquals(first, session.current)
        assertEquals(0, session.asked)
    }

    @Test
    fun richtigeAntwortZaehltUndSteigt() {
        val session = session(plan("h.a"))
        val kana = assertNotNull(session.start())
        val result = session.submit("a", 1000, now)

        assertEquals(Outcome.CORRECT, result.outcome)
        assertEquals(0, result.boxBefore)
        assertEquals(1, result.boxAfter)
        assertEquals(1, session.asked)
        assertEquals(1, session.correct)
        assertEquals(1, session.promoted)
        assertEquals(0, session.demoted)
        assertEquals(mapOf(kana.id to session.states.getValue(kana.id)), session.changedStates)
    }

    @Test
    fun kunreiAntwortLiefertDenHinweisMit() {
        val session = session(plan("h.shi"))
        session.start()
        val result = session.submit("si", 1000, now)
        assertEquals(Outcome.CORRECT, result.outcome)
        assertNotNull(result.hint)
    }

    @Test
    fun falscheAntwortStuftAbUndKommtZurueck() {
        val states = mapOf(KanaId("h.a") to ItemState(box = 6, reps = 5))
        val session = session(plan("h.a", "h.i", "h.u", "h.e", "h.o"), states)
        session.start()
        val result = session.submit("zzz", 2000, now)

        assertEquals(Outcome.WRONG, result.outcome)
        assertEquals(3, result.boxAfter)
        assertEquals(1, session.demoted)
        assertEquals("a", result.expected)

        // Es steht wieder in der Warteschlange.
        var seenAgain = false
        repeat(6) {
            if (session.advance()?.id == KanaId("h.a")) seenAgain = true
        }
        assertTrue(seenAgain, "das verpasste Zeichen muss zurueckkommen")
    }

    @Test
    fun leereAntwortIstUebersprungen() {
        val session = session(plan("h.a", "h.i", "h.u", "h.e"))
        session.start()
        assertEquals(Outcome.SKIPPED, session.submit("", 5000, now).outcome)
    }

    @Test
    fun tippfehlerZaehltNichtAlsAntwort() {
        val session = session(plan("h.word.kitte", "h.a", "h.i"))
        session.start()
        val result = session.submit("kittte", 1500, now)

        assertEquals(Outcome.TYPO, result.outcome)
        assertEquals(0, session.asked, "ein Tippfehler ist keine Antwort")
        assertEquals(result.boxBefore, result.boxAfter)
        assertTrue(session.changedStates.isEmpty(), "und aendert nichts am Lernstand")
    }

    @Test
    fun nachZweiTippfehlernIstEsEinFehler() {
        val session = session(plan("h.word.kitte", "h.a", "h.i", "h.u", "h.e"))
        session.start()
        assertEquals(Outcome.TYPO, session.submit("kittte", 1500, now).outcome)
        session.advance()
        while (session.current?.id != KanaId("h.word.kitte")) session.advance()
        assertEquals(Outcome.TYPO, session.submit("kittte", 1500, now).outcome)
        session.advance()
        while (session.current?.id != KanaId("h.word.kitte")) session.advance()
        assertEquals(Outcome.WRONG, session.submit("kittte", 1500, now).outcome)
    }

    @Test
    fun verwechslungZiehtDenPartnerMit() {
        val states = mapOf(
            KanaId("k.shi") to ItemState(box = 6, reps = 8),
            KanaId("k.tsu") to ItemState(box = 8, reps = 9),
        )
        val session = session(plan("k.shi", "h.a", "h.i", "h.u", "h.e", "h.o"), states)
        session.start()
        val result = session.submit("tsu", 2000, now)

        assertEquals(Outcome.CONFUSED, result.outcome)
        assertEquals(KanaId("k.tsu"), result.confusedWith)

        val partner = session.states.getValue(KanaId("k.tsu"))
        assertEquals(4, partner.box, "der Partner wird gedeckelt")
        assertEquals(mapOf("k.shi" to 1), partner.confusions)

        val target = session.states.getValue(KanaId("k.shi"))
        assertEquals(mapOf("k.tsu" to 1), target.confusions)
        assertTrue(KanaId("k.tsu") in session.changedStates.keys)
    }

    @Test
    fun verwechslungspaarWirdVerschachteltEingereiht() {
        val ids = (1..12).map { "h.a" }
        val session = session(
            plan("k.shi", *List(12) { "h.ka" }.toTypedArray()),
            mapOf(KanaId("k.shi") to ItemState(box = 4, reps = 3)),
        )
        session.start()
        session.submit("tsu", 2000, now)

        val upcoming = ArrayList<String>()
        repeat(12) { session.advance()?.let { upcoming += it.id.v } }
        assertTrue(upcoming.contains("k.tsu"), "der Partner muss auftauchen: $upcoming")
        assertTrue(upcoming.count { it == "k.shi" } >= 1, upcoming.toString())
        assertEquals(ids.size, 12)
    }

    @Test
    fun sessionEndetNachDerZielzahl() {
        val session = session(plan(*List(40) { "h.a" }.toTypedArray()), target = 5, overtime = 0)
        playThrough(session) { "a" }
        assertEquals(5, session.asked)
        assertTrue(session.finished)
        assertNull(session.current)
    }

    @Test
    fun nachspielzeitSetztOffeneFehlerFest() {
        val session = session(plan(*List(30) { "h.a" }.toTypedArray()), target = 2)
        session.start()
        session.submit("a", 1000, now)
        session.advance()
        session.submit("zzz", 1000, now)

        // Zielzahl erreicht, aber ein Fehler ist offen: es geht weiter.
        assertNotNull(session.advance(), "offener Fehler verlaengert die Session")

        session.submit("a", 1000, now)
        session.advance()
        session.submit("a", 1000, now)
        assertNull(session.advance(), "nach zwei Treffern ist der Fehler erledigt")
    }

    @Test
    fun nachspielzeitIstBegrenzt() {
        val session = session(plan(*List(30) { "h.a" }.toTypedArray()), target = 1, overtime = 3)
        session.start()
        session.submit("zzz", 1000, now)
        var extra = 0
        while (session.advance() != null) {
            session.submit("zzz", 1000, now)
            extra++
            if (extra > 20) break
        }
        assertEquals(3, extra, "hoechstens die erlaubte Nachspielzeit")
    }

    @Test
    fun leerlaufendeWarteschlangeBeendetDieSession() {
        val session = session(plan("h.a", "h.i"), target = 30)
        playThrough(session) { it.substringAfter('.') }
        assertEquals(2, session.asked)
        assertTrue(session.finished)
    }

    @Test
    fun medianAntwortzeit() {
        val session = session(plan("h.a", "h.i", "h.u"))
        session.start()
        session.submit("a", 1000, now)
        session.advance()
        session.submit("i", 3000, now)
        session.advance()
        session.submit("u", 2000, now)
        assertEquals(2000, session.medianLatencyMs)
    }

    @Test
    fun schwaechsteZeichenZuerst() {
        val session = session(plan("h.a", "h.i", "h.u"))
        session.start()
        session.submit("zzz", 1000, now)
        session.advance()
        session.submit("i", 1000, now)

        val weakest = session.weakest(2)
        assertEquals(KanaId("h.a"), weakest.first().first)
    }

    @Test
    fun unberuehrteZeichenWerdenNichtGespeichert() {
        val session = session(plan("h.a", "h.i", "h.u"))
        session.start()
        session.submit("a", 1000, now)
        assertEquals(setOf(KanaId("h.a")), session.changedStates.keys)
    }

    @Test
    fun verdictWirdWeitergegeben() {
        val session = session(plan("h.shi", "h.a", "h.i", "h.u", "h.e"))
        session.start()
        val result = session.submit("hi", 1000, now)
        assertTrue(result.verdict is Verdict.Confused)
        assertEquals(KanaTable.require(KanaId("h.shi")), result.kana)
    }

    @Test
    fun relearningBleibtOffenWennDieSessionVorherEndet() {
        val session = session(plan("h.a", "h.i"), target = 2, overtime = 0)
        session.start()
        session.submit("zzz", 1000, now)
        session.advance()
        session.submit("i", 1000, now)
        session.advance()

        assertTrue(session.states.getValue(KanaId("h.a")).relearning)
        assertEquals(now, session.states.getValue(KanaId("h.a")).dueAtMs)
        assertFalse(session.states.getValue(KanaId("h.i")).relearning)
    }

    // ----------------------------------------------------------- Erstkontakt

    @Test
    fun erstkontaktZaehltNichtInDieQuote() {
        val session = session(plan("h.a", "h.i", new = listOf("h.a")))
        session.start()
        val result = session.submit("a", 1000, now)

        assertEquals(Outcome.INTRODUCED, result.outcome)
        assertTrue(result.introduction)
        assertEquals(0, session.asked, "kein Pruefungsversuch")
        assertEquals(0, session.correct)
        assertEquals(1, session.introduced)
        assertEquals(1, session.answered, "verbraucht aber eine Frage")

        val state = session.states.getValue(KanaId("h.a"))
        assertEquals(1, state.box)
        assertEquals(1, state.reps)
        assertEquals(0, state.recentCount, "der Ringpuffer bleibt unberuehrt")
        assertEquals(0, state.lapses)
        assertFalse(state.relearning)
    }

    @Test
    fun falscherErstkontaktIstAuchKeinFehler() {
        val session = session(plan("h.a", "h.i", "h.u", new = listOf("h.a")))
        session.start()
        val result = session.submit("zzz", 1000, now)

        assertEquals(Outcome.INTRODUCED, result.outcome)
        assertEquals(0, session.demoted)
        val state = session.states.getValue(KanaId("h.a"))
        assertEquals(1, state.box, "landet trotzdem in Box 1")
        assertEquals(0, state.lapses)
    }

    @Test
    fun abDemZweitenKontaktZaehltAlles() {
        val session = session(plan("h.a", "h.i", "h.a", new = listOf("h.a")))
        session.start()
        session.submit("a", 1000, now)
        session.advance()
        session.submit("i", 1000, now)
        session.advance()

        assertEquals(KanaId("h.a"), session.current?.id)
        val second = session.submit("zzz", 1000, now)
        assertEquals(Outcome.WRONG, second.outcome, "jetzt ist es eine echte Abfrage")
        assertEquals(1, session.states.getValue(KanaId("h.a")).lapses)
    }

    @Test
    fun bereitsGeseheneZeichenWerdenNichtVorgestellt() {
        // Als neu geplant, aber der Lernstand kennt es schon: keine Einfuehrung.
        val states = mapOf(KanaId("h.a") to ItemState(box = 3, reps = 4))
        val session = session(plan("h.a", "h.i", new = listOf("h.a")), states)
        session.start()
        assertEquals(Outcome.CORRECT, session.submit("a", 1000, now).outcome)
        assertEquals(1, session.asked)
        assertEquals(0, session.introduced)
    }
}
