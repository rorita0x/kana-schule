package moe.rorita.kanaschule.audio

import java.io.BufferedInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

/**
 * javax.sound spielt von Haus aus nur WAV, AIFF und AU - deshalb liegen die
 * Aufnahmen als WAV im Repository und nicht als MP3.
 */
private class DesktopAudioPlayer : AudioPlayer {

    private var clip: Clip? = null

    override fun play(name: String) {
        val path = "${AudioPlayer.DIRECTORY}/$name.wav"
        val stream = javaClass.classLoader?.getResourceAsStream(path) ?: return

        stop()
        try {
            AudioSystem.getAudioInputStream(BufferedInputStream(stream)).use { audio ->
                val fresh = AudioSystem.getClip()
                fresh.open(audio)
                clip = fresh
                fresh.start()
            }
        } catch (e: Exception) {
            // Eine fehlende Soundkarte oder ein nicht lesbares Format darf das
            // Lernen nicht aufhalten.
            clip = null
        }
    }

    override fun release() {
        stop()
    }

    private fun stop() {
        clip?.let { existing ->
            runCatching {
                existing.stop()
                existing.close()
            }
        }
        clip = null
    }
}

actual fun createAudioPlayer(): AudioPlayer = DesktopAudioPlayer()
