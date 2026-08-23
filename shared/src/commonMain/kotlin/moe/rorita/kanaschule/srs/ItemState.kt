package moe.rorita.kanaschule.srs

import kotlinx.serialization.Serializable

@Serializable
enum class SessionMode { LEARN, REVIEW, CONFUSION, SPEED, EXAM }

/**
 * Lernstand eines einzelnen Zeichens. Nie gesehene Zeichen existieren nicht in
 * der gespeicherten Map, sondern werden beim Lesen als Standardwert erzeugt.
 */
@Serializable
data class ItemState(
    val box: Int = 0,
    /** 0 bedeutet „jetzt fällig“. */
    val dueAtMs: Long = 0L,
    val firstSeenMs: Long? = null,
    val lastSeenMs: Long? = null,
    val reps: Int = 0,
    val lapses: Int = 0,
    /** Richtige Antworten in Folge. */
    val streak: Int = 0,
    /** Richtige Antworten in Folge unter [Scheduler.FAST_MS]. */
    val fastStreak: Int = 0,
    /** Exponentiell geglättete Antwortzeit, alpha = 0,3. */
    val latencyEwmaMs: Int = 0,
    /** Ringpuffer der letzten 20 Antworten, Bit 0 ist die neueste. */
    val recent: Int = 0,
    val recentCount: Int = 0,
    val leech: Boolean = false,
    /** Nach einem Fehler, bis das Zeichen in einer Session zweimal sass. */
    val relearning: Boolean = false,
    /** KanaId -> wie oft mit diesem Zeichen verwechselt. */
    val confusions: Map<String, Int> = emptyMap(),
) {
    val seen: Boolean get() = reps > 0

    /** Trefferquote der letzten (bis zu) 20 Antworten. */
    val accuracy20: Double
        get() = if (recentCount == 0) 0.0 else recent.countOneBits() / recentCount.toDouble()

    companion object {
        const val RECENT_BITS = 20
        internal const val RECENT_MASK = (1 shl RECENT_BITS) - 1
    }
}
