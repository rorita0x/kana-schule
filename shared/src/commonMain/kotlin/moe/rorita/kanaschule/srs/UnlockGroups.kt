package moe.rorita.kanaschule.srs

import moe.rorita.kanaschule.kana.KanaId
import moe.rorita.kanaschule.kana.KanaTable
import moe.rorita.kanaschule.kana.Row
import moe.rorita.kanaschule.kana.Script

data class UnlockGroup(
    val id: String,
    val labelDe: String,
    val script: Script,
    val itemIds: List<KanaId>,
)

/**
 * Die Leiter, in der neue Zeichen freigeschaltet werden. Hiragana zuerst,
 * Zeile für Zeile; Katakana öffnet, sobald Hiragana weit genug ist.
 *
 * Der Sinn der Leiter ist die Begrenzung: die aktive Menge bleibt jederzeit
 * klein, damit sich 230 Items nie wie 230 Items anfühlen.
 */
object UnlockGroups {

    /** Zeilen, die zusammen eine Gruppe bilden. */
    private val rowGroups: List<List<Row>> = listOf(
        listOf(Row.A), listOf(Row.KA), listOf(Row.SA), listOf(Row.TA), listOf(Row.NA),
        listOf(Row.HA), listOf(Row.MA), listOf(Row.YA), listOf(Row.RA),
        listOf(Row.WA, Row.N),
        listOf(Row.GA), listOf(Row.ZA), listOf(Row.DA), listOf(Row.BA), listOf(Row.PA),
        listOf(Row.KYA, Row.GYA),
        listOf(Row.SHA, Row.JA, Row.CHA),
        listOf(Row.NYA, Row.HYA, Row.BYA),
        listOf(Row.PYA, Row.MYA, Row.RYA),
    )

    val ordered: List<UnlockGroup> = build()

    val byId: Map<String, UnlockGroup> = ordered.associateBy { it.id }

    private val groupByItem: Map<KanaId, UnlockGroup> =
        ordered.flatMap { group -> group.itemIds.map { it to group } }.toMap()

    val first: UnlockGroup get() = ordered.first()

    fun of(script: Script): List<UnlockGroup> = ordered.filter { it.script == script }

    fun groupOf(id: KanaId): UnlockGroup? = groupByItem[id]

    /** Vorgängergruppe derselben Schrift, oder null bei der ersten. */
    fun previousInScript(group: UnlockGroup): UnlockGroup? {
        val siblings = of(group.script)
        val index = siblings.indexOf(group)
        return siblings.getOrNull(index - 1)
    }

    /** Alle Zeichen der freigeschalteten Gruppen, in Leiterreihenfolge. */
    fun itemsOf(groupIds: Set<String>): List<KanaId> =
        ordered.filter { it.id in groupIds }.flatMap { it.itemIds }

    private fun build(): List<UnlockGroup> = buildList {
        for (script in listOf(Script.HIRAGANA, Script.KATAKANA)) {
            for (rows in rowGroups) {
                val items = rows.flatMap { KanaTable.of(script, it) }
                add(
                    UnlockGroup(
                        id = "${prefix(script)}_${rows.joinToString("_") { it.name }}",
                        labelDe = label(script, rows),
                        script = script,
                        itemIds = items.map { it.id },
                    ),
                )
            }
            addAll(wordGroups(script))
        }
    }

    private fun wordGroups(script: Script): List<UnlockGroup> {
        val words = KanaTable.words.filter { it.script == script }
        val sokuon = words.filter { 'っ' in it.glyph || 'ッ' in it.glyph }
        val chouonpu = words.filter { 'ー' in it.glyph }
        return buildList {
            if (sokuon.isNotEmpty()) {
                add(
                    UnlockGroup(
                        id = "${prefix(script)}_SOKUON",
                        labelDe = "Kleines ${if (script == Script.HIRAGANA) "っ" else "ッ"}",
                        script = script,
                        itemIds = sokuon.map { it.id },
                    ),
                )
            }
            if (chouonpu.isNotEmpty()) {
                add(
                    UnlockGroup(
                        id = "${prefix(script)}_CHOUONPU",
                        labelDe = "Langer Vokal ー",
                        script = script,
                        itemIds = chouonpu.map { it.id },
                    ),
                )
            }
        }
    }

    /** „か-Reihe“, bei mehreren Zeilen „きゃ・ぎゃ-Reihe“. */
    private fun label(script: Script, rows: List<Row>): String =
        rows.mapNotNull { KanaTable.of(script, it).firstOrNull()?.glyph }
            .joinToString("・") + "-Reihe"

    private fun prefix(script: Script) = if (script == Script.HIRAGANA) "H" else "K"
}
