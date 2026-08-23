package moe.rorita.kanaschule.store

import java.nio.file.Path
import java.nio.file.Paths

actual fun appDataDir(): String {
    val xdg = System.getenv("XDG_DATA_HOME")?.takeIf { it.isNotBlank() }
    val base: Path = if (xdg != null) {
        Paths.get(xdg)
    } else {
        Paths.get(System.getProperty("user.home"), ".local", "share")
    }
    return ensureDir(base.resolve("kana-schule"))
}
