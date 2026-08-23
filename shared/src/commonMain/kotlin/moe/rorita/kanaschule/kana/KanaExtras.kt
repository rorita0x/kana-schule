package moe.rorita.kanaschule.kana

/**
 * Die einzigen wirklich handgepflegten Daten: welche Zeichen sich optisch
 * aehneln und wie man sie auseinanderhaelt. Stimmhaftigkeitspaare stehen hier
 * nicht, die werden strukturell ueber das Basiszeichen erkannt.
 *
 * Die Paare werden beim Aufbau der Tabelle symmetrisiert, jede Kante also nur
 * einmal notiert.
 */
internal object KanaExtras {

    val visualPairs: List<Pair<String, String>> = listOf(
        // Hiragana
        "あ" to "お", "い" to "り", "き" to "さ", "け" to "は", "は" to "ほ",
        "ぬ" to "め", "ね" to "れ", "れ" to "わ", "ね" to "わ", "る" to "ろ",
        "さ" to "ち", "す" to "む", "う" to "つ", "ま" to "も", "こ" to "に",
        "な" to "た", "そ" to "ろ",

        // Katakana
        "シ" to "ツ", "ソ" to "ン", "ソ" to "ノ", "ン" to "ノ", "ウ" to "ワ",
        "ワ" to "ラ", "ク" to "タ", "タ" to "ケ", "コ" to "ユ", "ス" to "ヌ",
        "ヌ" to "メ", "メ" to "ナ", "マ" to "ム", "レ" to "ル", "チ" to "テ",
        "フ" to "ワ", "オ" to "ホ", "ア" to "マ", "シ" to "ミ", "ク" to "ワ",
    )

    /**
     * Was den Unterschied ausmacht, in einem Satz. Wird im Feedback und im
     * Verwechslungs-Drill angezeigt. Schluessel ist ein sortiertes Glyphenpaar.
     */
    val discriminators: Map<Pair<String, String>, String> = mapOf(
        pair("シ", "ツ") to "シ hat die Striche links und oeffnet nach rechts, ツ hat sie oben.",
        pair("ソ", "ン") to "ソ setzt den kurzen Strich oben an, ン unten links.",
        pair("ソ", "ノ") to "ソ hat zwei Striche, ノ nur einen.",
        pair("ね", "れ") to "ね endet in einer Schleife, れ laeuft gerade nach rechts aus.",
        pair("ね", "わ") to "ね hat die Schleife, わ endet als offener Bogen.",
        pair("る", "ろ") to "る schliesst unten mit einer Schleife, ろ bleibt offen.",
        pair("き", "さ") to "き hat zwei Querstriche, さ nur einen.",
        pair("は", "ほ") to "ほ hat den zweiten Querstrich, は nicht.",
        pair("ぬ", "め") to "ぬ endet in einer Schleife, め nicht.",
        pair("コ", "ユ") to "コ ist oben geschlossen, ユ hat den Strich unten.",
        pair("マ", "ム") to "マ ist oben spitz, ム unten.",
        pair("レ", "ル") to "ル hat zwei Striche, レ nur einen.",
        pair("チ", "テ") to "チ hat den schraegen Strich oben, テ einen geraden.",
        pair("う", "つ") to "う hat den kurzen Strich oben, つ ist nur ein Bogen.",
    )

    private fun pair(a: String, b: String): Pair<String, String> =
        if (a < b) a to b else b to a

    fun discriminator(a: String, b: String): String? = discriminators[pair(a, b)]
}
