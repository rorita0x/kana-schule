package moe.rorita.kanaschule.store

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import moe.rorita.kanaschule.kana.KanaId
import moe.rorita.kanaschule.srs.ItemState
import moe.rorita.kanaschule.srs.SessionMode

class ProgressCodecTest {

    private val sample = AppState(
        createdAtMs = 1_700_000_000_000L,
        items = mapOf(
            "h.a" to ItemState(box = 4, dueAtMs = 1_700_000_100_000L, reps = 7, streak = 2),
            "k.shi" to ItemState(box = 1, lapses = 3, confusions = mapOf("k.tsu" to 2)),
        ),
        unlock = UnlockState(unlockedGroups = listOf("H_A", "H_KA"), lastUnlockDay = 20_000),
        settings = Settings(strictHepburn = true, theme = ThemeMode.DARK),
        days = mapOf("2026-08-23" to DayAgg(reviews = 30, correct = 27, medianMs = 1800)),
    )

    @Test
    fun rundlaufErhaeltAllesWieEsWar() {
        val decoded = assertIs<DecodeResult.Ok>(ProgressCodec.decode(ProgressCodec.encode(sample)))
        assertEquals(sample, decoded.state)
    }

    @Test
    fun schemaVersionWirdImmerGeschrieben() {
        val text = ProgressCodec.encode(AppState())
        assertTrue(text.contains("\"schemaVersion\":${ProgressCodec.CURRENT_SCHEMA}"), text.take(80))
    }

    @Test
    fun unbekannteFelderStoerenNicht() {
        val text = ProgressCodec.encode(sample)
            .replaceFirst("{", "{\"unbekanntesFeld\":42,")
        val decoded = assertIs<DecodeResult.Ok>(ProgressCodec.decode(text))
        assertEquals(sample, decoded.state)
    }

    @Test
    fun unbekannteZeichenVerschwindenBeimLaden() {
        val withGhost = sample.copy(items = sample.items + ("h.ghost" to ItemState(box = 5)))
        val decoded = assertIs<DecodeResult.Ok>(
            ProgressCodec.decode(ProgressCodec.encode(withGhost)),
        )
        assertEquals(sample.items.keys, decoded.state.items.keys)
    }

    @Test
    fun neueresSchemaWirdNichtErraten() {
        val text = ProgressCodec.encode(sample)
            .replaceFirst("\"schemaVersion\":1", "\"schemaVersion\":99")
        val result = assertIs<DecodeResult.TooNew>(ProgressCodec.decode(text))
        assertEquals(99, result.version)
    }

    @Test
    fun kaputtesJsonMeldetSichAlsKaputt() {
        assertIs<DecodeResult.Broken>(ProgressCodec.decode("{ das ist kein json"))
        assertIs<DecodeResult.Broken>(ProgressCodec.decode(""))
        assertIs<DecodeResult.Broken>(ProgressCodec.decode("   "))
    }

    @Test
    fun abgeschnitteneDateiMeldetSichAlsKaputt() {
        val text = ProgressCodec.encode(sample)
        assertIs<DecodeResult.Broken>(ProgressCodec.decode(text.take(text.length / 2)))
    }

    @Test
    fun fehlendeSchemaVersionGiltAlsEins() {
        val decoded = assertIs<DecodeResult.Ok>(
            ProgressCodec.decode("""{"createdAtMs":5,"items":{}}"""),
        )
        assertEquals(5L, decoded.state.createdAtMs)
    }

    @Test
    fun antwortenLaufenEinzelnDurch() {
        val entry = ReviewEntry(
            t = 1_700_000_000_000L,
            id = "k.shi",
            outcome = Outcome.CONFUSED,
            ms = 2100,
            typed = "tsu",
            boxBefore = 5,
            boxAfter = 2,
            mode = SessionMode.REVIEW,
            confusedWith = "k.tsu",
        )
        assertEquals(entry, ProgressCodec.decodeReview(ProgressCodec.encodeReview(entry)))
    }

    @Test
    fun kaputteLogzeileWirdUebersprungen() {
        assertNull(ProgressCodec.decodeReview("{ halb"))
        assertNull(ProgressCodec.decodeReview(""))
    }

    // ------------------------------------------------------ Zustandshelfer

    @Test
    fun tagesbudgetLaeuftAmTageswechselZurueck() {
        val state = AppState().withNewItemsToday(4, day = 100)
        assertEquals(4, state.unlock.newItemsToday)
        assertEquals(
            state.settings.dailyNewLimit - 4,
            state.newItemBudget(100),
        )
        assertEquals(state.settings.dailyNewLimit, state.newItemBudget(101))

        val nextDay = state.withNewItemsToday(3, day = 101)
        assertEquals(3, nextDay.unlock.newItemsToday, "am neuen Tag wird neu gezählt")
    }

    @Test
    fun nurEineGruppeProTag() {
        val state = AppState()
        assertTrue(state.canUnlockToday(100))
        val unlocked = state.withUnlockedGroup("H_KA", day = 100)
        assertTrue(!unlocked.canUnlockToday(100))
        assertTrue(unlocked.canUnlockToday(101))
        assertEquals(listOf("H_A", "H_KA"), unlocked.unlock.unlockedGroups)
    }

    @Test
    fun zustandsmapNutztKanaIds() {
        assertEquals(ItemState(box = 4, dueAtMs = 1_700_000_100_000L, reps = 7, streak = 2), sample.stateOf(KanaId("h.a")))
        assertEquals(ItemState(), sample.stateOf(KanaId("h.no")))
        assertEquals(sample.items.size, sample.states.size)
    }

    @Test
    fun sessionlisteWirdGedeckelt() {
        var state = AppState()
        repeat(AppState.MAX_SESSIONS + 20) { index ->
            state = state.withSession(
                SessionSummary(
                    startedAtMs = index.toLong(),
                    endedAtMs = index.toLong(),
                    mode = SessionMode.REVIEW,
                    asked = 30,
                    correct = 28,
                    promoted = 5,
                    demoted = 1,
                    readinessBefore = 10,
                    readinessAfter = 12,
                    medianMs = 1500,
                ),
            )
        }
        assertEquals(AppState.MAX_SESSIONS, state.sessions.size)
        assertEquals(20L, state.sessions.first().startedAtMs, "die ältesten fallen weg")
    }
}
