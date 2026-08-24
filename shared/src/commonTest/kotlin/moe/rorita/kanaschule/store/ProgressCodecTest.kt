package moe.rorita.kanaschule.store

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import moe.rorita.kanaschule.kana.KanaId
import moe.rorita.kanaschule.srs.ItemState
import moe.rorita.kanaschule.srs.SessionMode
import moe.rorita.kanaschule.srs.Unlock
import moe.rorita.kanaschule.srs.UnlockGroups

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
    fun tagesbudgetLaesstSichVonHandZuruecksetzen() {
        val verbraucht = AppState().withNewItemsToday(10, day = 100)
        assertEquals(0, verbraucht.newItemBudget(100))

        val frei = verbraucht.withResetNewItemsToday(day = 100)
        assertEquals(frei.settings.dailyNewLimit, frei.newItemBudget(100))
        assertEquals(0, frei.unlock.newItemsToday)
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

    @Test
    fun gruppenGrenzeProTagIstEinstellbar() {
        val einePro = AppState()
        assertTrue(einePro.canUnlockToday(100))
        val nachEiner = einePro.withUnlockedGroup("H_KA", day = 100)
        assertTrue(!nachEiner.canUnlockToday(100), "eine pro Tag ist die Voreinstellung")

        val dreiPro = nachEiner.copy(settings = nachEiner.settings.copy(groupsPerDay = 3))
        assertTrue(dreiPro.canUnlockToday(100))
        val nachZwei = dreiPro.withUnlockedGroup("H_SA", day = 100)
        val nachDrei = nachZwei.withUnlockedGroup("H_TA", day = 100)
        assertTrue(!nachDrei.canUnlockToday(100), "nach drei ist Schluss")

        val ohneGrenze = nachDrei.copy(
            settings = nachDrei.settings.copy(groupsPerDay = Unlock.GROUPS_PER_DAY_UNLIMITED),
        )
        assertTrue(ohneGrenze.canUnlockToday(100))
    }

    @Test
    fun vorspulenSchaltetAllesBisZurGruppeFrei() {
        val state = AppState().withUnlockedThrough("H_TA")
        assertEquals(
            listOf("H_A", "H_KA", "H_SA", "H_TA"),
            state.unlock.unlockedGroups,
        )
    }

    @Test
    fun vorspulenBehaeltBereitsOffeneGruppen() {
        val weit = AppState().withUnlockedThrough("H_MA")
        val zurueckGespult = weit.withUnlockedThrough("H_KA")
        assertTrue(
            "H_MA" in zurueckGespult.unlock.unlockedGroups,
            "Vorspulen nimmt nichts weg: ${zurueckGespult.unlock.unlockedGroups}",
        )
    }

    @Test
    fun vorspulenIgnoriertUnbekannteGruppen() {
        val state = AppState()
        assertEquals(state, state.withUnlockedThrough("GIBTS_NICHT"))
    }

    @Test
    fun sperrenNimmtGruppenWiederWeg() {
        val state = AppState().withUnlockedThrough("H_TA").withLockedFrom("H_SA")
        assertEquals(listOf("H_A", "H_KA"), state.unlock.unlockedGroups)
    }

    @Test
    fun gruppeZuruecksetzenMachtDieZeichenWiederUnbekannt() {
        val group = UnlockGroups.byId.getValue("H_KA")
        val gelernt = group.itemIds.associateWith { ItemState(box = 8, reps = 12, streak = 5) }
        val state = AppState().withUnlockedThrough("H_TA").withStates(gelernt)

        val zurueck = state.withGroupReset("H_KA")

        assertTrue(
            group.itemIds.none { zurueck.stateOf(it).seen },
            "Nach dem Zurücksetzen darf kein Zeichen der Gruppe gesehen sein",
        )
        assertTrue(
            group.itemIds.none { it.v in zurueck.items },
            "Der Eintrag muss weg sein, nicht auf Standard gesetzt - sonst zählt es als gesehen",
        )
    }

    @Test
    fun gruppeZuruecksetzenLaesstAlleAnderenInRuhe() {
        val ka = UnlockGroups.byId.getValue("H_KA")
        val sa = UnlockGroups.byId.getValue("H_SA")
        val gelernt = (ka.itemIds + sa.itemIds).associateWith { ItemState(box = 6, reps = 9) }
        val state = AppState().withUnlockedThrough("H_TA").withStates(gelernt)

        val zurueck = state.withGroupReset("H_KA")

        assertTrue(sa.itemIds.all { zurueck.stateOf(it).box == 6 }, "Andere Gruppen bleiben unberührt")
        assertEquals(
            state.unlock.unlockedGroups,
            zurueck.unlock.unlockedGroups,
            "Die Gruppe bleibt freigeschaltet",
        )
    }

    @Test
    fun gruppeZuruecksetzenIgnoriertUnbekannteGruppen() {
        val state = AppState().withUnlockedThrough("H_KA")
        assertEquals(state, state.withGroupReset("GIBTS_NICHT"))
    }

    @Test
    fun dieErsteGruppeLaesstSichNichtSperren() {
        val state = AppState().withUnlockedThrough("H_KA")
        assertEquals(state, state.withLockedFrom("H_A"))
    }

    @Test
    fun einstellungenLaufenDurchDenRundlauf() {
        val angepasst = sample.copy(
            settings = Settings(
                dailyNewLimit = 20,
                groupsPerDay = Unlock.GROUPS_PER_DAY_UNLIMITED,
                reviewSessionLength = 40,
                onScreenKeyboard = true,
                muteAudio = true,
                showWeakestDuringDrill = true,
                strictHepburn = true,
                theme = ThemeMode.LIGHT,
            ),
        )
        val decoded = assertIs<DecodeResult.Ok>(
            ProgressCodec.decode(ProgressCodec.encode(angepasst)),
        )
        assertEquals(angepasst.settings, decoded.state.settings)
    }

    @Test
    fun alteDateienOhneNeueFelderLadenMitStandardwerten() {
        val alt = """{"schemaVersion":1,"createdAtMs":7,"settings":{"strictHepburn":true}}"""
        val decoded = assertIs<DecodeResult.Ok>(ProgressCodec.decode(alt))
        assertTrue(decoded.state.settings.strictHepburn)
        assertEquals(Unlock.MAX_GROUPS_PER_DAY, decoded.state.settings.groupsPerDay)
        assertEquals(false, decoded.state.settings.muteAudio)
        assertEquals(false, decoded.state.settings.showWeakestDuringDrill)
    }
}
