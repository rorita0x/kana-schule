package moe.rorita.kanaschule

import android.app.Application
import moe.rorita.kanaschule.store.AndroidPlatform

class KanaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidPlatform.init(this)
    }
}
