package moe.rorita.kanaschule.audio

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import moe.rorita.kanaschule.kana.KanaTable
import moe.rorita.kanaschule.kana.Pronunciation

/**
 * Die Aufnahmen liegen nicht im Repository - das Urheberrecht liegt bei
 * Tofugu. Geholt werden sie von `fetch-audio.sh`, und dessen Liste ist eine
 * zweite Fassung derselben Wahrheit wie [Pronunciation.audioName].
 *
 * Zwei Fassungen laufen auseinander, sobald der Datensatz wächst: ein neues
 * Zeichen im Code hätte sonst stillschweigend keine Aufnahme, und das fiele
 * erst beim Lernen auf. Also werden sie hier verglichen.
 */
class AudioSourcesTest {

    private val script: File = findScript()

    private fun findScript(): File {
        var dir: File? = File(System.getProperty("user.dir"))
        repeat(4) {
            val candidate = File(dir, "fetch-audio.sh")
            if (candidate.isFile) return candidate
            dir = dir?.parentFile
        }
        error("fetch-audio.sh nicht gefunden, ausgehend von ${System.getProperty("user.dir")}")
    }

    /** Die Zeilen aus dem RECORDINGS-Feld: Slug und Zeichen. */
    private fun entries(): List<Pair<String, String>> =
        script.readLines()
            .dropWhile { !it.startsWith("RECORDINGS=(") }
            .drop(1)
            .takeWhile { !it.startsWith(")") }
            .mapNotNull { line ->
                Regex("""^\s*"(\S+) (\S+)"\s*$""").find(line)?.let {
                    it.groupValues[1] to it.groupValues[2]
                }
            }

    @Test
    fun dasSkriptHoltGenauDieBenoetigtenAufnahmen() {
        val needed = KanaTable.singles.mapNotNull(Pronunciation::audioName).toSet()
        val offered = entries().map { it.first }.toSet()

        assertEquals(emptySet(), needed - offered, "Das Skript holt diese Aufnahmen nicht")
        assertEquals(emptySet(), offered - needed, "Das Skript holt Aufnahmen, die niemand braucht")
    }

    @Test
    fun jederSlugZeigtAufDasPassendeHiragana() {
        // Die Quelle ist nach dem Zeichen benannt, unser Dateiname nach dem
        // Slug. Ein Dreher hier gäbe die falsche Aussprache zum richtigen
        // Zeichen - der Fehler, den man am schwersten bemerkt.
        val glyphOf = KanaTable.singles
            .filter { it.script == moe.rorita.kanaschule.kana.Script.HIRAGANA }
            .mapNotNull { kana -> Pronunciation.audioName(kana)?.let { it to kana.glyph } }
            .toMap()

        for ((slug, glyph) in entries()) {
            assertEquals(glyphOf[slug], glyph, "Falsches Zeichen für $slug")
        }
    }

    @Test
    fun dasSkriptIstAusfuehrbar() {
        assertTrue(script.canExecute(), "fetch-audio.sh muss ausführbar sein")
    }
}
