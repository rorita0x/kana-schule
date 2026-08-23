package moe.rorita.kanaschule.audio

import javax.sound.sampled.AudioSystem
import java.io.BufferedInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import moe.rorita.kanaschule.kana.KanaTable
import moe.rorita.kanaschule.kana.Pronunciation

/**
 * Fängt genau den Fehler, der sonst erst beim Lernen auffällt: ein Zeichen,
 * dessen Aufnahme fehlt oder in einem Format vorliegt, das die Desktop-JVM
 * ohne Zusatzbibliothek nicht lesen kann.
 */
class AudioAssetsTest {

    private val names = KanaTable.singles.mapNotNull(Pronunciation::audioName).toSet()

    private fun resource(name: String) =
        javaClass.classLoader?.getResourceAsStream("audio/$name.wav")

    @Test
    fun jedesZeichenHatEineHinterlegteAufnahme() {
        val missing = names.filter { resource(it) == null }
        assertTrue(missing.isEmpty(), "Aufnahmen fehlen: $missing")
    }

    @Test
    fun alleAufnahmenSindLesbaresWav() {
        for (name in names) {
            val stream = assertNotNull(resource(name), name)
            val audio = AudioSystem.getAudioInputStream(BufferedInputStream(stream))
            audio.use {
                assertEquals(1, it.format.channels, "$name sollte mono sein")
                assertTrue(it.format.sampleRate > 0f, name)
                assertTrue(it.frameLength > 0, "$name ist leer")
            }
        }
    }

    @Test
    fun esGibtKeineUeberzaehligenDateien() {
        // Eine Datei, die kein Zeichen referenziert, ist entweder ein Tippfehler
        // im Namen oder Ballast im Repository.
        val known = names.map { "$it.wav" }.toSet() + "HERKUNFT.md"
        val directory = javaClass.classLoader?.getResource("audio")
        assertNotNull(directory, "Ressourcenverzeichnis audio/ fehlt")

        val listed = java.io.File(directory.toURI()).list()?.toSet().orEmpty()
        assertTrue(listed.isNotEmpty(), "Verzeichnis ist leer")
        assertEquals(emptySet(), listed - known, "unerwartete Dateien")
    }
}
