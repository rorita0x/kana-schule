package moe.rorita.kanaschule.ui.drill

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Der Abstand in Worten. Vorher stand die Formatierung inline und schrieb
 * „in 1 Minuten“ - deshalb steht sie jetzt an einer Stelle, mit Test.
 */
class InWordsTest {

    private fun minutes(n: Long) = inWords(n * 60_000L)

    @Test
    fun singularformenStimmen() {
        assertEquals("in einer Minute", minutes(1))
        assertEquals("in einer Stunde", minutes(60))
        assertEquals("in einer Stunde", minutes(119))
    }

    @Test
    fun pluralformenStimmen() {
        assertEquals("in 2 Minuten", minutes(2))
        assertEquals("in 59 Minuten", minutes(59))
        assertEquals("in 2 Stunden", minutes(120))
        assertEquals("in 23 Stunden", minutes(23 * 60))
        assertEquals("in 3 Tagen", minutes(3 * 24 * 60))
    }

    @Test
    fun derNaechsteTagHeisstMorgen() {
        assertEquals("morgen", minutes(24 * 60))
        assertEquals("morgen", minutes(2 * 24 * 60 - 1))
    }

    @Test
    fun nullUndNegativWerdenNichtZuNullMinuten() {
        // Kann vorkommen, wenn zwischen Rechnung und Anzeige die Uhr weiterläuft.
        assertEquals("in einer Minute", inWords(0))
        assertEquals("in einer Minute", inWords(-5_000))
    }
}
