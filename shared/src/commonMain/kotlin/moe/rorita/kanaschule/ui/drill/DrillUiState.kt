package moe.rorita.kanaschule.ui.drill

import androidx.compose.runtime.Immutable
import moe.rorita.kanaschule.kana.Kana
import moe.rorita.kanaschule.kana.KanaId
import moe.rorita.kanaschule.store.Settings
import moe.rorita.kanaschule.ui.learn.LearnState

/** Eine Gruppe der Freischaltleiter, wie sie in den Einstellungen erscheint. */
@Immutable
data class GroupInfo(
    val id: String,
    val labelDe: String,
    val scriptDe: String,
    val itemCount: Int,
    val unlocked: Boolean,
    val seenCount: Int,
    val mastered: Boolean,
)

/** Was nach einer Antwort angezeigt wird. */
@Immutable
sealed interface Feedback {
    /** [hint] steht nur da, wenn die Antwort nicht Hepburn war. */
    data class Correct(val hint: String?) : Feedback

    data class Wrong(
        val expected: String,
        val typed: String,
        val confusedWith: Kana?,
        val discriminator: String?,
        val explanation: String?,
    ) : Feedback

    data class Skipped(val expected: String) : Feedback

    /** Neutral, zählt nicht - der Lernende tippt einfach nochmal. */
    data class Typo(val nearest: String) : Feedback

    /** Erstkontakt direkt nach der Karte: eingeprägt, nicht geprüft. */
    data class Introduced(
        val expected: String,
        val typed: String,
        val wasCorrect: Boolean,
    ) : Feedback
}

@Immutable
data class WeakEntry(
    val kana: Kana,
    val accuracy: Double,
    val box: Int,
)

@Immutable
data class SessionResult(
    val asked: Int,
    val correct: Int,
    val promoted: Int,
    val demoted: Int,
    val readinessBefore: Int,
    val readinessAfter: Int,
    val medianMs: Int,
    val missed: List<MissedEntry>,
    val topConfusion: Pair<Kana, Kana>?,
    val unlockedGroupLabel: String?,
    val newItems: Int,
)

@Immutable
data class MissedEntry(val kana: Kana, val typed: String, val expected: String)

@Immutable
data class HomeInfo(
    val readinessAll: Int = 0,
    val readinessHiragana: Int = 0,
    val readinessKatakana: Int = 0,
    val readinessUnlocked: Int = 0,
    val dueNow: Int = 0,
    val unlockedItems: Int = 0,
    val totalItems: Int = 0,
    val currentGroupLabel: String = "",
    val newItemsAvailable: Int = 0,
    val dayStreak: Int = 0,
    val loadProblem: String? = null,
)

@Immutable
data class DrillUiState(
    val loading: Boolean = true,
    val home: HomeInfo = HomeInfo(),
    /** Automatisches Vorspielen im Lernmodus abgeschaltet. */
    val muted: Boolean = false,
    val settings: Settings = Settings(),
    val settingsOpen: Boolean = false,
    /** Freischaltstand für den Einstellungs-Bildschirm. */
    val groups: List<GroupInfo> = emptyList(),
    /** Gesetzt, solange Zeichen vorgestellt werden. */
    val learn: LearnState? = null,
    val kana: Kana? = null,
    val typed: String = "",
    val feedback: Feedback? = null,
    /** Solange gesetzt, wartet der Bildschirm auf eine Bestätigung. */
    val awaitingContinue: Boolean = false,
    val asked: Int = 0,
    /** Erstkontakte: zählen für den Fortschritt, nicht für die Quote. */
    val introduced: Int = 0,
    val correct: Int = 0,
    val target: Int = 0,
    val streak: Int = 0,
    val readinessAtStart: Int = 0,
    val readinessNow: Int = 0,
    val weakest: List<WeakEntry> = emptyList(),
    val isNewItem: Boolean = false,
    val result: SessionResult? = null,
) {
    /** Verbrauchte Fragen, Erstkontakte eingeschlossen. */
    val answered: Int get() = asked + introduced

    val progress: Float
        get() = if (target == 0) 0f else (answered.toFloat() / target).coerceIn(0f, 1f)

    /** Nur echte Abfragen - der Erstkontakt war Abschreiben. */
    val accuracy: Int
        get() = if (asked == 0) 0 else (100.0 * correct / asked).toInt()
}
