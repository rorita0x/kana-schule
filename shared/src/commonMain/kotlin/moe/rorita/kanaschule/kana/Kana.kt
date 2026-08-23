package moe.rorita.kanaschule.kana

import kotlin.jvm.JvmInline
import kotlinx.serialization.Serializable

@Serializable
enum class Script { HIRAGANA, KATAKANA }

@Serializable
enum class KanaClass { GOJUON, DAKUTEN, HANDAKUTEN, YOON, SPECIAL }

/** Zeile der Gojuon-Tafel bzw. Gruppe fuer Dakuten und Yoon. */
@Serializable
enum class Row {
    A, KA, SA, TA, NA, HA, MA, YA, RA, WA, N,
    GA, ZA, DA, BA, PA,
    KYA, GYA, SHA, JA, CHA, NYA, HYA, BYA, PYA, MYA, RYA,
    SPECIAL,
}

/**
 * Umschriftsystem einer akzeptierten Antwort. HEPBURN ist das, was die App
 * lehrt; die anderen werden akzeptiert und kommentiert.
 */
@Serializable
enum class RomajiSystem { HEPBURN, KUNREI, WAPURO, TOLERATED }

@Serializable
enum class ItemKind { SINGLE, DIGRAPH, WORD }

/** Stabil und lesbar, wird nie neu durchnummeriert: "h.shi", "k.kya", "h.word.kitte". */
@Serializable
@JvmInline
value class KanaId(val v: String)

@Serializable
data class Answer(val text: String, val system: RomajiSystem)

data class Kana(
    val id: KanaId,
    val script: Script,
    val glyph: String,
    val kind: ItemKind,
    val kClass: KanaClass,
    val row: Row,
    /** Modifiziertes Hepburn: was die App als richtige Schreibweise anzeigt. */
    val canonical: String,
    val answers: List<Answer>,
    /** Basiszeichen: が -> か, きゃ -> き. Bei Gojuon und Woertern null. */
    val baseId: KanaId? = null,
    /** Dasselbe Zeichen in der anderen Schrift: し <-> シ. */
    val partnerId: KanaId? = null,
    /** Visuell verwechselbare Zeichen derselben Schrift. */
    val visualNeighbors: List<KanaId> = emptyList(),
    val noteDe: String? = null,
) {
    val accepted: Set<String> = answers.mapTo(HashSet()) { it.text }

    fun systemOf(answer: String): RomajiSystem? = answers.firstOrNull { it.text == answer }?.system
}
