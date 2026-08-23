package moe.rorita.kanaschule.audio

import android.media.MediaPlayer
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock
import moe.rorita.kanaschule.store.AndroidPlatform

/**
 * Die Aufnahmen liegen als Assets. Sie werden beim ersten Abspielen in den
 * Cache kopiert und von dort geöffnet: ein Asset-Dateideskriptor funktioniert
 * nur, wenn die Datei unkomprimiert im APK liegt, und das hängt an
 * Verpackungsregeln, die wir hier nicht erzwingen wollen.
 *
 * Wie auf dem Desktop kehrt [play] erst zurück, wenn die Aufnahme durch ist:
 * sonst würgt jeder neue Ton den laufenden ab und man hört gar nichts.
 */
private class AndroidAudioPlayer : AudioPlayer {

    private val lock = ReentrantLock()
    private var player: MediaPlayer? = null

    override fun play(name: String) {
        lock.lock()
        try {
            val context = runCatching { AndroidPlatform.requireContext() }.getOrNull() ?: return
            val relative = "${AudioPlayer.DIRECTORY}/$name.wav"

            val cached = File(context.cacheDir, relative)
            if (!cached.exists()) {
                runCatching {
                    cached.parentFile?.mkdirs()
                    context.assets.open(relative).use { input ->
                        cached.outputStream().use(input::copyTo)
                    }
                }.onFailure { return }
            }

            stopLocked()
            val finished = CountDownLatch(1)
            runCatching {
                player = MediaPlayer().apply {
                    setDataSource(cached.absolutePath)
                    setOnCompletionListener { finished.countDown() }
                    prepare()
                    start()
                }
            }

            finished.await(MAX_WAIT_MS, TimeUnit.MILLISECONDS)
            stopLocked()
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

    private companion object {
        /** Sicherheitsnetz: die Aufnahmen sind alle unter einer Sekunde lang. */
        const val MAX_WAIT_MS = 3000L
    }

    private fun stopLocked() {
        player?.let { existing ->
            runCatching {
                existing.stop()
                existing.release()
            }
        }
        player = null
    }
}

actual fun createAudioPlayer(): AudioPlayer = AndroidAudioPlayer()
