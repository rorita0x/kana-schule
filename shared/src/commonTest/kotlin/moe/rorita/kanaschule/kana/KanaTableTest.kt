package moe.rorita.kanaschule.kana

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KanaTableTest {

    private val singles = KanaTable.singles
    private val words = KanaTable.words

    @Test
    fun anzahlenProSchrift() {
        for (script in Script.entries) {
            val ofScript = singles.filter { it.script == script }
            assertEquals(46, ofScript.count { it.kClass == KanaClass.GOJUON }, "Gojuon $script")
            assertEquals(20, ofScript.count { it.kClass == KanaClass.DAKUTEN }, "Dakuten $script")
            assertEquals(5, ofScript.count { it.kClass == KanaClass.HANDAKUTEN }, "Handakuten $script")
            assertEquals(33, ofScript.count { it.kClass == KanaClass.YOON }, "Yoon $script")
            assertEquals(104, ofScript.size, "Summe $script")
        }
        assertEquals(208, singles.size)
        assertEquals(22, words.size)
        assertEquals(230, KanaTable.all.size)
    }

    @Test
    fun idsSindEindeutig() {
        val ids = KanaTable.all.map { it.id.v }
        assertEquals(ids.size, ids.toSet().size, "doppelte IDs: ${duplicates(ids)}")
    }

    @Test
    fun glyphenLiegenImRichtigenUnicodeBlock() {
        for (kana in singles) {
            val range = when (kana.script) {
                Script.HIRAGANA -> 'ぁ'..'ゖ'
                Script.KATAKANA -> 'ァ'..'ヺ'
            }
            for (char in kana.glyph) {
                assertTrue(char in range, "${kana.id.v}: ${kana.glyph} enthaelt $char")
            }
        }
    }

    @Test
    fun digraphenHabenZweiZeichen() {
        for (kana in singles) {
            val expected = if (kana.kClass == KanaClass.YOON) 2 else 1
            assertEquals(expected, kana.glyph.length, "${kana.id.v}: ${kana.glyph}")
            val kind = if (expected == 2) ItemKind.DIGRAPH else ItemKind.SINGLE
            assertEquals(kind, kana.kind, kana.id.v)
        }
    }

    @Test
    fun schriftpartnerSindSymmetrisch() {
        for (kana in singles) {
            val partnerId = assertNotNull(kana.partnerId, "${kana.id.v} ohne Partner")
            val partner = KanaTable.require(partnerId)
            assertEquals(kana.id, partner.partnerId, "${kana.id.v} <-> ${partner.id.v}")
            assertTrue(kana.script != partner.script, "${kana.id.v}: Partner gleiche Schrift")
            assertEquals(kana.canonical, partner.canonical, kana.id.v)
            assertEquals(kana.row, partner.row, kana.id.v)
        }
        for (word in words) {
            assertNull(word.partnerId, "${word.id.v} sollte keinen Partner haben")
        }
    }

    @Test
    fun basiszeichenExistierenUndPassen() {
        for (kana in singles) {
            when (kana.kClass) {
                KanaClass.GOJUON -> assertNull(kana.baseId, kana.id.v)
                KanaClass.DAKUTEN, KanaClass.HANDAKUTEN, KanaClass.YOON -> {
                    val baseId = assertNotNull(kana.baseId, "${kana.id.v} ohne Basiszeichen")
                    val base = KanaTable.require(baseId)
                    assertEquals(kana.script, base.script, kana.id.v)
                    assertTrue(
                        base.kClass != KanaClass.YOON,
                        "${kana.id.v}: Basiszeichen ${base.glyph} ist selbst ein Yoon",
                    )
                }
                KanaClass.SPECIAL -> Unit
            }
        }
    }

    @Test
    fun optischeNachbarnSindSymmetrischUndGleichschriftig() {
        for (kana in singles) {
            for (neighborId in kana.visualNeighbors) {
                val neighbor = KanaTable.require(neighborId)
                assertEquals(kana.script, neighbor.script, "${kana.id.v} -> ${neighbor.id.v}")
                assertTrue(
                    kana.id in neighbor.visualNeighbors,
                    "${neighbor.id.v} kennt ${kana.id.v} nicht",
                )
            }
        }
        assertTrue(
            KanaTable.require(KanaId("k.shi")).visualNeighbors
                .contains(KanaId("k.tsu")),
            "シ/ツ muss als Verwechslungspaar erfasst sein",
        )
    }

    @Test
    fun kanonischeAntwortIstErsteUndHepburn() {
        for (kana in KanaTable.all) {
            val first = kana.answers.first()
            assertEquals(kana.canonical, first.text, kana.id.v)
            assertEquals(RomajiSystem.HEPBURN, first.system, kana.id.v)
            assertEquals(
                kana.answers.size,
                kana.answers.map { it.text }.toSet().size,
                "${kana.id.v}: doppelte Antworten",
            )
        }
    }

    @Test
    fun antwortenSindNormalisiert() {
        for (kana in KanaTable.all) {
            for (answer in kana.answers) {
                assertEquals(answer.text.trim().lowercase(), answer.text, kana.id.v)
                assertTrue(answer.text.isNotBlank(), kana.id.v)
                assertTrue(' ' !in answer.text, "${kana.id.v}: '${answer.text}'")
            }
        }
    }

    @Test
    fun rueckwaertsindexFindetJedeAntwort() {
        for (kana in KanaTable.all) {
            for (answer in kana.accepted) {
                assertTrue(
                    kana.id in KanaTable.reverseIndex.getValue(answer),
                    "${kana.id.v}: '$answer' fehlt im Index",
                )
            }
        }
    }

    /**
     * Innerhalb einer Schrift darf eine Antwort nur dort mehrfach vorkommen, wo
     * das Japanische selbst nicht unterscheidet: じ/ぢ, ず/づ und お/を.
     */
    @Test
    fun mehrdeutigeAntwortenSindNurDieDokumentierten() {
        val ambiguous = KanaTable.all
            .flatMap { kana -> kana.accepted.map { (kana.script to it) to kana.id } }
            .groupBy({ it.first }, { it.second })
            .filterValues { it.size > 1 }

        val expected = setOf(
            Script.HIRAGANA to "ji", Script.HIRAGANA to "zu", Script.HIRAGANA to "o",
            Script.KATAKANA to "ji", Script.KATAKANA to "zu", Script.KATAKANA to "o",
        )
        assertEquals(expected, ambiguous.keys, "unerwartete Mehrdeutigkeiten: $ambiguous")
    }

    @Test
    fun woerterDeckenSokuonUndChouonpuAb() {
        val sokuon = words.filter { 'っ' in it.glyph || 'ッ' in it.glyph }
        val chouonpu = words.filter { 'ー' in it.glyph }
        assertEquals(14, sokuon.size)
        assertEquals(8, chouonpu.size)
        assertEquals(words.size, sokuon.size + chouonpu.size)
    }

    @Test
    fun toleranzenSindVorhanden() {
        fun accepts(id: String, vararg answers: String) {
            val kana = KanaTable.require(KanaId(id))
            for (answer in answers) {
                assertTrue(answer in kana.accepted, "${kana.glyph} akzeptiert '$answer' nicht")
            }
        }
        accepts("h.shi", "shi", "si")
        accepts("h.tsu", "tsu", "tu", "tzu")
        accepts("h.chi", "chi", "ti")
        accepts("h.fu", "fu", "hu")
        accepts("h.ji", "ji", "zi")
        accepts("h.wo", "wo", "o")
        accepts("h.n", "n", "nn", "n'", "m")
        accepts("h.sha", "sha", "sya")
        accepts("h.ja", "ja", "zya", "jya")
        accepts("h.cha", "cha", "tya", "cya")
        accepts("k.tsu", "tsu", "tu")
        accepts("k.word.ramen", "raamen", "rāmen", "ramen")
    }

    private fun <T> duplicates(items: List<T>): List<T> =
        items.groupBy { it }.filterValues { it.size > 1 }.keys.toList()
}
