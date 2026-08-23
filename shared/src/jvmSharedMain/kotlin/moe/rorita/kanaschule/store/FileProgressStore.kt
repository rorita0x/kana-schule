package moe.rorita.kanaschule.store

import java.io.FileOutputStream
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption

/**
 * Der Lernstand als eine JSON-Datei plus ein Anhaenge-Log.
 *
 * 230 Zeichen mit je einem Dutzend Feldern sind rund 60 Kilobyte, und der
 * Scheduler laedt immer den vollstaendigen Zustand - es gibt keine Abfrage,
 * fuer die ein Index etwas braechte. Der Rohlog waechst dagegen unbegrenzt,
 * deshalb steht er als JSONL daneben und wird nur beim Anhaengen beruehrt.
 */
class FileProgressStore(
    directory: String = appDataDir(),
    private val clock: () -> Long = ::currentTimeMs,
) : ProgressStore {

    private val dir: Path = Paths.get(directory)
    private val stateFile: Path = dir.resolve("state.json")
    private val tempFile: Path = dir.resolve("state.json.tmp")
    private val backupFile: Path = dir.resolve("state.json.bak")
    private val reviewLog: Path = dir.resolve("reviews.jsonl")

    /** Was beim letzten Laden schiefging - fuer eine Meldung im UI. */
    var lastLoadProblem: String? = null
        private set

    override fun load(): AppState {
        lastLoadProblem = null

        when (val result = readFrom(stateFile)) {
            is DecodeResult.Ok -> return result.state
            is DecodeResult.TooNew -> lastLoadProblem =
                "Der Lernstand stammt aus einer neueren Version (Schema ${result.version})."
            is DecodeResult.Broken -> lastLoadProblem = result.reason
            null -> return AppState.fresh(clock())
        }

        // Der Hauptstand ist unbrauchbar: die Sicherung von vor dem letzten
        // Schreiben ist immer noch besser als bei null anzufangen.
        (readFrom(backupFile) as? DecodeResult.Ok)?.let { fromBackup ->
            lastLoadProblem = (lastLoadProblem ?: "") +
                " Die Sicherung von vor dem letzten Schreiben wurde verwendet."
            return fromBackup.state
        }

        return AppState.fresh(clock())
    }

    /**
     * Schreibt in eine Nebendatei, erzwingt das Durchschreiben auf den
     * Datentraeger und benennt dann atomar um. Ohne das zerstoert ein
     * Prozessabbruch mitten im Schreiben den gesamten Lernstand - und genau
     * das ist der Fehlermodus, fuer den man hinterher JSON verantwortlich
     * macht.
     */
    override fun save(state: AppState) {
        Files.createDirectories(dir)
        val payload = ProgressCodec.encode(state).encodeToByteArray()

        FileOutputStream(tempFile.toFile()).use { out ->
            out.write(payload)
            out.flush()
            out.fd.sync()
        }

        if (Files.exists(stateFile)) {
            Files.copy(stateFile, backupFile, StandardCopyOption.REPLACE_EXISTING)
        }

        try {
            Files.move(
                tempFile,
                stateFile,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        } catch (e: IOException) {
            // Manche Dateisysteme koennen ATOMIC_MOVE nicht; dann ohne.
            Files.move(tempFile, stateFile, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    override fun appendReview(entry: ReviewEntry) {
        Files.createDirectories(dir)
        Files.write(
            reviewLog,
            listOf(ProgressCodec.encodeReview(entry)),
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND,
        )
    }

    override fun readReviews(limit: Int): List<ReviewEntry> {
        if (!Files.exists(reviewLog)) return emptyList()
        return Files.readAllLines(reviewLog)
            .takeLast(limit)
            .mapNotNull(ProgressCodec::decodeReview)
    }

    private fun readFrom(path: Path): DecodeResult? {
        if (!Files.exists(path)) return null
        val text = try {
            Files.readString(path)
        } catch (e: IOException) {
            return DecodeResult.Broken(e.message ?: "Datei nicht lesbar")
        }
        return ProgressCodec.decode(text)
    }
}
