package moe.rorita.kanaschule.kana

/**
 * Der vollständige Datensatz, aus [KanaSeed] mechanisch aufgebaut.
 *
 * Katakana entsteht aus Hiragana durch den Unicode-Versatz von 0x60, Dakuten
 * durch +1 und Handakuten durch +2 auf dem Basiszeichen. Deshalb steht in der
 * Seed-Matrix nur Hiragana.
 */
object KanaTable {

    private const val KATAKANA_OFFSET = 0x60

    val all: List<Kana> = build()

    val byId: Map<KanaId, Kana> = all.associateBy { it.id }

    val byScript: Map<Script, List<Kana>> = all.groupBy { it.script }

    val byRow: Map<Row, List<Kana>> = all.groupBy { it.row }

    /** Normalisierte Antwort -> alle Zeichen, die sie berechtigt akzeptieren. */
    val reverseIndex: Map<String, List<KanaId>> =
        all.flatMap { kana -> kana.accepted.map { it to kana.id } }
            .groupBy({ it.first }, { it.second })

    val singles: List<Kana> = all.filter { it.kind != ItemKind.WORD }

    val words: List<Kana> = all.filter { it.kind == ItemKind.WORD }

    fun require(id: KanaId): Kana = byId[id] ?: error("Unbekannte Kana-ID: ${id.v}")

    fun of(script: Script, row: Row): List<Kana> =
        byRow[row]?.filter { it.script == script }.orEmpty()

    // ---------------------------------------------------------------- Aufbau

    private fun build(): List<Kana> {
        val staged = ArrayList<Kana>(232)

        for (rowSeed in KanaSeed.rows) {
            for (seed in rowSeed.seeds) {
                val answers = answersOf(seed)
                for (script in Script.entries) {
                    val glyph = glyphFor(seed.hiragana, script)
                    staged += Kana(
                        id = idFor(script, seed.slug),
                        script = script,
                        glyph = glyph,
                        kind = if (glyph.length > 1) ItemKind.DIGRAPH else ItemKind.SINGLE,
                        kClass = rowSeed.kClass,
                        row = rowSeed.row,
                        canonical = seed.canonical,
                        answers = answers,
                        noteDe = seed.noteDe,
                    )
                }
            }
        }

        for (word in KanaSeed.words) {
            staged += Kana(
                id = KanaId("${prefix(word.script)}.word.${word.slug}"),
                script = word.script,
                glyph = word.glyph,
                kind = ItemKind.WORD,
                kClass = KanaClass.SPECIAL,
                row = Row.SPECIAL,
                canonical = word.canonical,
                answers = answersOf(word),
                noteDe = word.noteDe,
            )
        }

        return link(staged)
    }

    /** Zweiter Durchgang: Basiszeichen, Schriftpartner und optische Nachbarn. */
    private fun link(staged: List<Kana>): List<Kana> {
        val ids: Set<KanaId> = staged.mapTo(HashSet()) { it.id }
        val idByGlyph: Map<Pair<Script, String>, KanaId> =
            staged.associate { (it.script to it.glyph) to it.id }
        val singleIdByGlyph: Map<String, KanaId> =
            staged.filter { it.kind != ItemKind.WORD }.associate { it.glyph to it.id }

        val neighbors = HashMap<KanaId, MutableList<KanaId>>()
        for ((a, b) in KanaExtras.visualPairs) {
            val idA = singleIdByGlyph[a] ?: error("Unbekanntes Zeichen in visualPairs: $a")
            val idB = singleIdByGlyph[b] ?: error("Unbekanntes Zeichen in visualPairs: $b")
            neighbors.getOrPut(idA) { ArrayList() } += idB
            neighbors.getOrPut(idB) { ArrayList() } += idA
        }

        return staged.map { kana ->
            kana.copy(
                baseId = baseGlyph(kana)?.let { idByGlyph[kana.script to it] },
                partnerId = partnerId(kana)?.takeIf { it in ids },
                visualNeighbors = neighbors[kana.id]?.distinct().orEmpty(),
            )
        }
    }

    /** Dieselbe Lesung in der anderen Schrift. Wörter haben keinen Partner. */
    private fun partnerId(kana: Kana): KanaId? =
        if (kana.kind == ItemKind.WORD) {
            null
        } else {
            KanaId("${prefix(partnerScript(kana.script))}.${kana.id.v.substringAfter('.')}")
        }

    private fun partnerScript(script: Script): Script = when (script) {
        Script.HIRAGANA -> Script.KATAKANA
        Script.KATAKANA -> Script.HIRAGANA
    }

    /** Basiszeichen desselben Skripts: が -> か, ぱ -> は, きゃ -> き. */
    private fun baseGlyph(kana: Kana): String? = when (kana.kClass) {
        KanaClass.DAKUTEN -> (kana.glyph[0] - 1).toString()
        KanaClass.HANDAKUTEN -> (kana.glyph[0] - 2).toString()
        KanaClass.YOON -> kana.glyph[0].toString()
        KanaClass.GOJUON, KanaClass.SPECIAL -> null
    }

    private fun glyphFor(hiragana: String, script: Script): String = when (script) {
        Script.HIRAGANA -> hiragana
        Script.KATAKANA -> hiragana.map { it + KATAKANA_OFFSET }.joinToString("")
    }

    private fun prefix(script: Script): String = when (script) {
        Script.HIRAGANA -> "h"
        Script.KATAKANA -> "k"
    }

    private fun idFor(script: Script, slug: String) = KanaId("${prefix(script)}.$slug")

    private fun answersOf(seed: Seed): List<Answer> = assemble(
        hepburn = listOf(seed.canonical) + seed.hepburnAlt,
        kunrei = listOfNotNull(seed.kunrei),
        wapuro = seed.wapuro,
        tolerated = seed.tolerated,
    )

    private fun answersOf(word: WordSeed): List<Answer> = assemble(
        hepburn = listOf(word.canonical) + word.hepburnAlt,
        kunrei = word.kunrei,
        wapuro = emptyList(),
        tolerated = word.tolerated,
    )

    private fun assemble(
        hepburn: List<String>,
        kunrei: List<String>,
        wapuro: List<String>,
        tolerated: List<String>,
    ): List<Answer> {
        val out = LinkedHashMap<String, RomajiSystem>()
        hepburn.forEach { out.getOrPut(it) { RomajiSystem.HEPBURN } }
        kunrei.forEach { out.getOrPut(it) { RomajiSystem.KUNREI } }
        wapuro.forEach { out.getOrPut(it) { RomajiSystem.WAPURO } }
        tolerated.forEach { out.getOrPut(it) { RomajiSystem.TOLERATED } }
        return out.map { (text, system) -> Answer(text, system) }
    }
}
