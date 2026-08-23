package moe.rorita.kanaschule.audio

import java.io.BufferedInputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import javax.sound.sampled.LineEvent

/**
 * javax.sound spielt von Haus aus nur WAV, AIFF und AU - deshalb liegen die
 * Aufnahmen als WAV im Repository und nicht als MP3.
 *
 * [play] kehrt erst zurück, wenn die Aufnahme durchgelaufen ist. Vorher wurde
 * jeder neue Ton dem laufenden übergestülpt: wer zwei Mal kurz hintereinander
 * etwas anforderte, hörte den ersten Ton nach null bis dreitausend von 13000
 * Frames abgeschnitten - also gar nicht. Das Warten ist unkritisch, weil der
 * Aufrufer die Wünsche über einen einzigen Hintergrund-Verbraucher schickt.
 */
private class DesktopAudioPlayer : AudioPlayer {

    private val lock = ReentrantLock()
    private var clip: Clip? = null

    override fun play(name: String) {
        lock.lock()
        try {
            val path = "${AudioPlayer.DIRECTORY}/$name.wav"
            val stream = javaClass.classLoader?.getResourceAsStream(path) ?: return

            stopLocked()
            val finished = CountDownLatch(1)

            AudioSystem.getAudioInputStream(BufferedInputStream(stream)).use { audio ->
                val fresh = AudioSystem.getClip()
                fresh.addLineListener { event ->
                    if (event.type == LineEvent.Type.STOP) finished.countDown()
                }
                fresh.open(audio)
                clip = fresh
                fresh.start()
            }

            finished.await(MAX_WAIT_MS, TimeUnit.MILLISECONDS)
            stopLocked()
        } catch (e: Exception) {
            // Eine fehlende Soundkarte oder ein belegtes Ausgabegerät darf das
            // Lernen nicht aufhalten. Aber stillschweigend verschlucken auch
            // nicht: ein ausgefallener Ton soll nachvollziehbar sein.
            System.err.println("Aussprache '$name' nicht abspielbar: ${e.message}")
            clip = null
        } finally {
            lock.unlock()
        }
    }

    override fun release() {
        lock.lock()
        try {
            stopLocked()
        } finally {
            lock.unlock()
        }
    }

    private fun stopLocked() {
        clip?.let { existing ->
            runCatching {
                existing.stop()
                existing.flush()
                existing.close()
            }
        }
        clip = null
    }

    private companion object {
        /** Sicherheitsnetz: die Aufnahmen sind alle unter einer Sekunde lang. */
        const val MAX_WAIT_MS = 3000L
    }
}

actual fun createAudioPlayer(): AudioPlayer = DesktopAudioPlayer()
