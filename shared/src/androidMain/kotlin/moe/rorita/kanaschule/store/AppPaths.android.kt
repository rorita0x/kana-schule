package moe.rorita.kanaschule.store

import android.content.Context

/**
 * Hält den Application-Context, damit der gemeinsame Code ohne
 * Context-Parameter an sein Datenverzeichnis kommt. Wird von der
 * Application-Klasse in :androidApp gesetzt.
 */
object AndroidPlatform {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    internal fun requireContext(): Context =
        appContext ?: error("AndroidPlatform.init() wurde nicht aufgerufen")
}

actual fun appDataDir(): String = ensureDir(AndroidPlatform.requireContext().filesDir.toPath())
