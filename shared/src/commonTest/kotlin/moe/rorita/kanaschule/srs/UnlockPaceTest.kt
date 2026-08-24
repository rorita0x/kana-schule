package moe.rorita.kanaschule.srs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import moe.rorita.kanaschule.kana.Row

/**
 * Tempo der Freischaltleiter.
 *
 * Seit ein Aufstieg Fälligkeit voraussetzt, braucht [Unlock.MASTER_BOX] drei
 * Aufstiege mit Abstand - 10 Minuten, 1 Stunde, 8 Stunden, also mindestens gut
 * neun Stunden pro Zeichen. Das ist gewollt, könnte aber die Leiter unbenutzbar
 * langsam machen; deshalb steht das Tempo hier als Behauptung und nicht als
 * Vermutung.
 */
class UnlockPaceTest {

    /** Die zehn Gojūon-Gruppen der Hiragana, zusammen 46 Zeichen. */
    private val gojuon = 10

    private fun learner(skill: Double, hours: List<Long>) = LearningSimulation(
        unlockedGroups = null,
        skill = Row.entries.associateWith { skill },
        sessionHours = hours,
    )

    @Test
    fun fleissigesLernenOeffnetDieGojuonAmTagesLimit() {
        val result = learner(skill = 0.95, hours = listOf(8L, 12L, 16L, 20L)).run(days = 40)
        val day = result.dayGroupsReached(gojuon)

        // Eine Gruppe pro Tag ist die Bremse, und die erste ist von Anfang an
        // offen: neun weitere Gruppen brauchen also mindestens neun Tage,
        // gezählt ab Tag null. Schneller wäre ein Fehler in der Tagesgrenze.
        assertEquals(8, day, "46 Hiragana offen an Tag $day statt am Tageslimit")
    }

    @Test
    fun ordentlichesLernenKommtVoran() {
        val result = learner(skill = 0.90, hours = listOf(9L, 19L)).run(days = 60)
        val day = result.dayGroupsReached(gojuon)
        assertTrue(day != null, "Mit 90 Prozent muss die Leiter in 60 Tagen durch die Gojūon sein")
        assertTrue(day!! <= 30, "Zu langsam: Tag $day für 46 Zeichen")
    }

    @Test
    fun schwachesLernenHaeltDieLeiterAn() {
        // Absicht, nicht Fehler: Unlock.MASTER_ACCURACY verlangt 85 Prozent im
        // Schnitt. Wer dauerhaft 70 Prozent trifft, bekommt keine neuen
        // Zeichen dazu - neue Zeichen auf ein wackliges Fundament zu setzen
        // wäre die schlechtere Antwort.
        //
        // Die Schwelle ist hart: sie greift schon bei der ersten Gruppe, es
        // bleibt also über Wochen bei あいうえお. Zwei Dinge folgen daraus, und
        // beide sind Bringschuld der Oberfläche: sie muss sagen, *warum* nichts
        // Neues kommt, und sie hat mit der Freischaltung von Hand in den
        // Einstellungen ein Ventil, auf das sie dann hinweisen sollte.
        val result = learner(skill = 0.70, hours = listOf(19L)).run(days = 60)
        assertNull(result.dayGroupsReached(gojuon), "70 Prozent dürfen die Leiter nicht öffnen")
        assertEquals(
            1,
            result.app.unlockedGroups.size,
            "Bei 70 Prozent hält die Leiter vollständig - das ist die Absicht, aber es muss erklärt werden",
        )
    }

    @Test
    fun hoechstensEineGruppeProTag() {
        // Ohne diese Grenze schaltet ein euphorischer erster Tag alles frei und
        // man erstickt an Tag drei in Wiederholungen.
        val result = learner(skill = 1.0, hours = (6L..22L).toList()).run(days = 12)
        val perDay = result.rounds.groupBy { it.day }
            .mapValues { (_, rounds) -> rounds.maxOf { it.unlockedGroups } }

        var previous = 1
        for ((day, open) in perDay.toSortedMap()) {
            assertTrue(open - previous <= 1, "Tag $day hat ${open - previous} Gruppen aufgemacht")
            previous = open
        }
    }
}
