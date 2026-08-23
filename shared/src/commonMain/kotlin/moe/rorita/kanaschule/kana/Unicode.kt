package moe.rorita.kanaschule.kana

/**
 * NFKC-Normalisierung. Notwendig, weil eine japanische Tastatur Buchstaben in
 * Vollbreite liefert (ｓｈｉ) und Makron-Vokale zusammengesetzt ankommen können.
 */
internal expect fun nfkc(text: String): String
