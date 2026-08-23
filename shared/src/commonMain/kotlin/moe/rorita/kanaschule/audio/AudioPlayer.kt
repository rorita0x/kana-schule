package moe.rorita.kanaschule.audio

/**
 * Spielt eine der gebündelten Aussprache-Aufnahmen.
 *
 * Die Dateien liegen einmal unter shared/media/audio; das Desktop-Ziel
 * bindet sie als Klassenpfad-Ressource ein, Android als Asset. Namen sind
 * die Slugs aus der Kana-Tabelle, also „ka“, „shi“, „kya“, „di“.
 *
 * Hiragana und Katakana klingen gleich, deshalb genügt eine Datei je Lesung.
 */
interface AudioPlayer {
    fun play(name: String)

    fun release()

    companion object {
        /** Nur eine Aufnahme läuft gleichzeitig. */
        const val DIRECTORY = "audio"
    }
}

expect fun createAudioPlayer(): AudioPlayer

/** Tut nichts - für Tests und für Zeichen ohne Aufnahme. */
object SilentAudioPlayer : AudioPlayer {
    override fun play(name: String) = Unit
    override fun release() = Unit
}
