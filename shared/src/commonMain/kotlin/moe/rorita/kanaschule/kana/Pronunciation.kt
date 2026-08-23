package moe.rorita.kanaschule.kana

/**
 * Aussprachehilfe für deutsche Ohren. Getrennt in Konsonant, Vokal und
 * Stolperfalle, weil japanische Silben systematisch gebaut sind: wer einmal
 * weiß, wie das japanische u klingt, braucht es nicht 20 Mal zu lesen.
 *
 * Die Warnungen sind der eigentliche Wert. Sie stehen genau dort, wo ein
 * deutscher Sprecher zuverlässig daneben liegt.
 */
data class PronunciationHint(
    val consonant: String?,
    val vowel: String?,
    val warning: String?,
)

object Pronunciation {

    /** Der Dateiname der Aufnahme, oder null wenn es keine gibt. */
    fun audioName(kana: Kana): String? =
        if (kana.kind == ItemKind.WORD) null else kana.id.v.substringAfter('.')

    fun of(kana: Kana): PronunciationHint = PronunciationHint(
        consonant = consonants[kana.row],
        vowel = vowelOf(kana.canonical)?.let(vowels::get),
        warning = warnings[kana.id.v.substringAfter('.')]
            ?: if (kana.kClass == KanaClass.YOON) YOON_WARNING else null,
    )

    private fun vowelOf(canonical: String): Char? = canonical.lastOrNull { it in "aiueo" }

    private val vowels: Map<Char, String> = mapOf(
        'a' to "a wie in „Wasser“ - kurz und offen, nie lang gezogen.",
        'i' to "i wie in „Mitte“ - kurz, nicht wie das „ie“ in „Liebe“.",
        'u' to "u wie in „Mutter“, aber mit flachen Lippen statt gerundet.",
        'e' to "e wie in „Bett“ - offen, nicht wie das „e“ in „See“.",
        'o' to "o wie in „Sonne“ - kurz und offen.",
    )

    private val consonants: Map<Row, String?> = mapOf(
        Row.A to null,
        Row.KA to "k wie in „Kanne“, aber ohne den Hauch danach.",
        Row.SA to "s immer scharf wie in „Sonne“, nie stimmhaft wie in „Rose“.",
        Row.TA to "t wie in „Tante“, ohne Hauch.",
        Row.NA to "n wie in „Nase“.",
        Row.HA to "h deutlich gehaucht wie in „Hand“.",
        Row.MA to "m wie in „Mond“.",
        Row.YA to "j wie in „Jahr“.",
        Row.RA to "Ein einzelner Zungenschlag am Zahndamm - zwischen deutschem d " +
            "und l. Kein gerolltes r.",
        Row.WA to "w mit locker gerundeten Lippen wie im englischen „water“, " +
            "nicht mit den Zähnen wie im deutschen „Wasser“.",
        Row.N to null,
        Row.GA to "g wie in „Gast“.",
        Row.ZA to "Stimmhaftes s wie in „Rose“, am Wortanfang mit leichtem " +
            "d-Ansatz: „dz“.",
        Row.DA to "d wie in „Dach“.",
        Row.BA to "b wie in „Ball“.",
        Row.PA to "p wie in „Post“, ohne Hauch.",
        Row.KYA to "k mit angehängtem j, in einer Silbe.",
        Row.GYA to "g mit angehängtem j, in einer Silbe.",
        Row.SHA to "Weiches „sch“ mit angehängtem j-Klang.",
        Row.JA to "Wie „dsch“ in „Dschungel“.",
        Row.CHA to "Wie „tsch“ in „Tschüss“.",
        Row.NYA to "n mit angehängtem j, in einer Silbe.",
        Row.HYA to "h mit angehängtem j, in einer Silbe.",
        Row.BYA to "b mit angehängtem j, in einer Silbe.",
        Row.PYA to "p mit angehängtem j, in einer Silbe.",
        Row.MYA to "m mit angehängtem j, in einer Silbe.",
        Row.RYA to "Zungenschlag-r mit angehängtem j, in einer Silbe.",
        Row.SPECIAL to null,
    )

    private const val YOON_WARNING =
        "Eine Silbe, nicht zwei: nicht „ki-ja“, sondern „kja“ in einem Zug."

    private val warnings: Map<String, String> = mapOf(
        "u" to "Die Lippen bleiben flach. Ein deutsches, gerundetes „u“ klingt " +
            "sofort fremd.",
        "shi" to "Weicher als das deutsche „sch“: die Zunge liegt flacher, " +
            "eher Richtung „ßj“.",
        "su" to "Das u ist oft fast stumm - „ss“ mit angedeutetem u.",
        "chi" to "Wie „tschi“, aber weicher als das deutsche „tsch“.",
        "tsu" to "Genau das „z“ aus „Zug“, dann u. Niemals „tu“.",
        "fu" to "Zwischen f und h: Lippen locker wie beim Kerzenauspusten, die " +
            "Zähne berühren die Lippe nicht.",
        "ha" to "Als Themenpartikel wird は „wa“ gesprochen, nicht „ha“.",
        "he" to "Als Richtungspartikel wird へ „e“ gesprochen, nicht „he“.",
        "ji" to "Wie „dsch“ in „Dschungel“.",
        "n" to "Eine eigene Silbe, kein angehängter Konsonant. Vor k und g " +
            "klingt es wie „ng“ in „lang“, vor p, b und m wie „m“.",
        "wo" to "Wird als Objektpartikel gebraucht und dabei „o“ gesprochen - " +
            "das w fällt weg.",
        "di" to "Klingt heute identisch zu じ. Kommt fast nur in 続く vor.",
        "du" to "Klingt heute identisch zu ず.",
        "ra" to "Kein deutsches r. Die Zungenspitze tippt einmal kurz an, " +
            "wie ein sehr weiches d.",
        "ri" to "Kein deutsches r - ein einzelner Zungenschlag.",
        "ru" to "Kein deutsches r - ein einzelner Zungenschlag.",
        "re" to "Kein deutsches r - ein einzelner Zungenschlag.",
        "ro" to "Kein deutsches r - ein einzelner Zungenschlag.",
    )
}
