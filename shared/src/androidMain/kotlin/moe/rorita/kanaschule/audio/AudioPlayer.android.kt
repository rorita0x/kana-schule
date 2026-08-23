package moe.rorita.kanaschule.audio

import android.media.MediaPlayer
import moe.rorita.kanaschule.store.AndroidPlatform

/**
 * Die Aufnahmen liegen als Assets. Sie werden beim ersten Abspielen in den
 * Cache kopiert und von dort geoeffnet: ein Asset-Dateideskriptor funktioniert
 * nur, wenn die Datei unkomprimiert im APK liegt, und das haengt an
 * Verpackungsregeln, die wir hier nicht erzwingen wollen.
 */
private class AndroidAudioPlayer : AudioPlayer {

    private var player: MediaPlayer? = null

    override fun play(name: String) {
        val context = runCatching { AndroidPlatform.requireContext() }.getOrNull() ?: return
        val relative = "${AudioPlayer.DIRECTORY}/$name.wav"

        val cached = java.io.File(context.cacheDir, relative)
        if (!cached.exists()) {
            runCatching {
                cached.parentFile?.mkdirs()
                context.assets.open(relative).use { input ->
                    cached.outputStream().use(input::copyTo)
                }
            }.onFailure { return }
        }

        stop()
        runCatching {
            player = MediaPlayer().apply {
                setDataSource(cached.absolutePath)
                prepare()
                start()
            }
        }
    }

    override fun release() {
        stop()
    }

    private fun stop() {
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
