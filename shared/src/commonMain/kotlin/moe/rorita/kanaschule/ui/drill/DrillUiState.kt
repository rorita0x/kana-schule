package moe.rorita.kanaschule.ui.drill

import androidx.compose.runtime.Immutable
import moe.rorita.kanaschule.kana.Kana
import moe.rorita.kanaschule.kana.KanaId
import moe.rorita.kanaschule.ui.learn.LearnCard

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

    /** Neutral, zaehlt nicht - der Lernende tippt einfach nochmal. */
    data class Typo(val nearest: String) : Feedback
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
    /** Gesetzt, solange neue Zeichen vorgestellt werden. */
    val learn: LearnCard? = null,
    val kana: Kana? = null,
    val typed: String = "",
    val feedback: Feedback? = null,
    /** Solange gesetzt, wartet der Bildschirm auf eine Bestaetigung. */
    val awaitingContinue: Boolean = false,
    val asked: Int = 0,
    val correct: Int = 0,
    val target: Int = 0,
    val streak: Int = 0,
    val readinessAtStart: Int = 0,
    val readinessNow: Int = 0,
    val weakest: List<WeakEntry> = emptyList(),
    val isNewItem: Boolean = false,
    val result: SessionResult? = null,
) {
    val progress: Float
        get() = if (target == 0) 0f else (asked.toFloat() / target).coerceIn(0f, 1f)

    val accuracy: Int
        get() = if (asked == 0) 0 else (100.0 * correct / asked).toInt()
}
