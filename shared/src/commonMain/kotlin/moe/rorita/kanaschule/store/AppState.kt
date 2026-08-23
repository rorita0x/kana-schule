package moe.rorita.kanaschule.store

import kotlinx.serialization.Serializable
import moe.rorita.kanaschule.kana.KanaId
import moe.rorita.kanaschule.srs.ItemState
import moe.rorita.kanaschule.srs.SessionBuilder
import moe.rorita.kanaschule.srs.SessionMode
import moe.rorita.kanaschule.srs.Unlock
import moe.rorita.kanaschule.srs.UnlockGroups

@Serializable
enum class ScopeSetting { HIRAGANA_FIRST, HIRAGANA_ONLY, KATAKANA_ONLY, BOTH }

@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
enum class Outcome {
    CORRECT,
    TYPO,
    CONFUSED,
    WRONG,
    SKIPPED,

    /**
     * Erste Antwort direkt nach der Vorstellungskarte. Abschreiben aus dem
     * Kurzzeitgedächtnis, kein Erinnern - zählt deshalb nicht in die
     * Trefferquote und hebt die Box nicht über die erste hinaus.
     */
    INTRODUCED,
}

@Serializable
data class Settings(
    val scope: ScopeSetting = ScopeSetting.HIRAGANA_FIRST,
    /** Nicht-Hepburn wird dann neutral behandelt statt voll gezählt. */
    val strictHepburn: Boolean = false,
    val dailyNewLimit: Int = Unlock.MAX_NEW_ITEMS_PER_DAY,
    val reviewSessionLength: Int = SessionBuilder.TARGET_SIZE,
    /** null bedeutet: was die Plattform vorgibt. */
    val onScreenKeyboard: Boolean? = null,
    /**
     * Schaltet das automatische Vorspielen im Lernmodus ab. Erreichbar im
     * Hauptmenü, also bevor der erste Ton kommt.
     */
    val muteAudio: Boolean = false,
    val theme: ThemeMode = ThemeMode.SYSTEM,
)

@Serializable
data class UnlockState(
    val unlockedGroups: List<String> = listOf(FIRST_GROUP),
    /** Tag der letzten Freischaltung, begrenzt auf eine Gruppe pro Tag. */
    val lastUnlockDay: Long? = null,
    val newItemsToday: Int = 0,
    val newItemsDay: Long? = null,
) {
    companion object {
        const val FIRST_GROUP = "H_A"
    }
}

/** Ein Tag in der Rückschau. Wird nie gekürzt, das sind rund 80 Byte. */
@Serializable
data class DayAgg(
    val reviews: Int = 0,
    val correct: Int = 0,
    val newItems: Int = 0,
    val medianMs: Int = 0,
    val readinessAll: Int = 0,
    val readinessHiragana: Int = 0,
    val readinessKatakana: Int = 0,
    val activeSeconds: Int = 0,
)

@Serializable
data class SessionSummary(
    val startedAtMs: Long,
    val endedAtMs: Long,
    val mode: SessionMode,
    val asked: Int,
    val correct: Int,
    val promoted: Int,
    val demoted: Int,
    val readinessBefore: Int,
    val readinessAfter: Int,
    val medianMs: Int,
)

/** Eine einzelne Antwort. Landet zeilenweise in reviews.jsonl, nicht hier. */
@Serializable
data class ReviewEntry(
    val t: Long,
    val id: String,
    val outcome: Outcome,
    val ms: Int,
    val typed: String,
    val boxBefore: Int,
    val boxAfter: Int,
    val mode: SessionMode,
    val confusedWith: String? = null,
)

/**
 * Der gesamte gespeicherte Zustand.
 *
 * [items] enthält nur berührte Zeichen; nie gesehene sind der Standardwert.
 * Das hält die Datei am Anfang klein und macht das Erweitern des Datensatzes
 * zum Nicht-Ereignis.
 */
@Serializable
data class AppState(
    val schemaVersion: Int = ProgressCodec.CURRENT_SCHEMA,
    val createdAtMs: Long = 0L,
    val items: Map<String, ItemState> = emptyMap(),
    val unlock: UnlockState = UnlockState(),
    val settings: Settings = Settings(),
    val days: Map<String, DayAgg> = emptyMap(),
    val sessions: List<SessionSummary> = emptyList(),
) {
    val unlockedGroups: Set<String> get() = unlock.unlockedGroups.toSet()

    val unlockedItems: Set<KanaId> get() = UnlockGroups.itemsOf(unlockedGroups).toSet()

    /** Zustandsmap in der Form, die die SRS-Funktionen erwarten. */
    val states: Map<KanaId, ItemState> get() = items.mapKeys { KanaId(it.key) }

    fun stateOf(id: KanaId): ItemState = items[id.v] ?: ItemState()

    fun withState(id: KanaId, state: ItemState): AppState =
        copy(items = items + (id.v to state))

    fun withStates(updated: Map<KanaId, ItemState>): AppState =
        copy(items = items + updated.mapKeys { it.key.v })

    fun withSession(summary: SessionSummary): AppState =
        copy(sessions = (sessions + summary).takeLast(MAX_SESSIONS))

    /**
     * Zählt das Tagesbudget für neue Zeichen fort und setzt es beim
     * Tageswechsel zurück.
     */
    fun withNewItemsToday(count: Int, day: Long): AppState {
        val carried = if (unlock.newItemsDay == day) unlock.newItemsToday else 0
        return copy(unlock = unlock.copy(newItemsToday = carried + count, newItemsDay = day))
    }

    fun newItemBudget(day: Long): Int {
        val used = if (unlock.newItemsDay == day) unlock.newItemsToday else 0
        return (settings.dailyNewLimit - used).coerceAtLeast(0)
    }

    fun canUnlockToday(day: Long): Boolean = unlock.lastUnlockDay != day

    fun withUnlockedGroup(groupId: String, day: Long): AppState =
        copy(
            unlock = unlock.copy(
                unlockedGroups = unlock.unlockedGroups + groupId,
                lastUnlockDay = day,
            ),
        )

    companion object {
        const val MAX_SESSIONS = 100

        fun fresh(nowMs: Long): AppState = AppState(createdAtMs = nowMs)
    }
}
