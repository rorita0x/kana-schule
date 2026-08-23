package moe.rorita.kanaschule.srs

import moe.rorita.kanaschule.kana.KanaId

/**
 * Die Reihenfolge innerhalb einer Session. Hier steckt das Fahrschul-Gefuehl,
 * nicht in den Boxen: ein verpasstes Zeichen kommt nach genau drei anderen
 * Fragen wieder, und noch einmal nach zehn. Feste Abstaende, nicht zufaellige -
 * der Lernende lernt unterbewusst „das kommt zurueck“, und das ist der ganze
 * psychologische Motor.
 */
class SessionQueue(initial: List<KanaId>) {

    private val queue: MutableList<KanaId> = initial.toMutableList()
    private var lastServed: KanaId? = null

    val remaining: Int get() = queue.size

    val pending: List<KanaId> get() = queue.toList()

    /**
     * Nimmt das naechste Zeichen. Dasselbe Zeichen kommt nie zweimal
     * hintereinander; notfalls wird mit dem folgenden getauscht.
     */
    fun next(): KanaId? {
        if (queue.isEmpty()) return null
        if (queue.size > 1 && queue[0] == lastServed) {
            val first = queue[0]
            queue[0] = queue[1]
            queue[1] = first
        }
        val id = queue.removeAt(0)
        lastServed = id
        return id
    }

    /** Nach einem Fehler: nach 3 und nach 10 weiteren Fragen erneut. */
    fun requeueAfterMiss(id: KanaId) {
        insertAt(MISS_NEAR, id)
        insertAt(MISS_FAR, id)
    }

    /** Tippfehler: direkt nach der naechsten Frage nochmal. */
    fun requeueTypo(id: KanaId) = insertAt(TYPO_GAP, id)

    /**
     * Verwechslungspaar abwechselnd einstreuen. Verschachtelter Kontrast ist
     * die einzige Praesentationsform, die Unterscheidung wirklich aufbaut;
     * denselben Partner mehrfach am Stueck zu zeigen tut es nicht.
     */
    fun requeueConfusionPair(target: KanaId, other: KanaId) {
        insertAt(2, other)
        insertAt(3, target)
        insertAt(6, other)
        insertAt(9, target)
    }

    /** Haengt Nachschub hinten an, etwa fuer die Nachspielzeit. */
    fun append(ids: List<KanaId>) {
        queue += ids
    }

    /**
     * Einfuegen nach [after] weiteren Fragen. Liegt dasselbe Zeichen schon
     * direkt daneben, wird nicht eingefuegt - sonst entstehen Doppel.
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
