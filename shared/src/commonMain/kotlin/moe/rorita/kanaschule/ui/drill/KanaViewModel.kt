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
import kotlinx.coroutines.launch
import moe.rorita.kanaschule.kana.ConfusionKind
import moe.rorita.kanaschule.kana.Kana
import moe.rorita.kanaschule.kana.KanaExtras
import moe.rorita.kanaschule.kana.KanaId
import moe.rorita.kanaschule.kana.KanaTable
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
import moe.rorita.kanaschule.store.SessionSummary
import moe.rorita.kanaschule.store.ThemeMode
import moe.rorita.kanaschule.store.currentTimeMs
import moe.rorita.kanaschule.store.epochDayOf

/**
 * Haelt den Lernstand und die laufende Session.
 *
 * Der Zustand liegt in Compose-eigenen Snapshot-States statt in einem
 * StateFlow: die App hat genau einen Beobachter, und so bleibt die
 * Coroutine-Nutzung auf das beschraenkt, was sie wirklich braucht - das
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

    /**
     * Eigener Scope statt viewModelScope: der laeuft auf Dispatchers.Main, und
     * den gibt es auf Compose Desktop nur mit kotlinx-coroutines-swing im
     * Klassenpfad. Genutzt wird er ausschliesslich zum Schreiben - das
     * braucht keine Rueckmeldung an die Oberflaeche.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Geladen wird synchron. Die Datei ist wenige Dutzend Kilobyte gross und
     * wird genau einmal gelesen; das nebenlaeufig zu tun war Hoeflichkeit
     * ohne Nutzen und hat die Oberflaeche auf dem Desktop haengen lassen -
     * eine Zustandsaenderung aus einem Hintergrund-Thread erreicht den
     * Recomposer dort nicht zuverlaessig.
     */
    init {
        appState = store.load()
        theme = appState.settings.theme
        ui = DrillUiState(loading = false, home = homeInfo())
    }

    override fun onCleared() {
        scope.cancel()
    }

    // ------------------------------------------------------------- Session

    fun startSession() {
        val now = clock()
        val day = epochDayOf(now)

        unlockedThisSession = maybeUnlock(day)

        val budget = minOf(SessionBuilder.NEW_MAX, appState.newItemBudget(day))
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
        )
        session = drill
        sessionStartMs = now
        missed.clear()

        val readiness = readinessAll()
        val first = drill.start()
        questionStartMs = now

        ui = ui.copy(
            kana = first,
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
            isNewItem = first != null && first.id in drill.newItems,
            result = null,
        )
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

    /** Enter: bestaetigt Feedback oder gibt die Antwort ab. */
    fun submit() {
        if (ui.awaitingContinue) {
            advance()
            return
        }
        answer(ui.typed)
    }

    /** Bewusstes Ueberspringen ist etwas anderes als eine falsche Antwort. */
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
        if (outcome.outcome != Outcome.CORRECT) {
            missed += MissedEntry(kana, Romaji.normalize(typed), outcome.expected)
        }

        ui = ui.copy(
            typed = "",
            feedback = feedback,
            awaitingContinue = outcome.outcome != Outcome.CORRECT,
            asked = drill.asked,
            correct = drill.correct,
            streak = if (outcome.outcome == Outcome.CORRECT) ui.streak + 1 else 0,
            readinessNow = readinessAll(),
            weakest = weakest(drill),
        )
    }

    /**
     * Holt das naechste Zeichen. Bei richtigen Antworten ruft das der
     * Bildschirm nach der kurzen Rueckmeldung auf - nicht die Antwortlogik
     * selbst, sonst wird zweimal geschaltet und eine Frage uebersprungen.
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

    // ------------------------------------------------------------ Ableitungen

    private fun feedbackFor(outcome: moe.rorita.kanaschule.srs.AnswerOutcome, typed: String): Feedback =
        when (outcome.outcome) {
            Outcome.CORRECT -> Feedback.Correct(outcome.hint)
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
        const val WEAKEST_COUNT = 8
        private const val MAX_LATENCY_MS = 120_000L
    }
}

/** Tagesschluessel als ISO-Datum, damit die Datei lesbar bleibt. */
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
