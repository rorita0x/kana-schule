package moe.rorita.kanaschule.ui.drill

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import kotlin.random.Random
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import moe.rorita.kanaschule.kana.ConfusionKind
import moe.rorita.kanaschule.kana.Kana
import moe.rorita.kanaschule.kana.KanaExtras
import moe.rorita.kanaschule.kana.KanaId
import moe.rorita.kanaschule.kana.KanaTable
import moe.rorita.kanaschule.kana.Pronunciation
import moe.rorita.kanaschule.kana.Romaji
import moe.rorita.kanaschule.kana.Script
import moe.rorita.kanaschule.kana.Verdict
import moe.rorita.kanaschule.srs.DrillSession
import moe.rorita.kanaschule.srs.ItemState
import moe.rorita.kanaschule.srs.Readiness
import moe.rorita.kanaschule.srs.SessionBuilder
import moe.rorita.kanaschule.srs.SessionMode
import moe.rorita.kanaschule.srs.Unlock
import moe.rorita.kanaschule.srs.UnlockGroups
import moe.rorita.kanaschule.store.AppState
import moe.rorita.kanaschule.store.DayAgg
import moe.rorita.kanaschule.store.Outcome
import moe.rorita.kanaschule.store.ProgressStore
import moe.rorita.kanaschule.store.ReviewEntry
import moe.rorita.kanaschule.store.Settings
import moe.rorita.kanaschule.store.SessionSummary
import moe.rorita.kanaschule.store.ThemeMode
import moe.rorita.kanaschule.store.currentTimeMs
import moe.rorita.kanaschule.store.epochDayOf
import moe.rorita.kanaschule.audio.AudioPlayer
import moe.rorita.kanaschule.audio.createAudioPlayer
import moe.rorita.kanaschule.ui.learn.LearnCard
import moe.rorita.kanaschule.ui.learn.LearnState

/**
 * Hält den Lernstand und die laufende Session.
 *
 * Der Zustand liegt in Compose-eigenen Snapshot-States statt in einem
 * StateFlow: die App hat genau einen Beobachter, und so bleibt die
 * Coroutine-Nutzung auf das beschränkt, was sie wirklich braucht - das
 * Schreiben auf die Platte abseits des Hauptthreads.
 */
class KanaViewModel(
    private val store: ProgressStore = moe.rorita.kanaschule.store.defaultProgressStore(),
    private val clock: () -> Long = ::currentTimeMs,
) : ViewModel() {

    var ui: DrillUiState by mutableStateOf(DrillUiState())
        private set

    var theme: ThemeMode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    private var appState: AppState = AppState()
    private var session: DrillSession? = null
    private var questionStartMs: Long = 0L
    private var sessionStartMs: Long = 0L
    private var unlockedThisSession: String? = null
    private val missed = ArrayList<MissedEntry>()
    private val audio: AudioPlayer = createAudioPlayer()

    /** Die Zeichen, die gerade vorgestellt werden. */
    private var learnQueue: List<Kana> = emptyList()
    private var learnIndex: Int = 0

    /** true, wenn der Lernmodus aus dem Hauptmenü kommt. */
    private var learnStandalone: Boolean = false
    private var learnShowAll: Boolean = false

    /**
     * Eigener Scope statt viewModelScope: der läuft auf Dispatchers.Main, und
     * den gibt es auf Compose Desktop nur mit kotlinx-coroutines-swing im
     * Klassenpfad. Genutzt wird er ausschliesslich zum Schreiben - das
     * braucht keine Rückmeldung an die Oberfläche.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Abspielwünsche laufen ueber einen einzigen Verbraucher, damit sich zwei
     * nie ins Gehege kommen. CONFLATED heißt: bei mehreren Wünschen gewinnt
     * der neueste, statt dass einer verworfen wird - wer schnell blättert,
     * hört das Zeichen, auf dem er stehen bleibt.
     */
    private val audioRequests = Channel<String>(Channel.CONFLATED)

    /**
     * Geladen wird synchron. Die Datei ist wenige Dutzend Kilobyte groß und
     * wird genau einmal gelesen; das nebenläufig zu tun war Höflichkeit
     * ohne Nutzen und hat die Oberfläche auf dem Desktop hängen lassen -
     * eine Zustandsänderung aus einem Hintergrund-Thread erreicht den
     * Recomposer dort nicht zuverlässig.
     */
    init {
        scope.launch {
            for (name in audioRequests) audio.play(name)
        }
        appState = store.load()
        theme = appState.settings.theme
        ui = DrillUiState(
            loading = false,
            home = homeInfo(),
            muted = appState.settings.muteAudio,
            settings = appState.settings,
        )
    }

    override fun onCleared() {
        audio.release()
        scope.cancel()
    }

    // ------------------------------------------------------------- Session

    fun startSession() {
        val now = clock()
        val day = epochDayOf(now)

        unlockedThisSession = maybeUnlock(day)

        // Mehr als eine Handvoll neuer Zeichen ertränkt eine Runde, auch wenn
        // das Tagesbudget mehr erlaubt.
        val budget = minOf(NEW_PER_SESSION_MAX, appState.newItemBudget(day))
        val seed = day * 31 + appState.sessions.size
        val plan = SessionBuilder.review(
            unlockedGroups = appState.unlockedGroups,
            states = appState.states,
            nowMs = now,
            random = Random(seed),
            newBudget = budget,
            targetSize = appState.settings.reviewSessionLength,
        )

        if (plan.items.isEmpty()) {
            ui = ui.copy(home = homeInfo())
            return
        }

        appState = appState.withNewItemsToday(plan.newItems.size, day)

        val drill = DrillSession(
            plan = plan,
            initialStates = appState.states,
            random = Random(seed + 1),
            targetAnswers = appState.settings.reviewSessionLength,
            strictHepburn = appState.settings.strictHepburn,
        )
        session = drill
        sessionStartMs = now
        missed.clear()

        val readiness = readinessAll()
        learnQueue = plan.newItems.map(KanaTable::require)
        learnIndex = 0
        learnStandalone = false

        ui = ui.copy(
            learn = null,
            kana = null,
            typed = "",
            feedback = null,
            awaitingContinue = false,
            asked = 0,
            correct = 0,
            target = minOf(appState.settings.reviewSessionLength, plan.items.size),
            streak = 0,
            readinessAtStart = readiness,
            readinessNow = readiness,
            weakest = emptyList(),
            isNewItem = false,
            result = null,
        )

        if (learnQueue.isEmpty()) beginDrill() else showLearnCard()
    }

    // --------------------------------------------------------------- Lernen

    /**
     * Lernmodus aus dem Hauptmenü: zum Blättern, ohne dass danach abgefragt
     * wird. Ohne Haken sind es die noch nicht gelernten Zeichen der
     * freigeschalteten Gruppen - das ist das, was als Nächstes dran ist.
     */
    fun startLearning(showAll: Boolean = learnShowAll) {
        learnStandalone = true
        learnShowAll = showAll
        learnQueue = learnScope()
        learnIndex = 0

        if (learnQueue.isEmpty()) {
            // Alles der freigeschalteten Gruppen schon gesehen: dann zeigen
            // wir eben alles, statt eine leere Liste anzubieten.
            learnShowAll = true
            learnQueue = learnScope()
        }
        if (learnQueue.isEmpty()) return
        showLearnCard()
    }

    /**
     * Eine Karte je Lesung, nicht je Zeichen: die Karte zeigt ohnehin beide
     * Schriften, sonst käme jedes Paar zweimal. Entschieden wird nach Slug
     * und nicht nach der Lesung, weil じ und ぢ beide "ji" sind.
     */
    private fun learnScope(): List<Kana> {
        val singles = KanaTable.singles
        if (learnShowAll) return singles.distinctBy(::slugOf)

        val unlocked = appState.unlockedItems
        val unseen = singles.filter { it.id in unlocked && !appState.stateOf(it.id).seen }
        return unseen.distinctBy(::slugOf)
            .ifEmpty { singles.filter { it.id in unlocked }.distinctBy(::slugOf) }
    }

    private fun slugOf(kana: Kana): String = kana.id.v.substringAfter('.')

    fun toggleShowAll() {
        if (!learnStandalone) return
        val current = ui.learn?.card?.hiragana ?: ui.learn?.card?.katakana
        startLearning(showAll = !learnShowAll)
        // Nach Möglichkeit beim gerade gezeigten Zeichen bleiben.
        current?.let { kana ->
            val index = learnQueue.indexOfFirst { slugOf(it) == slugOf(kana) }
            if (index >= 0) {
                learnIndex = index
                showLearnCard()
            }
        }
    }

    fun toggleMute() = updateSettings { it.copy(muteAudio = !it.muteAudio) }

    private fun showLearnCard() {
        ui = ui.copy(
            learn = LearnState(
                card = cardFor(learnQueue[learnIndex]),
                showAll = learnShowAll,
                muted = appState.settings.muteAudio,
                standalone = learnStandalone,
            ),
            kana = null,
        )
    }

    /** Weiter zur nächsten Karte, danach beginnt das Abfragen. */
    fun nextLearnCard() {
        if (learnIndex + 1 >= learnQueue.size) {
            if (learnStandalone) {
                leaveLearning()
            } else {
                ui = ui.copy(learn = null)
                beginDrill()
            }
            return
        }
        learnIndex++
        showLearnCard()
    }

    fun previousLearnCard() {
        if (learnIndex == 0) return
        learnIndex--
        showLearnCard()
    }

    fun leaveLearning() {
        learnQueue = emptyList()
        learnIndex = 0
        learnStandalone = false
        ui = ui.copy(learn = null, home = homeInfo())
    }

    /**
     * Abspielen gehört nicht auf den Oberflächen-Thread: eine Tonleitung zu
     * öffnen dauert und kann blockieren, und aus einem Klick-Handler heraus
     * friert das die App ein.
     */
    fun playCurrentAudio() {
        val name = ui.learn?.card?.audioName ?: return
        audioRequests.trySend(name)
    }

    /**
     * Beide Schriften auf eine Karte: し und シ sind dieselbe Lesung, und die
     * Verbindung einmal gesehen zu haben kostet nichts.
     */
    private fun cardFor(kana: Kana): LearnCard {
        val partner = kana.partnerId?.let(KanaTable::require)
        val hiragana = listOfNotNull(kana, partner).firstOrNull { it.script == Script.HIRAGANA }
        val katakana = listOfNotNull(kana, partner).firstOrNull { it.script == Script.KATAKANA }
        return LearnCard(
            hiragana = hiragana,
            katakana = katakana,
            romaji = kana.canonical,
            hint = Pronunciation.of(kana),
            audioName = Pronunciation.audioName(kana),
            noteDe = kana.noteDe,
            index = learnIndex,
            total = learnQueue.size,
        )
    }

    private fun beginDrill() {
        val drill = session ?: return
        val first = drill.start()
        questionStartMs = clock()
        ui = ui.copy(
            learn = null,
            kana = first,
            typed = "",
            feedback = null,
            awaitingContinue = false,
            isNewItem = first != null && first.id in drill.newItems,
        )
        if (first == null) finish(drill)
    }

    private fun maybeUnlock(day: Long): String? {
        if (!appState.canUnlockToday(day)) return null
        val group = Unlock.nextGroup(appState.unlockedGroups, appState.states, clock())
            ?: return null
        appState = appState.withUnlockedGroup(group.id, day)
        return group.labelDe
    }

    // --------------------------------------------------------------- Eingabe

    fun type(char: Char) {
        if (ui.awaitingContinue || ui.kana == null) return
        if (ui.typed.length >= MAX_INPUT) return
        ui = ui.copy(typed = ui.typed + char, feedback = null)
    }

    fun backspace() {
        if (ui.awaitingContinue || ui.typed.isEmpty()) return
        ui = ui.copy(typed = ui.typed.dropLast(1), feedback = null)
    }

    fun clearInput() {
        if (ui.awaitingContinue) return
        ui = ui.copy(typed = "", feedback = null)
    }

    /** Enter: bestätigt Feedback oder gibt die Antwort ab. */
    fun submit() {
        if (ui.awaitingContinue) {
            advance()
            return
        }
        answer(ui.typed)
    }

    /** Bewusstes Überspringen ist etwas anderes als eine falsche Antwort. */
    fun skip() {
        if (ui.awaitingContinue) return
        answer("")
    }

    private fun answer(typed: String) {
        val drill = session ?: return
        val kana = ui.kana ?: return
        val now = clock()
        val latency = (now - questionStartMs).coerceIn(0, MAX_LATENCY_MS).toInt()

        val outcome = drill.submit(typed, latency, now)

        if (outcome.outcome == Outcome.TYPO) {
            ui = ui.copy(typed = "", feedback = Feedback.Typo(outcome.expected))
            return
        }

        appState = appState.withStates(drill.changedStates)
        persist(
            ReviewEntry(
                t = now,
                id = kana.id.v,
                outcome = outcome.outcome,
                ms = latency,
                typed = Romaji.normalize(typed),
                boxBefore = outcome.boxBefore,
                boxAfter = outcome.boxAfter,
                mode = drill.mode,
                confusedWith = outcome.confusedWith?.v,
            ),
        )

        val feedback = feedbackFor(outcome, typed)
        val counted = outcome.outcome != Outcome.CORRECT && !outcome.introduction
        if (counted) {
            missed += MissedEntry(kana, Romaji.normalize(typed), outcome.expected)
        }
        // Stehen bleiben muss alles, was nicht gesessen hat - auch beim
        // Erstkontakt, denn 500 Millisekunden reichen nicht, um eine Lösung zu
        // lesen. Ein richtiger Erstkontakt läuft dagegen einfach durch.
        val sat = outcome.verdict is Verdict.Correct

        ui = ui.copy(
            typed = "",
            feedback = feedback,
            awaitingContinue = !sat,
            asked = drill.asked,
            introduced = drill.introduced,
            correct = drill.correct,
            streak = if (outcome.outcome == Outcome.CORRECT) ui.streak + 1 else 0,
            readinessNow = readinessAll(),
            weakest = weakest(drill),
        )
    }

    /**
     * Holt das nächste Zeichen. Bei richtigen Antworten ruft das der
     * Bildschirm nach der kurzen Rückmeldung auf - nicht die Antwortlogik
     * selbst, sonst wird zweimal geschaltet und eine Frage übersprungen.
     */
    fun advance() {
        val drill = session ?: return
        val next = drill.advance()
        questionStartMs = clock()

        if (next == null) {
            finish(drill)
            return
        }

        ui = ui.copy(
            kana = next,
            typed = "",
            feedback = null,
            awaitingContinue = false,
            isNewItem = next.id in drill.newItems,
        )
    }

    fun dismissFeedback() {
        if (ui.feedback is Feedback.Correct || ui.feedback is Feedback.Typo) {
            ui = ui.copy(feedback = null)
        }
    }

    fun abandonSession() {
        val drill = session ?: return
        finish(drill)
    }

    private fun finish(drill: DrillSession) {
        val now = clock()
        val day = epochDayOf(now)
        val readinessAfter = readinessAll()

        val summary = SessionSummary(
            startedAtMs = sessionStartMs,
            endedAtMs = now,
            mode = SessionMode.REVIEW,
            asked = drill.asked,
            correct = drill.correct,
            promoted = drill.promoted,
            demoted = drill.demoted,
            readinessBefore = ui.readinessAtStart,
            readinessAfter = readinessAfter,
            medianMs = drill.medianLatencyMs,
        )

        appState = appState
            .withStates(drill.changedStates)
            .withSession(summary)
            .withDay(day, drill, readinessAfter, appState.states)

        persist(null)

        ui = ui.copy(
            kana = null,
            typed = "",
            feedback = null,
            awaitingContinue = false,
            readinessNow = readinessAfter,
            home = homeInfo(),
            result = SessionResult(
                asked = drill.asked,
                correct = drill.correct,
                promoted = drill.promoted,
                demoted = drill.demoted,
                readinessBefore = ui.readinessAtStart,
                readinessAfter = readinessAfter,
                medianMs = drill.medianLatencyMs,
                missed = missed.toList(),
                topConfusion = topConfusion(drill),
                unlockedGroupLabel = unlockedThisSession,
                newItems = drill.newItems.size,
            ),
        )
        session = null
    }

    fun leaveSummary() {
        ui = ui.copy(result = null, home = homeInfo())
    }

    // ----------------------------------------------------------- Einstellungen

    fun openSettings() {
        ui = ui.copy(settingsOpen = true, settings = appState.settings, groups = groupInfos())
    }

    fun closeSettings() {
        ui = ui.copy(settingsOpen = false, home = homeInfo())
    }

    fun updateSettings(change: (Settings) -> Settings) {
        val settings = change(appState.settings)
        appState = appState.copy(settings = settings)
        theme = settings.theme
        persist(null)
        ui = ui.copy(
            settings = settings,
            muted = settings.muteAudio,
            learn = ui.learn?.copy(muted = settings.muteAudio),
            home = homeInfo(),
        )
    }

    /**
     * Alles bis zu dieser Gruppe aufmachen. Wer schon bis か kommt, soll nicht
     * bei あ anfangen müssen - und auch nicht vier Tage auf die Freischaltung
     * warten.
     */
    fun unlockThrough(groupId: String) {
        appState = appState.withUnlockedThrough(groupId)
        persist(null)
        ui = ui.copy(groups = groupInfos(), home = homeInfo())
    }

    fun lockFrom(groupId: String) {
        appState = appState.withLockedFrom(groupId)
        persist(null)
        ui = ui.copy(groups = groupInfos(), home = homeInfo())
    }

    private fun groupInfos(): List<GroupInfo> {
        val unlocked = appState.unlockedGroups
        val states = appState.states
        return UnlockGroups.ordered.map { group ->
            GroupInfo(
                id = group.id,
                labelDe = group.labelDe,
                scriptDe = if (group.script == Script.HIRAGANA) "Hiragana" else "Katakana",
                itemCount = group.itemIds.size,
                unlocked = group.id in unlocked,
                seenCount = group.itemIds.count { appState.stateOf(it).seen },
                mastered = Unlock.isMastered(group, states),
            )
        }
    }

    // ------------------------------------------------------------ Ableitungen

    private fun feedbackFor(outcome: moe.rorita.kanaschule.srs.AnswerOutcome, typed: String): Feedback =
        when (outcome.outcome) {
            Outcome.CORRECT -> Feedback.Correct(outcome.hint)
            Outcome.INTRODUCED -> Feedback.Introduced(
                expected = outcome.expected,
                typed = Romaji.normalize(typed),
                wasCorrect = outcome.verdict is Verdict.Correct,
            )
            Outcome.SKIPPED -> Feedback.Skipped(outcome.expected)
            Outcome.TYPO -> Feedback.Typo(outcome.expected)
            Outcome.CONFUSED, Outcome.WRONG -> {
                val other = outcome.confusedWith?.let(KanaTable::require)
                Feedback.Wrong(
                    expected = outcome.expected,
                    typed = Romaji.normalize(typed),
                    confusedWith = other,
                    discriminator = other?.let { KanaExtras.discriminator(outcome.kana.glyph, it.glyph) },
                    explanation = explain(outcome.verdict, outcome.kana, other),
                )
            }
        }

    private fun explain(verdict: Verdict, target: Kana, other: Kana?): String? {
        val confusion = verdict as? Verdict.Confused ?: return null
        val partner = other ?: return null
        return when (confusion.kind) {
            ConfusionKind.DAKUTEN ->
                "${target.glyph} ist ${partner.glyph} mit Dakuten - stimmhaft: ${target.canonical}."
            ConfusionKind.YOON_BASE ->
                "${target.glyph} ist ${partner.glyph} mit kleinem Zeichen: ${target.canonical}."
            ConfusionKind.VOWEL_ROW ->
                "Richtige Zeile, falscher Vokal: ${partner.glyph} ist ${partner.canonical}."
            ConfusionKind.CONSONANT_COL ->
                "Richtiger Vokal, falscher Konsonant: ${partner.glyph} ist ${partner.canonical}."
            ConfusionKind.VISUAL, ConfusionKind.UNRELATED -> null
        }
    }

    private fun weakest(drill: DrillSession): List<WeakEntry> =
        drill.weakest(WEAKEST_COUNT).map { (id, state) ->
            WeakEntry(KanaTable.require(id), state.accuracy20, state.box)
        }

    private fun topConfusion(drill: DrillSession): Pair<Kana, Kana>? {
        val best = drill.changedStates.entries
            .flatMap { (id, state) -> state.confusions.map { Triple(id, KanaId(it.key), it.value) } }
            .maxByOrNull { it.third } ?: return null
        return KanaTable.require(best.first) to KanaTable.require(best.second)
    }

    private fun readinessAll(): Int =
        Readiness.percent(KanaTable.all, currentStates(), clock())

    private fun currentStates(): Map<KanaId, ItemState> =
        session?.states ?: appState.states

    private fun homeInfo(): HomeInfo {
        val now = clock()
        val states = appState.states
        val unlocked = appState.unlockedItems
        val day = epochDayOf(now)

        return HomeInfo(
            readinessAll = Readiness.percent(KanaTable.all, states, now),
            readinessHiragana = Readiness.percent(
                KanaTable.byScript.getValue(Script.HIRAGANA), states, now,
            ),
            readinessKatakana = Readiness.percent(
                KanaTable.byScript.getValue(Script.KATAKANA), states, now,
            ),
            readinessUnlocked = Readiness.percentOfUnlocked(KanaTable.all, states, unlocked, now),
            dueNow = unlocked.count { id ->
                val state = states[id] ?: return@count false
                state.reps > 0 && state.dueAtMs <= now
            },
            unlockedItems = unlocked.size,
            totalItems = KanaTable.all.size,
            currentGroupLabel = appState.unlock.unlockedGroups.lastOrNull()
                ?.let { UnlockGroups.byId[it]?.labelDe } ?: "",
            newItemsAvailable = unlocked.count { (states[it]?.reps ?: 0) == 0 },
            dayStreak = dayStreak(day),
            loadProblem = store.lastLoadProblem,
        )
    }

    private fun dayStreak(today: Long): Int {
        var streak = 0
        var day = today
        while (appState.days.containsKey(dayKey(day))) {
            streak++
            day--
        }
        return streak
    }

    private fun persist(entry: ReviewEntry?) {
        val snapshot = appState
        scope.launch {
            if (entry != null) store.appendReview(entry)
            store.save(snapshot)
        }
    }

    companion object {
        const val MAX_INPUT = 12

        /** Obergrenze für neue Zeichen in einer einzelnen Runde. */
        const val NEW_PER_SESSION_MAX = 6
        const val WEAKEST_COUNT = 8
        private const val MAX_LATENCY_MS = 120_000L
    }
}

/** Tagesschlüssel als ISO-Datum, damit die Datei lesbar bleibt. */
internal fun dayKey(epochDay: Long): String = epochDay.toString()

private fun AppState.withDay(
    day: Long,
    drill: DrillSession,
    readinessAfter: Int,
    states: Map<KanaId, ItemState>,
): AppState {
    val key = dayKey(day)
    val previous = days[key] ?: DayAgg()
    val now = currentTimeMs()
    return copy(
        days = days + (
            key to previous.copy(
                reviews = previous.reviews + drill.asked,
                correct = previous.correct + drill.correct,
                newItems = previous.newItems + drill.newItems.size,
                medianMs = drill.medianLatencyMs,
                readinessAll = readinessAfter,
                readinessHiragana = Readiness.percent(
                    KanaTable.byScript.getValue(Script.HIRAGANA), states, now,
                ),
                readinessKatakana = Readiness.percent(
                    KanaTable.byScript.getValue(Script.KATAKANA), states, now,
                ),
            )
            ),
    )
}
