package moe.rorita.kanaschule.kana

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PronunciationTest {

    @Test
    fun jedesZeichenHatEineAussprachehilfe() {
        for (kana in KanaTable.singles) {
            val hint = Pronunciation.of(kana)
            assertTrue(
                hint.consonant != null || hint.vowel != null || hint.warning != null,
                "${kana.id.v} (${kana.glyph}) hat keine Aussprachehilfe",
            )
        }
    }

    @Test
    fun jedesZeichenAusserDenVokalenHatEinenKonsonantenhinweis() {
        for (kana in KanaTable.singles) {
            if (kana.row == Row.A || kana.row == Row.N) continue
            assertNotNull(Pronunciation.of(kana).consonant, "${kana.id.v}: ${kana.glyph}")
        }
    }

    @Test
    fun jedesZeichenMitVokalHatEinenVokalhinweis() {
        for (kana in KanaTable.singles) {
            val hint = Pronunciation.of(kana)
            if (kana.canonical == "n") {
                assertNull(hint.vowel, "ん hat keinen Vokal")
            } else {
                assertNotNull(hint.vowel, "${kana.id.v}: ${kana.canonical}")
            }
        }
    }

    @Test
    fun yoonWarnenVorZweiSilben() {
        for (kana in KanaTable.singles.filter { it.kClass == KanaClass.YOON }) {
            assertNotNull(Pronunciation.of(kana).warning, kana.id.v)
        }
    }

    @Test
    fun stolperfallenSindGesetzt() {
        fun warnung(id: String): String =
            assertNotNull(Pronunciation.of(KanaTable.require(KanaId(id))).warning, id)

        assertTrue(warnung("h.tsu").contains("Zug"))
        assertTrue(warnung("h.fu").contains("Lippen"))
        assertTrue(warnung("h.n").contains("Silbe"))
        assertTrue(warnung("h.ra").contains("Zungenspitze"))
        assertTrue(warnung("h.wo").contains("partikel", ignoreCase = true))
        // Auch die Katakana-Seite muss die Warnung bekommen.
        assertTrue(warnung("k.tsu").contains("Zug"))
    }

    @Test
    fun einzelzeichenHabenEineAufnahmeWoerterNicht() {
        for (kana in KanaTable.singles) {
            assertNotNull(Pronunciation.audioName(kana), kana.id.v)
        }
        for (word in KanaTable.words) {
            assertNull(Pronunciation.audioName(word), word.id.v)
        }
    }

    @Test
    fun beideSchriftenTeilenDieselbeAufnahme() {
        for (kana in KanaTable.singles.filter { it.script == Script.HIRAGANA }) {
            val partner = KanaTable.require(assertNotNull(kana.partnerId))
            assertEquals(
                Pronunciation.audioName(kana),
                Pronunciation.audioName(partner),
                "${kana.glyph} und ${partner.glyph} klingen gleich",
            )
        }
    }

    @Test
    fun genauEinhundertvierAufnahmenFuerZweihundertachtZeichen() {
        val names = KanaTable.singles.mapNotNull(Pronunciation::audioName).toSet()
        assertEquals(104, names.size)
    }
}
