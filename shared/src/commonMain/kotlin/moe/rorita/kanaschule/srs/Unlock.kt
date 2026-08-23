package moe.rorita.kanaschule.srs

import moe.rorita.kanaschule.kana.KanaId
import moe.rorita.kanaschule.kana.KanaTable
import moe.rorita.kanaschule.kana.Script

/**
 * Wann die nächste Gruppe aufgeht. Die Tagesgrenzen sind der wichtigste Teil:
 * ohne sie schaltet ein euphorischer Tag-eins-Nutzer 46 Zeichen frei und
 * erstickt an Tag drei in Wiederholungen.
 */
object Unlock {

    /** Jedes Zeichen der Gruppe muss mindestens hier stehen. */
    const val MASTER_BOX = 3
    const val MASTER_ACCURACY = 0.85

    const val MAX_NEW_ITEMS_PER_DAY = 10
    const val MAX_GROUPS_PER_DAY = 1

    /** Ab diesem Wert gilt die Tagesgrenze als abgeschaltet. */
    const val GROUPS_PER_DAY_UNLIMITED = 99
    const val NEW_ITEMS_UNLIMITED = 999

    /** Ab dieser Hiragana-Prüfungsreife öffnet Katakana. */
    const val KATAKANA_GATE_PERCENT = 70

    fun isMastered(group: UnlockGroup, states: Map<KanaId, ItemState>): Boolean {
        if (group.itemIds.isEmpty()) return false
        val items = group.itemIds.map { states[it] ?: ItemState() }
        if (items.any { it.box < MASTER_BOX }) return false
        return items.map { it.accuracy20 }.average() >= MASTER_ACCURACY
    }

    /**
     * Die nächste freischaltbare Gruppe, oder null wenn noch nichts ansteht.
     * Prüft nur die Lernbedingung; Tagesgrenzen entscheidet der Aufrufer,
     * der den Kalender kennt.
     */
    fun nextGroup(
        unlockedGroups: Set<String>,
        states: Map<KanaId, ItemState>,
        nowMs: Long,
    ): UnlockGroup? {
        for (group in UnlockGroups.ordered) {
            if (group.id in unlockedGroups) continue
            val previous = UnlockGroups.previousInScript(group)
            val eligible = when {
                previous == null -> group.script == Script.HIRAGANA || katakanaOpen(states, nowMs)
                else -> previous.id in unlockedGroups && isMastered(previous, states)
            }
            if (eligible) return group
        }
        return null
    }

    private fun katakanaOpen(states: Map<KanaId, ItemState>, nowMs: Long): Boolean =
        Readiness.percent(
            KanaTable.byScript.getValue(Script.HIRAGANA),
            states,
            nowMs,
        ) >= KATAKANA_GATE_PERCENT
}
