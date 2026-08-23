package moe.rorita.kanaschule

import android.app.Application
import android.content.Context

class KanaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
    }

    companion object {
        /**
         * Wird von AppPaths.android benoetigt, um filesDir zu finden, ohne den
         * Context durch die gesamte gemeinsame Codebasis zu schleifen.
         */
        lateinit var appContext: Context
            private set
    }
}
