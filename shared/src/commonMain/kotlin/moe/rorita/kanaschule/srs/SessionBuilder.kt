package moe.rorita.kanaschule.srs

import kotlin.random.Random
import moe.rorita.kanaschule.kana.KanaId
import moe.rorita.kanaschule.kana.KanaTable
import moe.rorita.kanaschule.kana.Row

data class SessionPlan(
    val mode: SessionMode,
    val items: List<KanaId>,
    /** Teilmenge von [items], die zum ersten Mal auftaucht. */
    val newItems: List<KanaId>,
)

/**
 * Stellt eine Übungssession zusammen. Rein und deterministisch: derselbe
 * Zufallsgenerator ergibt dieselbe Session, damit Fehlerberichte
 * reproduzierbar und der Aufbau testbar bleibt.
 */
object SessionBuilder {

    const val TARGET_SIZE = 30
    const val NEW_MAX = 4

    private const val DUE_MAX = 20
    private const val WEAK_MAX = 6
    private const val WEAK_BOX = 3
    private const val LEECH_MAX = 2

    /** Vor dieser Position soll kein neues Zeichen stehen. */
    private const val NEW_ITEM_MIN_INDEX = 2

    fun review(
        unlockedGroups: Set<String>,
        states: Map<KanaId, ItemState>,
        nowMs: Long,
        random: Random,
        newBudget: Int = NEW_MAX,
        targetSize: Int = TARGET_SIZE,
    ): SessionPlan {
        val unlocked = UnlockGroups.itemsOf(unlockedGroups)
        fun state(id: KanaId) = states[id] ?: ItemState()

        val seen = unlocked.filter { state(it).seen }
        val picked = LinkedHashSet<KanaId>()

        seen.filter { state(it).dueAtMs <= nowMs }
            .sortedBy { state(it).dueAtMs }
            .take(DUE_MAX)
            .let(picked::addAll)

        seen.filter { it !in picked && state(it).box <= WEAK_BOX }
            .sortedWith(compareBy({ state(it).box }, { -state(it).lapses }))
            .take(WEAK_MAX)
            .let(picked::addAll)

        seen.filter { it !in picked && state(it).leech }
            .take(LEECH_MAX)
            .let(picked::addAll)

        val newItems = unlocked.filter { !state(it).seen }.take(newBudget.coerceAtLeast(0))
        picked.addAll(newItems)

        if (picked.size < targetSize) {
            seen.filter { it !in picked }
                .sortedBy { state(it).box }
                .take(targetSize - picked.size)
                .let(picked::addAll)
        }

        val items = arrange(picked.take(targetSize), newItems.toSet(), random)
        return SessionPlan(SessionMode.REVIEW, items, newItems)
    }

    /**
     * Mischen und dabei zwei Regeln durchsetzen: keine zwei Zeichen derselben
     * Zeile nebeneinander (sonst kommt die ganze か-Reihe im Block), und kein
     * neues Zeichen ganz am Anfang.
     */
    private fun arrange(
        items: List<KanaId>,
        newItems: Set<KanaId>,
        random: Random,
    ): List<KanaId> {
        val arranged = spreadRows(items, random)
        delayNewItems(arranged, newItems)
        return arranged
    }

    /**
     * Nimmt immer aus der größten noch offenen Zeile, die nicht die vorige
     * ist. Nur irgendeine andere Zeile zu nehmen genügt nicht: dann bleibt
     * regelmäßig die häufigste Zeile bis zum Schluss übrig und die letzten
     * beiden Zeichen sind doch ein Paar.
     */
    private fun spreadRows(items: List<KanaId>, random: Random): MutableList<KanaId> {
        val buckets: MutableMap<Row, MutableList<KanaId>> =
            items.shuffled(random).groupByTo(LinkedHashMap()) { row(it) }
        val out = ArrayList<KanaId>(items.size)
        var lastRow: Row? = null

        while (out.size < items.size) {
            val entry = buckets.entries
                .filter { it.value.isNotEmpty() && it.key != lastRow }
                .maxByOrNull { it.value.size }
                ?: buckets.entries.first { it.value.isNotEmpty() }
            out += entry.value.removeAt(entry.value.size - 1)
            lastRow = entry.key
        }
        return out
    }

    /**
     * Schiebt neue Zeichen aus den ersten Positionen nach hinten. Getauscht
     * wird nur gegen ein Zeichen, das an der neuen Stelle kein Zeilenpaar
     * erzeugt.
     */
    private fun delayNewItems(items: MutableList<KanaId>, newItems: Set<KanaId>) {
        if (newItems.isEmpty() || items.size <= NEW_ITEM_MIN_INDEX) return
        for (i in 0 until minOf(NEW_ITEM_MIN_INDEX, items.size)) {
            if (items[i] !in newItems) continue
            val swap = (NEW_ITEM_MIN_INDEX until items.size).firstOrNull { candidate ->
                items[candidate] !in newItems &&
                    row(items[candidate]) != rowAt(items, i - 1) &&
                    row(items[candidate]) != rowAt(items, i + 1) &&
                    row(items[i]) != rowAt(items, candidate - 1) &&
                    row(items[i]) != rowAt(items, candidate + 1)
            } ?: return
            val tmp = items[i]
            items[i] = items[swap]
            items[swap] = tmp
        }
    }

    private fun rowAt(items: List<KanaId>, index: Int): Row? =
        items.getOrNull(index)?.let { row(it) }

    private fun row(id: KanaId): Row = KanaTable.require(id).row
}
