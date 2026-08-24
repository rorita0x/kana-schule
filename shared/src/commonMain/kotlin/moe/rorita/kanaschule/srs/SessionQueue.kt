package moe.rorita.kanaschule.srs

import moe.rorita.kanaschule.kana.KanaId

/**
 * Die Reihenfolge innerhalb einer Session. Hier steckt das Fahrschul-Gefühl,
 * nicht in den Boxen: ein verpasstes Zeichen kommt nach genau drei anderen
 * Fragen wieder, und noch einmal nach zehn. Feste Abstände, nicht zufällige -
 * der Lernende lernt unterbewusst „das kommt zurück“, und das ist der ganze
 * psychologische Motor.
 */
class SessionQueue(initial: List<KanaId>) {

    private val queue: MutableList<KanaId> = initial.toMutableList()

    /** Alle Zeichen der Runde, als Vorrat für Lückenfüller. */
    private val pool: List<KanaId> = initial.distinct()
    private var poolCursor = 0
    private var lastServed: KanaId? = null

    val remaining: Int get() = queue.size

    val pending: List<KanaId> get() = queue.toList()

    /**
     * Nimmt das nächste Zeichen. Dasselbe Zeichen kommt nie zweimal
     * hintereinander; notfalls wird mit dem folgenden getauscht.
     */
    fun next(): KanaId? {
        if (queue.isEmpty()) return null

        // Am Anfang des Lernens ist die Runde so klein, dass nach einem Fehler
        // kein Abstand mehr im Vorrat steckt - dann stand dasselbe Zeichen
        // zweimal hintereinander, und die zweite Antwort war vom Bildschirm
        // abgeschrieben statt gewusst. Also einen Lückenfüller einschieben.
        if (queue.size == 1 && queue[0] == lastServed) {
            filler()?.let { queue.add(0, it) }
        }
        if (queue.size > 1 && queue[0] == lastServed) {
            val first = queue[0]
            queue[0] = queue[1]
            queue[1] = first
        }
        val id = queue.removeAt(0)
        lastServed = id
        return id
    }

    /**
     * Ein anderes Zeichen als das letzte, der Reihe nach durch den Vorrat -
     * damit nicht immer dasselbe als Füller herhält.
     */
    private fun filler(): KanaId? {
        if (pool.size < 2) return null
        for (i in pool.indices) {
            val candidate = pool[(poolCursor + i) % pool.size]
            if (candidate != lastServed) {
                poolCursor = (poolCursor + i + 1) % pool.size
                return candidate
            }
        }
        return null
    }

    /** Nach einem Fehler: nach 3 und nach 10 weiteren Fragen erneut. */
    fun requeueAfterMiss(id: KanaId) {
        insertAt(MISS_NEAR, id)
        insertAt(MISS_FAR, id)
    }

    /** Tippfehler: direkt nach der nächsten Frage nochmal. */
    fun requeueTypo(id: KanaId) = insertAt(TYPO_GAP, id)

    /**
     * Verwechslungspaar abwechselnd einstreuen. Verschachtelter Kontrast ist
     * die einzige Präsentationsform, die Unterscheidung wirklich aufbaut;
     * denselben Partner mehrfach am Stück zu zeigen tut es nicht.
     */
    fun requeueConfusionPair(target: KanaId, other: KanaId) {
        insertAt(2, other)
        insertAt(3, target)
        insertAt(6, other)
        insertAt(9, target)
    }

    /** Hängt Nachschub hinten an, etwa für die Nachspielzeit. */
    fun append(ids: List<KanaId>) {
        queue += ids
    }

    /**
     * Einfügen nach [after] weiteren Fragen. Liegt dasselbe Zeichen schon
     * direkt daneben, wird nicht eingefügt - sonst entstehen Doppel.
     */
    private fun insertAt(after: Int, id: KanaId) {
        val index = after.coerceIn(0, queue.size)
        val from = (index - 1).coerceAtLeast(0)
        val to = (index + 1).coerceAtMost(queue.size - 1)
        for (i in from..to) {
            if (queue[i] == id) return
        }
        queue.add(index, id)
    }

    companion object {
        const val MISS_NEAR = 3
        const val MISS_FAR = 10
        const val TYPO_GAP = 1
    }
}
