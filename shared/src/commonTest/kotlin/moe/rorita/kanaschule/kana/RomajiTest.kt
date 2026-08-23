package moe.rorita.kanaschule.kana

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class RomajiTest {

    private fun kana(id: String) = KanaTable.require(KanaId(id))

    // ------------------------------------------------------------ normalize

    @Test
    fun normalisiertGrossschreibungUndLeerzeichen() {
        assertEquals("shi", Romaji.normalize("SHI"))
        assertEquals("shi", Romaji.normalize("  Shi "))
        assertEquals("kitte", Romaji.normalize("ki tte"))
        assertEquals("shi", Romaji.normalize("shi!"))
        assertEquals("n'", Romaji.normalize("n'"))
        assertEquals("ra-men", Romaji.normalize("ra-men"))
    }

    @Test
    fun normalisiertVollbreiteEingabe() {
        assertEquals("shi", Romaji.normalize("ｓｈｉ"))
        assertEquals("kitte", Romaji.normalize("ＫＩＴＴＥ"))
    }

    @Test
    fun makronBleibtErhalten() {
        assertEquals("rāmen", Romaji.normalize("Rāmen"))
        assertEquals("rāmen", Romaji.normalize("Rāmen"))
    }

    // -------------------------------------------------------------- correct

    @Test
    fun hepburnIstOhneHinweisRichtig() {
        val verdict = assertIs<Verdict.Correct>(Romaji.evaluate(kana("h.shi"), "shi"))
        assertEquals(RomajiSystem.HEPBURN, verdict.system)
        assertNull(verdict.hint)
    }

    @Test
    fun kunreiIstRichtigMitHinweis() {
        val verdict = assertIs<Verdict.Correct>(Romaji.evaluate(kana("h.shi"), "si"))
        assertEquals(RomajiSystem.KUNREI, verdict.system)
        val hint = assertNotNull(verdict.hint)
        assertEquals(true, hint.contains("shi"))
        assertEquals(true, hint.contains("Kunrei"))
    }

    @Test
    fun wapuroFormIstRichtigMitHinweis() {
        val verdict = assertIs<Verdict.Correct>(Romaji.evaluate(kana("h.n"), "nn"))
        assertEquals(RomajiSystem.WAPURO, verdict.system)
        assertNotNull(verdict.hint)
    }

    @Test
    fun woHatKeinenHinweisFuerO() {
        val verdict = assertIs<Verdict.Correct>(Romaji.evaluate(kana("h.wo"), "o"))
        assertEquals(RomajiSystem.HEPBURN, verdict.system)
        assertNull(verdict.hint)
    }

    @Test
    fun makronFormDerChouonpuWoerterIstGleichwertig() {
        val verdict = assertIs<Verdict.Correct>(Romaji.evaluate(kana("k.word.ramen"), "rāmen"))
        assertEquals(RomajiSystem.HEPBURN, verdict.system)
        assertNull(verdict.hint)
    }

    // ------------------------------------------------------------ confusion

    @Test
    fun verwechslungWirdVorTippfehlerErkannt() {
        // hi hat zu shi den Abstand 1, ist aber eine echte Verwechslung mit ひ.
        val verdict = assertIs<Verdict.Confused>(Romaji.evaluate(kana("h.shi"), "hi"))
        assertEquals(KanaId("h.hi"), verdict.with)
    }

    @Test
    fun optischeVerwechslungSchlaegtStrukturelle() {
        val verdict = assertIs<Verdict.Confused>(Romaji.evaluate(kana("k.shi"), "tsu"))
        assertEquals(KanaId("k.tsu"), verdict.with)
        assertEquals(ConfusionKind.VISUAL, verdict.kind)
    }

    @Test
    fun stimmhaftigkeitWirdAlsDakutenErkannt() {
        val verdict = assertIs<Verdict.Confused>(Romaji.evaluate(kana("h.ga"), "ka"))
        assertEquals(KanaId("h.ka"), verdict.with)
        assertEquals(ConfusionKind.DAKUTEN, verdict.kind)
    }

    @Test
    fun uebersehenesKleinesYaWirdErkannt() {
        val verdict = assertIs<Verdict.Confused>(Romaji.evaluate(kana("h.kya"), "ki"))
        assertEquals(KanaId("h.ki"), verdict.with)
        assertEquals(ConfusionKind.YOON_BASE, verdict.kind)
    }

    @Test
    fun falscherVokalInDerselbenZeile() {
        val verdict = assertIs<Verdict.Confused>(Romaji.evaluate(kana("h.ka"), "ko"))
        assertEquals(KanaId("h.ko"), verdict.with)
        assertEquals(ConfusionKind.VOWEL_ROW, verdict.kind)
    }

    @Test
    fun falscherKonsonantBeiGleichemVokal() {
        val verdict = assertIs<Verdict.Confused>(Romaji.evaluate(kana("h.ka"), "sa"))
        assertEquals(KanaId("h.sa"), verdict.with)
        assertEquals(ConfusionKind.CONSONANT_COL, verdict.kind)
    }

    @Test
    fun gleicheSchriftHatVorrang() {
        val verdict = assertIs<Verdict.Confused>(Romaji.evaluate(kana("h.ka"), "ki"))
        assertEquals(Script.HIRAGANA, KanaTable.require(verdict.with).script)
    }

    // ---------------------------------------------------------------- rest

    @Test
    fun tippfehlerIstNeutral() {
        val verdict = assertIs<Verdict.Typo>(Romaji.evaluate(kana("h.word.kitte"), "kittte"))
        assertEquals("kitte", verdict.nearest)
    }

    @Test
    fun kurzeAntwortenKennenKeineTippfehler() {
        // Bei zweibuchstabigen Antworten wäre jeder Nachbar ein "Tippfehler".
        assertIs<Verdict.Wrong>(Romaji.evaluate(kana("h.ka"), "qz"))
    }

    @Test
    fun leereEingabeIstUebersprungen() {
        assertIs<Verdict.Skipped>(Romaji.evaluate(kana("h.ka"), ""))
        assertIs<Verdict.Skipped>(Romaji.evaluate(kana("h.ka"), "   "))
        assertIs<Verdict.Skipped>(Romaji.evaluate(kana("h.ka"), "?!"))
    }

    @Test
    fun levenshteinRechnetRichtig() {
        assertEquals(0, Romaji.levenshtein("shi", "shi"))
        assertEquals(1, Romaji.levenshtein("shi", "hi"))
        assertEquals(1, Romaji.levenshtein("kitte", "kittte"))
        assertEquals(1, Romaji.levenshtein("tsu", "su"))
        assertEquals(2, Romaji.levenshtein("cha", "tya"))
        assertEquals(3, Romaji.levenshtein("tsu", "shi"))
        assertEquals(3, Romaji.levenshtein("", "abc"))
        assertEquals(3, Romaji.levenshtein("abc", ""))
    }

    @Test
    fun jedeAkzeptierteAntwortIstBereitsNormalisiert() {
        for (kana in KanaTable.all) {
            for (answer in kana.accepted) {
                assertEquals(answer, Romaji.normalize(answer), kana.id.v)
            }
        }
    }
}
