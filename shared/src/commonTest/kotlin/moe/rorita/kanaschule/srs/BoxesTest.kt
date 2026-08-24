package moe.rorita.kanaschule.srs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class BoxesTest {

    @Test
    fun dieBoxfolgeIstDieVersprocheneTabelle() {
        // Steht so im README; wer hier etwas ändert, muss es dort auch tun.
        assertEquals(0L, Boxes.baseIntervalMs(0))
        assertEquals(10 * 60_000L, Boxes.baseIntervalMs(1))
        assertEquals(60 * 60_000L, Boxes.baseIntervalMs(2))
        assertEquals(8 * 60 * 60_000L, Boxes.baseIntervalMs(3))
        assertEquals(24 * 60 * 60_000L, Boxes.baseIntervalMs(4))
        assertEquals(60L * 24 * 60 * 60_000L, Boxes.baseIntervalMs(Boxes.MAX))
    }

    @Test
    fun ausserhalbDerTabelleWirdGeklammert() {
        assertEquals(Boxes.baseIntervalMs(0), Boxes.baseIntervalMs(-3))
        assertEquals(Boxes.baseIntervalMs(Boxes.MAX), Boxes.baseIntervalMs(99))
    }

    @Test
    fun gleicherZeitpunktErgibtGleichesIntervall() {
        // Die Eigenschaft, wegen der die Streuung nicht mehr aus einem
        // Generator kommt: derselbe Verlauf muss dieselben Fälligkeiten
        // ergeben, sonst ist kein Lernverlauf im Test wiederholbar.
        val t = 1_787_506_702_997L
        assertEquals(Boxes.intervalMs(5, t), Boxes.intervalMs(5, t))
    }

    @Test
    fun box0WirdNichtHinausgeplant() {
        assertEquals(0L, Boxes.intervalMs(0, 12_345L))
    }

    @Test
    fun streuungBleibtImBandUndNutztEsAus() {
        val box = 6
        val base = Boxes.baseIntervalMs(box).toDouble()
        val factors = (0 until 4000).map { i ->
            // Millisekundenabstände, wie sie zwischen zwei Antworten liegen.
            Boxes.intervalMs(box, 1_700_000_000_000L + i * 137L) / base
        }

        assertTrue(factors.all { it in 0.90..1.10 }, "Band verlassen: ${factors.minOrNull()}..${factors.maxOrNull()}")
        assertTrue(factors.min() < 0.92, "Untere Hälfte wird nicht genutzt: ${factors.min()}")
        assertTrue(factors.max() > 1.08, "Obere Hälfte wird nicht genutzt: ${factors.max()}")

        // Gleichmässig genug: der Mittelwert liegt nahe der Bandmitte.
        val mean = factors.average()
        assertTrue(mean in 0.99..1.01, "Streuung ist schief, Mittelwert $mean")
    }

    @Test
    fun benachbarteMillisekundenStreuenAuseinander() {
        // Sonst bekämen alle Antworten einer Runde fast dieselbe Streuung und
        // die Wiederholungen liefen doch als Lawine auf.
        val t = 1_700_000_000_000L
        val a = Boxes.intervalMs(7, t)
        val b = Boxes.intervalMs(7, t + 1)
        val spread = Boxes.baseIntervalMs(7) * 0.20
        assertTrue(
            kotlin.math.abs(a - b) > spread * 0.02,
            "Zwei aufeinanderfolgende Millisekunden ergaben fast dasselbe: $a vs $b",
        )
    }
}
