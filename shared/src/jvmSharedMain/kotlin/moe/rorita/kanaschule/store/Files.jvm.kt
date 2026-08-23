package moe.rorita.kanaschule.store

import java.nio.file.Files
import java.nio.file.Path

/**
 * Legt das Verzeichnis samt Elternverzeichnissen an und gibt den absoluten
 * Pfad zurück.
 */
internal fun ensureDir(path: Path): String {
    Files.createDirectories(path)
    return path.toAbsolutePath().toString()
}
