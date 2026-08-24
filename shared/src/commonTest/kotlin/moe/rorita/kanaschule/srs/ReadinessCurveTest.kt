package moe.rorita.kanaschule.srs

import kotlin.test.Test
import kotlin.test.assertTrue
import moe.rorita.kanaschule.kana.KanaTable
import moe.rorita.kanaschule.kana.Row
import moe.rorita.kanaschule.kana.Script

/**
 * Der Verlauf der Prüfungsreif-Zahl über Wochen.
 *
 * Die Schwellen in [Readiness] waren an Einzelgrenzfällen geprüft, nie an einem
 * Lernverlauf. Seit der Aufstieg Fälligkeit voraussetzt, steigen Boxen
 * langsamer - die Zahl ist damit ehrlicher, könnte aber zu träge geworden sein,
 * und träge heisst hier: demotivierend.
 *
 * Die Grenzen sind Bänder, nach oben und nach unten. Nach unten, weil eine
 * Zahl, die nach vier Wochen Fleiss bei 40 Prozent steht, niemanden weiterlernen
 * lässt. Nach oben, weil eine Zahl, die nach einer Woche 80 Prozent zeigt, log.
 */
class ReadinessCurveTest {

    private val hiragana = KanaTable.byScript.getValue(Script.HIRAGANA)

    /** Vier Runden am Tag, 95 Prozent Treffer. */
    private fun diligent() = LearningSimulation(
        unlockedGroups = null,
        skill = Row.entries.associateWith { 0.95 },
        sessionHours = listOf(8L, 12L, 16L, 20L),
    )

    /** Zwei Runden am Tag, 90 Prozent Treffer. */
    private fun moderate() = LearningSimulation(
        unlockedGroups = null,
        skill = Row.entries.associateWith { 0.90 },
        sessionHours = listOf(9L, 19L),
    )

    private fun endOf(days: Int): Long = (days * 24L - 1) * 3_600_000L

    private fun hiraganaPercent(result: LearningSimulation.Result, days: Int): Int =
        Readiness.percent(hiragana, result.states, endOf(days))

    @Test
    fun dieErsteWocheZeigtSichtbarenFortschritt() {
        val percent = hiraganaPercent(diligent().run(7), 7)
        assertTrue(percent in 10..40, "Nach einer Woche Fleiss: $percent Prozent Hiragana")
    }

    @Test
    fun vierWochenFleissReichenFuerHiragana() {
        // Das Versprechen im README: Vollbeherrschung in wenigen Wochen.
        val percent = hiraganaPercent(diligent().run(30), 30)
        assertTrue(percent >= 85, "Nach vier Wochen Fleiss erst $percent Prozent Hiragana")
    }

    @Test
    fun zweiRundenAmTagBrauchenLaengerAberKommenAn() {
        val month = hiraganaPercent(moderate().run(30), 30)
        val twoMonths = hiraganaPercent(moderate().run(60), 60)

        assertTrue(month in 25..70, "Gemässigt nach einem Monat: $month Prozent")
        assertTrue(twoMonths >= 85, "Gemässigt nach zwei Monaten erst $twoMonths Prozent")
    }

    @Test
    fun dieEhrlicheZahlBleibtHinterDerSchriftZurueck() {
        // Der Nenner ist der volle Umfang, gesperrte Zeichen eingeschlossen.
        // Solange nur Hiragana offen ist, muss die Schlagzeile deutlich unter
        // der Hiragana-Zahl liegen - sonst wäre sie geschönt.
        val result = diligent().run(30)
        val all = Readiness.percent(KanaTable.all, result.states, endOf(30))
        val hira = hiraganaPercent(result, 30)
        assertTrue(all < hira, "Gesamtzahl $all darf nicht über der Hiragana-Zahl $hira liegen")
    }

    @Test
    fun zweiWochenPauseKostenNichtAlles() {
        // Der Sockel aus dem README: Urlaub darf wehtun, aber nicht einbrechen.
        // Ein Totaleinbruch nach einer Pause ist das demotivierendste
        // Verhalten, das eine Wiederholungs-App zeigen kann.
        val result = diligent().run(30)
        val before = hiraganaPercent(result, 30)
        val after = Readiness.percent(hiragana, result.states, endOf(30 + 14))

        assertTrue(after < before, "Eine Pause muss sichtbar sein")
        assertTrue(
            after >= before * 6 / 10,
            "Zwei Wochen Pause haben von $before auf $after gedrückt",
        )
    }
}
