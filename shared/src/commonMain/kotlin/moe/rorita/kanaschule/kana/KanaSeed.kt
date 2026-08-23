package moe.rorita.kanaschule.kana

/**
 * Handgeschriebene Seed-Matrix. Nur Hiragana wird eingetragen; Katakana
 * entsteht mechanisch durch den Unicode-Versatz von 0x60, Basiszeichen und
 * IDs berechnet [KanaTable] daraus. Das hält die Zahl der möglichen
 * Tippfehler bei fünf Tabellen statt bei 208 Zeilen.
 */
internal class Seed(
    val hiragana: String,
    /** Modifiziertes Hepburn. */
    val canonical: String,
    /** Eindeutiger Namensteil der ID, falls canonical nicht eindeutig ist (じ/ぢ). */
    val slug: String = canonical,
    /** Weitere Hepburn-gleichwertige Schreibweisen, die keinen Hinweis auslösen. */
    val hepburnAlt: List<String> = emptyList(),
    val kunrei: String? = null,
    /** Nur angeben, wenn die Wapuro-Form von der Kunrei-Form abweicht. */
    val wapuro: List<String> = emptyList(),
    val tolerated: List<String> = emptyList(),
    val noteDe: String? = null,
)

internal class RowSeed(val row: Row, val kClass: KanaClass, val seeds: List<Seed>)

internal class WordSeed(
    val slug: String,
    val script: Script,
    val glyph: String,
    /**
     * Die Form, die sich mit der 22-Tasten-Romaji-Tastatur tippen lässt und
     * die Kana eins zu eins abbildet: ー wird zum verdoppelten Vokal.
     */
    val canonical: String,
    /** Gleichwertig, löst keinen Hinweis aus - etwa die Makron-Schreibweise. */
    val hepburnAlt: List<String> = emptyList(),
    val kunrei: List<String> = emptyList(),
    val tolerated: List<String> = emptyList(),
    val noteDe: String? = null,
)

private fun s(
    hiragana: String,
    canonical: String,
    slug: String = canonical,
    hepburnAlt: List<String> = emptyList(),
    kunrei: String? = null,
    wapuro: List<String> = emptyList(),
    tolerated: List<String> = emptyList(),
    noteDe: String? = null,
) = Seed(hiragana, canonical, slug, hepburnAlt, kunrei, wapuro, tolerated, noteDe)

internal object KanaSeed {

    val rows: List<RowSeed> = listOf(
        // ---- Gojuon: 46 ----
        RowSeed(Row.A, KanaClass.GOJUON, listOf(
            s("あ", "a"), s("い", "i"), s("う", "u"), s("え", "e"), s("お", "o"),
        )),
        RowSeed(Row.KA, KanaClass.GOJUON, listOf(
            s("か", "ka"), s("き", "ki"), s("く", "ku"), s("け", "ke"), s("こ", "ko"),
        )),
        RowSeed(Row.SA, KanaClass.GOJUON, listOf(
            s("さ", "sa"),
            s("し", "shi", kunrei = "si"),
            s("す", "su"), s("せ", "se"), s("そ", "so"),
        )),
        RowSeed(Row.TA, KanaClass.GOJUON, listOf(
            s("た", "ta"),
            s("ち", "chi", kunrei = "ti"),
            s("つ", "tsu", kunrei = "tu", tolerated = listOf("tzu")),
            s("て", "te"), s("と", "to"),
        )),
        RowSeed(Row.NA, KanaClass.GOJUON, listOf(
            s("な", "na"), s("に", "ni"), s("ぬ", "nu"), s("ね", "ne"), s("の", "no"),
        )),
        RowSeed(Row.HA, KanaClass.GOJUON, listOf(
            s("は", "ha", noteDe = "Als Themenpartikel wird は „wa“ gesprochen."),
            s("ひ", "hi"),
            s("ふ", "fu", kunrei = "hu"),
            s("へ", "he", noteDe = "Als Richtungspartikel wird へ „e“ gesprochen."),
            s("ほ", "ho"),
        )),
        RowSeed(Row.MA, KanaClass.GOJUON, listOf(
            s("ま", "ma"), s("み", "mi"), s("む", "mu"), s("め", "me"), s("も", "mo"),
        )),
        RowSeed(Row.YA, KanaClass.GOJUON, listOf(
            s("や", "ya"), s("ゆ", "yu"), s("よ", "yo"),
        )),
        RowSeed(Row.RA, KanaClass.GOJUON, listOf(
            s("ら", "ra"), s("り", "ri"), s("る", "ru"), s("れ", "re"), s("ろ", "ro"),
        )),
        RowSeed(Row.WA, KanaClass.GOJUON, listOf(
            s("わ", "wa"),
            s(
                "を", "wo", hepburnAlt = listOf("o"),
                noteDe = "Nur als Objektpartikel gebraucht und dabei „o“ gesprochen.",
            ),
        )),
        RowSeed(Row.N, KanaClass.GOJUON, listOf(
            s("ん", "n", wapuro = listOf("nn"), tolerated = listOf("n'", "m")),
        )),

        // ---- Dakuten: 20 ----
        RowSeed(Row.GA, KanaClass.DAKUTEN, listOf(
            s("が", "ga"), s("ぎ", "gi"), s("ぐ", "gu"), s("げ", "ge"), s("ご", "go"),
        )),
        RowSeed(Row.ZA, KanaClass.DAKUTEN, listOf(
            s("ざ", "za"),
            s("じ", "ji", kunrei = "zi", tolerated = listOf("jyi")),
            s("ず", "zu"), s("ぜ", "ze"), s("ぞ", "zo"),
        )),
        RowSeed(Row.DA, KanaClass.DAKUTEN, listOf(
            s("だ", "da"),
            s("で", "de"), s("ど", "do"),
            s(
                "ぢ", "ji", slug = "di", kunrei = "di", tolerated = listOf("dji", "dzi"),
                noteDe = "Sehr selten, klingt identisch zu じ.",
            ),
            s(
                "づ", "zu", slug = "du", kunrei = "du", tolerated = listOf("dzu"),
                noteDe = "Sehr selten, klingt identisch zu ず.",
            ),
        )),
        RowSeed(Row.BA, KanaClass.DAKUTEN, listOf(
            s("ば", "ba"), s("び", "bi"), s("ぶ", "bu"), s("べ", "be"), s("ぼ", "bo"),
        )),

        // ---- Handakuten: 5 ----
        RowSeed(Row.PA, KanaClass.HANDAKUTEN, listOf(
            s("ぱ", "pa"), s("ぴ", "pi"), s("ぷ", "pu"), s("ぺ", "pe"), s("ぽ", "po"),
        )),

        // ---- Yoon: 33 ----
        RowSeed(Row.KYA, KanaClass.YOON, listOf(
            s("きゃ", "kya"), s("きゅ", "kyu"), s("きょ", "kyo"),
        )),
        RowSeed(Row.GYA, KanaClass.YOON, listOf(
            s("ぎゃ", "gya"), s("ぎゅ", "gyu"), s("ぎょ", "gyo"),
        )),
        RowSeed(Row.SHA, KanaClass.YOON, listOf(
            s("しゃ", "sha", kunrei = "sya"),
            s("しゅ", "shu", kunrei = "syu"),
            s("しょ", "sho", kunrei = "syo"),
        )),
        RowSeed(Row.JA, KanaClass.YOON, listOf(
            s("じゃ", "ja", kunrei = "zya", tolerated = listOf("jya")),
            s("じゅ", "ju", kunrei = "zyu", tolerated = listOf("jyu")),
            s("じょ", "jo", kunrei = "zyo", tolerated = listOf("jyo")),
        )),
        RowSeed(Row.CHA, KanaClass.YOON, listOf(
            s("ちゃ", "cha", kunrei = "tya", tolerated = listOf("cya", "chya")),
            s("ちゅ", "chu", kunrei = "tyu", tolerated = listOf("cyu", "chyu")),
            s("ちょ", "cho", kunrei = "tyo", tolerated = listOf("cyo", "chyo")),
        )),
        RowSeed(Row.NYA, KanaClass.YOON, listOf(
            s("にゃ", "nya"), s("にゅ", "nyu"), s("にょ", "nyo"),
        )),
        RowSeed(Row.HYA, KanaClass.YOON, listOf(
            s("ひゃ", "hya"), s("ひゅ", "hyu"), s("ひょ", "hyo"),
        )),
        RowSeed(Row.BYA, KanaClass.YOON, listOf(
            s("びゃ", "bya"), s("びゅ", "byu"), s("びょ", "byo"),
        )),
        RowSeed(Row.PYA, KanaClass.YOON, listOf(
            s("ぴゃ", "pya"), s("ぴゅ", "pyu"), s("ぴょ", "pyo"),
        )),
        RowSeed(Row.MYA, KanaClass.YOON, listOf(
            s("みゃ", "mya"), s("みゅ", "myu"), s("みょ", "myo"),
        )),
        RowSeed(Row.RYA, KanaClass.YOON, listOf(
            s("りゃ", "rya"), s("りゅ", "ryu"), s("りょ", "ryo"),
        )),
    )

    /**
     * っ, ッ und ー haben keine eigene Lesung. Sie werden an Wörtern geübt,
     * denn das Lernziel ist die Regel, nicht das Zeichen.
     */
    val words: List<WordSeed> = listOf(
        // Sokuon Hiragana: kleines つ verdoppelt den folgenden Konsonanten
        WordSeed("kitte", Script.HIRAGANA, "きって", "kitte"),
        WordSeed("ippai", Script.HIRAGANA, "いっぱい", "ippai"),
        WordSeed("zasshi", Script.HIRAGANA, "ざっし", "zasshi", kunrei = listOf("zassi")),
        WordSeed("massugu", Script.HIRAGANA, "まっすぐ", "massugu"),
        WordSeed("kippu", Script.HIRAGANA, "きっぷ", "kippu"),
        WordSeed("shippai", Script.HIRAGANA, "しっぱい", "shippai", kunrei = listOf("sippai")),
        WordSeed("issho", Script.HIRAGANA, "いっしょ", "issho", kunrei = listOf("issyo")),
        WordSeed("asatte", Script.HIRAGANA, "あさって", "asatte"),

        // Sokuon Katakana
        WordSeed("koppu", Script.KATAKANA, "コップ", "koppu"),
        WordSeed("roketto", Script.KATAKANA, "ロケット", "roketto"),
        WordSeed("katto", Script.KATAKANA, "カット", "katto"),
        WordSeed("netto", Script.KATAKANA, "ネット", "netto"),
        WordSeed("baggu", Script.KATAKANA, "バッグ", "baggu"),
        WordSeed("setto", Script.KATAKANA, "セット", "setto"),

        // Chouonpu: ー verlängert den Vokal. Kanonisch ist die tippbare
        // Doppelvokal-Form; die Makron-Schreibweise gilt als gleichwertig.
        WordSeed(
            "ramen", Script.KATAKANA, "ラーメン", "raamen",
            hepburnAlt = listOf("rāmen"), tolerated = listOf("ramen", "ra-men"),
        ),
        WordSeed(
            "kohi", Script.KATAKANA, "コーヒー", "koohii",
            hepburnAlt = listOf("kōhī"), tolerated = listOf("kohi", "koohi", "kohii"),
        ),
        WordSeed(
            "keki", Script.KATAKANA, "ケーキ", "keeki",
            hepburnAlt = listOf("kēki"), tolerated = listOf("keki"),
        ),
        WordSeed(
            "supa", Script.KATAKANA, "スーパー", "suupaa",
            hepburnAlt = listOf("sūpā"), tolerated = listOf("supa", "suupa", "supaa"),
        ),
        WordSeed(
            "kado", Script.KATAKANA, "カード", "kaado",
            hepburnAlt = listOf("kādo"), tolerated = listOf("kado"),
        ),
        WordSeed(
            "biru", Script.KATAKANA, "ビール", "biiru",
            hepburnAlt = listOf("bīru"), tolerated = listOf("biru"),
        ),
        WordSeed(
            "teburu", Script.KATAKANA, "テーブル", "teeburu",
            hepburnAlt = listOf("tēburu"), tolerated = listOf("teburu"),
        ),
        WordSeed(
            "noto", Script.KATAKANA, "ノート", "nooto",
            hepburnAlt = listOf("nōto"), tolerated = listOf("noto"),
        ),
    )
}
