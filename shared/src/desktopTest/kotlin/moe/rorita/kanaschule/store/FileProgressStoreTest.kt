package moe.rorita.kanaschule.store

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.deleteRecursively
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import moe.rorita.kanaschule.srs.ItemState
import moe.rorita.kanaschule.srs.SessionMode

class FileProgressStoreTest {

    private lateinit var dir: Path
    private lateinit var store: FileProgressStore

    private val now = 1_700_000_000_000L

    private val sample = AppState(
        createdAtMs = now,
        items = mapOf("h.a" to ItemState(box = 4, reps = 9, streak = 3)),
        unlock = UnlockState(unlockedGroups = listOf("H_A", "H_KA")),
    )

    @BeforeTest
    fun setUp() {
        dir = Files.createTempDirectory("kana-schule-test")
        store = FileProgressStore(dir.toString()) { now }
    }

    @OptIn(kotlin.io.path.ExperimentalPathApi::class)
    @AfterTest
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun stateFile(): Path = dir.resolve("state.json")

    @Test
    fun leeresVerzeichnisGibtFrischenZustand() {
        val loaded = store.load()
        assertEquals(now, loaded.createdAtMs)
        assertTrue(loaded.items.isEmpty())
        assertEquals(listOf(UnlockState.FIRST_GROUP), loaded.unlock.unlockedGroups)
        assertNull(store.lastLoadProblem)
    }

    @Test
    fun rundlaufUeberDieDatei() {
        store.save(sample)
        assertEquals(sample, store.load())
        assertNull(store.lastLoadProblem)
    }

    @Test
    fun schreibenLaesstKeineNebendateiZurueck() {
        store.save(sample)
        assertFalse(Files.exists(dir.resolve("state.json.tmp")), "tmp muss weg sein")
        assertTrue(Files.exists(stateFile()))
    }

    @Test
    fun zweitesSchreibenLegtEineSicherungAn() {
        store.save(sample)
        assertFalse(Files.exists(dir.resolve("state.json.bak")), "beim ersten Mal gibt es nichts zu sichern")

        store.save(sample.copy(createdAtMs = now + 1))
        assertTrue(Files.exists(dir.resolve("state.json.bak")))
        assertEquals(now + 1, store.load().createdAtMs)
    }

    @Test
    fun abgeschnittenerZustandFaelltAufDieSicherungZurueck() {
        store.save(sample)
        store.save(sample.copy(items = sample.items + ("h.i" to ItemState(box = 2))))

        // Prozessabbruch mitten im Schreiben simulieren.
        val text = Files.readString(stateFile())
        Files.writeString(stateFile(), text.take(text.length / 2))

        val recovered = store.load()
        assertEquals(sample, recovered, "die Sicherung ist der Stand von vor dem letzten Schreiben")
        val problem = assertNotNull(store.lastLoadProblem)
        assertTrue(problem.contains("Sicherung"), problem)
    }

    @Test
    fun ohneSicherungGibtEsEinenFrischenZustand() {
        store.save(sample)
        Files.writeString(stateFile(), "{ kaputt")

        val recovered = store.load()
        assertTrue(recovered.items.isEmpty())
        assertNotNull(store.lastLoadProblem)
    }

    @Test
    fun neueresSchemaWirdGemeldetUndNichtUeberschrieben() {
        store.save(sample)
        val text = Files.readString(stateFile())
        Files.writeString(stateFile(), text.replaceFirst("\"schemaVersion\":1", "\"schemaVersion\":42"))

        store.load()
        val problem = assertNotNull(store.lastLoadProblem)
        assertTrue(problem.contains("neueren Version"), problem)
    }

    @Test
    fun antwortenWerdenAngehaengtUndGelesen() {
        val entries = (1..5).map { index ->
            ReviewEntry(
                t = now + index,
                id = "h.a",
                outcome = Outcome.CORRECT,
                ms = 1000 + index,
                typed = "a",
                boxBefore = index,
                boxAfter = index + 1,
                mode = SessionMode.REVIEW,
            )
        }
        entries.forEach(store::appendReview)

        assertEquals(entries, store.readReviews())
        assertEquals(entries.takeLast(2), store.readReviews(limit = 2))
    }

    @Test
    fun logOhneDateiIstLeer() {
        assertTrue(store.readReviews().isEmpty())
    }

    @Test
    fun kaputteLogzeilenWerdenUebersprungen() {
        val entry = ReviewEntry(
            t = now,
            id = "h.a",
            outcome = Outcome.WRONG,
            ms = 4000,
            typed = "o",
            boxBefore = 3,
            boxAfter = 1,
            mode = SessionMode.REVIEW,
        )
        store.appendReview(entry)
        Files.writeString(
            dir.resolve("reviews.jsonl"),
            "{ halbe zeile\n",
            java.nio.file.StandardOpenOption.APPEND,
        )
        store.appendReview(entry.copy(t = now + 1))

        assertEquals(2, store.readReviews().size)
    }

    @Test
    fun logUeberlebtDasNeuschreibenDesZustands() {
        val entry = ReviewEntry(
            t = now,
            id = "h.a",
            outcome = Outcome.CORRECT,
            ms = 900,
            typed = "a",
            boxBefore = 0,
            boxAfter = 1,
            mode = SessionMode.REVIEW,
        )
        store.appendReview(entry)
        store.save(sample)
        store.save(sample.copy(createdAtMs = now + 5))

        assertEquals(listOf(entry), store.readReviews())
    }
}
