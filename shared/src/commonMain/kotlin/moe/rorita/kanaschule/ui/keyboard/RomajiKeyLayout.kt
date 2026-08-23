package moe.rorita.kanaschule.ui.keyboard

/**
 * Romaji braucht genau 22 Buchstaben. l, q, v und x kommen nicht vor, also
 * gibt es sie auch nicht - das eliminiert keine echte Verwechslung, spart aber
 * Fehlgriffe auf dem Touchscreen.
 *
 * Die Anordnung folgt QWERTY statt dem Alphabet, damit die Muskelerinnerung
 * von der normalen Tastatur trägt.
 */
object RomajiKeyLayout {

    val rows: List<String> = listOf(
        "wertyuiop",
        "asdfghjk",
        "zcbnm",
    )

    val letters: Set<Char> = rows.flatMap { it.toList() }.toSet()

    fun accepts(char: Char): Boolean = char.lowercaseChar() in letters

    init {
        require(letters.size == EXPECTED_LETTERS) {
            "Romaji braucht $EXPECTED_LETTERS Buchstaben, das Layout hat ${letters.size}"
        }
    }

    private const val EXPECTED_LETTERS = 22
}
