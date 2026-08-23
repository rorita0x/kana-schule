package moe.rorita.kanaschule.srs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import moe.rorita.kanaschule.kana.KanaId

class SessionQueueTest {

    private fun ids(vararg names: String) = names.map { KanaId(it) }

    private fun drain(queue: SessionQueue): List<String> {
        val out = ArrayList<String>()
        while (true) {
            out += (queue.next() ?: break).v
        }
        return out
    }

    @Test
    fun liefertInReihenfolgeUndDannNull() {
        val queue = SessionQueue(ids("a", "b", "c"))
        assertEquals(listOf("a", "b", "c"), drain(queue))
        assertNull(queue.next())
    }

    @Test
    fun verpasstesZeichenKommtNachDreiUndNachZehn() {
        val queue = SessionQueue(ids("b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m"))
        queue.requeueAfterMiss(KanaId("a"))
        val order = queue.pending.map { it.v }
        assertEquals("a", order[3], "nach genau drei anderen Fragen")
        assertEquals("a", order[10], "und noch einmal nach zehn")
        assertEquals(2, order.count { it == "a" })
    }

    @Test
    fun tippfehlerKommtDirektNachDerNaechstenFrage() {
        val queue = SessionQueue(ids("b", "c", "d"))
        queue.requeueTypo(KanaId("a"))
        assertEquals(listOf("b", "a", "c", "d"), queue.pending.map { it.v })
    }

    @Test
    fun nieZweimalHintereinanderDasselbeZeichen() {
        val queue = SessionQueue(ids("a", "b"))
        queue.next()
        // a steht jetzt wieder vorn und wuerde direkt folgen
        queue.requeueTypo(KanaId("a"))
        val served = drain(queue)
        for (i in 1 until served.size) {
            assertTrue(served[i] != served[i - 1], "Doppel bei $served")
        }
    }

    @Test
    fun keinDoppelEintragDirektNebeneinander() {
        val queue = SessionQueue(ids("a", "b", "c", "d", "e"))
        queue.requeueTypo(KanaId("b"))
        // b liegt bereits auf Index 1, direkt an der Einfuegestelle
        assertEquals(listOf("a", "b", "c", "d", "e"), queue.pending.map { it.v })
    }

    @Test
    fun verwechslungspaarWirdVerschachtelt() {
        val queue = SessionQueue(ids("p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z"))
        queue.requeueConfusionPair(KanaId("shi"), KanaId("tsu"))
        val order = queue.pending.map { it.v }
        val positions = order.withIndex()
            .filter { it.value == "shi" || it.value == "tsu" }
            .map { it.value }
        assertEquals(listOf("tsu", "shi", "tsu", "shi"), positions, "abwechselnd: $order")
    }

    @Test
    fun einfuegenHinterDemEndeLandetAmEnde() {
        val queue = SessionQueue(ids("a"))
        queue.requeueAfterMiss(KanaId("z"))
        assertEquals(listOf("a", "z"), queue.pending.map { it.v })
    }

    @Test
    fun nachschubHaengtHintenAn() {
        val queue = SessionQueue(ids("a", "b"))
        queue.append(ids("c", "d"))
        assertEquals(4, queue.remaining)
        assertEquals(listOf("a", "b", "c", "d"), drain(queue))
    }
}
